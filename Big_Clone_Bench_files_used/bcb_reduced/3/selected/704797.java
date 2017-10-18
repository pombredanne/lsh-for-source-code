package com.ericdaugherty.mail.server.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;

/**
 * Creates encrypted passwords and validating passwords.
 * 
 * @author Eric Daugherty
 */
public class PasswordManager {

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
            int hashLength = hash.length;
            StringBuffer hashStringBuf = new StringBuffer();
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
            System.out.println("Error getting password hash - " + nsae.getMessage());
            return null;
        }
    }
}
