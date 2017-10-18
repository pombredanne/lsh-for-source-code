package com.juliashine.common.util;

import java.util.*;

/**
 * MD5���㷨��RFC1321 �ж���
 * ��RFC 1321�У������Test suite�����������ʵ���Ƿ���ȷ�� 
 * MD5 ("") = d41d8cd98f00b204e9800998ecf8427e 
 * MD5 ("a") = 0cc175b9c0f1b6a831c399e269772661 
 * MD5 ("abc") = 900150983cd24fb0d6963f7d28e17f72 
 * MD5 ("message digest") = f96b697d7cb7938d525a2f31aaf161d0 
 * MD5 ("abcdefghijklmnopqrstuvwxyz") = c3fcd3d76192e4007dfb496cca67e13b 
 * 
 * @author haogj
 *
 * �������һ���ֽ�����
 * ���������ֽ������ MD5 ����ַ�
 */
public class MD5Util {

    public static String getMD5Str(String source) {
        String s = null;
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
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

    public static byte[] getMD5(String source) {
        byte[] tmp = null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
            tmp = md.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public static void main(String args[]) {
        System.out.println(MD5Util.getMD5Str("__cust=00000000CB4AEA4C586BC002022E1403"));
    }
}
