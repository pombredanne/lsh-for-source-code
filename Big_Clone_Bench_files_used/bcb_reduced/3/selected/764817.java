package com.mlib.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtil {

    private static final String hexits = "0123456789abcdef";

    private static byte[] Md5f(String plainText) {
        byte[] ab = new byte[16];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();
            ab = b;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return ab;
    }

    private static String toHex(byte block[]) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < block.length; i++) {
            buf.append(hexits.charAt(block[i] >>> 4 & 0xf));
            buf.append(hexits.charAt(block[i] & 0xf));
        }
        return String.valueOf(String.valueOf(buf));
    }

    public static String getMd5(String str) {
        byte[] mp = Md5f(str);
        return toHex(mp);
    }

    public static BigInteger getSHA(String str) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA");
        sha.update(str.getBytes());
        return new BigInteger(sha.digest());
    }
}
