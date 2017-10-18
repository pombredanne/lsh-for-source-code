package org.melati.util;

import java.security.MessageDigest;

/**
 * MD5 - utils for encoding using MD5
 */
public class MD5Util {

    public static String encode(String in) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(in.getBytes());
            return new String(digest);
        } catch (Exception e) {
            throw new MelatiBugMelatiException("For some reason I couldn't encode the password!", e);
        }
    }

    /**
     * Test harness
     */
    public static void main(String arg[]) {
        String in = "FIXME";
        if (arg.length > 0) {
            in = arg[1];
        }
        System.out.println("arg:" + in + ":");
        System.out.println(":" + encode(in) + ":");
        System.out.println(":" + encode(in) + ":");
    }
}
