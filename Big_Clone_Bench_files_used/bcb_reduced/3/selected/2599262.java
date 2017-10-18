package org.ss.psci.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加密�??
 * 
 * 对字符串进行MD5加密
 * 
 * @see java.security.MessageDigest
 * @see java.security.NoSuchAlgorithmException
 * @version V01-00
 * @since V01-00
 */
public class Encrypt {

    /**
	 * 系统上下文属性設定る
	 * 
	 * @param str
	 *            未加密字符串
	 * @return String 已加密字符串
	 * @exception なし
	 * @since 01-00
	 */
    public static String crypt(String str) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("String to encript cannot be null or zero length");
        }
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] hash = md.digest();
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }

    public static void main(String[] a) {
        System.out.print(Encrypt.crypt("1"));
    }
}
