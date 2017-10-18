package org.nightlabs.wstunnel.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 */
public class Util {

    public static String encodeHexStr(byte[] buf, int pos, int len) {
        StringBuffer hex = new StringBuffer();
        while (len-- > 0) {
            byte ch = buf[pos++];
            int d = (ch >> 4) & 0xf;
            hex.append((char) (d >= 10 ? 'a' - 10 + d : '0' + d));
            d = ch & 0xf;
            hex.append((char) (d >= 10 ? 'a' - 10 + d : '0' + d));
        }
        return hex.toString();
    }

    public static void die(String message, Throwable e) {
        if (message != null) System.err.println("Error: " + message); else if (e != null) System.err.println("Error: " + e.getLocalizedMessage());
        System.err.println("Shutting down.");
        if (e != null) {
            System.err.println("Full stack trace:");
            e.printStackTrace();
        }
    }

    public static String md5Encode(String plain) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] result = md5.digest(plain.getBytes("UTF-8"));
            String encoded = Util.encodeHexStr(result, 0, result.length);
            return encoded;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
