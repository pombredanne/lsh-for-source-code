package com.khotyn.heresy.util;

import java.security.MessageDigest;

/**
 * MD5的工具类
 * @author 王长乐
 *
 */
public class MD5Util {

    /**
	 * 利用MD5进行加密
	 * @param str  待加密的字符串字节流
     * @return  加密后的字符串
	 *
	 */
    public static String EncoderByMd5(byte[] source) {
        String s = null;
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            java.security.MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source);
            byte tmp[] = md.digest();
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            s = new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static boolean checkpassword(String newpasswd, String oldpasswd) {
        if (EncoderByMd5(newpasswd.getBytes()).equals(oldpasswd)) return true; else return false;
    }

    public static void main(String args[]) {
        String str = "bcd";
        System.out.println(MD5Util.EncoderByMd5(str.getBytes()));
    }
}
