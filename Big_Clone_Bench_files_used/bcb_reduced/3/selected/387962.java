package com.quickrss.helper;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;

public class String2SHA {

    private static Logger logger = Logger.getLogger(String2SHA.class);

    public String2SHA() {
    }

    public static String encrypt(String plaintext) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException:" + e);
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException:" + e);
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public static void main(String[] args) {
        String a = String2SHA.encrypt("cxnimm654");
        System.out.println(a);
    }
}
