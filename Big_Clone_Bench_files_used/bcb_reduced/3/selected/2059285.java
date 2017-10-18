package org.ultichat.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Password encrypter
 *
 * @author Anthony
 */
public class Encrypter {

    private static Encrypter instance;

    public static Encrypter getInstance() {
        if (instance == null) {
            instance = new Encrypter();
        }
        return instance;
    }

    private MessageDigest messageDigest;

    public Encrypter() {
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public String encryptPassword(String password) {
        try {
            messageDigest.update(password.getBytes("UTF-8"));
            byte digest[] = messageDigest.digest();
            return Base64Encoder.encode(digest);
        } catch (Exception e) {
            return password;
        }
    }
}
