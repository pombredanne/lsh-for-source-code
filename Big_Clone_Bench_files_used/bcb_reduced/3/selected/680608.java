package org.snipsnap.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Digest handler for encrypting passwords.
 * @author Matthias L. Jugel
 * @version $Id: Digest.java 954 2003-08-28 13:31:24Z leo $
 */
public class Digest {

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("UserManager: unable to load digest algorithm: " + e);
            digest = null;
        }
    }

    public static String getDigest(String s) {
        if (digest != null && s != null) {
            return digestToHexString(digest.digest(s.getBytes()));
        }
        return "";
    }

    /**
   * Compare a password with an encryped password.
   */
    public static boolean authenticate(String password, String encrypted) {
        return encrypted.equals(getDigest(password));
    }

    /**
   * Make a hexadecimal character string out of a byte array digest
   */
    public static String digestToHexString(byte[] digest) {
        byte b = 0;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < digest.length; ++i) {
            b = digest[i];
            int value = (b & 0x7F) + (b < 0 ? 128 : 0);
            buffer.append(value < 16 ? "0" : "");
            buffer.append(Integer.toHexString(value).toUpperCase());
        }
        return buffer.toString();
    }
}
