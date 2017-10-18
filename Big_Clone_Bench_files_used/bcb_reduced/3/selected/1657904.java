package Helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class used to perform some typical operations on String objects.
 * 
 * @author roger
 */
public class Strings {

    /**
     * Encodes text with given algorithm
     * 
     * @param text Text to encode
     * @param alg Given algorithm to encode with
     * @return encoded text
     */
    public static String encode(String text, String alg) {
        byte[] passwordBytes = text.getBytes();
        String encoded = null;
        try {
            MessageDigest algorithm = MessageDigest.getInstance(alg);
            algorithm.reset();
            algorithm.update(passwordBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            encoded = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return encoded;
    }
}
