package com.jmonkey.export;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * The Registry class stores properties used by JMonkey services. To get a
 * working registry, you must use the static methods:
 * 
 * <pre>
 *    Registry.loadForClass(Class)
 *    Registry.loadForName(String)
 * </pre>;
 * 
 *  The {@link loadForClass(Class)} method returns a Registry that 
 *  is guaranteed to be unique to the class.  It 
 *  essentially calls <code> loadForName(class.getName()) </code>, 
 *  except that the class name will be MD5 encrypted.
 *  If you requre the that the registry be accessible
 *  to more basic operation, then use the {@link loadForName(String)} method.
 * <P>
 *  Be careful with {@link loadForName(String)} as it is possible to
 *  load the registry for another application.
 * <P>
 *  A call to <code>isRegistry(String|Class)</code> will tell you if 
 *  there is already a Registry on a particular name in the system.
 * <p>
 *  Get methods that return numbers may throw NumberFormatException if you
 *  try and get a value that is not a number.
 * <p>
 *  Array value may also throw unexpected exceptions if the data type 
 *  in incorrect, or if the stored version of the property is corrupt.
 *  
 *  @author Brill Pappin
 *
 *  FIXME - it is a bad idea to expose registry groups as Properties objects,
 *  not least because the group entries have types.
 */
public abstract class Registry {

    private static String FS = File.separator;

    public static final int TYPE_NONE = 0;

    public static final int TYPE_STRING_SINGLE = 1;

    public static final int TYPE_STRING_ARRAY = 2;

    public static final int TYPE_OBJECT_SINGLE = 3;

    public static final int TYPE_OBJECT_ARRAY = 4;

    public static final int TYPE_BOOLEAN_SINGLE = 5;

    public static final int TYPE_BYTE_SINGLE = 6;

    public static final int TYPE_BYTE_ARRAY = 7;

    public static final int TYPE_CHAR_SINGLE = 8;

    public static final int TYPE_CHAR_ARRAY = 9;

    public static final int TYPE_SHORT_SINGLE = 10;

    public static final int TYPE_INT_SINGLE = 11;

    public static final int TYPE_INT_ARRAY = 12;

    public static final int TYPE_LONG_SINGLE = 13;

    public static final int TYPE_DOUBLE_SINGLE = 14;

    public static final int TYPE_FLOAT_SINGLE = 15;

    public static final int TYPE_CORRUPT = 16;

    public static String typeToJavaType(int type) throws RegistryException {
        switch(type) {
            case Registry.TYPE_STRING_SINGLE:
                return "String";
            case Registry.TYPE_STRING_ARRAY:
                return "String[]";
            case Registry.TYPE_OBJECT_SINGLE:
                return "Object";
            case Registry.TYPE_OBJECT_ARRAY:
                return "Object[]";
            case Registry.TYPE_BOOLEAN_SINGLE:
                return "boolean";
            case Registry.TYPE_BYTE_SINGLE:
                return "byte";
            case Registry.TYPE_BYTE_ARRAY:
                return "byte[]";
            case Registry.TYPE_CHAR_SINGLE:
                return "char";
            case Registry.TYPE_CHAR_ARRAY:
                return "char[]";
            case Registry.TYPE_SHORT_SINGLE:
                return "short";
            case Registry.TYPE_INT_SINGLE:
                return "int";
            case Registry.TYPE_INT_ARRAY:
                return "int[]";
            case Registry.TYPE_LONG_SINGLE:
                return "long";
            case Registry.TYPE_DOUBLE_SINGLE:
                return "double";
            case Registry.TYPE_FLOAT_SINGLE:
                return "float";
            default:
                return "unknown";
        }
    }

    public static int javaTypeToType(String type) throws RegistryException {
        if (type.equals("String")) {
            return Registry.TYPE_STRING_SINGLE;
        } else if (type.equals("String[]")) {
            return Registry.TYPE_STRING_ARRAY;
        } else if (type.equals("Object")) {
            return Registry.TYPE_OBJECT_SINGLE;
        } else if (type.equals("Object[]")) {
            return Registry.TYPE_OBJECT_ARRAY;
        } else if (type.equals("boolean")) {
            return Registry.TYPE_BOOLEAN_SINGLE;
        } else if (type.equals("byte")) {
            return Registry.TYPE_BYTE_SINGLE;
        } else if (type.equals("byte[]")) {
            return Registry.TYPE_BYTE_ARRAY;
        } else if (type.equals("char")) {
            return Registry.TYPE_CHAR_SINGLE;
        } else if (type.equals("char[]")) {
            return Registry.TYPE_CHAR_ARRAY;
        } else if (type.equals("short")) {
            return Registry.TYPE_SHORT_SINGLE;
        } else if (type.equals("int")) {
            return Registry.TYPE_INT_SINGLE;
        } else if (type.equals("int[]")) {
            return Registry.TYPE_INT_ARRAY;
        } else if (type.equals("long")) {
            return Registry.TYPE_LONG_SINGLE;
        } else if (type.equals("float")) {
            return Registry.TYPE_FLOAT_SINGLE;
        } else if (type.equals("double")) {
            return Registry.TYPE_DOUBLE_SINGLE;
        } else {
            throw new RegistryException("Type not supported (" + type + ")");
        }
    }

