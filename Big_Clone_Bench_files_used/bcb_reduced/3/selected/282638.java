package com.projeto.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encription {

    public static String hashMD5(String entrada) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(entrada.getBytes(), 0, entrada.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
