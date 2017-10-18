package org.zkoss.zreg.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encoder {

    public static final String enCode(String algorithm, String string) {
        MessageDigest md;
        String result = "";
        try {
            md = MessageDigest.getInstance(algorithm);
            md.update(string.getBytes());
            result = binaryToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static String binaryToString(byte[] digest) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < digest.length; ++i) {
            final byte b = digest[i];
            final int value = (b & 0x7F) + (b < 0 ? 128 : 0);
            buffer.append(value < 16 ? "0" : "");
            buffer.append(Integer.toHexString(value));
        }
        return buffer.toString();
    }
}
