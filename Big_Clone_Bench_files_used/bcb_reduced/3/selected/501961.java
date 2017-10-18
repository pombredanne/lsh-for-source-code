package com.cosmos.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Miro
 */
public class SecurityUtils {

    private static final char[] saltChars = new char[] { '!', 'b', '0', 'z', 'h', 'o' };

    private static String saltPassword(String password) {
        StringBuffer sb = new StringBuffer();
        char[] chars = password.toCharArray();
        int crystal = 0;
        for (int i = 0; i < chars.length; i++) {
            sb.append(chars[i]);
            if (i % 2 == 0 && crystal < saltChars.length) {
                sb.append(saltChars[crystal++]);
            }
        }
        return sb.toString();
    }

    public static String getHash(String password) {
        if (password == null || password.length() == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            password = saltPassword(password);
            digest.update(password.getBytes());
            String result = getHexString(digest.digest());
            return result;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String getHexString(byte[] array) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            hexString.append(Integer.toHexString(0xFF & array[i]));
        }
        return hexString.toString().toLowerCase();
    }
}
