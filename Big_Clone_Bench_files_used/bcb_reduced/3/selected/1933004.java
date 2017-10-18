package client.controller;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    public static String getHashedPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(password.getBytes());
            BigInteger hashedInt = new BigInteger(1, digest.digest());
            return String.format("%1$032X", hashedInt);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println(nsae.getMessage());
        }
        return "";
    }
}
