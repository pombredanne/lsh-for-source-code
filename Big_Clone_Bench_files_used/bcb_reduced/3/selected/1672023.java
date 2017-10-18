package com.frinika.server.util;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 *
 * @author pjl
 */
public class MD5CheckSum {

    static final int BUFSIZE = 2048;

    public static String caculate(InputStream fis) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(fis, md);
        byte[] b = new byte[BUFSIZE];
        int c;
        while ((c = dis.read(b)) != -1) {
        }
        byte[] messageDigest = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        System.out.println("MD5 sum = " + hexString);
        return hexString.toString();
    }
}
