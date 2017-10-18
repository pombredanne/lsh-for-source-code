package org.isportal.crypto;

import java.security.*;

/** @author Marko Bevc
 * 
 * Calculates MD5 Hash of a string
 */
public class MD5 {

    /**
	 * Metoda pretvori dan niz v hexa obliko.
	 * 
	 * @param n
	 * @return hexa form of given string
	 */
    public static final String toHexString(byte n) {
        if ((n >= 0) && (n <= 15)) return "0" + Integer.toHexString(n & 0xff); else return Integer.toHexString(n & 0xff);
    }

    /**
	 * Metoda zakodira dan niz s pomočjo MD5 algoritma.
	 * 
	 * @param buff
	 * @return MD5 hashed parameter <b>buff</b>
	 */
    public static String getEncodedPassword(String buff) {
        if (buff == null) return null;
        String t = new String();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buff.getBytes());
            byte[] r = md.digest();
            for (int i = 0; i < r.length; i++) {
                t += toHexString(r[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }
}
