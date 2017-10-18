package com.nonesole.persistence.tools;

import java.security.MessageDigest;

/**
 * MD5 Tools
 * @author JACK LEE
 * @version 1.0 - build in 2008-03-30
 */
public final class MD5 {

    /**
     * MD5 encrypt
     * @param s - data needs encrypt
     * @return String
     */
    public static String encrypt(String s) {
        byte hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            byte[] strTemp = s.getBytes();
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            byte str[] = new byte[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }
}
