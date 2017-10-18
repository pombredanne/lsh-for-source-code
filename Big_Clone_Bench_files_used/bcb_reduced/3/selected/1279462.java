package com.keppardo.dyndns.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptUserData {

    private User user;

    public EncryptUserData(User user) {
        this.user = user;
    }

    public String getAccountHash() throws GeneralSecurityException {
        String username = user.getUsername();
        String pwd = user.getPwd();
        String sha = username + "|" + pwd;
        byte[] b = MessageDigest.getInstance("SHA-1").digest(sha.getBytes());
        sha = hash(b);
        return sha;
    }

    private static String hash(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            byte c = b[i];
            sb.append(toHex(c));
        }
        return sb.toString();
    }

    private static String toHex(byte c) {
        return "00".substring(Integer.toHexString(c & 0xff).length()) + Integer.toHexString(c & 0xff);
    }
}
