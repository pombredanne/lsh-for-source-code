package com.easyblog.core.util;

import java.security.MessageDigest;

public class MD5Utils {

    public static String getAsMD5(String text) throws Exception {
        byte[] theTextToDigestAsBytes = text.getBytes("8859_1");
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(theTextToDigestAsBytes);
        byte[] digest = md.digest();
        StringBuffer result = new StringBuffer();
        for (byte b : digest) {
            result.append(Integer.toHexString(b & 0xff));
        }
        return result.toString();
    }

    public static String decodeUTF8String(String text) {
        if (text == null) {
            return null;
        }
        if (text.equals("")) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        try {
            int lastPos = 0;
            int index = text.indexOf("\\u");
            while (index >= 0) {
                result.append(text.substring(lastPos, index));
                result.append((char) Integer.parseInt(text.substring(index + 2, index + 6), 16));
                lastPos = index + 6;
                index = text.indexOf("\\u", index + 2);
            }
            result.append(text.substring(lastPos));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static String encodeUTF8String(String text) {
        return encodeUTF8String(text, false);
    }

    public static String encodeUTF8String(String text, boolean sqlEscaped) {
        if (text == null) {
            return null;
        }
        if (text.equals("")) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        try {
            for (int i = 0; i < text.length(); i++) {
                String symbol = Integer.toString(text.charAt(i), 16);
                if (symbol.length() == 1) {
                    symbol = "000" + symbol;
                } else if (symbol.length() == 2) {
                    symbol = "00" + symbol;
                } else if (symbol.length() == 3) {
                    symbol = "0" + symbol;
                }
                if (!sqlEscaped) {
                    result.append("\\u");
                }
                result.append(symbol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
