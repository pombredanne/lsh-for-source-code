package org.jxstar.util;

import org.jxstar.security.Password;

/**
 * 密码生成测试类。
 *
 * @author TonyTan
 * @version 1.0, 2010-11-23
 */
public class Md5Test {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        String pass = "888";
        System.out.println(Password.md5(pass));
        String old = "123456789abcdefghijklmnopqrstuvwxyz";
        String en = Password.encrypt(old);
        System.out.println(en);
        System.out.println(Password.decrypt(en));
        System.out.println(Md5Test.md5(old));
    }

    public static String md5(String source) {
        byte[] src = source.getBytes();
        String s = null;
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(src);
            byte tmp[] = md.digest();
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            s = new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }
}
