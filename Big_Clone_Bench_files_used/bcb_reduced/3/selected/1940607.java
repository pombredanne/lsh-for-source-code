package com.googlecode.xmpplib.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {

    public static byte[] MD5(String... strings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            for (String string : strings) {
                digest.update(string.getBytes("UTF-8"));
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static byte[] MD5(byte[]... strings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            for (byte[] string : strings) {
                digest.update(string);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static byte[] SHA1(String... strings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            digest.reset();
            for (String string : strings) {
                digest.update(string.getBytes("UTF-8"));
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static String toHex(byte[] data) {
        StringBuffer hexString = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            hexString.append(Integer.toHexString((data[i] >>> 4) & 0x0F));
            hexString.append(Integer.toHexString(0x0F & data[i]));
        }
        return hexString.toString();
    }

    public static byte[] parseHex(String hexString) {
        byte[] data = new byte[hexString.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }
}
