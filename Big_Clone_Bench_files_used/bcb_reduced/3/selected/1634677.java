package com.aconiac.apg.domain;

import java.security.*;

public class HasherImpl implements Hasher {

    public byte[] genByteHash(String data, HashAlg alg) {
        return hash(data, alg);
    }

    public String genHexStringHash(String data, HashAlg alg) {
        return toHex(hash(data, alg));
    }

    private String toHex(byte[] data) {
        StringBuilder hexStr = new StringBuilder();
        for (int i = 0; i < data.length; i++) hexStr.append(toHex(data[i]));
        return hexStr.toString();
    }

    private String toHex(byte data) {
        return Integer.toHexString(0xFF & data);
    }

    private byte[] hash(String data, HashAlg alg) {
        try {
            MessageDigest digest = MessageDigest.getInstance(alg.toString());
            digest.update(data.getBytes());
            byte[] hash = digest.digest();
            return hash;
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }
}
