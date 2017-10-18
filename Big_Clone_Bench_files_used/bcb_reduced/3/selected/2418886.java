package hu.bme.aait.picstore.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import sun.misc.BASE64Encoder;

public class TextUtil {

    /**
	 * Uses the MD5 hash algorithm and Base64 encoding to digest the password
	 * data for storage.
	 * 
	 * @param password
	 * @return
	 */
    public static String digest(String password) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            byte[] data = digest.digest(password.getBytes());
            BASE64Encoder encoder = new BASE64Encoder();
            return new String(encoder.encode(data));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Returns a random string consisting of letters and numbers with the given length
	 * 
	 * @param length
	 * @return
	 */
    public static String getRandomString(int length) {
        final String charset = "!0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rand = new Random(System.currentTimeMillis());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int pos = rand.nextInt(charset.length());
            sb.append(charset.charAt(pos));
        }
        return sb.toString();
    }
}
