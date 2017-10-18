package org.jiopi.ibean.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.logging.Level;
import org.jiopi.ibean.bootstrap.util.FileContentReplacer;
import org.jiopi.ibean.bootstrap.util.MD5Hash;
import org.jiopi.ibean.loader.log.LoaderLogUtil;
import org.jiopi.ibean.share.ShareUtil.FileUtil;
import sun.misc.CompoundEnumeration;

/**
 * 
 * Kernel Class Loader
 * 
 * 负责装载Kernel-Share相关程序
 * 
 * 加载逻辑为优先加载Kernel-Share程序,如果找不到,再从parent加载
 * parent应为客户端jar的加载ClassLoader
 * 
 * getResource 增加对xml配置文件的 ${jiopi-work-dir} 字段替换 为对应的工作目录
 * 如果存在 相同目录下的 .jiopi 结尾的文件
 * 
 * 
 * 
 * @since 2010.4.18
 *
 */
public class KernelClassLoader extends URLClassLoader {

    private final String workDir;

    private final String tempDir;

    private final ClassLoader parentClassLoader;

    public KernelClassLoader(String workDir, URL[] urls, ClassLoader parent) {
        super(urls, null);
        this.workDir = FileUtil.correctDirPath(workDir);
        String tempDirPath = FileUtil.joinPath(this.workDir, BootstrapConstants.TMP_DIR, "/kernelclassloader");
        File tempDirFile = FileUtil.confirmDir(tempDirPath, true);
        if (!tempDirFile.isDirectory()) {
            throw new RuntimeException("can't create tmp dir " + tempDirPath);
        }
        this.tempDir = tempDirPath;
        parentClassLoader = parent;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> c = null;
        if (name.startsWith(BootstrapConstants.JIOPI_FRAMEWORK_PACKAGE)) {
            return parentClassLoader.loadClass(name);
        }
        try {
            c = super.loadClass(name);
        } catch (ClassNotFoundException e) {
            if (parentClassLoader != null) c = parentClassLoader.loadClass(name); else throw e;
        }
        return c;
    }

    /**
	 * 
	 */
    public URL getResource(String name) {
        boolean fromParent = false;
        URL url = super.getResource(name);
        if (url == null && parentClassLoader != null) {
            url = parentClassLoader.getResource(name);
            fromParent = true;
        }
        if (url != null) {
            String jiopiName = name + ".jiopi";
            URL jiopiURL = null;
            if (!fromParent) {
                jiopiURL = super.getResource(jiopiName);
            } else {
                jiopiURL = parentClassLoader.getResource(jiopiName);
            }
            if (jiopiURL != null) {
                String fileName = new File(url.getFile()).getName();
                if (fileName.endsWith(".xml")) {
                    String nameMD5 = MD5Hash.digest(name).toString().toLowerCase();
                    String jiopiResourceFilePath = FileUtil.joinPath(tempDir, nameMD5, fileName);
                    File jiopiResourceFile = new File(jiopiResourceFilePath);
                    synchronized (jiopiResourceFilePath.intern()) {
                        if (!jiopiResourceFile.isFile()) {
                            try {
                                jiopiResourceFile = FileUtil.createNewFile(jiopiResourceFilePath, true);
                                FileContentReplacer.replaceAll(jiopiURL, jiopiResourceFile, new String[] { "\\$\\{jiopi-work-dir\\}" }, new String[] { workDir });
                            } catch (IOException e) {
                                LoaderLogUtil.logExceptionTrace(BootstrapConstants.bootstrapLogger, Level.WARNING, e);
                                return null;
                            }
                        }
                    }
                    if (jiopiResourceFile.isFile()) {
                        return FileUtil.toURL(jiopiResourceFilePath);
                    } else {
                        return null;
                    }
                }
            }
        }
        return url;
    }

    @SuppressWarnings("unchecked")
    public Enumeration<URL> getResources(String name) throws IOException {
        if (parentClassLoader == null) return super.getResources(name); else {
            Enumeration[] tmp = new Enumeration[2];
            tmp[0] = parentClassLoader.getResources(name);
            tmp[1] = super.getResources(name);
            return new CompoundEnumeration(tmp);
        }
    }
}
