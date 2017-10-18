package com.worldware.misc;

import java.io.*;
import java.security.*;

public class md5 {

    public static void main(String args[]) throws Exception {
        for (int i = 0; i < args.length; i++) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsa) {
                System.out.println("Can't get MD5 implementation " + nsa);
                return;
            }
            File f = new File(args[i]);
            FileInputStream fis = new FileInputStream(f);
            byte[] b = new byte[2048 * 1024];
            int count;
            while (0 < (count = fis.read(b))) {
                md.update(b, 0, count);
            }
            byte[] hash = md.digest();
            String hashed = byteToHex(hash);
            System.out.print(hashed);
        }
    }

    public static String byteToHex(byte[] b) {
        int l = b.length;
        char r[] = new char[l * 2];
        for (int i = 0; i < l * 2; i++) r[i] = Character.forDigit(b[i >> 1] >> 4 - (i << 2 & 4) & 0xF, 16);
        return new String(r);
    }
}
