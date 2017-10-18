package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for handling passwords.
 * @author Ben Hill
 * @version 0.1
 */
public class PasswordUtils {

    private static String toHexString(byte[] hash) {
        StringBuffer sb = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            String hexString = Integer.toHexString(0xFF & hash[i]);
            if (hexString.length() < 2) {
                hexString = "0" + hexString;
            }
            sb.append(hexString);
        }
        return sb.toString();
    }

    /** Create a 32 character MD5 hash of a String. */
    public static String createHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] digest = md.digest();
            return toHexString(digest);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println(nsae.getMessage());
        }
        return "";
    }
}
