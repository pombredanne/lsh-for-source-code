package com.etc.digest;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author magicbank
 */
public class MD5 {

    private static String toHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString().toUpperCase();
    }

    public static String encode(String text) {
        try {
            byte[] hash = new byte[32];
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes("UTF-8"), 0, text.length());
            hash = md.digest();
            return MD5.toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return ex.getMessage();
        } catch (UnsupportedEncodingException ex) {
            return ex.getMessage();
        }
    }
}
