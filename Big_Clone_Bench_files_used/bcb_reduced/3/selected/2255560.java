package br.ufma.sgdu.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    /**
	 * Retorna o hash md5 da string
	 * @param password string para o hash
	 * @return uma string em formato hexadecimal
	 */
    public static String get(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        BigInteger hash = new BigInteger(1, md.digest(password.getBytes()));
        return hash.toString(16);
    }
}
