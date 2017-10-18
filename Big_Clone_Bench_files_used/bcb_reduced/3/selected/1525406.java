package jk.spider.util;

import java.security.*;

public class MD5 {

    public static String encryption(String oldPass) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        md.update(oldPass.getBytes());
        byte b[] = md.digest();
        int i;
        StringBuffer buf = new StringBuffer();
        for (int offset = 0; offset < b.length; offset++) {
            i = b[offset];
            if (i < 0) {
                i += 256;
            }
            if (i < 16) {
                buf.append("0");
            }
            buf.append(Integer.toHexString(i));
        }
        String pass32 = buf.toString();
        return pass32;
    }

    public static void main(String[] args) {
        String a = MD5.encryption("�й��");
        System.out.println(a);
        System.out.println(MD5.encryption("�й��"));
    }
}
