package uturismu;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Random;

/**
 * @author "LagrecaSpaccarotella" team.
 * 
 */
public class HashUtil {

    private static final String SHA_512 = "SHA-512";

    /**
	 * Generates a random string
	 */
    public static String generateSalt() {
        return Long.toHexString(new Random(System.nanoTime()).nextLong());
    }

    /**
	 * Gets a salted hash code.
	 */
    public static String getHash(String password, String salt) {
        try {
            MessageDigest messageDigest = null;
            messageDigest = MessageDigest.getInstance(SHA_512);
            messageDigest.reset();
            messageDigest.update(salt.getBytes("UTF-8"));
            messageDigest.update(password.getBytes("UTF-8"));
            byte[] input = messageDigest.digest();
            for (int i = 0; i < 1000; i++) {
                messageDigest.reset();
                input = messageDigest.digest(input);
            }
            Formatter formatter = new Formatter();
            for (byte i : input) {
                formatter.format("%02x", i);
            }
            return formatter.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
