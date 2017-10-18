package com.blommersit.httpd.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class will return the MD5 of a string /and/
 * a pseudo-random MD5 String (length: 32 and 64)
 * <br><br>
 * You can use this file free in each project without
 * giving any credit. Please leave these copyrights in
 * the source file.<br><br>
 * On a 2.4 ghz pentium 4 - JDK 1.4.1 windows xp and no special
 * optimisation, this function will generate<br>
 * 15'000 'length 32' random string per seconds<br>
 * 10'000 'length 64' random string per seconds
 *
 * @author Gabriel Klein (Find my actual email on http://www.nuage.ch)
 * @since V1.01
 */
public final class CryptUtils {

    /**
	 * This function will return the MD5 of a string.
	 *
	 * @param name is the string we want the md5
	 * @return md5 of a string in hex format.
	 */
    public static final String md5(String name) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
        md.reset();
        md.update(name.getBytes());
        byte b[] = md.digest();
        String encoded = "";
        for (int i = 0; i < b.length; i++) {
            int b2 = (int) b[i] % 16;
            if (b2 < 0) b2 += 16;
            int b1 = (int) b[i];
            if (b1 < 0) b1 += 16 * 16;
            b1 -= b2;
            b1 /= 16;
            if (b1 <= 9) encoded += (char) ((int) '0' + (int) b1); else encoded += (char) ((int) 'a' + (int) b1 - (int) 10);
            if (b2 <= 9) encoded += (char) ((int) '0' + (int) b2); else encoded += (char) ((int) 'a' + (int) b2 - (int) 10);
        }
        return encoded;
    }

    /**
	 * Return a random 32 char length String or a random
	 * number if the md5 cannot be generated (No such algo)
	 *
	 * @return
	 */
    public static String generateRandom32() {
        try {
            return md5(Math.random() + "x" + System.currentTimeMillis());
        } catch (RuntimeException e) {
            return (("" + Math.random()).substring(2) + ("" + Math.random()).substring(2)).substring(0, 32);
        }
    }

    /**
	 * Return a random 64 char length String or a random
	 * number if the md5 cannot be generated (No such algo)
	 *
	 * @return
	 */
    public static String generateRandom64() {
        try {
            return md5(Math.random() + "x" + System.currentTimeMillis()) + md5(Math.random() + "y" + System.currentTimeMillis());
        } catch (RuntimeException e) {
            return (("" + Math.random()).substring(2) + ("" + Math.random()).substring(2) + ("" + Math.random()).substring(2) + ("" + Math.random()).substring(2)).substring(0, 64);
        }
    }
}
