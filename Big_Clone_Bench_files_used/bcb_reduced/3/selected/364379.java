package org.t2framework.oneclick.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserUtils {

    private static final String CHARSET = "UTF-8";

    public static String createHash(String seed) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Can't happen!", e);
        }
        try {
            md.update(seed.getBytes(CHARSET));
            md.update(String.valueOf(System.currentTimeMillis()).getBytes(CHARSET));
            return toHexString(md.digest());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Can't happen!", e);
        }
    }

    private static String toHexString(byte[] buf) {
        String digestText = "";
        for (int i = 0; i < buf.length; i++) {
            int n = buf[i] & 0xff;
            if (n < 16) {
                digestText += "0";
            }
            digestText += Integer.toHexString(n).toUpperCase();
        }
        return digestText;
    }
}
