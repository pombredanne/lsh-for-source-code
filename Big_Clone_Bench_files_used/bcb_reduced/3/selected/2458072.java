package com.simpleblog.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加密器
 * @author hxl
 * @date 2011-11-15下午11:44:11
 */
public final class Encipher {

    private static MessageDigest MD5 = null;

    static {
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 通过md5加密
	 * @param plaintext 明文
	 * @return 16进制密文
	 */
    public static String encryptByMD5(String plaintext) {
        MD5.reset();
        MD5.update(plaintext.getBytes());
        byte[] digest = MD5.digest();
        return byteToHexString(digest);
    }

    /**
	 * 将一个字节数组转化为16进制字符串
	 * @param b
	 * @return
	 */
    private static String byteToHexString(byte[] b) {
        assert b != null : "b is null";
        StringBuilder hex = new StringBuilder();
        for (byte bb : b) {
            hex.append(Integer.toHexString(0xFF & bb).toUpperCase());
        }
        return hex.toString();
    }

    public static void main(String[] args) {
        System.out.println(Encipher.encryptByMD5("123"));
    }
}
