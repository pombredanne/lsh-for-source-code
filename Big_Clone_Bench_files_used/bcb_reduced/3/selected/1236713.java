package com.peterhi.crypto;

import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import sun.misc.BASE64Encoder;

public class CryptoUtil {

    public static byte[] hash(String s) throws GeneralSecurityException {
        MessageDigest d = MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(s.getBytes());
        return d.digest();
    }

    public static String string(byte[] b) {
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

    public static byte[] bytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
}
