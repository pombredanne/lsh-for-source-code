package data;

import java.security.*;
import java.math.*;

/**
 * Generates a MD5 hash of a string.
 *
 * @author willjasen
 */
public class MD5 {

    /**
	 * @param input -
	 *            string to get MD5 hash of
	 *
	 * @return MD5 hash of string
	 *
	 *
	 */
    public static String getHash(String input) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        m.update(input.getBytes(), 0, input.length());
        return new BigInteger(1, m.digest()).toString(16);
    }
}
