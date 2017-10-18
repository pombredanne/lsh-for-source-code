package org.kablink.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Miscellaneous utilities.
 */
public class PasswordEncryptor {

    private static Long PASSWORD_DIGEST = new Long(32958);

    /**
     * Encrypt the password using the specified algorithm.
     * @param algorithm available values are SHA, SHA-256, and MD5.
     * @param password
     * @return
     */
    public static String encrypt(String algorithm, String password) {
        if (algorithm.equals("MD5")) return encryptMD5(password, PASSWORD_DIGEST); else return encrypt(algorithm, password, PASSWORD_DIGEST);
    }

    private static String encryptMD5(String password, Long digestSeed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(password.getBytes("UTF-8"));
            digest.update(digestSeed.toString().getBytes("UTF-8"));
            byte[] messageDigest = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xff & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return new StringBuffer().toString();
        }
    }

    private static String encrypt(String algorithm, String password, Long digestSeed) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.reset();
            digest.update(password.getBytes("UTF-8"));
            digest.update(digestSeed.toString().getBytes("UTF-8"));
            byte[] messageDigest = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString((0xf0 & messageDigest[i]) >> 4));
                hexString.append(Integer.toHexString(0x0f & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return new StringBuffer().toString();
        }
    }
}