    /**
   * The directory that resources will be stored in.<BR>
   * The directory will be called <user_home>/.lexi.
   */
    public static final File RESOURCE_DIRECTORY = new File(Runtime.ensureDirectory(System.getProperty("user.home") + FS + ".lexi" + FS + "export"));

    public static final File REGISTRY_DIRECTORY = new File(Runtime.ensureDirectory(RESOURCE_DIRECTORY.getAbsolutePath() + FS + "registry"));

    protected Registry() {
        super();
    }

    /**
   * Loads a registry with no data.
   * 
   * @param version the new registry's version numbers, or <code>null</code>.
   * @return com.jmonkey.office.service.Registry
   */
    public static Registry blankRegistry(int[] version) {
        return new RegistryImpl(version);
    }

    /**
   * Commits the registry data to disk. Registry objects created from streams,
   * or as blanks must have their file set before they can be commited.
   * 
   * @throws IOException if the commit fails.
   */
    public abstract void commit() throws IOException;

    /**
   * Deletes all data in the Registry.
   */
    public abstract void deleteAll();

    /**
   * Removes the specified group from the Registry.
   * 
   * @param group the name of the group remove
   */
    public abstract void deleteGroup(String group);

    /**
   * Deletes the specified key from the specified group.
   * 
   * @param group the name of the group
   * @param key the key to delete.
   */
    public abstract void deleteProperty(String group, String key);

    /**
   * Dumps the contents of the registry to the comsole. Used for debugging.
   */
    public abstract String toString();

