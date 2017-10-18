package com.rich.oa.util;

import java.io.PrintStream;
import java.security.MessageDigest;

public class MD5 {

    public MD5() {
    }

    public static String getMD5(byte source[]) {
        String s = null;
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source);
            byte tmp[] = md.digest();
            char str[] = new char[32];
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

    public static void main(String args[]) {
        String aaa = getMD5("888888".getBytes()).toLowerCase().substring(8, 24);
        System.out.println(aaa);
    }
}
