package org.mil.util;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestUtil {

    public static String calcSHA1String(byte[] b) {
        return ByteArrayToHexString(calcSHA1(b));
    }

    /**
	 * use a exterior win32 shell tool "sha1.exe" from http://www.hashcash.org
	 * to calculate sha1 string of the specific file
	 * @param file 
	 */
    public static String calcSHA1ByHashCash(File file) {
        return null;
    }

    public static byte[] calcSHA1(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(b);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String ByteArrayToHexString(byte[] b) {
        if (b == null) return null;
        StringBuffer hexStr = new StringBuffer("");
        int x, y, i;
        char p, q;
        for (i = 0; i < b.length; i++) {
            x = b[i] >> 4 & 15;
            y = b[i] & 15;
            if (x > 9) p = (char) (x + 87); else p = (char) (x + 48);
            if (y > 9) q = (char) (y + 87); else q = (char) (y + 48);
            hexStr.append(p).append(q);
        }
        return hexStr.toString();
    }

    /**
	 *  二行制转字符串，第二种方法
	 */
    public static String ByteArrayToHexString2(byte[] b) {
        StringBuffer hexStr = new StringBuffer("");
        String stmp = "";
        for (int i = 0; i < b.length; i++) {
            stmp = (Integer.toHexString(b[i] & 0xFF));
            if (stmp.length() == 1) hexStr.append("0").append(stmp); else hexStr.append(stmp);
        }
        return hexStr.toString();
    }

    public static void main(String[] arg) {
        byte[] b1 = new byte[] { 123, 0, -128, 1, 127, 64 };
        System.out.println(ByteArrayToHexString(b1));
        System.out.println(ByteArrayToHexString2(b1));
        System.out.println(ByteArrayToHexString(calcSHA1(b1)));
        System.out.println(calcSHA1String(b1));
    }
}
