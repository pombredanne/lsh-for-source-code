package com.psm.core.commons;

import java.security.NoSuchAlgorithmException;

public class EncryptionService {

    public static String encrypt(String password) throws NoSuchAlgorithmException {
        java.security.MessageDigest d = null;
        d = java.security.MessageDigest.getInstance("MD5");
        d.reset();
        d.update(password.getBytes());
        byte[] cr = d.digest();
        return getString(cr).toLowerCase();
    }

    private static String getString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }
}
