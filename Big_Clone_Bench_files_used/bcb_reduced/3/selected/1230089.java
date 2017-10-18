package com.astromine.crypt;

import java.security.*;
import java.util.Formatter;

public class Hash {

    private static Hash instance;

    public static synchronized byte[] md5(byte[] source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(source);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String md5(String source) {
        try {
            byte[] in = source.getBytes("UTF-8");
            byte[] out = md5(in);
            Formatter format = new Formatter();
            for (int i = 0; i < out.length; i++) {
                format.format("%02x", out[i]);
            }
            return format.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static synchronized byte[] sha(byte[] source) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(source);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String sha(String source) {
        try {
            byte[] in = source.getBytes("UTF-8");
            byte[] out = sha(in);
            Formatter format = new Formatter();
            for (int i = 0; i < out.length; i++) {
                format.format("%02x", out[i]);
            }
            return format.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static synchronized Hash getInstance() {
        if (instance == null) {
            instance = new Hash();
        }
        return instance;
    }
}
