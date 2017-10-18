package org.jiopi.ibean.kernel.context.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.jiopi.ibean.bootstrap.util.FileContentReplacer;
import org.jiopi.ibean.bootstrap.util.MD5Hash;
import org.jiopi.ibean.share.ShareUtil.FileUtil;
import sun.misc.CompoundEnumeration;

public class CommonLibClassLoader extends ClassLoader {

    private final String groupName;

    private final String poolName;

    private final String commonDir;

    private final String commonTempDir;

    private final ClassLoader contextClassLoader;

    private static Logger logger = Logger.getLogger(CommonLibClassLoader.class);

    public CommonLibClassLoader(ClassLoader contextClassLoader, String poolName, String groupName, String commonDir, String commonTempDir) {
        this.contextClassLoader = contextClassLoader;
        this.groupName = groupName;
        this.poolName = poolName;
        this.commonDir = commonDir;
        this.commonTempDir = commonTempDir;
    }

    private HashMap<String, LocalJarClassLoader> jarClassLoaders = new HashMap<String, LocalJarClassLoader>();

    private static final ThreadLocal<List<String>> commonLibList = new ThreadLocal<List<String>>();

    public synchronized void addJar(String fileName, URL jarURL) {
        if (!jarClassLoaders.containsKey(fileName)) {
            HashMap<String, LocalJarClassLoader> newJarClassLoaders = new HashMap<String, LocalJarClassLoader>();
            newJarClassLoaders.putAll(jarClassLoaders);
            newJarClassLoaders.put(fileName, new LocalJarClassLoader(jarURL, this));
            resetJarClassLoaders(newJarClassLoaders);
        }
    }

    private void resetJarClassLoaders(HashMap<String, LocalJarClassLoader> jarClassLoaders) {
        this.jarClassLoaders = jarClassLoaders;
    }

