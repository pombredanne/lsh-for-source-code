package com.smth.infobox.utils;

import java.security.MessageDigest;

public class SecurityUtils {

    public static String toMD5(byte[] bytes) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(bytes);
            return toHexString(algorithm.digest(), "");
        } catch (Exception e) {
        }
        return null;
    }

    private static String toHexString(byte[] bytes, String separator) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(Integer.toHexString(0xFF & b)).append(separator);
        }
        return hexString.toString();
    }
}
