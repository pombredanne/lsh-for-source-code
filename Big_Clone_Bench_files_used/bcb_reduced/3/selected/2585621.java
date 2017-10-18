package com.pioneer.app.util;

import java.security.MessageDigest;

/**
 * @author pioneer
 * 口令加密md5算法。
 *
 */
public final class MD5Encode {

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /**
	 * ת���ֽ�����Ϊ16�����ִ�
	 * 
	 * @param b
	 *            �ֽ�����
	 * @return 16�����ִ�
	 */
    private static String byteArrayToHexString(byte[] b) {
        StringBuffer resSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resSb.append(byteToHexString(b[i]));
        }
        return resSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public static String getEncode(String str) {
        String resStr = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            resStr = byteArrayToHexString(md.digest(str.getBytes()));
        } catch (Exception ex) {
        }
        return resStr;
    }

    public static String getNewPWD(String userId, String pwd) {
        return getEncode(userId.toUpperCase() + ":" + pwd);
    }

    public static boolean Validate(String sourceStr, String validStr) {
        boolean res = false;
        if (validStr.equals(getEncode(sourceStr))) {
            res = true;
        }
        return res;
    }

    public static void main(String[] args) {
        String sourceStr = "abcdfjfhsj这里是dfh22222sdjfsjdfdjf" + "abc";
        String str = getEncode(sourceStr);
        System.out.println(sourceStr + "<------>" + str);
        System.out.println("<------>" + str.length());
        if (Validate(sourceStr, str)) {
            System.out.println("OK");
        }
        String userId = "superuser";
        String pwd = "superuser";
        System.out.println(MD5Encode.getEncode(userId + ":" + pwd));
        System.out.println("newpwd=" + getNewPWD("superuser", "cltr2008"));
        System.out.println("ootc pwd=" + getNewPWD("ootc", "12345"));
    }
}
