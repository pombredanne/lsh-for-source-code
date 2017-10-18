package com.sample.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class PasswordEncryptorImpl implements PasswordEncryptor {

    private String algorithm;

    private String key;

    /**
	 * The choices are probably "MD5" and "SHA"
	 * @param algorithm
	 */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String encryptPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        if (key != null) {
            password = password + key;
        }
        byte[] bytes = md.digest(password.getBytes());
        return bytesToHexString(bytes);
    }

    public static String bytesToHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte what : bytes) {
            formatter.format("%02x", what);
        }
        return formatter.toString();
    }
}
