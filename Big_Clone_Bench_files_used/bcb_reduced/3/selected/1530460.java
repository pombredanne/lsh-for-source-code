package org.scohen.juploadr.uploadapi.zooomr;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringSigner {

    private static MessageDigest md5;

    private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public static String md5(String toSign) {
        byte[] stringBytes;
        try {
            stringBytes = toSign.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            stringBytes = toSign.getBytes();
        }
        byte[] hashed = md5.digest(stringBytes);
        StringBuffer rv = new StringBuffer();
        for (int i = 0; i < hashed.length; i++) {
            rv.append(hexChars[0x00000F & hashed[i] >> 4]);
            rv.append(hexChars[0x00000F & hashed[i]]);
        }
        return rv.toString();
    }

    public static void main(String[] args) {
        System.out.println(StringSigner.md5("SECRETbar2baz3foo1"));
    }
}
