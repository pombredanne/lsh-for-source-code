package it.jwallpaper.plugins.vladstudio.util;

import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CryptoUtils {

    private static Log logger = LogFactory.getLog(CryptoUtils.class);

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encryptAsHex(String string) {
        return asHex(encrypt(string));
    }

    public static byte[] encrypt(String string) {
        java.security.MessageDigest messageDigest = null;
        try {
            messageDigest = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exc) {
            logger.fatal(exc);
            throw new RuntimeException();
        }
        messageDigest.reset();
        messageDigest.update(string.getBytes());
        return messageDigest.digest();
    }

    public static String asHex(byte hash[]) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }
}