    public Class<?> loadClass(String name, List<String> jarFileNames) throws ClassNotFoundException {
        commonLibList.set(jarFileNames);
        return loadClass(name);
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        List<String> jarFileNames = commonLibList.get();
        if (jarFileNames == null) throw new ClassNotFoundException("Can't call directly");
        HashMap<String, LocalJarClassLoader> localJarClassLoaders = jarClassLoaders;
        Class<?> c = null;
        for (String jarFileName : jarFileNames) {
            LocalJarClassLoader ljcl = localJarClassLoaders.get(jarFileName);
            try {
                c = ljcl.loadClassLocal(name);
                if (contextClassLoader != null) {
                    try {
                        Class<?> cc = contextClassLoader.loadClass(name);
                        String contextPath = cc.getName().replaceAll("\\.", "/") + ".class";
                        URL clURL = contextClassLoader.getResource(contextPath);
                        String endWith = jarFileName + "!/" + contextPath;
                        if (clURL != null && clURL.toString().endsWith(endWith)) {
                            return cc;
                        }
                    } catch (ClassNotFoundException e) {
                    }
                }
                break;
            } catch (ClassNotFoundException e) {
            }
        }
        if (c == null && contextClassLoader != null) {
            c = contextClassLoader.loadClass(name);
        }
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

    public URL getResource(String name, List<String> jarFileNames) {
        URL url = null;
        HashMap<String, LocalJarClassLoader> localJarClassLoaders = jarClassLoaders;
        for (String jarFileName : jarFileNames) {
            LocalJarClassLoader ljcl = localJarClassLoaders.get(jarFileName);
            url = ljcl.getResourceLocal(name);
            if (url != null) return url;
        }
        return url;
    }

    /**
	 * can't load Resource from a group of common lib classloader
	 */
    public URL getResource(String name) {
        List<String> jarFileNames = commonLibList.get();
        if (jarFileNames == null) return null;
        return getResource(name, jarFileNames);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Enumeration<URL> getResources(String name, List<String> jarFileNames) throws IOException {
        ArrayList<Enumeration<URL>> tmp = new ArrayList<Enumeration<URL>>();
        HashMap<String, LocalJarClassLoader> localJarClassLoaders = jarClassLoaders;
        for (String jarFileName : jarFileNames) {
            LocalJarClassLoader ljcl = localJarClassLoaders.get(jarFileName);
            tmp.add(ljcl.getResourcesLocal(name));
        }
        return new CompoundEnumeration(tmp.toArray(new Enumeration[tmp.size()]));
    }

    /**
	 * can't load Resources from a group of common lib classloader
	 */
    public Enumeration<URL> getResources(String name) throws IOException {
        List<String> jarFileNames = commonLibList.get();
        if (jarFileNames == null) return null;
        return getResources(name, jarFileNames);
    }

    /**
	 * inner class
	 *
	 */
    private static class LocalJarClassLoader extends URLClassLoader {

        public final CommonLibClassLoader parentClassLoader;

        public LocalJarClassLoader(URL jarUrl, CommonLibClassLoader parent) {
            super(new URL[] { jarUrl }, null);
            if (parent == null) throw new NullPointerException();
            parentClassLoader = parent;
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            List<String> jarFileNames = commonLibList.get();
            if (jarFileNames == null) {
                return loadClassLocal(name);
            } else {
                try {
                    Class<?> c = loadClassLocal(name);
                    return c;
                } catch (ClassNotFoundException e) {
                }
            }
            return parentClassLoader.loadClass(name);
        }

        public Class<?> loadClassLocal(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }

        public URL getResourceLocal(String name) {
            URL url = super.getResource(name);
            if (url != null) {
                if (name.endsWith(".xml") || name.endsWith(".properties")) {
                    String contextResourceName = new StringBuilder(name).append(".jiopi.").append(parentClassLoader.poolName).append(".").append(parentClassLoader.groupName).toString();
                    if (parentClassLoader.contextClassLoader != null) {
                        URL contextURL = parentClassLoader.contextClassLoader.getResource(contextResourceName);
                        if (contextURL != null) {
                            String nameMD5 = MD5Hash.digest(contextResourceName).toString().toLowerCase();
                            String jiopiResourceFilePath = FileUtil.joinPath(parentClassLoader.commonTempDir, parentClassLoader.poolName, parentClassLoader.groupName, nameMD5, name);
                            File jiopiResourceFile = new File(jiopiResourceFilePath);
                            synchronized (jiopiResourceFilePath.intern()) {
                                if (!jiopiResourceFile.isFile()) {
                                    try {
                                        jiopiResourceFile = FileUtil.createNewFile(jiopiResourceFilePath, true);
                                        FileContentReplacer.replaceAll(contextURL, jiopiResourceFile, new String[] {}, new String[] {});
                                    } catch (IOException e) {
                                        logger.warn("", e);
                                    }
                                }
                            }
                            if (jiopiResourceFile.isFile()) return FileUtil.toURL(jiopiResourceFilePath);
                        }
                    }
                }
                String fileName = new File(url.getFile()).getName();
                if (fileName.endsWith(".xml") || name.endsWith(".properties")) {
                    String jiopiName = name + ".jiopi";
                    URL jiopiURL = super.getResource(jiopiName);
                    if (jiopiURL != null) {
                        String nameMD5 = MD5Hash.digest(name).toString().toLowerCase();
                        String jiopiResourceFilePath = FileUtil.joinPath(parentClassLoader.commonTempDir, parentClassLoader.poolName, parentClassLoader.groupName, nameMD5, fileName);
                        File jiopiResourceFile = new File(jiopiResourceFilePath);
                        synchronized (jiopiResourceFilePath.intern()) {
                            if (!jiopiResourceFile.isFile()) {
                                try {
                                    jiopiResourceFile = FileUtil.createNewFile(jiopiResourceFilePath, true);
                                    String commonDir = FileUtil.joinPath(this.parentClassLoader.commonDir, parentClassLoader.poolName, parentClassLoader.groupName);
                                    String commonTempDir = FileUtil.joinPath(this.parentClassLoader.commonTempDir, parentClassLoader.poolName, parentClassLoader.groupName);
                                    FileUtil.confirmDir(commonDir, true);
                                    FileUtil.confirmDir(commonTempDir, true);
                                    FileContentReplacer.replaceAll(jiopiURL, jiopiResourceFile, new String[] { "\\$\\{common-dir\\}", "\\$\\{common-temp-dir\\}" }, new String[] { commonDir, commonTempDir });
                                } catch (IOException e) {
                                    logger.warn("", e);
                                }
                            }
                        }
                        if (jiopiResourceFile.isFile()) return FileUtil.toURL(jiopiResourceFilePath);
                    }
                }
            }
            return url;
        }

        /**
		 * 1. load from context
		 * 1.1 use special name : name.jiopi.pool.group
		 * 2. load from local
		 */
        public URL getResource(String name) {
            return getResourceLocal(name);
        }

        public Enumeration<URL> getResourcesLocal(String name) throws IOException {
            return super.getResources(name);
        }

        /**
		 * 1. load from context
		 * 2. load from local
		 */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Enumeration<URL> getResources(String name) throws IOException {
            ArrayList<Enumeration<URL>> tmp = new ArrayList<Enumeration<URL>>();
            if (parentClassLoader.contextClassLoader != null) tmp.add(parentClassLoader.contextClassLoader.getResources(name));
            tmp.add(super.getResources(name));
            return new CompoundEnumeration(tmp.toArray(new Enumeration[tmp.size()]));
        }
    }
}
