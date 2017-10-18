package org.dbe.toolkit.pa.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility methods
 */
public class SdlUtil {

    /** Separator for project-relative paths. */
    private static final String PROJECT_PATH_SEPARATOR = "/";

    /** Prefix for project-relative URI. */
    private static final String PROJECT_RELATIVE_URI = "project:";

    /** XML Schema namespace declaration */
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    /** XML Schema instance namespace declaration */
    public static final String W3C_XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    /** XML Namespace declaration for W3C XML Namespace declaration */
    public static final String W3C_XMLNS = "http://www.w3.org/2000/xmlns/";

    /** Web Services Addressing namespace declaration. */
    public static final String WSA_NAMESPACE_URI = "http://schemas.xmlsoap.org/ws/2003/03/addressing";

    /** Web Services Policy namespace declaration. */
    public static final String WSP_NAMESPACE_URI = "http://schemas.xmlsoap.org/ws/2002/12/policy";

    /**
     * Private ctor to prevent instantiations
     */
    private SdlUtil() {
    }

    /**
     * Convert "true" or "false" to the appropriate boolean value.  If the arg
     * is not null, it will be converted to lowercase before evaluation.
     * Anything other than "true" will return false.
     * @param aTrueFalseString
     */
    public static boolean toBoolean(String aTrueFalseString) {
        String value = getSafeString(aTrueFalseString).toLowerCase();
        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Returns the location this class was loaded from (which .jar file or class directory)
     * or null if that location cannot be determined.  Note: if the class was loaded by the
     * system classloader, this method will return null.
     *
     * @param aClass
     */
    public static String getLocationForClass(Class aClass) {
        try {
            ProtectionDomain pDomain = aClass.getProtectionDomain();
            CodeSource cSource = pDomain.getCodeSource();
            URL loc = cSource.getLocation();
            return loc.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns true if the map object is null or of size 0.
     *
     * @param aMap A map that may be empty or null.
     * @return True if the map is null or empty.
     */
    public static boolean isNullOrEmpty(Map aMap) {
        return (aMap == null) || aMap.isEmpty();
    }

    /**
     * Returns true if the array object is null or of size 0.
     *
     * @param aArray
     * @return boolean
     */
    public static boolean isNullOrEmpty(Object[] aArray) {
        return (aArray == null) || (aArray.length == 0);
    }

    /**
     * Returns true if the string object is null or doesn't contain at least one
     * non-space character.
     *
     * @param aString The string to check.
     *
     * @return boolean
     */
    public static boolean isNullOrEmpty(String aString) {
        boolean result = true;
        if (aString != null) if (aString.trim().length() > 0) result = false;
        return result;
    }

    /**
     * Helper method that offers the negation of <code>isNullOrEmpty</code>
     *
     * @param aString
     */
    public static boolean notNullOrEmpty(String aString) {
        return !isNullOrEmpty(aString);
    }

    /**
     * Returns true if the collection passed in is either null or empty.
     * @param aCollection
     */
    public static boolean isNullOrEmpty(Collection aCollection) {
        return aCollection == null || aCollection.isEmpty();
    }

    /**
     * Compares 2 objects for equality and either object or both objects can be
     * null.
     * @param aOne can be null
     * @param aTwo can be null
     * @return boolean
     */
    public static boolean compareObjects(Object aOne, Object aTwo) {
        boolean same = false;
        if (aOne != null) {
            if (aOne.equals(aTwo)) same = true;
        } else if (aTwo == null) {
            same = true;
        }
        return same;
    }

    /**
     * Walks the element's ancestors looking for the namespace declaration given
     * a prefix name.
     * @param aElement the element context to find the namespace in
     * @param aKey the prefix we want the namespace for
     */
    public static String getNamespaceForPrefix(Element aElement, String aPrefix) {
        String namespace = aElement.getAttributeNS(W3C_XMLNS, aPrefix);
        if (SdlUtil.isNullOrEmpty(namespace)) {
            Node parentNode = aElement.getParentNode();
            if (!(parentNode instanceof Element)) {
                return null;
            }
            return getNamespaceForPrefix((Element) parentNode, aPrefix);
        }
        return namespace;
    }

    /**
     * Extracts the prefix from a QName value. This is the value that comes before
     * the colon in a qname. i.e. foo:x, this method returns 'foo'
     * @param aQNameValue can be null
     * @return String or null if there is no prefix or the param is null
     */
    public static String extractPrefix(String aQNameValue) {
        if (aQNameValue == null) {
            return null;
        }
        int colonOffset = aQNameValue.indexOf(':');
        if (colonOffset == -1) {
            return null;
        }
        return aQNameValue.substring(0, colonOffset).trim();
    }

    /**
     * Extracts the localname from a QName value. This is the value that comes
     * after the colon in a qname. i.e. foo:x, this method returns 'x'
     * @param aQNameValue can be null
     * @return String or null the input is null or aQNameValue if there is no colon.
     */
    public static String extractLocalPart(String aQNameValue) {
        if (aQNameValue == null) {
            return null;
        }
        int colonOffset = aQNameValue.indexOf(':');
        if (colonOffset == -1) {
            return aQNameValue.trim();
        }
        if (colonOffset == aQNameValue.length() - 1) {
            return null;
        }
        return aQNameValue.substring(colonOffset + 1).trim();
    }

    /**
     * Trims text lines within the given string.  Lines are substrings delimited
     * by newline characters.
     * E.g. argument " One  \n  Two  \n \nThree " yields "One\nTwo\n\nThree".
     * @param aText the string to be trimmed.
     * @return String a new trimmed string object.
     */
    public static String trimText(String aText) {
        StringBuffer buffer = new StringBuffer("");
        if (!isNullOrEmpty(aText)) {
            String line;
            BufferedReader buffReader = new BufferedReader(new StringReader(aText));
            try {
                String delim = "";
                while (((line = buffReader.readLine()) != null)) {
                    buffer.append(delim);
                    buffer.append(line.trim());
                    delim = "\n";
                }
            } catch (IOException e) {
            }
        }
        return buffer.toString();
    }

    /**
     * Helper method to check for file existence
     * @param aFilename the filename to be checked
     */
    public static boolean fileExists(String aFilename) {
        boolean exists = false;
        try {
            exists = new File(aFilename).exists();
        } catch (Throwable th) {
        }
        return exists;
    }

    /**
     * Takes an input stream for a file and returns the MD5 message digest
     * which represents it. This value can be used to facilitate file comparisons.
     * Note this method will return null if there is an error obtaining the
     * MD5 code.
     * @param aInput the stream to obtain the message digest for
     */
    public static byte[] getMessageDigest(InputStream aInput) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = new byte[512];
            for (int bytesRead, i = 0; (bytesRead = aInput.read(data)) > 0; i += bytesRead) md.update(data, 0, bytesRead);
            return md.digest();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Returns an empty string if the arg value is null or empty, otherwise
     * it returns the value arg untouched.
     * @param aValue the string to check
     */
    public static String getSafeString(String aValue) {
        if (isNullOrEmpty(aValue)) {
            return "";
        }
        return aValue;
    }

    /**
     * Appends additional path components to a project relative path.
     */
    public static String appendProjectRelativePath(String aProjectPath, String aAppendPath) {
        if (aAppendPath.startsWith(PROJECT_PATH_SEPARATOR)) {
            return PROJECT_RELATIVE_URI + aAppendPath;
        }
        String projectPath = aProjectPath;
        while (projectPath.endsWith(PROJECT_PATH_SEPARATOR)) {
            int i = projectPath.lastIndexOf(PROJECT_PATH_SEPARATOR);
            projectPath = projectPath.substring(0, i);
        }
        StringBuffer buffer = new StringBuffer(projectPath);
        StringTokenizer tokenizer = new StringTokenizer(aAppendPath, PROJECT_PATH_SEPARATOR);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if ("".equals(token)) {
            } else if (".".equals(token)) {
            } else if ("..".equals(token)) {
                int i = buffer.lastIndexOf(PROJECT_PATH_SEPARATOR);
                if (i < 0) {
                    throw new RuntimeException(MessageFormat.format("Too many double dots in {0} for {1}", new Object[] { aAppendPath, aProjectPath }));
                }
                buffer.setLength(i);
            } else {
                buffer.append(PROJECT_PATH_SEPARATOR);
                buffer.append(token);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the project relative base directory for the specified project
     * relative path.
     */
    public static String getProjectRelativeBaseDir(String aProjectPath) {
        int i = aProjectPath.lastIndexOf(PROJECT_PATH_SEPARATOR);
        String base = (i >= 0) ? aProjectPath.substring(0, i) : PROJECT_RELATIVE_URI;
        while (base.endsWith(PROJECT_PATH_SEPARATOR)) {
            i = base.lastIndexOf(PROJECT_PATH_SEPARATOR);
            base = base.substring(0, i);
        }
        return base;
    }

    /**
     * Returns <code>true</code> if and only if the specified path is a project
     * relative path.
     */
    public static boolean isProjectRelativePath(String aPath) {
        return !SdlUtil.isNullOrEmpty(aPath) && aPath.startsWith(PROJECT_RELATIVE_URI);
    }

    /**
     * Returns absolute location for specified WSDL or XSD import relative to
     * the specified parent document location (i.e., the location of the
     * document doing the importing).
     */
    public static String resolveImport(String aParentLocation, String aImportLocation) {
        if (isProjectRelativePath(aImportLocation)) {
            return aImportLocation;
        }
        try {
            new URL(aImportLocation);
            return aImportLocation;
        } catch (MalformedURLException e) {
        }
        try {
            if (new URI(aImportLocation).isAbsolute()) {
                return aImportLocation;
            }
        } catch (URISyntaxException e) {
        }
        if (SdlUtil.isProjectRelativePath(aParentLocation)) {
            String base = SdlUtil.getProjectRelativeBaseDir(aParentLocation);
            return SdlUtil.appendProjectRelativePath(base, aImportLocation);
        }
        try {
            return new URL(new URL(aParentLocation), aImportLocation).toExternalForm();
        } catch (MalformedURLException e) {
        }
        try {
            return new URI(aParentLocation).resolve(aImportLocation).toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return aImportLocation;
    }

    /**
     * Given two dates, this method will return the earliest.
     *
     * @param aDate1
     * @param aDate2
     */
    public static Date getMinDate(Date aDate1, Date aDate2) {
        if (aDate1 != null && aDate2 != null) {
            if (aDate1.before(aDate2)) {
                return aDate1;
            } else {
                return aDate2;
            }
        }
        if (aDate1 == null) return aDate2; else return aDate1;
    }

    /**
     * Utility method used to convert an object into a byte array.
     * May return null if an exception occurs.
     * @param aObject the object to be serialized
     */
    public static byte[] serializeObject(Serializable aObject) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(aObject);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Utility method used to convert a byte array result into an object.
     * May return null if an exception occurs.
     * @param aData the data to be converted
     */
    public static Object deserializeObject(byte[] aData) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(aData));
            Object object = ois.readObject();
            ois.close();
            return object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Locate resources on the classpath.  If a thread context
     * classloader is set it will try to load the resource
     * through it.  If it can't and a non-null class is passed
     * it will attempt to use its classloader to load the resource.
     * @param aResourceName
     * @param aClass
     * @return The resource <code>URL<code> or null if the resource could
     * not be located.
     */
    public static URL findOnClasspath(String aResourceName, Class aClass) {
        URL resourceURL = null;
        if (Thread.currentThread().getContextClassLoader() != null) {
            resourceURL = Thread.currentThread().getContextClassLoader().getResource(aResourceName);
        }
        if (resourceURL == null && aClass != null) {
            resourceURL = aClass.getClassLoader().getResource(aResourceName);
        }
        return resourceURL;
    }

    /**
     * Utility method to perform substitution of environment variables within a given string.
     * The substitution variable(s) should be in the format of ${VAR_TO_BE_REPLACED}. There may
     * be multiple substition variables in an input pattern, but they may not be nested within
     * each other. The caller may optionally supply the Properties to be used for substitution
     * in addition to the System Properties. User properties take precedence over System properties.
     * The user may "escape" a "$" character by preceding it with another "$" character.
     *
     * @param aPattern the string containing environment variables to be replaced with system property values
     * @param aProperties an optional set of properties to be used in prior to using System properties during
     *         substitution, if null System properties will be used
     * @return a new string with substitution variables replaced with their proper environment variables, if
     * a replacement cannot be found, the substitution variable will be left intact.
     */
    public static String replacePropertyVars(String aPattern, Properties aProperties) {
        if (aPattern == null) return null;
        boolean getVariable = false;
        StringBuffer buff = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(aPattern, "${}", true);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if ("$".equals(token)) {
                if (tok.hasMoreTokens()) {
                    token = tok.nextToken();
                    if ("{".equals(token)) getVariable = true; else buff.append(token);
                }
            } else if ("}".equals(token) && getVariable) {
                getVariable = false;
            } else {
                if (getVariable) {
                    String var = null;
                    if (aProperties != null) var = aProperties.getProperty(token);
                    if (var == null) var = System.getProperties().getProperty(token);
                    if (var == null) token = "${" + token + "}"; else token = var;
                }
                buff.append(token);
            }
        }
        return buff.toString();
    }

    /**
     * This method creates a temporary file.  In addition, it will throw nicely formatted
     * AeExceptions if the temp directory does not exist or is not writable.
     *
     * @param aPrefix
     * @param aSuffix
     * @throws Exception
     */
    public static File getTempFile(String aPrefix, String aSuffix) throws Exception {
        Properties props = System.getProperties();
        String tmpDir = props.getProperty("java.io.tmpdir");
        if (!new File(tmpDir).isDirectory()) {
            throw new IOException(MessageFormat.format("Could not write to temp location [{0}].", new Object[] { tmpDir }));
        }
        try {
            File file = File.createTempFile(aPrefix, aSuffix);
            if (!file.canWrite()) {
                throw new IOException(MessageFormat.format("Could not write to temp location [{0}].", new Object[] { file.getParent() }));
            }
            return file;
        } catch (IOException ioe) {
            throw new Exception(ioe);
        }
    }

    /**
     * Gets the file name given a (potentially) full path.  This method will work no matter
     * what the path separator is.
     *
     * @param aFilePath
     */
    public static String getFilename(String aFilePath) {
        if (aFilePath != null) {
            String[] split = aFilePath.split("[/\\\\]");
            if (split != null && split.length > 0) {
                return split[split.length - 1];
            }
        }
        return aFilePath;
    }

    /**
     * Returns a concatenation of all of the text nodes and CDATA nodes that are
     * immediate children of this element.
     * @param aElement
     */
    public static String getText(Element aElement) {
        StringBuffer buffer = new StringBuffer();
        NodeList nl = aElement.getChildNodes();
        for (int i = 0; nl.item(i) != null; i++) {
            Node child = nl.item(i);
            switch(child.getNodeType()) {
                case Node.CDATA_SECTION_NODE:
                case Node.TEXT_NODE:
                    buffer.append(child.getNodeValue());
            }
        }
        return buffer.toString();
    }
}
