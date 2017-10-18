package util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Rick
 */
public class DigestHelper {

    public static String md5(String data) {
        byte[] bytes = getHashBytes(data, "MD5");
        return byteToHex(bytes);
    }

    public static String sha1(String data) {
        byte[] bytes = getHashBytes(data, "SHA-1");
        return byteToHex(bytes);
    }

    private static byte[] getHashBytes(String data, String algorithm) {
        MessageDigest md;
        byte[] digest = null;
        try {
            md = MessageDigest.getInstance(algorithm);
            md.update(data.getBytes("UTF-8"), 0, data.length());
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return digest;
    }

    private static String byteToHex(byte[] data) {
        BigInteger digestContent = new BigInteger(1, data);
        String hashText = digestContent.toString(16);
        while (hashText.length() < 32) {
            hashText = "0" + hashText;
        }
        return hashText;
    }
}
