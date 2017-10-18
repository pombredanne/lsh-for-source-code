package com.zeromessenger.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Dummy class to create hashings.
 * @author Zeng Ziting
 *
 */
public class CryptoUtil {

    public static byte[] MD5(String input) {
        try {
            return MessageDigest.getInstance("MD5").digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Unable to create hash");
            return null;
        }
    }
}
