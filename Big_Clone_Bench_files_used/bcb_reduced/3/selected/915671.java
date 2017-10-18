package cn.jsprun.utils;

import java.security.MessageDigest;

public final class Md5Token {

    private static Md5Token instance = null;

    private Md5Token() {
    }

    public static synchronized Md5Token getInstance() {
        if (instance == null) {
            instance = new Md5Token();
        }
        return instance;
    }

    public String getShortToken(String md5Str) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(md5Str.getBytes(JspRunConfig.charset));
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuffer token = toHex(md5.digest());
        return token.substring(8, 24);
    }

    public String getLongToken(String md5Str) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(md5Str.getBytes(JspRunConfig.charset));
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuffer token = toHex(md5.digest());
        return token.toString();
    }

    private StringBuffer toHex(byte[] bytes) {
        StringBuffer str = new StringBuffer();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            str.append(Character.forDigit((bytes[i] & 0xf0) >> 4, 16));
            str.append(Character.forDigit((bytes[i] & 0x0f), 16));
        }
        bytes = null;
        return str;
    }
}
