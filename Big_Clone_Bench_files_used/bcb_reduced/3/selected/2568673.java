package net.sf.dict4j;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {

    private BigInteger number;

    public Digest(String algorithm, String text) throws NoSuchAlgorithmException {
        this(algorithm, text.getBytes());
    }

    public Digest(String algorithm, char[] text) throws NoSuchAlgorithmException {
        this(algorithm, new String(text).getBytes());
    }

    public Digest(String algorithm, byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
        digest.update(bytes);
        number = new BigInteger(1, digest.digest());
    }

    @Override
    public String toString() {
        return number.toString(16);
    }
}
