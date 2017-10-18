package com.adpython.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class StrUtil {

    static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

    public static boolean empty(String s) {
        if (s == null || s.trim().equals("")) return true;
        return false;
    }

    public static boolean empty(Object obj) {
        if (obj == null || obj.toString().trim().equals("")) return true;
        return false;
    }

    public static int parseInt(String s) {
        return parseInt(s, 0);
    }

    public static int parseInt(String s, int iDefault) {
        if (s == null || s.equals("")) return iDefault;
        if (s.equals("true")) return 1;
        if (s.equals("false")) return 0;
        try {
            s = s.replaceAll(",", "");
            int l = s.indexOf(".");
            if (l > 0) s = s.substring(0, l);
            return Integer.parseInt(s);
        } catch (Exception e) {
            return iDefault;
        }
    }

    public static Cookie getCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public static String md5(byte[] in) {
        MessageDigest md5 = null;
        try {
            if (md5 == null) md5 = MessageDigest.getInstance("MD5");
            md5.update(in);
            byte[] theDigest = md5.digest();
            if (theDigest == null) return null;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < theDigest.length; i++) {
                sb.append(toUnsignedString(theDigest[i] & 0x00FF, 4, 2));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String toUnsignedString(int i, int shift, int width) {
        char[] buf = new char[8];
        Arrays.fill(buf, digits[0]);
        int charPos = 8;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = digits[i & mask];
            i >>>= shift;
        } while (i != 0);
        return new String(buf, Math.min(charPos, 8 - width), Math.max(8 - charPos, width));
    }
}
