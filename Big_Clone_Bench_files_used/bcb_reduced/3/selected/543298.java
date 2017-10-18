package dalvik.runner;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.logging.Logger;

/**
 * Caches content by MD5.
 */
public final class Md5Cache {

    private static final Logger logger = Logger.getLogger(Md5Cache.class.getName());

    private static final File CACHE_ROOT = new File("/tmp/vogar-md5-cache/");

    private final String keyPrefix;

    /**
     * Creates a new cache accessor. There's only one directory on disk, so 'keyPrefix' is really
     * just a convenience for humans inspecting the cache.
     */
    public Md5Cache(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * Returns an ASCII hex representation of the MD5 of the content of 'file'.
     */
    private static String md5(File file) {
        byte[] digest = null;
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[8192];
            FileInputStream in = new FileInputStream(file);
            try {
                int byteCount;
                while ((byteCount = in.read(bytes)) > 0) {
                    digester.update(bytes, 0, byteCount);
                }
                digest = digester.digest();
            } finally {
                in.close();
            }
        } catch (Exception cause) {
            throw new RuntimeException("Unable to compute MD5 of \"" + file + "\"", cause);
        }
        return (digest == null) ? null : byteArrayToHexString(digest);
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toHexString((b >> 4) & 0xf));
            result.append(Integer.toHexString(b & 0xf));
        }
        return result.toString();
    }

    /**
     * Returns the appropriate key for a dex file corresponding to the contents of 'classpath'.
     * Returns null if we don't think it's possible to cache the given classpath.
     */
    public File makeKey(Classpath classpath) {
        String key = keyPrefix;
        for (File element : classpath.getElements()) {
            if (!element.toString().endsWith(".jar")) {
                return null;
            }
            key += "-" + md5(element);
        }
        return new File(CACHE_ROOT, key);
    }

    /**
     * Copy the file 'content' into the cache with the given 'key'.
     * This method assumes you're using the appropriate key for the content (and has no way to
     * check because the key is a function of the inputs that made the content, not the content
     * itself).
     * We accept a null so the caller doesn't have to pay attention to whether we think we can
     * cache the content or not.
     */
    public void insert(File key, File content) {
        if (key == null) {
            return;
        }
        logger.fine("inserting " + key);
        if (!key.toString().startsWith(CACHE_ROOT.toString())) {
            throw new IllegalArgumentException("key '" + key + "' not a valid cache key");
        }
        new Mkdir().mkdirs(CACHE_ROOT);
        File temporary = new File(key + ".tmp");
        new Command.Builder().args("cp", content, temporary).execute();
        new Command.Builder().args("mv", temporary, key).execute();
    }
}
