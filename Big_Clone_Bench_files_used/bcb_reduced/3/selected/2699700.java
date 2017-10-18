package org.jiopi.ibean.kernel.context.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jiopi.ibean.bootstrap.BootstrapConstants;
import org.jiopi.ibean.bootstrap.util.FileContentReplacer;
import org.jiopi.ibean.bootstrap.util.MD5Hash;
import org.jiopi.ibean.kernel.NameVersion;
import org.jiopi.ibean.kernel.Version;
import org.jiopi.ibean.kernel.config.BlueprintAnnotations;
import org.jiopi.ibean.kernel.config.ConfigConstants;
import org.jiopi.ibean.kernel.config.ModuleAnnotations;
import org.jiopi.ibean.kernel.context.ContextCentralConsoleKernel;
import org.jiopi.ibean.kernel.repository.ModuleResource;
import org.jiopi.ibean.kernel.util.ClassUtil;
import org.jiopi.ibean.share.ShareUtil.FileUtil;
import sun.misc.CompoundEnumeration;

/**
 * 
 * IBean Class Loader
 * <P>
 * use JIOPi rules to load class<br/>
 * 1.resolve module's classpath,find the depending blueprints<br/>
 * 2.depending blueprint's jar should be load by the specified blueprint classloader<br/>
 * 3.load class or resources from module's classpath first(resources only load from local jars)<br/>
 * 4.load class or resources from parent<br/>
 * </P>
 * 
 * @since 2010.4.19
 *
 */
public class IBeanClassLoader extends ClassLoader {

    private static Logger logger = Logger.getLogger(IBeanClassLoader.class);

    /**
	 * tempDir of this ClassLoader
	 */
    private final String tempDir;

    private final String moduleDir;

    /**
	 * reference to the context of this class loader
	 */
    public final ContextCentralConsoleKernel ccck;

    public final JIOPiJarClassLoader jiopiClassLoader;

    private final IBeanClassLoader[] dependentBlueprintClassLoaders;

    private final LocalClassLoader localClassLoader;

    private final HashMap<CommonLibClassLoader, ArrayList<String>> commonJarList = new HashMap<CommonLibClassLoader, ArrayList<String>>();

    private static final ThreadLocal<HashMap<String, HashSet<IBeanClassLoader>>> classLoaders = new ThreadLocal<HashMap<String, HashSet<IBeanClassLoader>>>();

    private final HashMap<String, String> registerInterfaces = new HashMap<String, String>();

    private final RedefineableClassLoader redefineableClassLoader;

    private final NameVersion name;

