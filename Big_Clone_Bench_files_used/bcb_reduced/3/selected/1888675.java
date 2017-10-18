package com.vb.testproj.helpers;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoHelper {

    private static final String key = "verySecretKey!";

    public static String crypt(String target) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(target.getBytes("UTF-16"));
        BigInteger res = new BigInteger(1, md.digest(key.getBytes()));
        return res.toString(16);
    }

    public static boolean isMatch(String target, String cryptedSource) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return crypt(target).equals(cryptedSource);
    }

    public static String cryptSha(String target) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(target.getBytes("UTF-16"));
        BigInteger res = new BigInteger(1, md.digest(key.getBytes()));
        return res.toString(16);
    }
}
