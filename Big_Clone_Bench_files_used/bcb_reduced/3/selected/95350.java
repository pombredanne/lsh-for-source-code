package br.com.goals.tableedit.util;

import java.security.*;

public class MD5 {

    private static String bytesToHex(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            sb.append((Integer.toHexString((b[i] & 0xFF) | 0x100)).substring(1, 3));
        }
        return sb.toString();
    }

    public static String toPassword(String data) throws NoSuchAlgorithmException {
        byte[] mybytes = data.getBytes();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md5digest = md5.digest(mybytes);
        return bytesToHex(md5digest);
    }
}
