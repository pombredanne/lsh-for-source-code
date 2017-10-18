package jkad.builders;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jkad.controller.io.*;

public class SHA1Digester {

    private static MessageDigest digester;

    private static String linea;

    private static FichLog fich = new FichLog();

    ;

    private static MessageDigest getDigester() {
        if (digester == null) {
            try {
                digester = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                linea = ("ERROR : " + e);
                fich.writelog(linea);
                throw new RuntimeException(e);
            }
        }
        return digester;
    }

    public static BigInteger digest(String string) {
        byte[] data = string.getBytes();
        byte[] dig = getDigester().digest(data);
        BigInteger result = new BigInteger(dig);
        result = result.abs();
        return result;
    }

    public static BigInteger digest(byte[] data) {
        byte[] dig = getDigester().digest(data);
        BigInteger result = new BigInteger(dig);
        result = result.abs();
        return result;
    }

    public static BigInteger hash(String string) {
        byte[] data = string.getBytes();
        byte[] hashed = getDigester().digest(data);
        BigInteger dev = new BigInteger(hashed);
        dev = dev.abs();
        return (dev);
    }

    public static BigInteger hash(byte[] data) {
        byte[] hashed = getDigester().digest(data);
        BigInteger dev = new BigInteger(hashed);
        dev = dev.abs();
        return dev;
    }
}
