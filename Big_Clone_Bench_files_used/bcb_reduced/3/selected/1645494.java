package com.hzzk.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author ���� 
 */
public class Encoder {

    private static final String MESSAGE_DIGEST_TYPE = "MD5";

    /**
	 * ���ܣ�MD5
	 * @param str
	 * @return
	 */
    public static String str2md5(String str) {
        try {
            MessageDigest alga = MessageDigest.getInstance(MESSAGE_DIGEST_TYPE);
            alga.update(str.getBytes());
            byte[] digesta = alga.digest();
            return byte2hex(digesta);
        } catch (NoSuchAlgorithmException ex) {
            return str;
        }
    }

    /**
	 * ������ת�ַ�
	 * @param b
	 * @return
	 */
    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) hs = hs + "0" + stmp; else hs = hs + stmp;
        }
        return hs.toUpperCase();
    }

    /**
	 * IP�ַ�ת���ɶ������ַ�
	 * @param ip
	 * @return
	 */
    public static String ip2hex(String ip) {
        byte[] b = new byte[4];
        String[] items = ip.split("[.]");
        for (int i = 0; i < 4; i++) {
            b[i] = str2byte(items[i]);
        }
        return byte2hex(b);
    }

    private static byte str2byte(String value) {
        int iValue = Integer.parseInt(value);
        if (iValue > Byte.MAX_VALUE) {
            return (byte) (iValue - 256);
        } else {
            return (byte) iValue;
        }
    }
}
