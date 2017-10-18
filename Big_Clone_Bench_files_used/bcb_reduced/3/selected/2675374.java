package org.middleheaven.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple wrapper around {@code java.security.MessageDigest} for MD5 encription
 */
public class MD5CipherAlgorithm implements CipherAlgorithm {

    /**
	 * @param message a byte array with the message
	 * @throws NoSuchAlgorithmCipherException if the MD5 algorithm is not found.
	 * @throws IllegalArgumentException if {@code message} is {@code null}. 
	 */
    @Override
    public byte[] cipher(byte[] message) throws NoSuchAlgorithmCipherException {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        try {
            return MessageDigest.getInstance("MD5").digest(message);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmCipherException(e);
        }
    }
}
