package com.smssalama.security;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * A class that provides basic cryptographic functions for the rest of the
 * application.
 * 
 * 
 * @author Arnold P. Minde
 */
public class Security {

    protected Security() {
    }

    public static Digest getDigest() {
        return new SHA256Digest();
    }

    public static byte[] digest(byte[] data) {
        Digest digest = Security.getDigest();
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static int getDigestSize() {
        return Security.getDigest().getDigestSize();
    }

    static BlockCipher createEngine() {
        return new CBCBlockCipher(new AESEngine());
    }

    private static byte[] encrypt(boolean encrypt, byte[] key, byte[] input) throws CryptoException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(Security.createEngine());
        cipher.init(encrypt, new KeyParameter(Security.digest(key)));
        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        int outputLen = cipher.processBytes(input, 0, input.length, cipherText, 0);
        cipher.doFinal(cipherText, outputLen);
        return cipherText;
    }

    public static byte[] encrypt(byte[] key, byte[] input) {
        try {
            return Security.encrypt(true, key, input);
        } catch (CryptoException ex) {
            throw new RuntimeCryptoException(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    public static byte[] decrypt(byte[] key, byte[] input) throws CryptoException {
        return Security.encrypt(false, key, input);
    }

    public static void fill(byte[] data, byte b) {
        for (int i = 0; i < data.length; i++) {
            data[i] = b;
        }
    }

    public static boolean test() {
        String data = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890~!@#$%^&*;  abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890~!@#$%^&*;  abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890~!@#$%^&*;  abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890~!@#$%^&*;";
        String password1 = "testing";
        String password2 = "testing";
        byte[] plain = null;
        try {
            byte[] cipher = Security.encrypt(true, password1.getBytes(), data.getBytes());
            plain = Security.encrypt(false, password2.getBytes(), cipher);
        } catch (CryptoException ex) {
            throw new RuntimeException(ex.getClass().getName() + ": " + ex.getMessage());
        }
        String plainData = new String(plain).trim();
        return plainData.equals(data);
    }
}
