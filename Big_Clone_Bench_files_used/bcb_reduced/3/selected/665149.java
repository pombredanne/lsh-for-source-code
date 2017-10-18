package org.ice.utils;

import java.security.MessageDigest;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class Encode {

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(input.getBytes("UTF-8"), 0, input.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static String random(int length) {
        String seeds = "abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        char[] chars = new char[length];
        Random rand = new Random(new Date().getTime());
        for (int i = 0; i < chars.length; i++) {
            chars[i] = seeds.charAt(rand.nextInt(seeds.length()));
        }
        return new String(chars);
    }

    public static String random() throws Exception {
        return UUID.randomUUID().toString();
    }
}
