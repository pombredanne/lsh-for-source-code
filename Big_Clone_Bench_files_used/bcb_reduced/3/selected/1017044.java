package com.study.pepper.client.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UtilsMD5 {

    public static String code(String s) {
        byte[] content = s.getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(content);
            byte digest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            int digestLength = digest.length;
            for (int i = 0; i < digestLength; i++) {
                hexString.append(hexDigit(digest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String hexDigit(byte x) {
        StringBuffer sb = new StringBuffer();
        char c;
        c = (char) ((x >> 4) & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        c = (char) (x & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        return sb.toString();
    }
}
