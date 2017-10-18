package org.yehongyu.websale.common.util;

import java.security.MessageDigest;

/**
 * ����˵������ȡ�ִ�MD5����
 * @author yehongyu.org
 * @version 1.0 2007-11-11 ����11:06:41
 */
public class MyMD5 {

    public MyMD5() {
        super();
    }

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /**
	 * �������ܡ�ת���ֽ�����Ϊ16�����ִ�
	 * @param b �ֽ�����
	 * @return 16�����ִ�
	 */
    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    /**
	 * �������ܡ����ִ�����MD5����
	 * @param origin
	 * @return ���ܺ���ִ�
	 */
    public static String MD5Encode(String origin) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
        } catch (Exception ex) {
        }
        return resultString;
    }

    public static void main(String[] args) {
        System.err.println(MD5Encode("1"));
    }
}
