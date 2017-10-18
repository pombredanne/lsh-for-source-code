package org.lc.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * ������MD5�����㷨��ʵ�֣� ������java�����㷨����Ҫ����java.security.MessageDigest
 *  
 * 
 */
public class MD5Util {

    private static char[] DigitLower = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static char[] DigitUpper = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
	 * Ĭ�Ϲ��캯��
	 * 
	 */
    public MD5Util() {
    }

    /**
	 * ����֮����ַ�ȫΪСд
	 * 
	 * @param srcStr
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NullPointerException
	 */
    public static String getMD5Lower(String srcStr) throws NoSuchAlgorithmException, NullPointerException {
        String sign = "lower";
        return processStr(srcStr, sign);
    }

    private static String processStr(String srcStr, String sign) throws NoSuchAlgorithmException, NullPointerException {
        if (null == srcStr) {
            throw new java.lang.NullPointerException("��Ҫ���ܵ��ַ�ΪNull");
        }
        MessageDigest digest;
        String algorithm = "MD5";
        String result = "";
        digest = MessageDigest.getInstance(algorithm);
        digest.update(srcStr.getBytes());
        byte[] byteRes = digest.digest();
        int length = byteRes.length;
        for (int i = 0; i < length; i++) {
            result = result + byteHEX(byteRes[i], sign);
        }
        return result;
    }

    /**
	 * ��btye����ת�����ַ�
	 * 
	 * @param bt
	 * @return
	 */
    private static String byteHEX(byte bt, String sign) {
        char[] temp = null;
        if (sign.equalsIgnoreCase("lower")) {
            temp = DigitLower;
        } else if (sign.equalsIgnoreCase("upper")) {
            temp = DigitUpper;
        } else {
            throw new java.lang.NullPointerException("����ȱ�ٱ�Ҫ������");
        }
        char[] ob = new char[2];
        ob[0] = temp[(bt >>> 4) & 0X0F];
        ob[1] = temp[bt & 0X0F];
        return new String(ob);
    }

    public static void main(String args[]) throws NoSuchAlgorithmException, NullPointerException {
        System.out.println(MD5Util.getMD5Lower("admin"));
    }
}
