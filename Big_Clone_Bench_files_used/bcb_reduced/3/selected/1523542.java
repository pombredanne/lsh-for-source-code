package org.dbstore.utils;

import java.security.MessageDigest;

/**
 * MD5加密常用方法
 * @author <a href="mailto:zhanghz@staff.chinabyte.com">张宏泽</a>
 * 
 * @version 1.0
 * 
 * @since 2007-4-28
 */
public class Md5Util {

    private static final String ALGORITHM = "MD5";

    private static char[] hexDump(byte src[]) {
        char buf[] = new char[src.length * 2];
        for (int b = 0; b < src.length; b++) {
            String byt = Integer.toHexString(src[b] & 255);
            if (byt.length() < 2) {
                buf[b * 2 + 0] = '0';
                buf[b * 2 + 1] = byt.charAt(0);
            } else {
                buf[b * 2 + 0] = byt.charAt(0);
                buf[b * 2 + 1] = byt.charAt(1);
            }
        }
        return buf;
    }

    public static void smudge(char pwd[]) {
        if (pwd != null) {
            for (int b = 0; b < pwd.length; b++) pwd[b] = '\0';
        }
    }

    public static void smudge(byte pwd[]) {
        if (pwd != null) {
            for (int b = 0; b < pwd.length; b++) pwd[b] = 0;
        }
    }

    public static char[] cryptPassword(char pwd[]) throws Exception {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        byte pwdb[] = new byte[pwd.length];
        for (int b = 0; b < pwd.length; b++) pwdb[b] = (byte) pwd[b];
        char crypt[] = hexDump(md.digest(pwdb));
        smudge(pwdb);
        return crypt;
    }

    public static String getMD5Str(String password) {
        char[] pw = new char[password.length()];
        for (int i = 0; i < pw.length; i++) {
            pw[i] = password.charAt(i);
        }
        try {
            return new String(Md5Util.cryptPassword(pw));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
