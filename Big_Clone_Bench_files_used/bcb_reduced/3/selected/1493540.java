package sun.jkernel;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class BundleCheck {

    private static final int DIGEST_STREAM_BUFFER_SIZE = 2048;

    private static final String BUNDLE_SUFFIX = ".zip";

    private static volatile Properties properties;

    /**
     * The bytes of the check value. Guarded by the bundle Mutex (in
     * sun.jkernel.DownloadManager) or the fact that sun.kernel.SplitJRE
     * and/or DownloadManager with "-download all" runs a single thread.
     */
    private byte[] checkBytes;

    private BundleCheck() {
    }

    /**
     * Store the bundle check values as properties to the path specified.
     * Only invoked by SplitJRE.
     */
    public static void storeProperties(String fullPath) {
        try {
            File f = new File(fullPath);
            f.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(f);
            properties.store(out, null);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException("BundleCheck: storing properties threw: " + e);
        }
    }

    /**
     * Fetch the check value properties as a DownloadManager resource.
     */
    private static void loadProperties() {
        properties = new Properties();
        try {
            InputStream in = new BufferedInputStream(DownloadManager.class.getResourceAsStream(DownloadManager.CHECK_VALUES_FILE));
            if (in == null) throw new RuntimeException("BundleCheck: unable to locate " + DownloadManager.CHECK_VALUES_FILE + " as resource");
            properties.load(in);
            in.close();
        } catch (Exception e) {
            throw new RuntimeException("BundleCheck: loadProperties threw " + e);
        }
    }

    private static synchronized Properties getProperties() {
        if (properties == null) {
            loadProperties();
        }
        return properties;
    }

    public static void resetProperties() {
        properties = null;
    }

    public String toString() {
        return ByteArrayToFromHexDigits.bytesToHexString(checkBytes);
    }

    private void addProperty(String name) {
        if (properties == null) {
            properties = new Properties();
        }
        getProperties().put(name, toString());
    }

    private BundleCheck(byte[] checkBytes) {
        this.checkBytes = checkBytes;
    }

    private BundleCheck(String name) {
        String hexString = getProperties().getProperty(name);
        if (hexString == null) {
            throw new RuntimeException("BundleCheck: no check property for bundle: " + name);
        }
        this.checkBytes = ByteArrayToFromHexDigits.hexStringToBytes(hexString);
    }

    private static BundleCheck getInstance(String name, File file, boolean saveProperty) {
        if (file == null) {
            return new BundleCheck(name);
        } else {
            StandaloneMessageDigest checkDigest = null;
            try {
                FileInputStream checkFileStream = new FileInputStream(file);
                checkDigest = StandaloneMessageDigest.getInstance("SHA-1");
                int readCount;
                byte[] messageStreamBuff = new byte[DIGEST_STREAM_BUFFER_SIZE];
                do {
                    readCount = checkFileStream.read(messageStreamBuff);
                    if (readCount > 0) {
                        checkDigest.update(messageStreamBuff, 0, readCount);
                    }
                } while (readCount != -1);
                checkFileStream.close();
            } catch (Exception e) {
                throw new RuntimeException("BundleCheck.addProperty() caught: " + e);
            }
            BundleCheck bc = new BundleCheck(checkDigest.digest());
            if (saveProperty) {
                bc.addProperty(name);
            }
            return bc;
        }
    }

    public static BundleCheck getInstance(File file) {
        return getInstance(null, file, false);
    }

    static BundleCheck getInstance(String name) {
        return getInstance(name, null, false);
    }

    public static void addProperty(String name, File file) {
        getInstance(name, file, true);
    }

    static void add(String name, File file) {
        getInstance(name, file, true).addProperty(name);
    }

    boolean equals(BundleCheck b) {
        if ((checkBytes == null) || (b.checkBytes == null)) {
            return false;
        }
        if (checkBytes.length != b.checkBytes.length) {
            return false;
        }
        for (int i = 0; i < checkBytes.length; i++) {
            if (checkBytes[i] != b.checkBytes[i]) {
                if (DownloadManager.debug) {
                    System.out.println("BundleCheck.equals mismatch between this: " + toString() + " and param: " + b.toString());
                }
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java BundleCheck <jre path> " + "<bundle 1 name> ... <bundle N name>");
            return;
        }
        for (int arg = 1; arg < args.length; arg++) {
            BundleCheck.addProperty(args[arg], new File(args[arg] + BUNDLE_SUFFIX));
        }
        BundleCheck.storeProperties(DownloadManager.CHECK_VALUES_DIR);
        try {
            int status = Runtime.getRuntime().exec("jar uf " + args[0] + "\\lib\\rt.jar " + DownloadManager.CHECK_VALUES_DIR).waitFor();
            if (status != 0) {
                System.err.println("BundleCheck: exec of jar uf gave nonzero status");
                return;
            }
        } catch (Exception e) {
            System.err.println("BundleCheck: exec of jar uf threw: " + e);
            return;
        }
    }
}
