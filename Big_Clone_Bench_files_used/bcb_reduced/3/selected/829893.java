package com.ericdaugherty.mail.server.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.security.*;

/**
 * Creates encrypted passwords. Also used to validate passwords from incoming
 * connections.
 * 
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public class PasswordManager {

    /** Logger Category for this class. */
    private static Log log = LogFactory.getLog(PasswordManager.class);

    /**
     * Creates a one-way has of the specified password.  This allows passwords to be
     * safely stored in the database without any way to retrieve the original value.
     * 
     * @param password the string to encrypt.
     * 
     * @return the encrypted password, or null if encryption failed.
     */
    public static String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes());
            byte[] hash = md.digest();
            StringBuilder hashStringBuf = new StringBuilder("{SHA}");
            String byteString;
            int byteLength;
            for (int index = 0; index < hash.length; index++) {
                byteString = String.valueOf(hash[index] + 128);
                byteLength = byteString.length();
                switch(byteLength) {
                    case 1:
                        byteString = "00" + byteString;
                        break;
                    case 2:
                        byteString = "0" + byteString;
                        break;
                }
                hashStringBuf.append(byteString);
            }
            return hashStringBuf.toString();
        } catch (NoSuchAlgorithmException nsae) {
            log.error("Error getting password hash - " + nsae.getMessage());
            return null;
        }
    }

    public static String encryptPassword(String username, String realm, String password) throws GeneralSecurityException {
        MessageDigest md = null;
        md = MessageDigest.getInstance("MD5");
        md.update(username.getBytes());
        md.update(":".getBytes());
        md.update(realm.getBytes());
        md.update(":".getBytes());
        md.update(password.getBytes());
        byte[] hash = md.digest();
        return toHex(hash, hash.length);
    }

    private static String toHex(byte[] b, int len) {
        if (b == null) return "";
        StringBuilder s = new StringBuilder("");
        int i;
        for (i = 0; i < len; i++) s.append(toHex(b[i]));
        return s.toString();
    }

    private static String toHex(byte b) {
        Integer I = new Integer((((int) b) << 24) >>> 24);
        int i = I.intValue();
        if (i < (byte) 16) return "0" + Integer.toString(i, 16); else return Integer.toString(i, 16);
    }
}
