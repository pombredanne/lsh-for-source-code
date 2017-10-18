package org.bing.engine.utility.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Helper {

    private static final String HEXES = "0123456789abcdef";

    public static String calcMd5(String arg) {
        byte[] source = arg.getBytes();
        MessageDigest digest = getMd5Digest();
        digest.update(source);
        byte[] target = digest.digest();
        return toHex(target);
    }

    public static String calcMd5(String[] args) {
        if (args == null || args.length == 0) {
            throw new RuntimeException("Pls use a array and length > 0 !");
        }
        StringBuilder sb = new StringBuilder(512);
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]);
        }
        return calcMd5(sb.toString());
    }

    private static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Current JDK not impl MD5 algorithm! ", e);
        }
    }

    private static String toHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static void main(String[] args) {
        System.out.println(Md5Helper.calcMd5(new String[] { "" }));
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 100 * 10000; i++) {
            Md5Helper.calcMd5(new String[] { "" });
        }
        long t2 = System.currentTimeMillis();
        System.out.println("TT " + (t2 - t1));
    }
}
