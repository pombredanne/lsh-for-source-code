package ranab.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * String encryption utility methods.
 *
 * @author <a href="mailto:rana_b@yahoo.com">Rana Bhattacharyya</a>
 */
public class EncryptUtils {

    /**
     * Encrypt byte array.
     */
    public static byte[] encrypt(byte[] source, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.reset();
        md.update(source);
        return md.digest();
    }

    /**
     * Encrypt string
     */
    public static String encrypt(String source, String algorithm) throws NoSuchAlgorithmException {
        byte[] resByteArray = encrypt(source.getBytes(), algorithm);
        return StringUtils.toHexString(resByteArray);
    }

    /**
     * Encrypt string using MD5 algorithm
     */
    public static String encryptMD5(String source) {
        if (source == null) {
            source = "";
        }
        String result = "";
        try {
            result = encrypt(source, "MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Encrypt string using SHA algorithm
     */
    public static String encryptSHA(String source) {
        if (source == null) {
            source = "";
        }
        String result = "";
        try {
            result = encrypt(source, "SHA");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return result;
    }
}
