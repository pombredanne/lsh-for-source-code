package com.metanology.mde.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Digest {

    private static final char[] hexTab = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String getDigest(String input) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(input.getBytes());
        byte[] outDigest = md5.digest();
        StringBuffer outBuf = new StringBuffer(33);
        for (int i = 0; i < outDigest.length; i++) {
            byte b = outDigest[i];
            int hi = (b >> 4) & 0x0f;
            outBuf.append(MD5Digest.hexTab[hi]);
            int lo = b & 0x0f;
            outBuf.append(MD5Digest.hexTab[lo]);
        }
        return outBuf.toString();
    }
}
