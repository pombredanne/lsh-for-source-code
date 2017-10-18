package com.t5mobile.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {

    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

    public static String getHexString(byte[] raw) throws UnsupportedEncodingException {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }

    public static String getMD5AsHexString(String s) {
        if (s == null) return null;
        StringBuffer b = new StringBuffer();
        try {
            b.append(StringUtils.getHexString(getMD5(s)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return b.toString();
    }

    public static String getMD5AsBase64String(String s) {
        if (s == null) return null;
        return Base64RFC3548.encodeBytes(getMD5(s)).replace('=', ' ').trim();
    }

    public static byte[] getMD5(String s) {
        byte[] hash = new byte[0];
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            hash = digest.digest(s.getBytes("utf-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return hash;
    }

    public static String sanitizePath(String spath) {
        if (spath == null) return null;
        while (spath.indexOf("//") >= 0) {
            spath = spath.replaceAll("//", "/");
        }
        return spath;
    }
}
