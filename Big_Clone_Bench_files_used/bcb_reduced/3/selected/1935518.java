package org.xmldap.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.xmldap.exceptions.CryptoException;

public class EncryptedStoreKeys {

    private static byte[] encKeyEntropy = { (byte) 0xd9, (byte) 0x59, (byte) 0x7b, (byte) 0x26, (byte) 0x1e, (byte) 0xd8, (byte) 0xb3, (byte) 0x44, (byte) 0x93, (byte) 0x23, (byte) 0xb3, (byte) 0x96, (byte) 0x85, (byte) 0xde, (byte) 0x95, (byte) 0xfc };

    private static byte[] integrityKeyEntropy = { (byte) 0xc4, (byte) 0x01, (byte) 0x7b, (byte) 0xf1, (byte) 0x6b, (byte) 0xad, (byte) 0x2f, (byte) 0x42, (byte) 0xaf, (byte) 0xf4, (byte) 0x97, (byte) 0x7d, (byte) 0x4, (byte) 0x68, (byte) 0x3, (byte) 0xdb };

    private byte[] encryptionKey;

    private byte[] integrityKey;

    public EncryptedStoreKeys(String password, byte[] salt) throws CryptoException {
        byte[] key = null;
        try {
            key = password.getBytes("UTF-16LE");
        } catch (Exception e) {
            throw new CryptoException("Error getting bytes for password", e);
        }
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Error generating SHA256 Digest", e);
        }
        byte[] derivedKey = generateDerivedKey(digest, key, salt, 1000);
        digest.reset();
        byte[] encKeyBytes = new byte[encKeyEntropy.length + derivedKey.length];
        System.arraycopy(encKeyEntropy, 0, encKeyBytes, 0, encKeyEntropy.length);
        System.arraycopy(derivedKey, 0, encKeyBytes, encKeyEntropy.length, derivedKey.length);
        digest.update(encKeyBytes);
        encryptionKey = digest.digest();
        digest.reset();
        byte[] integrityKeyBytes = new byte[integrityKeyEntropy.length + derivedKey.length];
        System.arraycopy(integrityKeyEntropy, 0, integrityKeyBytes, 0, integrityKeyEntropy.length);
        System.arraycopy(derivedKey, 0, integrityKeyBytes, integrityKeyEntropy.length, derivedKey.length);
        digest.update(integrityKeyBytes);
        integrityKey = digest.digest();
    }

    private byte[] generateDerivedKey(MessageDigest digest, byte[] password, byte[] salt, int iterationCount) {
        digest.update(password);
        digest.update(salt);
        byte[] digestBytes = digest.digest();
        for (int i = 1; i < iterationCount; i++) {
            digest.update(digestBytes);
            digestBytes = digest.digest();
        }
        return digestBytes;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public byte[] getIntegrityKey() {
        return integrityKey;
    }
}
