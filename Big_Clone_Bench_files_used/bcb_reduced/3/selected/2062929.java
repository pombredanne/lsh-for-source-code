package org.jiopi.ibean.kernel.repository;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.log4j.Logger;
import org.jiopi.ibean.bootstrap.util.MD5Hash;
import org.jiopi.ibean.kernel.KernelConstants;
import org.jiopi.ibean.kernel.NameVersion;
import org.jiopi.ibean.kernel.config.BlueprintAnnotations;
import org.jiopi.ibean.kernel.config.ConfigConstants;
import org.jiopi.ibean.kernel.config.ModuleAnnotations;
import org.jiopi.ibean.kernel.repository.config.ModuleConfig;
import org.jiopi.ibean.kernel.util.AnnotationParser;
import org.jiopi.ibean.kernel.util.FileUtil;
import org.jiopi.ibean.kernel.util.ResourceUtil;
import org.jiopi.ibean.share.ShareConstants;
import org.jiopi.ibean.share.ShareUtil.IOUtil;
import org.jiopi.ibean.share.ShareUtil.MyFileLock;

/**
 * 
 * 资源库管理类
 * 
 * 统一处理资源仓库的下载,更新等
 * 
 * 根据资源库地址URL区分资源库
 * 
 * 根据调用时刻的上下文配置信息 按优先级进行资源库内容查找
 * 
 * 定时自动下载更新正在使用的资源库(可设置最长不使用时间)
 * 
 * 自动监听本地资源库类文件变更情况,并通知监听程序
 * 
 * 资源库本地镜像(可选择是否复制本地文件),本地镜像自动增加下载时刻的版本信息,以便恢复上一个版本
 * 
 * @since 2010.4.17
 */
public class Repository {

    private static Logger logger = Logger.getLogger(Repository.class);

    public static final int MODULE = 1;

    public static final int BLUEPRINT = 2;

