package de.pangaea.metadataportal.utils;

import java.security.*;

public final class HashGenerator {

    private HashGenerator() {
    }

    public static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toLowerCase().substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            return "hash" + message.hashCode();
        }
    }

    public static String sha1(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            return hex(md.digest(message.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            return "hash" + message.hashCode();
        }
    }
}
