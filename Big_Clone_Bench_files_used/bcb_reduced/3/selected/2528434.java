package com.apbetioli.mapr.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexandre Parra Betioli
 */
public class MD5String {

    public static String digest(final String all) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(all.getBytes());
            return new String(hexCodes(bytes));
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(MD5String.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Extracted from:
     * http://www.devmedia.com.br/articles/viewcomp.asp?comp=2944
     * 27/04/2008
     */
    private static char[] hexCodes(final byte[] text) {
        char[] hexOutput = new char[text.length * 2];
        String hexString;
        for (int i = 0; i < text.length; i++) {
            hexString = "00" + Integer.toHexString(text[i]);
            hexString.toUpperCase().getChars(hexString.length() - 2, hexString.length(), hexOutput, i * 2);
        }
        return hexOutput;
    }
}
