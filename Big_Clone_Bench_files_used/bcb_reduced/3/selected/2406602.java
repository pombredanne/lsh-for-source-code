package com.spikesource.lam.xmlrpc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

/**
 * Helper class to generate MD5
 *
 * Code snippet copied from Apache commons-net file POP3Client.java
 */
public class MD5 {

    public static String MD5digest() {
        String timestamp = Long.toString(new Date().getTime());
        Random randomizer = new Random();
        long random_bits = randomizer.nextLong();
        timestamp += Long.toString(random_bits);
        try {
            return MD5.MD5digest(timestamp);
        } catch (NoSuchAlgorithmException nsae) {
            return timestamp;
        }
    }

    /**
     * Generates MD5 digest based on current timestamp
     *
     * @return MD5 string
     * @exception NoSuchAlgorithmException
     */
    public static String MD5digest(String timestamp) throws NoSuchAlgorithmException {
        int i;
        byte[] digest;
        StringBuffer buffer, digestBuffer;
        MessageDigest md5;
        md5 = MessageDigest.getInstance("MD5");
        digest = md5.digest(timestamp.getBytes());
        digestBuffer = new StringBuffer(128);
        for (i = 0; i < digest.length; i++) digestBuffer.append(Integer.toHexString(digest[i] & 0xff));
        return digestBuffer.toString();
    }
}
