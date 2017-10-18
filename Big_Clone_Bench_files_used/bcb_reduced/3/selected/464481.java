package com.shengyijie.util;

import java.security.MessageDigest;

public class SHA256 {

    /**
     * ���ַ����,�����㷨ʹ��MD5,SHA-1,SHA-256,Ĭ��ʹ��SHA-256
     * 
     * @param strSrc
     *            Ҫ���ܵ��ַ�
     * @param encName
     *            ��������
     * @return
     */
    public static String toSHA256(String strSrc) {
        MessageDigest md = null;
        String strDes = null;
        byte[] bt = strSrc.getBytes();
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(bt);
            strDes = bytes2Hex(md.digest());
        } catch (Exception e) {
            return null;
        }
        return strDes;
    }

    public static String bytes2Hex(byte[] bts) {
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
