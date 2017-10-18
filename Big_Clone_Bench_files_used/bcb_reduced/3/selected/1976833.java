package de.fuh.xpairtise.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Formatter;
import org.apache.log4j.Level;
import de.fuh.xpairtise.common.LogConstants;
import de.fuh.xpairtise.common.XPLog;

/**
 * an utility class to compute unique hash values from given input.
 */
public class GenerateHash {

    private static final String METHOD = "MD5";

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance(METHOD);
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_GENERATEHASH + "initialized digest: " + digest.getAlgorithm());
            }
        } catch (Exception e) {
            XPLog.logException(Level.WARN, 0, null, e);
        }
    }

    private GenerateHash() {
    }

    /**
   * returns the hash String for the given input String.
   * 
   * @param input
   *          the String to compute the hash value of
   * @return the hash value
   */
    public static synchronized String getHashOfString(String input) {
        return getHash(input.getBytes());
    }

    /**
   * returns the hash String for the file at the given path if it exists.
   * 
   * @param path
   *          the path of the file
   * @return the hash value of the file if it exists
   */
    public static synchronized String getHashOfFile(String path) {
        return getHashOfFile(new File(path));
    }

    /**
   * returns the hash String for the given file if it exists.
   * 
   * @param file
   *          the file to compute the hash for
   * @return the hash value of the file if it exists
   */
    public static synchronized String getHashOfFile(File file) {
        if (file.exists() && file.canRead()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                try {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = stream.read(buffer)) != -1) {
                        digest.update(buffer, 0, len);
                    }
                } finally {
                    stream.close();
                }
                byte[] hash = digest.digest();
                return hashToString(hash);
            } catch (Exception e) {
                XPLog.logException(Level.WARN, 0, null, e);
            }
        }
        return null;
    }

    /**
   * returns the hash String for the given byte array input.
   * 
   * @param input
   *          the input to compute the hash value of
   * @return the hash value of the input
   */
    public static synchronized String getHash(byte[] input) {
        digest.update(input);
        byte[] hash = digest.digest();
        return hashToString(hash);
    }

    private static String hashToString(byte[] hash) {
        StringBuffer result = new StringBuffer(digest.getDigestLength() * 2);
        Formatter f = new Formatter(result);
        for (byte b : hash) {
            f.format("%02x", b);
        }
        return result.toString();
    }
}
