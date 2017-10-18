package com.herestudio.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encodes a string using MD5 hashing
 * 
 * @author Rafael Steil
 * @version $Id: MD5.java,v 1.6 2005/07/26 03:05:26 rafaelsteil Exp $
 */
public class MD5 {

    /**
	 * Encodes a string
	 * 
	 * @param str
	 *            String to encode
	 * @return Encoded String
	 * @throws NoSuchAlgorithmException
	 */
    public static String encrypt(String str) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("String to encript cannot be null or zero length");
        }
        StringBuffer hexString = new StringBuffer();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(str.getBytes());
        byte[] hash = md.digest();
        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }
        return hexString.toString();
    }

    public static void main(String[] args) {
        String str = MD5.encrypt("luzm");
        System.out.println(str);
    }
}
