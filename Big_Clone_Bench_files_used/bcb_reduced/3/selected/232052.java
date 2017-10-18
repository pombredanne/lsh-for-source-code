package com.bccapi.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.bouncycastle.crypto.digests.RIPEMD160Digest;

/**
 * Various hashing utilities used in the Bitcoin system.
 */
public class HashUtils {

    private static final String SHA256 = "SHA-256";

    public static byte[] sha256(byte[] data) {
        return sha256(data, 0, data.length);
    }

    public static byte[] sha256(byte[] data1, byte[] data2) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance(SHA256);
            digest.update(data1, 0, data1.length);
            digest.update(data2, 0, data2.length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha256(byte[] data, int offset, int length) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance(SHA256);
            digest.update(data, offset, length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] doubleSha256(byte[] data) {
        return doubleSha256(data, 0, data.length);
    }

    public static byte[] doubleSha256(byte[] data, int offset, int length) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance(SHA256);
            digest.update(data, offset, length);
            return digest.digest(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * Calculate the RIPEMD160 value of the SHA-256 of an array of bytes. This is
    * how a Bitcoin address is derived from public key bytes.
    * 
    * @param pubkeyBytes
    *           A Bitcoin public key as an array of bytes.
    * @return The Bitcoin address as an array of bytes.
    */
    public static byte[] addressHash(byte[] pubkeyBytes) {
        try {
            byte[] sha256 = MessageDigest.getInstance(SHA256).digest(pubkeyBytes);
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(sha256, 0, sha256.length);
            byte[] out = new byte[20];
            digest.doFinal(out, 0);
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
