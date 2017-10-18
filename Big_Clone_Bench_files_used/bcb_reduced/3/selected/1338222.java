package com.jmonkey.universal.shared;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Registry {

    protected final class Impl extends Registry implements Serializable {

        private static final String _PROPERTY_CHAR_ENCODING = "ASCII";

        private static final String _ID_STR = "ST@";

        private static final String _ID_STA = "SA@";

        private static final String _ID_OBJ = "OB@";

        private static final String _ID_OBA = "OA@";

        private static final String _ID_BOO = "BO@";

        private static final String _ID_BYT = "BY@";

        private static final String _ID_BYA = "BA@";

        private static final String _ID_CHR = "CH@";

        private static final String _ID_CHA = "CA@";

        private static final String _ID_SHO = "SH@";

        private static final String _ID_INT = "IN@";

        private static final String _ID_INA = "IA@";

        private static final String _ID_LON = "LO@";

        private static final String _ID_DBL = "DO@";

        private static final String _ID_FLT = "FL@";

        private boolean _ALTERED;

        private Hashtable _GROUPS;

        private File _DATA_FILE;

        public void commit() throws IOException {
            storeData();
        }

        private Object decode(String in) throws OptionalDataException, ClassNotFoundException, IOException {
            String byteStr = trimCode(in);
            StringTokenizer stok = new StringTokenizer(byteStr, "|");
            ArrayList list = new ArrayList();
            for (; stok.hasMoreTokens(); list.add(stok.nextToken())) ;
            byte byteList[] = new byte[list.size()];
            for (int l = 0; l < list.size(); l++) byteList[l] = Byte.parseByte((String) list.get(l));
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            ois.close();
            bais.close();
            return o;
        }

        public void deleteAll() {
            _GROUPS.clear();
            _ALTERED = true;
        }

        public void deleteGroup(String group) {
            _GROUPS.remove(group);
            _ALTERED = true;
        }

        public void deleteProperty(String group, String key) {
            if (isGroup(group)) {
                ((Properties) _GROUPS.get(group)).remove(key);
                _ALTERED = true;
            }
        }

        public void dump() {
            System.out.println("Registry dump: " + (new Date()).toString());
            for (Enumeration ge = _GROUPS.keys(); ge.hasMoreElements(); ) {
                String key = (String) ge.nextElement();
                System.out.println(key);
                Properties temp = (Properties) _GROUPS.get(key);
                String pkey;
                for (Enumeration pe = temp.keys(); pe.hasMoreElements(); System.out.println("\t" + pkey + "=" + temp.getProperty(pkey))) pkey = (String) pe.nextElement();
            }
        }

        private String encode(Object o) throws InvalidClassException, NotSerializableException, IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            byte output[] = baos.toByteArray();
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < output.length; i++) buffer.append(output[i] + "|");
            baos.close();
            return buffer.toString();
        }

        private void ensureGroup(String group) {
            if (!_GROUPS.containsKey(group)) {
                _GROUPS.put(group, new Properties());
                _ALTERED = true;
            }
        }

        public Properties exportGroup(String group) {
            if (isGroup(group)) return (Properties) ((Properties) _GROUPS.get(group)).clone(); else return null;
        }

        private String getBasicProperty(String group, String key, String value) {
            if (value == null) if (isGroup(group)) return ((Properties) _GROUPS.get(group)).getProperty(key); else return null;
            ensureGroup(group);
            if (!((Properties) _GROUPS.get(group)).containsKey(key)) {
                setProperty(group, key, value);
                return ((Properties) _GROUPS.get(group)).getProperty(key);
            } else {
                return ((Properties) _GROUPS.get(group)).getProperty(key);
            }
        }

        public boolean getBoolean(String group, String key, boolean defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("BO@")) return (new Boolean(trimCode(so))).booleanValue(); else return defaultValue;
            }
            if (out.startsWith("BO@")) return (new Boolean(trimCode(out))).booleanValue(); else return defaultValue;
        }

        public byte getByte(String group, String key, byte defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("BY@")) return Byte.parseByte(trimCode(so)); else return defaultValue;
            }
            if (out.startsWith("BY@")) return Byte.parseByte(trimCode(out)); else return defaultValue;
        }

        public byte[] getByteArray(String group, String key, byte defaultValue[]) {
            try {
                String out = getBasicProperty(group, key, null);
                if (out == null) {
                    setProperty(group, key, defaultValue);
                    String so = getBasicProperty(group, key, null);
                    if (so.startsWith("BA@")) return (byte[]) decode(so); else return defaultValue;
                }
                if (out.startsWith("BA@")) return (byte[]) decode(out); else return defaultValue;
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
            return defaultValue;
        }

        public char getChar(String group, String key, char defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("CH@")) return trimCode(so).charAt(0); else return defaultValue;
            }
            if (out.startsWith("CH@")) return trimCode(out).charAt(0); else return defaultValue;
        }

        public char[] getCharArray(String group, String key, char defaultValue[]) {
            try {
                String out = getBasicProperty(group, key, null);
                if (out == null) {
                    setProperty(group, key, defaultValue);
                    String so = getBasicProperty(group, key, null);
                    if (so.startsWith("CA@")) return (char[]) decode(so); else return defaultValue;
                }
                if (out.startsWith("CA@")) return (char[]) decode(out); else return defaultValue;
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
            return defaultValue;
        }

        public double getDouble(String group, String key, double defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("DO@")) return Double.parseDouble(trimCode(so)); else return defaultValue;
            }
            if (out.startsWith("DO@")) return Double.parseDouble(trimCode(out)); else return defaultValue;
        }

        public File getFile() {
            return _DATA_FILE;
        }

        public float getFloat(String group, String key, float defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("FL@")) return Float.parseFloat(trimCode(so)); else return defaultValue;
            }
            if (out.startsWith("FL@")) return Float.parseFloat(trimCode(out)); else return defaultValue;
        }

        public Enumeration getGroups() {
            return _GROUPS.keys();
        }

        public int getInteger(String group, String key, int defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("IN@")) return Integer.parseInt(trimCode(so)); else return defaultValue;
            }
            if (out.startsWith("IN@")) return Integer.parseInt(trimCode(out)); else return defaultValue;
        }

        public int[] getIntegerArray(String group, String key, int defaultValue[]) {
            try {
                String out = getBasicProperty(group, key, null);
                if (out == null) {
                    setProperty(group, key, defaultValue);
                    String so = getBasicProperty(group, key, null);
                    if (so.startsWith("IA@")) return (int[]) decode(so); else return defaultValue;
                }
                if (out.startsWith("IA@")) return (int[]) decode(out); else return defaultValue;
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
            return defaultValue;
        }

        public Enumeration getKeys(String group) {
            ensureGroup(group);
            return ((Properties) _GROUPS.get(group)).keys();
        }

        public long getLong(String group, String key, long defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("LO@")) return Long.parseLong(trimCode(so)); else return defaultValue;
            }
            if (out.startsWith("LO@")) return Long.parseLong(trimCode(out)); else return defaultValue;
        }

        public Object getObject(String group, String key, Serializable defaultValue) {
            try {
                String out = getBasicProperty(group, key, null);
                if (out == null) {
                    setProperty(group, key, defaultValue);
                    String so = getBasicProperty(group, key, null);
                    if (so.startsWith("OB@")) return decode(so); else return defaultValue;
                }
                if (out.startsWith("OB@")) return decode(out); else return defaultValue;
            } catch (Throwable _ex) {
                return defaultValue;
            }
        }

        public Object[] getObject(String group, String key, Serializable defaultValue[]) {
            try {
                String out = getBasicProperty(group, key, null);
                if (out == null) {
                    setProperty(group, key, defaultValue);
                    String so = getBasicProperty(group, key, null);
                    if (so.startsWith("OA@")) return (Object[]) decode(so); else return defaultValue;
                }
                if (out.startsWith("OA@")) return (Object[]) decode(out); else return defaultValue;
            } catch (Throwable _ex) {
                return defaultValue;
            }
        }

        public short getShort(String group, String key, short defaultValue) {
            String out = getBasicProperty(group, key, null);
            if (out == null) {
                setProperty(group, key, defaultValue);
                String so = getBasicProperty(group, key, null);
                if (so.startsWith("SH@")) return Short.parseShort(trimCode(so)); else return defaultValue;
            }
            if (out.startsWith("SH@")) return Short.parseShort(trimCode(out)); else return defaultValue;
        }

        public String getString(String group, String key, String defaultValue) {
            return trimCode(getBasicProperty(group, key, defaultValue));
        }

        public String[] getStringArray(String group, String key, String defaultValue[]) {
            try {
                String out = getBasicProperty(group, key, null);
                if (out == null) {
                    setProperty(group, key, defaultValue);
                    String so = getBasicProperty(group, key, null);
                    if (so.startsWith("SA@")) return (String[]) decode(so); else return defaultValue;
                }
                if (out.startsWith("SA@")) return (String[]) decode(out); else return defaultValue;
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
            return defaultValue;
        }

        public int getType(String group, String key) {
            String value = ((Properties) _GROUPS.get(group)).getProperty(key);
            if (value == null) return 0;
            String code = value.substring(0, 3);
            System.out.println("TYPE: " + code);
            if (code.equals("ST@")) return 2;
            if (code.equals("SA@")) return 4;
            if (code.equals("OB@")) return 8;
            if (code.equals("OA@")) return 16;
            if (code.equals("BO@")) return 32;
            if (code.equals("BY@")) return 64;
            if (code.equals("BA@")) return 128;
            if (code.equals("CH@")) return 240;
            if (code.equals("CA@")) return 256;
            if (code.equals("SH@")) return 512;
            if (code.equals("IN@")) return 1024;
            if (code.equals("IA@")) return 2048;
            if (code.equals("LO@")) return 3840;
            if (code.equals("DO@")) return 4096;
            return !code.equals("FL@") ? 16384 : 8192;
        }

        public void importGroup(String group, Properties properties) {
            if (!isGroup(group)) {
                Properties p = (Properties) properties.clone();
                _GROUPS.put(group, p);
                _ALTERED = true;
            }
        }

        public boolean isAltered() {
            return _ALTERED;
        }

        public boolean isArrayType(String group, String key) {
            int type = getType(group, key);
            switch(type) {
                case 4:
                case 16:
                case 128:
                case 256:
                case 2048:
                    return true;
            }
            return false;
        }

        public boolean isGroup(String group) {
            return _GROUPS.containsKey(group);
        }

        public boolean isProperty(String group, String key) {
            if (isGroup(group)) return ((Properties) _GROUPS.get(group)).containsKey(key); else return false;
        }

        private void loadData() throws IOException {
            if (_DATA_FILE != null) {
                _DATA_FILE.createNewFile();
                read(new FileReader(_DATA_FILE));
                _ALTERED = false;
            } else {
                throw new IOException("Data file not set.");
            }
        }

        public void mergRegistry(Registry registry) {
            if (registry instanceof Map) {
                _GROUPS.putAll((Map) registry);
                _ALTERED = true;
            }
        }

        public void read(Reader reader) throws IOException {
            BufferedReader br = new BufferedReader(reader);
            String current_group = null;
            StringBuffer buffer = new StringBuffer();
            if (!_GROUPS.isEmpty()) deleteAll();
            try {
                int idx;
                for (String line = null; (line = br.readLine()) != null; ) if (!line.startsWith("//") && !line.startsWith(";") && !line.startsWith("#")) if (line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']') {
                    if (buffer.length() > 0 && current_group != null) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.toString().getBytes("ASCII"));
                        ((Properties) _GROUPS.get(current_group)).load(bais);
                        bais.close();
                        buffer.setLength(0);
                    }
                    current_group = line.substring(1, line.length() - 1);
                    ensureGroup(current_group);
                } else if ((idx = line.indexOf('=')) != -1) buffer.append(line + "\n");
                if (buffer.length() > 0 && current_group != null) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer.toString().getBytes("ASCII"));
                    ((Properties) _GROUPS.get(current_group)).load(bais);
                    bais.close();
                    buffer.setLength(0);
                }
            } finally {
                try {
                    br.close();
                } catch (IOException _ex) {
                }
            }
        }

        public Properties referenceGroup(String group) {
            if (isGroup(group)) return (Properties) _GROUPS.get(group); else return null;
        }

        public void replaceGroup(String group, Properties properties) {
            _GROUPS.put(group, properties);
            _ALTERED = true;
        }

        public void revert() throws IOException {
            if (_DATA_FILE != null && isAltered()) {
                _DATA_FILE.createNewFile();
                loadData();
            }
        }

        private void setBasicProperty(String group, String key, String value) {
            if (value == null) {
                throw new IllegalArgumentException("Can not set a property to null.");
            } else {
                ensureGroup(group);
                ((Properties) _GROUPS.get(group)).setProperty(key, value);
                _ALTERED = true;
                return;
            }
        }

        public void setFile(File file) {
            if (_DATA_FILE != null) _ALTERED = true;
            _DATA_FILE = file;
        }

        public void setProperty(String group, String key, byte value) {
            setBasicProperty(group, key, "BY@" + value);
        }

        public void setProperty(String group, String key, char value) {
            setBasicProperty(group, key, "CH@" + value);
        }

        public void setProperty(String group, String key, double value) {
            setBasicProperty(group, key, "DO@" + value);
        }

        public void setProperty(String group, String key, float value) {
            setBasicProperty(group, key, "FL@" + value);
        }

        public void setProperty(String group, String key, int value) {
            setBasicProperty(group, key, "IN@" + value);
        }

        public void setProperty(String group, String key, long value) {
            setBasicProperty(group, key, "LO@" + value);
        }

        public void setProperty(String group, String key, Serializable value) {
            try {
                setBasicProperty(group, key, "OB@" + encode(value));
            } catch (Throwable _ex) {
            }
        }

        public void setProperty(String group, String key, String value) {
            setBasicProperty(group, key, "ST@" + value);
        }

        public void setProperty(String group, String key, short value) {
            setBasicProperty(group, key, "SH@" + value);
        }

        public void setProperty(String group, String key, boolean value) {
            setBasicProperty(group, key, "BO@" + value);
        }

        public void setProperty(String group, String key, byte value[]) {
            try {
                setBasicProperty(group, key, "BA@" + encode(value));
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }

        public void setProperty(String group, String key, char value[]) {
            try {
                setBasicProperty(group, key, "CA@" + encode(value));
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }

        public void setProperty(String group, String key, int value[]) {
            try {
                setBasicProperty(group, key, "IA@" + encode(value));
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }

        public void setProperty(String group, String key, Serializable value[]) {
            try {
                setBasicProperty(group, key, "OA@" + encode(value));
            } catch (Throwable _ex) {
            }
        }

        public void setProperty(String group, String key, String value[]) {
            try {
                setBasicProperty(group, key, "SA@" + encode(value));
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }

        public int size() {
            return _GROUPS.size();
        }

        public int sizeOf(String group) {
            if (isGroup(group)) return ((Properties) _GROUPS.get(group)).size(); else return 0;
        }

        private void storeData() throws IOException {
            if (_DATA_FILE != null) {
                _DATA_FILE.createNewFile();
                write(new FileWriter(_DATA_FILE));
                _ALTERED = false;
            } else {
                throw new IOException("Data file not set.");
            }
        }

        private String trimCode(String in) {
            return in.substring(3, in.length());
        }

        public void write(Writer writer) throws IOException {
            try {
                ByteArrayOutputStream baos;
                for (Enumeration groups = _GROUPS.keys(); groups.hasMoreElements(); baos.close()) {
                    String group = (String) groups.nextElement();
                    String writeSec = "[" + group + "]" + System.getProperty("line.separator");
                    writer.write(writeSec);
                    Properties temp = (Properties) _GROUPS.get(group);
                    baos = new ByteArrayOutputStream();
                    temp.store(baos, _DATA_FILE.getName());
                    writer.write(baos.toString("ASCII"));
                }
            } finally {
                try {
                    writer.flush();
                } catch (IOException _ex) {
                }
                try {
                    writer.close();
                } catch (IOException _ex) {
                }
            }
        }

        protected Impl() {
            _ALTERED = false;
            _GROUPS = new Hashtable();
            _DATA_FILE = null;
        }

        protected Impl(File data_file) throws IOException {
            _ALTERED = false;
            _GROUPS = new Hashtable();
            _DATA_FILE = null;
            setFile(data_file);
            loadData();
        }

        protected Impl(Reader reader) throws IOException {
            _ALTERED = false;
            _GROUPS = new Hashtable();
            _DATA_FILE = null;
            read(reader);
        }
    }

    public static final int VERSION[] = { 0, 1, 0 };

    public static final int TYPE_NULL = 0;

    public static final int TYPE_STRING_SINGLE = 2;

    public static final int TYPE_STRING_ARRAY = 4;

    public static final int TYPE_OBJECT_SINGLE = 8;

    public static final int TYPE_OBJECT_ARRAY = 16;

    public static final int TYPE_BOOLEAN_SINGLE = 32;

    public static final int TYPE_BYTE_SINGLE = 64;

    public static final int TYPE_BYTE_ARRAY = 128;

    public static final int TYPE_CHAR_SINGLE = 240;

    public static final int TYPE_CHAR_ARRAY = 256;

    public static final int TYPE_SHORT_SINGLE = 512;

    public static final int TYPE_INT_SINGLE = 1024;

    public static final int TYPE_INT_ARRAY = 2048;

    public static final int TYPE_LONG_SINGLE = 3840;

    public static final int TYPE_DOUBLE_SINGLE = 4096;

    public static final int TYPE_FLOAT_SINGLE = 8192;

    public static final int TYPE_CORRUPT = 16384;

    public static final File RESOURCE_DIRECTORY;

    public static final File REGISTRY_DIRECTORY;

    private static Registry _INSTANCE = null;

    protected Registry() {
    }

    public static final Registry blankRegistery() {
        return instance().new Impl();
    }

    public void commit() throws IOException {
    }

    public void deleteAll() {
    }

    public void deleteGroup(String s) {
    }

    public void deleteProperty(String s, String s1) {
    }

    public void dump() {
    }

    public static final String encryptMD5(String decrypted) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(decrypted.getBytes());
            byte hash[] = md5.digest();
            md5.reset();
            return hashToHex(hash);
        } catch (NoSuchAlgorithmException _ex) {
            return null;
        }
    }

    public static final String ensureDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists() || dir.exists() && !dir.isDirectory()) dir.mkdirs();
        return dir.getAbsolutePath();
    }

    public Properties exportGroup(String group) {
        return null;
    }

    public boolean getBoolean(String group, String key, boolean defaultValue) {
        return false;
    }

    public byte getByte(String group, String key, byte defaultValue) {
        return 0;
    }

    public byte[] getByteArray(String group, String key, byte defaultValue[]) {
        return null;
    }

    public char getChar(String group, String key, char defaultValue) {
        return '\0';
    }

    public char[] getCharArray(String group, String key, char defaultValue[]) {
        return null;
    }

    public double getDouble(String group, String key, double defaultValue) {
        return 0.0D;
    }

    public File getFile() {
        return null;
    }

    public float getFloat(String group, String key, float defaultValue) {
        return 0.0F;
    }

    public Enumeration getGroups() {
        return null;
    }

    public int getInteger(String group, String key, int defaultValue) {
        return 0;
    }

    public int[] getIntegerArray(String group, String key, int defaultValue[]) {
        return null;
    }

    public Enumeration getKeys(String group) {
        return null;
    }

    public long getLong(String group, String key, long defaultValue) {
        return 0L;
    }

    public Object getObject(String group, String key, Object defaultValue) {
        return null;
    }

    public Object[] getObject(String group, String key, Object defaultValue[]) {
        return null;
    }

    public short getShort(String group, String key, short defaultValue) {
        return 0;
    }

    public String getString(String group, String key, String defaultValue) {
        return null;
    }

    public String[] getStringArray(String group, String key, String defaultValue[]) {
        return null;
    }

    public int getType(String group, String key) {
        return 16384;
    }

    private static final String hashToHex(byte hash[]) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            if ((hash[i] & 0xff) < 16) buf.append("0");
            buf.append(Integer.toHexString(hash[i] & 0xff));
        }
        return buf.toString();
    }

    public void importGroup(String s, Properties properties1) {
    }

    private static Registry instance() {
        if (_INSTANCE == null) _INSTANCE = new Registry();
        return _INSTANCE;
    }

    public boolean isAltered() {
        return false;
    }

    public boolean isArrayType(String group, String key) {
        return false;
    }

    public boolean isGroup(String group) {
        return false;
    }

    public boolean isProperty(String group, String key) {
        return false;
    }

    public static final boolean isRegistry(Class testClass) {
        return isRegistry(testClass.getName(), false);
    }

    public static final boolean isRegistry(String name) {
        return isRegistry(name, false);
    }

    public static final boolean isRegistry(String name, boolean encrypted) {
        if (encrypted) name = encryptMD5(name).toUpperCase();
        return (new File(REGISTRY_DIRECTORY, name + ".jmr")).exists();
    }

    public static final Registry loadForClass(Class requestingClass) throws IOException {
        return loadForName(requestingClass.getName(), false);
    }

    public static final Registry loadForName(String name) throws IOException {
        return loadForName(name, false);
    }

    public static final Registry loadForName(String name, boolean encrypted) throws IOException {
        if (encrypted) name = encryptMD5(name).toUpperCase();
        File file = new File(REGISTRY_DIRECTORY, name + ".jmr");
        return instance().new Impl(file);
    }

    public static final Registry loadForReader(Reader reader) throws IOException {
        return instance().new Impl(reader);
    }

    public void mergRegistry(Registry registry1) {
    }

    public void read(Reader reader1) throws IOException {
    }

    public Properties referenceGroup(String group) {
        return null;
    }

    public void replaceGroup(String s, Properties properties1) {
    }

    public void revert() throws IOException {
    }

    public void setFile(File file1) {
    }

    public void setProperty(String s, String s1, byte byte0) {
    }

    public void setProperty(String s, String s1, char c) {
    }

    public void setProperty(String s, String s1, double d) {
    }

    public void setProperty(String s, String s1, float f) {
    }

    public void setProperty(String s, String s1, int i) {
    }

    public void setProperty(String s, String s1, long l) {
    }

    public void setProperty(String s, String s1, Object obj) {
    }

    public void setProperty(String s, String s1, String s2) {
    }

    public void setProperty(String s, String s1, short word0) {
    }

    public void setProperty(String s, String s1, boolean flag) {
    }

    public void setProperty(String s, String s1, byte abyte0[]) {
    }

    public void setProperty(String s, String s1, char ac[]) {
    }

    public void setProperty(String s, String s1, int ai[]) {
    }

    public void setProperty(String s, String s1, Object aobj[]) {
    }

    public void setProperty(String s, String s1, String as[]) {
    }

    public int size() {
        return 0;
    }

    public int sizeOf(String group) {
        return 0;
    }

    public void write(Writer writer1) throws IOException {
    }

    static {
        RESOURCE_DIRECTORY = new File(ensureDirectory(System.getProperty("user.home") + File.separator + ".jmonkey" + File.separator + "export"));
        REGISTRY_DIRECTORY = new File(ensureDirectory(RESOURCE_DIRECTORY.getAbsolutePath() + File.separator + "registry"));
    }
}
