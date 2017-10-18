package br.com.sysconstruct.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Anderson Zanichelli
 */
public class CriptoMD5 {

    public static String produzirChaveMD5(String str) throws NoSuchAlgorithmException {
        if (str == null) {
            return null;
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        BigInteger hash = new BigInteger(1, md.digest(adicionarSalt(str).getBytes()));
        String s = hash.toString(16);
        if (s.length() % 2 != 0) {
            s = "0" + s;
        }
        return s;
    }

    private static String adicionarSalt(String str) {
        return str + Salt.SALT;
    }
}
