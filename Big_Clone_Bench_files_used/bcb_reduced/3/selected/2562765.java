package com.afri.base.common;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/** 
 * ���ü����㷨������ 
 * @author cq 
 */
public class EncryptUtils {

    /** 
     * ��MD5�㷨���м��� 
     * @param str ��Ҫ���ܵ��ַ� 
     * @return MD5���ܺ�Ľ�� 
     */
    public static String encodeMD5String(String str) {
        return encode(str, "MD5");
    }

    /** 
     * ��SHA�㷨���м��� 
     * @param str ��Ҫ���ܵ��ַ� 
     * @return SHA���ܺ�Ľ�� 
     */
    public static String encodeSHAString(String str) {
        return encode(str, "SHA");
    }

    /** 
     * ��base64�㷨���м��� 
     * @param str ��Ҫ���ܵ��ַ� 
     * @return base64���ܺ�Ľ�� 
     */
    public static String encodeBase64String(String str) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(str.getBytes());
    }

    /** 
     * ��base64�㷨���н��� 
     * @param str ��Ҫ���ܵ��ַ� 
     * @return base64���ܺ�Ľ�� 
     * @throws IOException  
     */
    public static String decodeBase64String(String str) throws IOException {
        BASE64Decoder encoder = new BASE64Decoder();
        return new String(encoder.decodeBuffer(str));
    }

    private static String encode(String str, String method) {
        MessageDigest md = null;
        String dstr = null;
        try {
            md = MessageDigest.getInstance(method);
            md.update(str.getBytes());
            dstr = new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return dstr;
    }

    public static void main(String[] args) throws IOException {
        String user = "ԭʼ�ַ�";
        System.out.println("ԭʼ�ַ� " + user);
        System.out.println("MD5���� " + encodeMD5String(user).length());
        System.out.println("SHA���� " + encodeSHAString(user).length());
        String base64Str = encodeBase64String(user + ":" + encodeSHAString(user));
        System.out.println("Base64���� " + base64Str.length());
        System.out.println("Base64���� " + decodeBase64String(base64Str));
    }
}
