package mindmonkey.birtserver.datalayer.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This utility class allows to encrypt a block of text (typically a password)
 * using the SHA-512 algorithm to ensure passwords are not disclosed even in
 * case of a breach in security on the database that stores the passwords.
 * 
 * To encrypt a block of text use the sha512Encrypt("password") method.
 * 
 * @author Marco L. Buschini <mbuschini@users.sourceforge.net>
 * 
 */
public class ShaUtils {

    /**
	 * Converts an array of bytes into their hexadecimal text representation.
	 * 
	 * @param bytes
	 *            the array of bytes to convert to hexadecimal text.
	 * @return the hexadecimal representation of the byte array.
	 */
    public static String bytesToHexString(byte[] bytes) {
        String retString = "";
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i]);
            switch(hex.length()) {
                case 0:
                    hex = "00";
                    break;
                case 1:
                    hex = "0" + hex;
            }
            retString += hex.substring(hex.length() - 2).toUpperCase();
        }
        return retString;
    }

    /**
	 * Performs the one-way encryption of the given clear text.
	 * 
	 * @param clearText
	 *            The text to be encrypted.
	 * @return The encrypted text.
	 * @throws NoSuchAlgorithmException
	 */
    public static String sha512Encrypt(String clearText) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        return bytesToHexString(md.digest(clearText.getBytes()));
    }
}
