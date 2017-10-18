package com.littleqworks.commons.security;

import java.security.*;

public class Encryption {

    /**
	 * ȡ��ָ���ַ��MD5����.
	 * @param strSrc Դ�ַ�
	 * @return
	 */
    public static final String getMD5(String strSrc) {
        return get(strSrc, "MD5");
    }

    /**
 * ȡ��ָ���ַ��SHA-1����.
 * @param strSrc Դ�ַ�
 */
    public static final String getSHA1(String strSrc) {
        return get(strSrc, "SHA-1");
    }

    /**
 * ȡ��ָ���ַ��SHA-256����.
 * @param strSrc Դ�ַ�
 */
    public static final String getSHA256(String strSrc) {
        return get(strSrc, "SHA-256");
    }

    /**
	 * ȡ��ָ���ַ����ض�����ı���(MD5,SHA-1,SHA-256).
	 * @param strSrc
	 * @param encName
	 * @return
	 */
    public static final String get(String strSrc, String encName) {
        MessageDigest md = null;
        String strDes = null;
        byte[] bt = strSrc.getBytes();
        try {
            if (encName == null || encName.equals("")) {
                encName = "MD5";
            }
            md = MessageDigest.getInstance(encName);
            md.update(bt);
            strDes = bytes2Hex(md.digest());
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return null;
        }
        return strDes;
    }

    private static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }
}
