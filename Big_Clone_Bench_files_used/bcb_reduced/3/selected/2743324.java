package genesis.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

public class SecurityUtil {

    private static final String ALGORITHM_MD5 = "MD5";

    public static byte[] encrypt(String string, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        byte[] bytesArray = string.getBytes();
        return messageDigest.digest(bytesArray);
    }

    public static byte[] encryptWithMD5(String string) throws NoSuchAlgorithmException {
        return encrypt(string, ALGORITHM_MD5);
    }

    public static String encodeWithBase64(byte[] bytesArray) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(bytesArray);
    }
}