    /**
	 * 根据 moduleName 和 兼容性 版本 获取 程序目录
	 * @param moduleName
	 * @param type 是模块还是蓝图
	 * @return
	 */
    public static ModuleResource getModuleResource(NameVersion moduleName, int type) {
        try {
            URI moduleURI = new URI(moduleName.name);
            String s = moduleURI.getScheme();
            if (s == null || s.equals("jiopi")) {
                return ResourcePool.getModuleResource(moduleName, type);
            } else {
                String fileName = FileUtil.getFileName(moduleName.name, false);
                String moduleNameHash = fileName + Long.toHexString(MD5Hash.digest(moduleName.name).halfDigest());
                if (logger.isDebugEnabled()) logger.debug("get single module " + moduleNameHash);
                String moduleProgramPath = FileUtil.joinPath(ResourceUtil.getProgramDir(), KernelConstants.MODULES_DIR, moduleNameHash);
                ModuleConfig moduleConfig = getModuleConfig(moduleNameHash, moduleURI.toURL(), moduleProgramPath, false, null);
                ModuleConfig.Release release = moduleConfig.getRelease(moduleName);
                if (release == null) return null;
                logger.info(moduleName.name + " get Version :" + moduleName.version + " use Version :" + release.version);
                URL[] releaseResources = release.getResources();
                if (releaseResources != null) {
                    return new ModuleResource(new NameVersion(moduleNameHash, release.version), releaseResources, type);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    /**
	 * get CommonJar By fileName
	 * @param fileName
	 * @return null if not a CommonJar
	 */
    public static CommonJar getCommonJarByFileName(String fileName) {
        return ResourcePool.getCommonJarByFileName(fileName);
    }

    /**
	 * 
	 * @param blueprintJarFile
	 * @return
	 */
    public static BlueprintAnnotations getBlueprintAnnotations(URL blueprintJarFile) {
        InputStream is = null;
        try {
            is = getJIOPiJarXMLInputStream(blueprintJarFile);
            if (is != null) {
                return BlueprintAnnotations.createBlueprintAnnotations(is);
            }
        } catch (Exception e) {
            logger.warn("getBlueprintAnnotations from " + blueprintJarFile + " error.", e);
        } finally {
            IOUtil.close(is);
        }
        return null;
    }

    public static ModuleAnnotations getModuleAnnotations(URL moduleJarFile) {
        InputStream is = null;
        try {
            is = getJIOPiJarXMLInputStream(moduleJarFile);
            if (is != null) {
                return ModuleAnnotations.createModuleAnnotations(is);
            }
        } catch (Exception e) {
            logger.warn("getModuleAnnotations from " + moduleJarFile + " error.", e);
        } finally {
            IOUtil.close(is);
        }
        return null;
    }

    /**
	 * 
	 * @param jarURL
	 * @return
	 */
    private static InputStream getJIOPiJarXMLInputStream(URL jarURL) {
        File jarFile = new File(jarURL.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int jarType = AnnotationParser.parseJarFile(jarFile, out, false, true);
        if (jarType != ConfigConstants.UNKNOWN_RES) {
            if (out.size() < 1) {
                if (jarFile.isFile()) {
                    String xmlFileName = AnnotationParser.configFileNames[jarType];
                    String tempDirName = MD5Hash.digest(jarFile.getName()).toString();
                    String xmlFilePath = FileUtil.joinPath(jarFile.getParent(), tempDirName, xmlFileName);
                    File newXmlFile = new File(xmlFilePath);
                    boolean rebuild = true;
                    if (newXmlFile.exists()) {
                        rebuild = false;
                        if (newXmlFile.isFile()) {
                            if (jarFile.lastModified() != newXmlFile.lastModified()) {
                                rebuild = true;
                            }
                        }
                    }
                    if (rebuild) {
                        String tempFilePath = xmlFilePath + ".tmp";
                        File tmpFile = null;
                        MyFileLock fl = null;
                        BufferedOutputStream fos = null;
                        try {
                            tmpFile = FileUtil.createNewFile(tempFilePath, false);
                            if (tmpFile.isFile()) {
                                fl = FileUtil.tryLockTempFile(tmpFile, 100, ShareConstants.connectTimeout);
                                if (fl != null) {
                                    if (!newXmlFile.isFile() || jarFile.lastModified() != newXmlFile.lastModified()) {
                                        AnnotationParser.parseJarFile(jarFile, out, true, false);
                                        fos = new BufferedOutputStream(new FileOutputStream(newXmlFile));
                                        fos.write(out.toByteArray());
                                        IOUtil.close(fos);
                                        if (newXmlFile.isFile()) {
                                            newXmlFile.setLastModified(jarFile.lastModified());
                                            logger.info("build " + newXmlFile);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("", e);
                        } finally {
                            IOUtil.close(fos);
                            if (tmpFile != null) tmpFile.delete();
                            if (fl != null) try {
                                fl.release();
                            } catch (Exception e) {
                                logger.warn("", e);
                            }
                        }
                    } else {
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(newXmlFile);
                            IOUtil.copyStreams(fis, out);
                        } catch (Exception e) {
                            logger.error("read file error " + newXmlFile, e);
                        } finally {
                            IOUtil.close(fis);
                        }
                    }
                } else {
                    AnnotationParser.parseJarFile(jarFile, out, true, false);
                }
            }
            byte[] xmlFileContent = out.toByteArray();
            IOUtil.close(out);
            return new ByteArrayInputStream(xmlFileContent);
        }
        return null;
    }

    private static HashMap<String, ModuleConfig> moduleResources = new HashMap<String, ModuleConfig>();

    /**
	 * 通过模块的配置文件和模块的安装路径获取模块的配置信息
	 * @param moduleConfigURL  模块的URL,如果 url为null,则只检查缓存
	 * @param moduleInstallPath 模块的安装路径
	 * @return
	 */
    protected static ModuleConfig getModuleConfig(String moduleName, URL moduleConfigURL, String moduleInstallPath, boolean isLocal, UsernamePasswordCredentials creds) {
        ModuleConfig moduleConfig = null;
        synchronized (moduleResources) {
            moduleConfig = moduleResources.get(moduleName);
        }
        if (moduleConfig == null && moduleConfigURL != null) {
            synchronized (moduleName.intern()) {
                synchronized (moduleResources) {
                    moduleConfig = moduleResources.get(moduleName);
                }
                if (moduleConfig == null) {
                    moduleConfig = ModuleConfig.getModuleConfig(moduleInstallPath, moduleConfigURL, isLocal, creds);
                    synchronized (moduleResources) {
                        moduleResources.put(moduleName, moduleConfig);
                    }
                }
            }
        }
        return moduleConfig;
    }

    public static class CommonJar {

        public final String poolName;

        public final String groupName;

        public final URL jarURL;

        public CommonJar(String poolName, String groupName, URL jarURL) {
            this.poolName = poolName;
            this.groupName = groupName;
            this.jarURL = jarURL;
        }
    }
}