    /**
   * Creates an MD5 Hash from the input string.
   * 
   * @param str the input string
   * @return the MD5 hash for the input string
   */
    public static String encryptMD5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            byte[] hash = md5.digest();
            md5.reset();
            return Format.hashToHex(hash);
        } catch (java.security.NoSuchAlgorithmException nsae0) {
            return null;
        }
    }

    /**
   * Return a copy of the group data for the specified group. 
   * 
   * @param group the name of the group to export
   * @return the group data as a new RegistryGroup object
   */
    public abstract RegistryGroup exportGroup(String group);

    /**
   * Gets a boolean value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract boolean getBoolean(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a byte value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract byte getByte(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a byte array value
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract byte[] getByteArray(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a char value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract char getChar(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a char array value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract char[] getCharArray(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a double value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract double getDouble(String group, String key) throws RegistryPropertyException;

    /**
   * Gets the file that data will be commited to.
   * 
   * @return java.io.File
   */
    public abstract File getFile();

    /**
   * Gets a float value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract float getFloat(String group, String key) throws RegistryPropertyException;

    /**
   * Return an Enumeration of the groups
   * 
   * @return an Enumeration of Properties values.
   */
    public abstract Enumeration getGroups();

    /**
   * Gets an int value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract int getInteger(String group, String key) throws RegistryPropertyException;

    /**
   * Gets an int array value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract int[] getIntegerArray(String group, String key) throws RegistryPropertyException;

    /**
   * Return an Enumeration of the keys for a specified group.
   * 
   * @param group the name of the group
   * @return an Enumeration of keys
   */
    public abstract Enumeration getKeys(String group);

    /**
   * Gets a long value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract long getLong(String group, String key) throws RegistryPropertyException;

    /**
   * Gets an Object array value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract Object[] getObjectArray(String group, String key) throws RegistryPropertyException;

    /**
   * Gets an Object value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract Object getObject(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a short value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract short getShort(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a string value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract String getString(String group, String key) throws RegistryPropertyException;

    /**
   * Gets a string array value.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the value if the key is found
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract String[] getStringArray(String group, String key) throws RegistryPropertyException;

    /**
   * Returns the type of the specified property.
   * 
   * @param group the name of the group
   * @param key the name of the key in the group
   * @return the type of the value
   * @throws RegistryPropertyException if the property is missing.
   */
    public abstract int getType(String group, String key) throws RegistryPropertyException;

    /**
   * Returns the version numbers of the registry.  Note: this is the
   * the version number of the specific registry's implicit schema
   * not the version number of the registry file syntax.
   * 
   * @return the version number as an array of ints.  The number
   * at offset 0, is the major version number, etcetera.
   */
    public abstract int[] getVersion();

    /**
   * Imports a copy of the contents of a RegistryGroup object as the 
   * specified group.  Does nothing if the group already exists.
   * 
   * @param group the name of the group
   * @param properties the properties to be imported
   */
    public abstract void importGroup(String group, RegistryGroup properties);

    /**
   * Initialize a Registry group from an array of Strings.  The array 
   * has dimensions N x 3 where N is the number of properties.  Each 
   * property is defined by the three array row entries, as follows:
   * <ul>
   * <li><code>props[i][0]</code> gives the property name.
   * <li><code>props[i][1]</code> gives the initial value of the
   *    property, encoded as a String.   If there is no 2nd row entry,
   *    the property is defined with type "String" and an empty value.
   * <li><code>props[i][2]</code> gives the Java type of the property.
   *    If there is no 3rd row entry, the property is assumed to have
   *    type "String".
   * </ul>
   * @param group the name of the group
   * @param props the property definitions are as described above.
   * @throws RegistryException if the property type is not supported,
   *     the value doesn't match the type, or there is some other 
   *     inconsistency.
   */
    public abstract void initGroup(String group, String[][] props);

    /**
   * Test if the registry is currently blank.
   * @return <code>true</code> if the registry contains no groups.
   */
    public abstract boolean isBlank();

    /**
   * Test if the registry is in sync with the stored version.
   * Calling commit() will sync the registry with the stored
   * version.
   * @return <code>true</code> if the registry is out of sync with
   * the stored version.
   */
    public abstract boolean isAltered();

    /**
   * Tests if the property specified is an array type.
   * 
   * @param group the name of the group
   * @param key the key within the group
   * @return <code>true</code> if the key has the required type.
   */
    public abstract boolean isArrayType(String group, String key);

    /**
   * Tests for the existence of the specified group.
   * 
   * @param group the name of the group
   * @return <code>true</code> if the group exists, <code>false</code>
   * otherwise.
   */
    public abstract boolean isGroup(String group);

    /**
   * Tests for the existence of the specified property in the specified group.
   * 
   * @param group the name of the group
   * @param key the key for the property
   * @return boolean <code>true</code> if the property exists, 
   *         <code>false</code>
   * otherwise.
   */
    public abstract boolean isProperty(String group, String key);

    /**
   * Tests for the existence of a Registry for the specified class.
   * 
   * @param c the Class 
   * @return boolean <code>true<code> if the Registry exists for this class, 
   * <code>false<code> otherwise.
   */
    public static boolean hasRegistry(Class c) {
        return Registry.hasRegistry(c.getName(), false);
    }

    /**
   * Tests for the existence of a Registry for the specified name.
   * 
   * @param name the registry name
   * @return boolean <code>true<code> if the Registry exists for this class, 
   * <code>false<code> otherwise.
   */
    public static boolean hasRegistry(String name) {
        return Registry.hasRegistry(name, false);
    }

    /**
   * Tests for the existence of a Registry for the specified name.
   * 
   * @param name the registry name
   * @param encrypted if <code>true</code> the registry name is encrypted
   * @return boolean <code>true<code> if the Registry exists for this class, 
   * <code>false<code> otherwise.
   */
    public static boolean hasRegistry(String name, boolean encrypted) {
        if (encrypted) {
            name = encryptMD5(name).toUpperCase();
        }
        return (new File(REGISTRY_DIRECTORY, name)).exists();
    }

    /**
   * Loads a registry guaranteed to be unique and constant for the specified
   * class. If the Registry does not yet exist, it will be created.
   * 
   * @param requestingClass the class requesting a Registry.
   * @return the required Registry
   * @throws IOException if the Registry file cannot be read or created.
   */
    public static Registry loadForClass(Class requestingClass) throws IOException {
        return Registry.loadForName(requestingClass.getName(), null, false);
    }

    /**
   * Loads a registry guaranteed to be unique and constant for the specified
   * class. If the Registry does not yet exist, it will be created.  This 
   * overload supplies a schema version for the existing / new Registry.
   * 
   * @param requestingClass the class requesting a Registry.
   * @param requestedVersion the requested Registry schema version or 
   *         <code>null </code>.
   * @return the required Registry
   * @throws IOException if the Registry file cannot be read or created.
   */
    public static Registry loadForClass(Class requestingClass, int[] requestedVersion) throws IOException {
        return Registry.loadForName(requestingClass.getName(), requestedVersion, false);
    }

    /**
   * Loads a registry with the specified name. If the Registry does not yet
   * exist, it will be created.
   * 
   * @param name the name for the Registry.
   * @return the required Registry
   * @throws IOException if the Registry file cannot be read or created.
   */
    public static Registry loadForName(String name) throws IOException {
        return Registry.loadForName(name, null, false);
    }

    /**
   * Loads a registry with the specified name. If the Registry does not yet
   * exist, it will be created.  This overload supplies a schema version
   * for the existing / new Registry.
   * 
   * @param name the name for the Registry.
   * @param requestedVersion the requested Registry schema version or 
   *         <code>null </code>.
   * @return the required Registry
   * @throws IOException if the Registry file cannot be read or created.
   */
    public static Registry loadForName(String name, int[] requestedVersion) throws IOException {
        return Registry.loadForName(name, requestedVersion, false);
    }

    /**
   * Loads a registry with the specified name. If the Registry does not yet
   * exist, it will be created.  If a non-null version number is supplied,
   * this will be used as the version for a new Registry; an existing 
   * Registry's version will be checked.
   * 
   * @param name the name of the Registry.
   * @param encrypted <code>true</code> if the registry name is encrypted.
   * @param requestedVersion the requested Registry schema version or 
   *         <code>null </code>.
   * @return the required Registry
   * @throws IOException if the Registry file cannot be read or created.
   */
    public static Registry loadForName(String name, int[] requestedVersion, boolean encrypted) throws IOException {
        if (encrypted) {
            name = encryptMD5(name).toUpperCase();
        }
        File file = new File(REGISTRY_DIRECTORY, name);
        return new RegistryImpl(file, requestedVersion);
    }

    /**
   * Loads a registry that reads its data from the specified Reader.
   * 
   * @param reader the source for registry data
   * @param requestedVersion the requested Registry schema version or 
   *         <code>null </code>.
   * @return a Registry populated from the reader
   * @throws IOException if the Registry data cannot be read.
   */
    public static Registry loadForReader(Reader reader, int[] requestedVersion) throws IOException {
        return new RegistryImpl(reader, requestedVersion);
    }

    /**
   * Adds the groups, keys and values of another Registry to this Registry.  
   * Where groups / keys match, values from the other Registry replace the
   * current values.
   * 
   * @param registry the source of imported registry data.
   */
    public abstract void mergeRegistry(Registry registry);

    /**
   * Reads data into this Registry from the specified Reader. All existing
   * groups, keys and values in the Registry will be deleted first.
   * 
   * @param reader the source of registry data.
   * @throws IOException is there is a problem reading the registry data
   */
    public abstract void read(Reader reader) throws IOException;

    /**
   * Return the group data for the specified group. The group will maintain its
   * reference in the registry. This is used to change the group externally
   * from the registry.  This method needs to be replaced.
   * 
   * @param group the name of the group
   * @return the group's RegistryGroup object
   */
    public abstract RegistryGroup referenceGroup(String group);

    /**
   * Replaces the RegistryGroup object for a group, with the specified 
   * Properties object. The Registry object takes ownership of the 
   * RegistryGroup.  This method needs to be replaced.
   * 
   * @param group the name of the group to replace.
   * @param properties the group data.
   */
    public abstract void replaceGroup(String group, RegistryGroup properties);

    /**
   * Reverts the Registry to its last commited state. Registry objects 
   * created from streams, or as blanks must have their file set and be 
   * commited, before they can be reverted.
   * 
   * @throws IOException if the commit fails.
   */
    public abstract void revert() throws IOException;

    /**
   * Sets the file that the registry's data will be commited to.
   * 
   * @param file the new registry file.
   */
    public abstract void setFile(File file);

    /**
   * Sets a byte array value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, byte[] value);

    /**
   * Sets a char array value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, char[] value);

    /**
   * Sets an int array value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, int[] value);

    /**
   * Sets an object array value
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, Serializable[] value);

    /**
   * Sets a string array value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, String[] value);

    /**
   * Sets a byte value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, byte value);

    /**
   * Sets a char value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, char value);

    /**
   * Sets a double value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, double value);

    /**
   * Sets a float value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, float value);

    /**
   * Sets an int value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, int value);

    /**
   * Sets a long value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, long value);

    /**
   * Sets an Object value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, Serializable value);

    /**
   * Sets a string value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, String value);

    /**
   * Sets a short value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, short value);

    /**
   * Sets a boolean value.
   * 
   * @param group the name of the group
   * @param key the key to be set
   * @param value the value to set
   */
    public abstract void setProperty(String group, String key, boolean value);

    /**
   * Returns the number of groups in the registry.
   * 
   * @return the number of groups
   */
    public abstract int size();

    /**
   * Returns the number of properties in the specified group.
   * 
   * @param group the group name
   * @return the number of properties
   */
    public abstract int sizeOf(String group);

    /**
   * Writes the data in this Registry to the specified Writer.
   * 
   * @param writer the place to send the data to
   * @throws IOException if there is a problem writing the data.
   */
    public abstract void write(Writer writer) throws IOException;
}
