package org.xito.boot;

import java.util.*;
import java.util.logging.*;
import java.net.*;
import java.io.*;
import java.security.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Encoder;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

/**
 * Describes information about an Executable. At a basic level the ExecutableDesc has 
 * a list of resource urls (jars). And a set of permissions the Application wants to be granted
 * <p>
 * It also provides a name, displayname, version, description, and mainClass which may or may
 * not be used during execution.
 *
 * @author  Deane Richan
 */
public abstract class ExecutableDesc implements Serializable {

    private static final transient Logger logger = Logger.getLogger(ExecutableDesc.class.getName());

    public static final int MAX_NAME_LENGTH = 15;

    public static final int MAX_DISPLAYNAME_LENGTH = 30;

    private String serialNum;

    protected String name;

    protected String displayName;

    protected String description;

    protected String version;

    protected String mainClass;

    protected Properties properties;

    protected URL contextURL;

    /** ArrayList of ClassPathEntry objects make up a ClassPath */
    protected ArrayList classpath = new ArrayList();

    /** ArrayList of NativeLibDesc objects make up the nativeLibs */
    protected ArrayList nativeLibs = new ArrayList();

    protected ArrayList serviceRefs = new ArrayList();

    protected PermissionCollection permissions;

    protected String permissionDesc;

    protected boolean useCache_flag = true;

    protected boolean checkForUpdates_flag = true;

    protected boolean restrictedPerms_flag = true;

    protected DocumentBuilderFactory builderFactory;

    protected DocumentBuilder builder;

    protected ExecutableDesc() {
    }

    protected void initParser() {
        try {
            builderFactory = DocumentBuilderFactory.newInstance();
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException parserExp) {
            throw new RuntimeException("can read service information file, error:" + parserExp.getMessage(), parserExp);
        } catch (DOMException domExp) {
            throw new RuntimeException("can't read services xml error:" + domExp.getMessage(), domExp);
        }
    }

    /**
    * Get the URL that this descriptor uses to obtain relative resources
    */
    public URL getContextURL() {
        return contextURL;
    }

