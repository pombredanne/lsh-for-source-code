package com.simpleftp.ftp.server.admin;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 * The class will abstract all sort of password manipulation for the 
 * FtpServer.
 * 
 * */
public class PasswordService {

    private static PasswordService instance;

    private PasswordService() {
    }

    public static synchronized PasswordService getService() {
        if (instance == null) {
            instance = new PasswordService();
        }
        return instance;
    }

    /**
	   * Encrypt the given password using SHA-256 one way hashing.
	   * @return the encrypted password of 256 bits  
	   * */
    public synchronized String encrypt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = null;
        md = MessageDigest.getInstance("SHA-256");
        md.update(password.getBytes("UTF-8"));
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }
}
