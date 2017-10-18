package com.wangyu001.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

public class MD5Util {

    /**
     * 利用MD5进行加密
     */
    public static String encoder(String str) {
        MessageDigest md5;
        String newstr = str;
        try {
            md5 = MessageDigest.getInstance("MD5");
            BASE64Encoder base64en = new BASE64Encoder();
            newstr = base64en.encode(md5.digest(str.getBytes("utf-8")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return newstr;
    }

    /**
     * 密码验证
     */
    public static boolean validate(String newPwd, String oldPwd) {
        if (encoder(newPwd).equals(oldPwd)) return true; else return false;
    }
}
