package br.com.sinapp.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Security {

    private static final String MD5 = "MD5";

    private static MessageDigest digest = null;

    static {
        try {
            digest = MessageDigest.getInstance(MD5);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    public static String getCrypto(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        StringBuffer hexString = new StringBuffer();
        digest.update(str.getBytes());
        byte[] hash = digest.digest();
        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }
        return hexString.toString();
    }
}
