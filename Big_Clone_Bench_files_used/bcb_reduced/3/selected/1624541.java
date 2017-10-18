package bgo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to hash a string with the SHA1 algorithm.
 * 
 * @author Matthias Becker, Sebastian Duevel, Observer
 *         (http://o9y.net/archives/2005/11/30/java-string-to-md5-hash/)
 * 
 */
public final class CryptUtil {

    /** static MessageDigest */
    private static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            System.exit(-1);
        }
    }

    /**
	 * overwrite the default constructor make checkstyle happy
	 */
    private CryptUtil() {
    }

    /**
	 * hashes a string
	 * 
	 * @param input
	 *            string
	 * @return hashed string
	 */
    public static String plainStringToSHA1(String input) {
        byte[] byteHash = null;
        StringBuffer resultString = new StringBuffer();
        md.reset();
        byteHash = md.digest(input.getBytes());
        for (int i = 0; i < byteHash.length; i++) {
            resultString.append(Integer.toHexString(0xFF & byteHash[i]));
        }
        return (resultString.toString());
    }
}
