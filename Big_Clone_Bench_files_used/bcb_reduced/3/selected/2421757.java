package net.mp3spider.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Esteban Fuentealba
 */
public class Utilidades {

    public static String getClassName(Class c) {
        return c.toString().substring(c.toString().lastIndexOf(".") + 1).trim();
    }

    public static String MD5(String val) throws NoSuchAlgorithmException {
        MessageDigest mdEnc = MessageDigest.getInstance("MD5");
        mdEnc.update(val.getBytes(), 0, val.length());
        return (new BigInteger(1, mdEnc.digest()).toString(16));
    }
}