    public IBeanClassLoader(NameVersion name, final ModuleResource resource, ContextCentralConsoleKernel ccck, String moduleDir, String tempDir) {
        super(null);
        this.name = name;
        this.tempDir = tempDir;
        this.moduleDir = moduleDir;
        this.ccck = ccck;
        if (resource.moduleResource == null) {
            logger.warn(resource + " is not a standard JIOPi Module");
            jiopiClassLoader = null;
        } else {
            jiopiClassLoader = new JIOPiJarClassLoader(resource.moduleResource, this);
        }
        if (resource.dependentBlueprintResources.length > 0) {
            dependentBlueprintClassLoaders = new IBeanClassLoader[resource.dependentBlueprintResources.length];
            for (int i = 0; i < resource.dependentBlueprintResources.length; i++) {
                NameVersion dependentBlueprint = resource.dependentBlueprintResources[i];
                try {
                    dependentBlueprintClassLoaders[i] = ccck.blueprintClassLoadManager.getBlueprintClassLoader(dependentBlueprint);
                } catch (Exception e) {
                    logger.error(resource.name + "-" + resource.version + " load dependent blueprint " + dependentBlueprint.name + "-" + dependentBlueprint.version + " error");
                }
            }
        } else {
            dependentBlueprintClassLoaders = null;
        }
        if (resource.localDependentJars.length > 0) {
            ArrayList<URL> localJars = new ArrayList<URL>();
            CommonLibClassLoaderManager clclm = ccck.commonLibClassLoaderManager;
            for (URL jarURL : resource.localDependentJars) {
                String fileName = new File(jarURL.getFile()).getName();
                CommonLibClassLoader clcl = clclm.getCommonLibClassLoader(fileName);
                if (clcl != null) {
                    ArrayList<String> jarList = commonJarList.get(clcl);
                    if (jarList == null) {
                        jarList = new ArrayList<String>();
                        commonJarList.put(clcl, jarList);
                    }
                    jarList.add(fileName);
                } else localJars.add(jarURL);
            }
            if (localJars.size() > 0) localClassLoader = new LocalClassLoader(localJars.toArray(new URL[localJars.size()]), this); else localClassLoader = null;
        } else {
            localClassLoader = null;
        }
        if (resource.moduleResource != null && resource.moduleResource.jarType == ConfigConstants.MODULE_JAR) {
            ModuleAnnotations ma = resource.moduleResource.module;
            if (ma.controlPanels.size() > 0) {
                for (String className : ma.controlPanels.keySet()) {
                    ModuleAnnotations.ControlPanel macp = ma.controlPanels.get(className);
                    try {
                        Class<?> registerModuleClass = this.loadLocalModuleClass(className);
                        List<Class<?>> superClasses = ClassUtil.getAllSuperClasses(registerModuleClass);
                        for (Class<?> superClass : superClasses) {
                            if (macp.registerControlpanels != null && macp.registerControlpanels.contains(superClass.getName())) {
                                this.registerInterfaces.put(superClass.getName(), className);
                            } else if (superClass.getClassLoader() != null) {
                                for (IBeanClassLoader blueprintLoader : dependentBlueprintClassLoaders) {
                                    try {
                                        Class<?> blueprintClass = blueprintLoader.loadLocalModuleClass(superClass.getName());
                                        if (superClass == blueprintClass) {
                                            if (macp.registerControlpanels == null || macp.registerControlpanels.size() < 1 || macp.registerControlpanels.contains(superClass.getName())) {
                                                this.registerInterfaces.put(superClass.getName(), className);
                                            }
                                            break;
                                        }
                                    } catch (ClassNotFoundException e) {
                                    }
                                }
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        logger.warn("Register Module Error,Class Not Found : " + className);
                    }
                }
            }
        }
        if (resource.moduleResource != null && resource.moduleResource.jarType == ConfigConstants.BLUEPRINT_JAR) {
            BlueprintAnnotations ba = resource.moduleResource.blueprint;
            for (String interfaceName : ba.interfaces.keySet()) {
                BlueprintAnnotations.Interface bai = ba.interfaces.get(interfaceName);
                if (bai.registerControlpanel != null) {
                    this.registerInterfaces.put(bai.registerControlpanel, interfaceName);
                }
                for (String methodName : bai.registerMethod.keySet()) {
                    String registerName = bai.registerMethod.get(methodName);
                    this.registerInterfaces.put(registerName, interfaceName + "." + methodName);
                }
                for (String varName : bai.registerVariable.keySet()) {
                    String registerName = bai.registerVariable.get(varName);
                    this.registerInterfaces.put(registerName, interfaceName + "." + varName);
                }
            }
        }
        redefineableClassLoader = RedefineableClassLoader.getRedefineableClassLoader(this);
    }

    public Class<?>[] getRegisterBlueprintClasses(Class<?> moduleClass) {
        String moduleClassName = moduleClass.getName();
        LinkedList<Class<?>> blueprintClasses = new LinkedList<Class<?>>();
        for (Map.Entry<String, String> entry : registerInterfaces.entrySet()) {
            if (moduleClassName.equals(entry.getValue())) {
                try {
                    blueprintClasses.add(loadBlueprintClass(entry.getKey()));
                } catch (ClassNotFoundException e) {
                    logger.warn("can't load blueprint class " + entry.getKey() + " at module " + jiopiClassLoader.moduleJar.jar, e);
                }
            }
        }
        if (blueprintClasses.size() > 0) return blueprintClasses.toArray(new Class<?>[blueprintClasses.size()]);
        return null;
    }

    public ModuleAnnotations.ControlPanel getControlPaneConfig(String className) {
        if (jiopiClassLoader != null && jiopiClassLoader.moduleJar != null && jiopiClassLoader.moduleJar.module != null) return jiopiClassLoader.moduleJar.module.controlPanels.get(className);
        return null;
    }

    public BlueprintAnnotations.Interface getBlueprintConfig(String className) {
        if (jiopiClassLoader != null && jiopiClassLoader.moduleJar != null && jiopiClassLoader.moduleJar.blueprint != null) return jiopiClassLoader.moduleJar.blueprint.interfaces.get(className);
        return null;
    }

    /**
	 * get Instantiation config of a module class
	 * @param className
	 * @return
	 */
    public ModuleAnnotations.ControlPanel.Instantiation getInstantiationConfig(String className) {
        if (jiopiClassLoader != null && jiopiClassLoader.moduleJar.module != null) {
            ModuleAnnotations.ControlPanel macp = jiopiClassLoader.moduleJar.module.controlPanels.get(className);
            if (macp != null) {
                return macp.instantiation;
            }
        }
        return null;
    }

    /**
	 * get blueprint version of given class 
	 * @param interfaceClass
	 * @return
	 */
    public Version getBlueprintVersion(Class<?> interfaceClass) {
        if (dependentBlueprintClassLoaders == null) return null;
        IBeanClassLoader loader = null;
        String interfaceClassName = interfaceClass.getName();
        for (IBeanClassLoader dependentBlueprint : dependentBlueprintClassLoaders) {
            if (dependentBlueprint.isLoader(interfaceClassName)) {
                loader = dependentBlueprint;
                break;
            }
        }
        if (loader != null) {
            return loader.jiopiClassLoader.blueprintName;
        }
        return null;
    }

    /**
	 * get the value of the given register name
	 * @param registerName
	 * @return return null if is not exist
	 */
    public String getRegisterValue(String registerName) {
        String value = null;
        if (dependentBlueprintClassLoaders != null) {
            for (IBeanClassLoader dbc : dependentBlueprintClassLoaders) {
                value = dbc.registerInterfaces.get(registerName);
                if (value != null) break;
            }
        }
        return value;
    }

    private static HashSet<IBeanClassLoader> getLoadedClassLoaders(String classname) {
        HashMap<String, HashSet<IBeanClassLoader>> loadedClassLoaderMap = classLoaders.get();
        if (loadedClassLoaderMap == null) {
            loadedClassLoaderMap = new HashMap<String, HashSet<IBeanClassLoader>>();
            classLoaders.set(loadedClassLoaderMap);
        }
        HashSet<IBeanClassLoader> loadedClassLoaders = loadedClassLoaderMap.get(classname);
        if (loadedClassLoaders == null) {
            loadedClassLoaders = new HashSet<IBeanClassLoader>();
            loadedClassLoaderMap.put(classname, loadedClassLoaders);
        }
        return loadedClassLoaders;
    }

    /**
	 * 
	 * @return
	 */
    public boolean isLoader(String name) {
        if (!name.startsWith(BootstrapConstants.JIOPI_FRAMEWORK_PACKAGE) && jiopiClassLoader != null) {
            try {
                jiopiClassLoader.loadClassLocal(name);
                return true;
            } catch (ClassNotFoundException e) {
            }
        }
        return false;
    }

    /**
	 * load class from local resources
	 * @param name
	 * @return
	 */
    public Class<?> loadLocalModuleClass(String name) throws ClassNotFoundException {
        Class<?> c = null;
        if (!name.startsWith(BootstrapConstants.JIOPI_FRAMEWORK_PACKAGE) && jiopiClassLoader != null) {
            c = jiopiClassLoader.loadClassLocal(name);
            if (c != null && jiopiClassLoader.blueprintName != null && ccck.contextBlueprints.contains(jiopiClassLoader.blueprintName)) {
                c = ccck.contextClassLoader.loadClass(name);
            }
        }
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (redefineableClassLoader != null) {
            Class<?> c = redefineableClassLoader.loadClass(name);
            if (c != null) return c;
        }
        return loadModuleClass(name, false, false);
    }

    public Class<?> loadModuleClass(String name, boolean useRegisterName) throws ClassNotFoundException {
        return loadModuleClass(name, useRegisterName, false);
    }

    /**
	 * get register interface class in dependentBlueprintClassLoaders
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
    public Class<?> loadBlueprintClass(String name) throws ClassNotFoundException {
        return loadModuleClass(name, true, true);
    }

    /**
	 * 1. load class from jiopiClassLoader
	 * 2. load class from localClassLoader
	 * 3. load class from dependentBlueprintClassLoaders
	 * 4. load from common jars
	 * 5. load class from context class loader
	 * 6. if class Name is begin with javassist, read from JIOPi Kernel's class loader(IBeanClassLoader.getClassLoader)
	 */
    private Class<?> loadModuleClass(String name, boolean useRegisterName, boolean getBlueprintClass) throws ClassNotFoundException {
        Class<?> c = null;
        if (name.startsWith(BootstrapConstants.JIOPI_FRAMEWORK_PACKAGE)) {
            return ccck.contextClassLoader.loadClass(name);
        }
        String replaceClassName = null;
        if (useRegisterName) {
            replaceClassName = this.registerInterfaces.get(name);
            if (replaceClassName != null && !getBlueprintClass) name = replaceClassName;
        }
        if (c == null && jiopiClassLoader != null) {
            if (jiopiClassLoader.blueprintName != null && ccck.contextBlueprints.contains(jiopiClassLoader.blueprintName)) {
                try {
                    c = ccck.contextClassLoader.loadClass(name);
                } catch (ClassNotFoundException e) {
                }
            } else {
                try {
                    c = jiopiClassLoader.loadClassLocal(name);
                } catch (ClassNotFoundException e) {
                }
            }
        }
        if (c == null && localClassLoader != null) {
            try {
                c = localClassLoader.loadClassLocal(name);
            } catch (ClassNotFoundException e) {
            }
        }
        HashSet<IBeanClassLoader> loadedClassLoaders = null;
        boolean clear = false;
        if (c == null) {
            loadedClassLoaders = getLoadedClassLoaders(name);
            if (loadedClassLoaders.size() == 0) clear = true;
            loadedClassLoaders.add(this);
        }
        try {
            if (c == null && dependentBlueprintClassLoaders != null) {
                for (IBeanClassLoader dependentBlueprint : dependentBlueprintClassLoaders) {
                    if (!loadedClassLoaders.contains(dependentBlueprint)) {
                        try {
                            c = dependentBlueprint.loadModuleClass(name, true);
                            if (getBlueprintClass) return c;
                            if (useRegisterName) {
                                replaceClassName = this.registerInterfaces.get(c.getName());
                                if (replaceClassName != null) return this.loadClass(replaceClassName);
                            }
                            break;
                        } catch (ClassNotFoundException e) {
                        }
                    }
                }
            }
        } finally {
            if (clear) {
                loadedClassLoaders.clear();
            }
        }
        if (c == null && !getBlueprintClass) {
            for (CommonLibClassLoader clcl : commonJarList.keySet()) {
                try {
                    c = clcl.loadClass(name, commonJarList.get(clcl));
                    break;
                } catch (ClassNotFoundException e) {
                }
            }
        }
        if (c == null && !getBlueprintClass) {
            if (!(this.jiopiClassLoader != null && this.jiopiClassLoader.moduleJar.jarType == ConfigConstants.BLUEPRINT_JAR && useRegisterName)) {
                try {
                    c = ccck.contextClassLoader.loadClass(name);
                } catch (ClassNotFoundException e) {
                }
            }
        }
        if (c == null && name.startsWith("javassist.")) {
            try {
                c = IBeanClassLoader.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

    /**
	 * 1.load resource from local module
	 * 2.load resource from local
	 * 3.load resource from context
	 */
    public URL getResource(String name) {
        URL url = getLocalResource(name);
        if (url == null) {
            url = ccck.contextClassLoader.getResource(name);
        }
        return url;
    }

    public URL getLocalResource(String name) {
        URL url = null;
        ClassLoader loader = null;
        if (url == null && jiopiClassLoader != null) {
            url = jiopiClassLoader.getResourceLocal(name);
            if (url != null) loader = jiopiClassLoader;
        }
        if (url == null && localClassLoader != null) {
            url = localClassLoader.getResourceLocal(name);
            if (url != null) loader = localClassLoader;
        }
        if (loader != null && url != null && this.tempDir != null) {
            String fileName = new File(url.getFile()).getName();
            if (fileName.endsWith(".xml") || name.endsWith(".properties")) {
                if (this.jiopiClassLoader != null && this.jiopiClassLoader.moduleJar != null && this.jiopiClassLoader.moduleJar.jarType == ConfigConstants.MODULE_JAR) {
                    if (ccck.contextClassLoader != null) {
                        String contextResourceName = new StringBuilder(name).append(".jiopi.").append(this.name.name).toString();
                        URL contextURL = ccck.contextClassLoader.getResource(contextResourceName);
                        if (contextURL != null) {
                            String[] version = this.name.version.split("\\.");
                            StringBuilder sb = new StringBuilder(contextResourceName);
                            for (String v : version) {
                                sb.append(".").append(v);
                                URL testLoad = ccck.contextClassLoader.getResource(sb.toString());
                                if (testLoad != null) contextURL = testLoad;
                            }
                        }
                        if (contextURL != null) {
                            String nameMD5 = MD5Hash.digest(contextResourceName).toString().toLowerCase();
                            String jiopiResourceFilePath = FileUtil.joinPath(tempDir, nameMD5, name);
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
                String jiopiName = name + ".jiopi";
                URL jiopiURL = loader.getResource(jiopiName);
                if (jiopiURL != null) {
                    String nameMD5 = MD5Hash.digest(name).toString().toLowerCase();
                    String jiopiResourceFilePath = FileUtil.joinPath(tempDir, nameMD5, fileName);
                    File jiopiResourceFile = new File(jiopiResourceFilePath);
                    synchronized (jiopiResourceFilePath.intern()) {
                        if (!jiopiResourceFile.isFile()) {
                            try {
                                jiopiResourceFile = FileUtil.createNewFile(jiopiResourceFilePath, true);
                                String moduleDir = FileUtil.joinPath(this.moduleDir, "module");
                                String moduleTempDir = FileUtil.joinPath(this.tempDir, "module");
                                FileUtil.confirmDir(moduleDir, true);
                                FileUtil.confirmDir(moduleTempDir, true);
                                FileContentReplacer.replaceAll(jiopiURL, jiopiResourceFile, new String[] { "\\$\\{module-dir\\}", "\\$\\{module-temp-dir\\}" }, new String[] { moduleDir, moduleTempDir });
                            } catch (IOException e) {
                                logger.warn("", e);
                            }
                        }
                    }
                    if (jiopiResourceFile.isFile()) return FileUtil.toURL(jiopiResourceFilePath);
                }
            }
        }
        if (url == null && name.endsWith(".class")) {
            HashSet<IBeanClassLoader> loadedClassLoaders = null;
            boolean clear = false;
            if (url == null) {
                loadedClassLoaders = getLoadedClassLoaders(name);
                if (loadedClassLoaders.size() == 0) clear = true;
                loadedClassLoaders.add(this);
            }
            try {
                if (url == null && dependentBlueprintClassLoaders != null) {
                    for (IBeanClassLoader dependentBlueprint : dependentBlueprintClassLoaders) {
                        if (!loadedClassLoaders.contains(dependentBlueprint)) {
                            url = dependentBlueprint.getLocalResource(name);
                            if (url != null) break;
                        }
                    }
                }
            } finally {
                if (clear) {
                    loadedClassLoaders.clear();
                }
            }
        }
        if (url == null) {
            for (CommonLibClassLoader clcl : commonJarList.keySet()) {
                url = clcl.getResource(name, commonJarList.get(clcl));
                if (url != null) break;
            }
        }
        return url;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Enumeration<URL> getResources(String name) throws IOException {
        ArrayList<Enumeration<URL>> tmp = new ArrayList<Enumeration<URL>>();
        if (jiopiClassLoader != null) {
            tmp.add(jiopiClassLoader.getResourcesLocal(name));
        }
        if (localClassLoader != null) {
            tmp.add(localClassLoader.getResourcesLocal(name));
        }
        for (CommonLibClassLoader clcl : commonJarList.keySet()) {
            tmp.add(clcl.getResources(name, commonJarList.get(clcl)));
        }
        tmp.add(ccck.contextClassLoader.getResources(name));
        return new CompoundEnumeration(tmp.toArray(new Enumeration[tmp.size()]));
    }
}
