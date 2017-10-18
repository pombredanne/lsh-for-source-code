package net.sourceforge.dfdr.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoService {

    public byte[] getRandomBytes(int length) {
        if (length < 0) {
            length = 0;
        }
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    public byte[] hashBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.update(bytes);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private SecureRandom secureRandom = new SecureRandom();
}
