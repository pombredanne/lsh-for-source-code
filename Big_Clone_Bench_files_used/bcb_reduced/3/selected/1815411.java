package oxygen.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class SimpleOxygenCacheManager implements OxygenPersistentCacheManager {

    private static MessageDigest md;

    private static String FILE_NAME_TO_HASH_FILE = "filenametohash.properties";

    private Properties filenameToHash;

    protected File dir;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public SimpleOxygenCacheManager() throws Exception {
        this(new File(System.getProperty("java.io.tmpdir"), "simple_data_cache"));
    }

    public SimpleOxygenCacheManager(File dir0) throws Exception {
        dir = dir0;
        filenameToHash = new Properties();
        File f0 = new File(dir, FILE_NAME_TO_HASH_FILE);
        if (f0.exists()) {
            InputStream is = new FileInputStream(f0);
            try {
                filenameToHash.load(is);
            } finally {
                CloseUtils.close(is);
            }
        }
    }

    public void prepare() throws Exception {
    }

    public void close() {
    }

    public void put(String group, String key, Object value) {
        ObjectOutputStream oos = null;
        try {
            File f = new File(getGroupDir(group), getFilename(key));
            oos = new ObjectOutputStream(new FileOutputStream(f));
            oos.writeObject(value);
            oos.flush();
        } catch (Exception exc) {
            OxygenUtils.error(exc);
        } finally {
            CloseUtils.close(oos);
        }
    }

    public Object get(String group, String key) {
        ObjectInputStream ois = null;
        Object value = null;
        try {
            File f = new File(getGroupDir(group), getFilename(key));
            if (f.exists()) {
                ois = new ObjectInputStream(new FileInputStream(f));
                value = ois.readObject();
            }
        } catch (Exception exc) {
            OxygenUtils.error(exc);
        } finally {
            CloseUtils.close(ois);
        }
        return value;
    }

    public Map getAll(String group, Pattern keyMatches) {
        Map m = new HashMap();
        for (Iterator itr = filenameToHash.keySet().iterator(); itr.hasNext(); ) {
            String s = (String) itr.next();
            if (keyMatches == null || keyMatches.matcher(s).matches()) {
                Object o = get(group, s);
                if (o != null) {
                    m.put(s, o);
                }
            }
        }
        return m;
    }

    public void remove(String group, String key) {
        try {
            File f = new File(getGroupDir(group), getFilename(key));
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception exc) {
            OxygenUtils.error(exc);
        }
    }

    private String getFilename(String key) throws Exception {
        String key0 = getHash(key) + ".ser";
        return key0;
    }

    protected synchronized String getHash(String key) throws Exception {
        String s = (String) filenameToHash.get(key);
        if (s == null) {
            s = getMD5Hash(key) + "." + String.valueOf(key.hashCode());
            filenameToHash.put(key, s);
            FileOutputStream fos = new FileOutputStream(new File(dir, FILE_NAME_TO_HASH_FILE));
            try {
                filenameToHash.store(fos, null);
            } finally {
                CloseUtils.close(fos);
            }
        }
        return s;
    }

    private static String getMD5Hash(String key) {
        byte[] bytes = md.digest(key.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append((int) (0x00FF & bytes[i]));
            if (i + 1 < bytes.length) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    public void clear() {
        try {
            OxygenUtils.deleteFile(dir, false);
        } catch (RuntimeException rexc) {
            throw rexc;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private File getGroupDir(String group) throws Exception {
        File f = new File(dir, getHash(group));
        f.mkdirs();
        return f;
    }
}
