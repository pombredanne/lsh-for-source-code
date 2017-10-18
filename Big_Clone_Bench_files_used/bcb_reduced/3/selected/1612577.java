package com.yict.common.util;

import java.security.MessageDigest;

/**
 * 对密码进行加密和验证的类
 * 
 * @author Solex
 */
public class CipherUtil {

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /**
	 * 把inputPassword加密
	 * 
	 * @author Solex
	 */
    public static String generatePassword(String inputPassword) {
        return encodeByMD5(inputPassword);
    }

    /**
	 * 验证输入的密码是否正确
	 * 
	 * @param password
	 *            加密后的密码
	 * @param inputPassword
	 *            输入的字符串
	 * @return 验证结果，TRUE:正确 FALSE:错误
	 * @author Solex
	 */
    public static boolean validatePassword(String password, String inputPassword) {
        if (password.equals(encodeByMD5(inputPassword))) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * 对字符串进行MD5加密
	 * 
	 * @author Solex
	 */
    private static String encodeByMD5(String originString) {
        if (originString != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] results = md.digest(originString.getBytes());
                String resultString = byteArrayToHexString(results);
                return resultString.toUpperCase();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
	 * 转换字节数组为十六进制字符串
	 * 
	 * @param 字节数组
	 * @return 十六进制字符串
	 * @author Solex
	 */
    private static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    /**
	 * 将一个字节转化成十六进制形式的字符串
	 * 
	 * @param b
	 * @return
	 * @author Solex
	 */
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }
}
