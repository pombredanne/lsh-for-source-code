package net.sourceforge.customercare.server.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * creates md5 hashes
 */
public class Md5 {

    public static String md5(String text) {
        String encrypted = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
            encrypted = hex(md.digest());
        } catch (NoSuchAlgorithmException nsaEx) {
        }
        return encrypted;
    }

    private static String hex(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; ++i) {
            sb.append(Integer.toHexString((data[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3));
        }
        return sb.toString();
    }
}
