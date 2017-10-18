package com.jdkcn.util;

import java.security.MessageDigest;

/**
 * @author <a href="mailto:Rory.cn@gmail.com">somebody</a>
 * 
 */
public class SHA {

    private SHA() {
    }

    public static String hashPassword(String password) {
        if (password == null) {
            password = "";
        }
        return digest(password);
    }

    private static String digest(String myinfo) {
        try {
            MessageDigest alga = MessageDigest.getInstance("SHA");
            alga.update(myinfo.getBytes());
            byte[] digesta = alga.digest();
            return byte2hex(digesta);
        } catch (Exception ex) {
            return myinfo;
        }
    }

    private static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
            if (n < b.length - 1) {
                hs = hs + "";
            }
        }
        return hs.toLowerCase();
    }

    public static void main(String[] args) {
        System.out.println(SHA.hashPassword("admin"));
    }
}
