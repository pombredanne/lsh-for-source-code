package br.com.agenda.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encrypt {

    private static final String MD5 = "MD5";

    private static final String SHA1 = "SHA-1";

    private static String encrypt(String password, String encryptType) {
        try {
            MessageDigest md = MessageDigest.getInstance(encryptType);
            md.update(password.getBytes());
            byte[] hash = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }
            password = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return password.toUpperCase();
    }

    public static String encryptMD5(String stringToEncrypt) {
        return encrypt(stringToEncrypt, MD5);
    }

    public static String encryptSHA(String stringToEncrypt) {
        return encrypt(stringToEncrypt, SHA1);
    }
}
