package be.abeel.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Tools {

    /**
	 * Returns the hexadecimal representation of an MD5 hash.
	 *
	 * @param pass input string
	 * @return hashed string
	 */
    public static String md5(String pass) {
        return hex(calcMd5(pass));
    }

    /**
	 * make an md5 hash from a string
	 *
	 * @param pass string to be hashed
	 * @return the hash as an byte array
	 */
    private static byte[] calcMd5(String pass) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(pass.getBytes(), 0, pass.length());
            byte[] hash = digest.digest();
            return hash;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No MD5 algorithm found");
            System.exit(1);
        }
        return null;
    }

    /**
	 * Convert an array of bytes to an uppercase hexadecimal representation
	 *
	 * @param array a byte array
	 * @return the byte array as a hexadecimal string representation
	 */
    private static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3));
        }
        return sb.toString();
    }
}
