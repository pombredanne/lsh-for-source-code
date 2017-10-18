package com.hyper9.common.text;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * A class for generated MD5 hashes.
 * 
 * @author akutz
 * 
 */
public class MD5 {

    /**
     * Computes an MD5 hash from plain text.
     * @param plainText The plain text.
     * @return The hash.
     * @throws Exception When an error occurs.
     */
    public static String hash(String plainText) throws Exception {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(plainText.getBytes(), 0, plainText.length());
        String hash = new BigInteger(1, m.digest()).toString(16);
        if (hash.length() == 31) {
            hash = "0" + hash;
        }
        return hash;
    }
}