    /**
    * Set the name of this executable
    * @param name
    */
    protected void setName(String name) {
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH - 1);
        }
        this.name = name;
    }

    /**
    * Set the DisplayName of this Executable
    * @param displayName
    */
    protected void setDisplayName(String displayName) {
        if (displayName != null && displayName.length() > MAX_DISPLAYNAME_LENGTH) {
            displayName = displayName.substring(0, MAX_DISPLAYNAME_LENGTH - 1);
        }
        this.displayName = displayName;
    }

    /**
    * Get the Version of this Executalbe Desc
    */
    protected void setVersion(String version) {
        this.version = version;
    }

    /**
    * Set the Version of this Executalbe Desc
    */
    public String getVersion() {
        return version;
    }

    /**
    * Set the Description of this Executable
    * @param desc
    */
    protected void setDescription(String desc) {
        this.description = desc;
    }

    /**
    * Get the Description of this Executalbe Desc
    */
    public String getDescription() {
        return description;
    }

    /**
    * Should we use the CacheManager to cache resources
    */
    public boolean useCache() {
        return useCache_flag;
    }

    /**
    * Set to true if we should Cache the resources
    */
    public void setUseCache(boolean c) {
        useCache_flag = c;
    }

    /**
    * A Serial Number is unique for a particular set of classpath URLs
    * If an application shares the same classpath URLs then they will share
    * the same SerialNumber
    */
    public synchronized String getSerialNumber() {
        if (serialNum != null) return serialNum;
        final StringBuffer buf = new StringBuffer();
        Iterator it = classpath.iterator();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            buf.append(entry.getResourceURL().toString());
            buf.append(":");
        }
        serialNum = (String) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA");
                    digest.update(buf.toString().getBytes());
                    byte[] data = digest.digest();
                    serialNum = new BASE64Encoder().encode(data);
                    return serialNum;
                } catch (NoSuchAlgorithmException exp) {
                    BootSecurityManager.securityLogger.log(Level.SEVERE, exp.getMessage(), exp);
                    return buf.toString();
                }
            }
        });
        return serialNum;
    }

    /**
    * If set to true the CacheManager will check for updates everytime the application
    * is executed when a new class loader is created. If set to false, as long as there is a
    * locally cached resource the CacheManager will not check for updates.
    * 
    * Defaults to true
    */
    public void setCheckForUpdates(boolean getUpdate) {
        checkForUpdates_flag = getUpdate;
    }

    /**
    * If  true the CacheManager will check for updates everytime the application
    * is executed when a new class loader is created. If false, as long as there is a
    * locally cached resource the CacheManager will not check for updates
    */
    public boolean checkForUpdates() {
        return checkForUpdates_flag;
    }

    /**
    * Return true if this Executable Java app is using Restricted Permissions
    */
    public boolean useRestrictedPermissions() {
        return restrictedPerms_flag;
    }

    /**
    * Get the permissions this application has requested
    */
    public PermissionCollection getPermissions() {
        return permissions;
    }

    /**
    * Set the permissions this application has requested
    */
    public void setPermissions(PermissionCollection permissions) {
        this.permissions = permissions;
        if (permissions == null) {
            setPermissionDescription("Restricted Permissions");
            restrictedPerms_flag = true;
        }
        if (permissions != null && permissions.implies(new AllPermission())) {
            setPermissionDescription("All Permissions");
            restrictedPerms_flag = false;
        } else {
            setPermissionDescription("Custom Permissions");
            restrictedPerms_flag = false;
        }
    }

    /**
    * Return an all-permissions collection.
    */
    public final Permissions getAllPermissions() {
        Permissions result = new Permissions();
        result.add(new AllPermission());
        return result;
    }

    /**
    * Set Description of Permissions requested by this Application
    */
    protected void setPermissionDescription(String s) {
        permissionDesc = s;
    }

    /**
    * Get Description of Permissions requested by this Application
    */
    public String getPermissionDescription() {
        return permissionDesc;
    }

    /**
    * Prompt the user to grant permission to run this application or service.
    * 
    * @return
    */
    protected boolean promptForPermission() {
        return true;
    }

    /**
    * Get Display Name of Executable
    */
    public String getDisplayName() {
        return displayName;
    }

    /**
    * Get the Name of the Executable
    */
    public String getName() {
        return name;
    }

    public String toString() {
        if (displayName != null) return displayName; else return name;
    }

    /**
    * Add a native library desc to native libraries for this service
    */
    public void addNativeLib(NativeLibDesc nativeLib) {
        nativeLibs.add(nativeLib);
    }

    /**
    * Clear all resources
    */
    public void clearResources() {
        classpath.clear();
        nativeLibs.clear();
    }

    /**
    * Clear Java resources
    */
    public void clearClassPath() {
        classpath.clear();
    }

    /**
    * Clear Java resources
    */
    public void clearNativeResources() {
        nativeLibs.clear();
    }

    /**
    * Add a URL to resouces this application will use
    */
    public void addClassPathEntry(ClassPathEntry entry) {
        classpath.add(entry);
    }

    /**
    * Get array or urls that make up the classpath for this service
    */
    public ClassPathEntry[] getClassPath() {
        return (ClassPathEntry[]) classpath.toArray(new ClassPathEntry[0]);
    }

    /**
    * Get the URLs for Native resources used by this application
    */
    public NativeLibDesc[] getNativeLibs() {
        return (NativeLibDesc[]) nativeLibs.toArray(new NativeLibDesc[0]);
    }

    /**
    * Get array or service names this service should reference
    */
    public ServiceDescStub[] getServiceRefs() {
        if (serviceRefs == null) return new ServiceDescStub[0];
        ServiceDescStub[] refs = new ServiceDescStub[serviceRefs.size()];
        return (ServiceDescStub[]) serviceRefs.toArray(refs);
    }

    /**
    * Process the main node of the XML
    * @param element to process
    * @param fullParse true if the full Service Element should be parsed
    */
    protected void processMainNode(URL contextURL, Element node) throws DOMException {
        this.contextURL = contextURL;
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cNode = nodes.item(i);
            if (cNode.getNodeType() == Node.TEXT_NODE) continue;
            if (cNode.getNodeName().equals("name")) this.name = cNode.getFirstChild().getNodeValue(); else if (cNode.getNodeName().equals("display-name")) this.displayName = cNode.getFirstChild().getNodeValue();
            if (cNode.getNodeName().equals("desc")) {
                if (cNode.getFirstChild() != null) this.description = cNode.getFirstChild().getNodeValue();
            } else if (cNode.getNodeName().equals("version")) {
                if (cNode.getFirstChild() != null) this.version = cNode.getFirstChild().getNodeValue();
            } else if (cNode.getNodeName().equals("classpath")) {
                processClassPathNode(contextURL, (Element) cNode);
            } else if (cNode.getNodeName().equals("native-libs")) {
                processNativeLibsNode(contextURL, (Element) cNode);
            } else if (cNode.getNodeName().equals("properties")) {
                processPropertiesNode(contextURL, (Element) cNode);
            } else if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                processCustomElement((Element) cNode);
            }
        }
    }

    /**
    * Process a custom desc xml element. Subclasses should override
    * @param element
    */
    protected void processCustomElement(Element element) throws DOMException {
    }

    /**
    * Get Properties associated with this Service
    */
    public Properties getProperties() {
        return properties;
    }

    /**
    * Process a properties Node
    */
    protected void processPropertiesNode(URL contextURL, Element node) throws DOMException {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cNode = nodes.item(i);
            if (cNode.getNodeName().equals("property")) {
                if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                    String name = ((Element) cNode).getAttribute("name");
                    String value = ((Element) cNode).getAttribute("value");
                    properties.put(name, value);
                }
            }
        }
    }

    /**
    * Process a classpath Node
    */
    protected void processClassPathNode(URL contextURL, Element node) throws DOMException {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cNode = nodes.item(i);
            if (cNode.getNodeName().equals("service-ref")) {
                if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                    String srvName = ((Element) cNode).getAttribute("name");
                    String srvHref = ((Element) cNode).getAttribute("href");
                    serviceRefs.add(new ServiceDescStub(srvName, srvHref, contextURL));
                }
            } else if (cNode.getNodeName().equals("lib")) {
                if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                    String libPath = ((Element) cNode).getAttribute("path");
                    String os = ((Element) cNode).getAttribute("os");
                    try {
                        URL libURL = new URL(contextURL, libPath);
                        addClassPathEntry(new ClassPathEntry(os, libURL));
                    } catch (MalformedURLException urlExp) {
                        logger.log(Level.WARNING, "Service: " + name + " lib path:" + libPath + " is invalid", urlExp);
                    }
                }
            }
        }
    }

    /**
    * Process a native-libs Node
    */
    protected void processNativeLibsNode(URL contextURL, Element node) throws DOMException {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cNode = nodes.item(i);
            if (cNode.getNodeName().equals("lib")) {
                if (cNode.getNodeType() == Node.ELEMENT_NODE) {
                    String libPath = ((Element) cNode).getAttribute("path");
                    String os = ((Element) cNode).getAttribute("os");
                    try {
                        URL libURL = new URL(contextURL, libPath);
                        addNativeLib(new NativeLibDesc(os, libURL));
                    } catch (MalformedURLException urlExp) {
                        logger.log(Level.WARNING, "Service: " + name + " native-lib path:" + libPath + " is invalid", urlExp);
                    }
                }
            }
        }
    }
}
