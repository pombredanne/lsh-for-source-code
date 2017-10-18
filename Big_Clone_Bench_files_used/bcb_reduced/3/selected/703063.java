package uk.ac.ebi.pride.data.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5Utils provides static method
 * <p/>
 * User: rwang
 * Date: 24/06/11
 * Time: 10:01
 */
public class MD5Utils {

    /**
     * Generate md5 hash from a given string
     *
     * @param msg input string
     * @return String  md5 hash
     * @throws java.security.NoSuchAlgorithmException java.security.NoSuchAlgorithmException
     *
     */
    public static String generateHash(String msg) throws NoSuchAlgorithmException {
        if (msg == null) {
            throw new IllegalArgumentException("Input string can not be null");
        }
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(msg.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        String hashText = bigInt.toString(16);
        while (hashText.length() < 32) {
            hashText = "0" + hashText;
        }
        return hashText;
    }
}
