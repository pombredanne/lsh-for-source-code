package net.woodstock.rockapi.security.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import net.woodstock.rockapi.utils.Base64Utils;

public abstract class Digester {

    private static Map<String, MessageDigest> digesters;

    private Digester() {
    }

    private static byte[] _digest(String data, Algorithm algorithm) throws NoSuchAlgorithmException {
        if (!Digester.digesters.containsKey(algorithm.algorithm())) {
            Digester.digesters.put(algorithm.algorithm(), MessageDigest.getInstance(algorithm.algorithm()));
        }
        MessageDigest d = Digester.digesters.get(algorithm.algorithm());
        byte[] b = d.digest(data.getBytes());
        d.reset();
        return b;
    }

    public static String digest(String data) throws NoSuchAlgorithmException {
        return Digester.digest(data, Algorithm.DEFAULT);
    }

    public static String digest(String data, Algorithm algorithm) throws NoSuchAlgorithmException {
        byte[] b = Digester._digest(data, algorithm);
        return new String(b);
    }

    public static String digestBase64(String data) throws NoSuchAlgorithmException {
        return Digester.digestBase64(data, Algorithm.DEFAULT);
    }

    public static String digestBase64(String data, Algorithm algorithm) throws NoSuchAlgorithmException {
        byte[] b = Digester._digest(data, algorithm);
        return Base64Utils.toBase64String(b);
    }

    public static boolean isEquals(String d1, String d2) {
        return MessageDigest.isEqual(d1.getBytes(), d2.getBytes());
    }

    static {
        Digester.digesters = new HashMap<String, MessageDigest>();
    }
}
