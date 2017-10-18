package com.market.b2c.suport.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ��ϢժҪ������ �ṩ���õ�MD5���� author: zhangde date: Sep 8, 2009
 */
public class MessageDigestUtils {

    /**
	 * MD5���� 32λ
	 * @param plainText
	 */
    public static String Md5By32(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String Md5By16(String plainText) {
        return Md5By32(plainText).substring(8, 24);
    }

    public static void main(String[] args) {
        MessageDigestUtils.Md5By32("zd");
    }
}
