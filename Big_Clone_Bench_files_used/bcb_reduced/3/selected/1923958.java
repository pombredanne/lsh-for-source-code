package com.etymgiko.spaceshipshop.password;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Is used to cipher password using MD5 algorithm.
 *
 * @author Ivan Holub
 */
public class MD5PasswordEncoder implements PasswordEncoder {

    /**
     * Cipher password using MD5 algorithm.
     *
     * @param username the username
     * @param password the password
     * @return ciphered password
     */
    public String encodePassword(String username, String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] usernameBuffer = username.getBytes("UTF8");
            byte[] passwordBuffer = password.getBytes("UTF8");
            messageDigest.update(usernameBuffer);
            messageDigest.update(passwordBuffer);
            byte[] digestBuffer = messageDigest.digest();
            String retValue = getMD5String(digestBuffer);
            return retValue;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMD5String(final byte[] b) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
