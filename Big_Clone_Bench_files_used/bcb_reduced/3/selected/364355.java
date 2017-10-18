package com.iclotho.foundation.pub.util;

import java.security.MessageDigest;

public class SecurityUtil {

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    public static String byte2hex(byte[] b) {
        String hexstr = "";
        String tmp = "";
        for (int n = 0; n < b.length; n++) {
            tmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (tmp.length() == 1) hexstr = hexstr + "0" + tmp; else hexstr = hexstr + tmp;
        }
        return hexstr.toUpperCase();
    }

    public static String MD5(String origin) {
        String resultString = null;
        if (origin == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(origin.getBytes()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultString;
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public static void main(String[] args) {
        SecurityUtil sec = new SecurityUtil();
        String ourt = sec.MD5("iclotho");
        try {
            String enc = sec.MD5("iclotho");
            System.out.println("enc=" + enc);
        } catch (Exception e) {
        }
    }
}
