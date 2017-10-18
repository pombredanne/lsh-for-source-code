package com.columboid.mailclient.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encoder {

    /**
     * This method return a String that has been encrypted as MD5 and then escaped using Base64.
     * @param input String need encrypted
     * @return String after encrypted
     */
    public static synchronized String getMD5_Base64(String input) {
        MessageDigest msgDigest = null;
        try {
            msgDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("System doesn't support MD5 algorithm.");
        }
        try {
            msgDigest.update(input.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("System doesn't support your  EncodingException.");
        }
        byte[] rawData = msgDigest.digest();
        byte[] encoded = Base64.encode(rawData);
        String retValue = new String(encoded);
        return retValue;
    }
}
