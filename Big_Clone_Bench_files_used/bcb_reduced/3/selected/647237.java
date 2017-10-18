package org.openmim.icq.util.joe;

import java.security.*;

public class MD5Util {

    public static String makeHash(byte[] password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password);
            return byteArrayToHexString(md.digest());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("" + ex);
        }
    }

    public static String makeHash(byte[] challenge, byte[] password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(challenge);
            md.update(password);
            return byteArrayToHexString(md.digest());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("" + ex);
        }
    }

    private static char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static String byteArrayToHexString(byte[] ba) {
        StringBuffer sb = new StringBuffer(ba.length << 1);
        for (int i = 0; i < ba.length; i++) {
            byte b = ba[i];
            sb.append(HEX_DIGITS[(b >> 4) & (byte) 0xf]).append(HEX_DIGITS[b & (byte) 0xf]);
        }
        return sb.toString();
    }
}
