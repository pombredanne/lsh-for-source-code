package com.fisoft.phucsinh.util;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil {

    /**
     * Replace all special chars 
     * @param aString String
     * @return String
     */
    public static String encodeSpecialChars(String aString) {
        if (aString == null || aString.length() <= 0) {
            return aString;
        }
        StringBuffer validBuffer = new StringBuffer();
        for (int i = 0, j = aString.length(); i < j; i++) {
            char c = aString.charAt(i);
            String convertedString = null;
            if (c == '&') {
                convertedString = "&amp;";
            } else if (c == '"') {
                convertedString = "&quot;";
            } else if (c == '<') {
                convertedString = "&lt;";
            } else if (c == '>') {
                convertedString = "&gt;";
            } else if (c == '\'') {
                convertedString = "&apos;";
            } else if (c < 127) {
                convertedString = Character.toString(c);
            } else if (c == '*' || c == ';') {
                convertedString = " ";
            } else {
                convertedString = "&#" + Integer.toString(c) + ";";
            }
            validBuffer.append(convertedString);
        }
        return validBuffer.toString();
    }

    public static Date convertStringToDate(String aMask, String strDate) {
        SimpleDateFormat df = null;
        Date date = null;
        df = new SimpleDateFormat(aMask);
        try {
            date = df.parse(strDate);
        } catch (Exception pe) {
            pe.printStackTrace();
        }
        return (date);
    }

    /**
     * Encode a string using algorithm specified in @com.fisoft.phucsinh.util.Constants.ALGORITHM and return the
     * resulting encrypted password. If exception, the plain credentials
     * string is returned
     *
     * @param password Password or other credentials to use in authenticating
     *        this username
     * @param algorithm Algorithm used to do the digest
     *
     * @return encypted password based on the algorithm.
     */
    public static String encodePassword(String password) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(Constants.ALGORITHM);
        } catch (Exception e) {
            return password;
        }
        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }
        return buf.toString();
    }
}
