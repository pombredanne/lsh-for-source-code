package org.bhf.security.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Miscellaneous cryptography utilities.
 * @exclude
 */
public final class Crypto {

    /**
     * Standard salt value for PBE
     */
    public static byte[] SALT = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };

    private static char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Standard iterations for PBE.
     */
    public static int ITERATIONS = 20;

    /**
     * Standard PBE algorithm.
     */
    public static String PBE_ALGORITHM = "PBEWithMD5AndDES";

    /**
     * Standard digest algorithm.
     */
    public static String DIGEST_ALGORITHM = "MD5";

    /**
     * Shared PBE key factory.
     */
    private static SecretKeyFactory keyFactory;

    /**
     * Shared and intialized param spec (does not include password).
     */
    private static PBEParameterSpec pbeParamSpec;

    static {
        try {
            keyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            pbeParamSpec = new PBEParameterSpec(SALT, ITERATIONS);
        } catch (NoSuchAlgorithmException e) {
            SecurityException se = new SecurityException();
            se.initCause(e);
            throw se;
        }
    }

    /**
     * Hex encode an array of bytes to a String.
     *
     * @param bytes bytes to be encoded.
     * @return encoded String
     */
    public static String hex(final byte[] bytes) {
        char[] c = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int j = i * 2;
            c[j] = HEX[(bytes[i] >> 4) & 0x0f];
            c[j + 1] = HEX[(bytes[i]) & 0x0f];
        }
        return new String(c);
    }

    /**
     * Hex decode an array of bytes from a String.
     *
     * @param s String to be decoded.
     * @return decoded bytes
     */
    public static byte[] hex(final String s) {
        if ((s.length() % 2) != 0) throw new IllegalArgumentException("(s.length() % 2) != 0");
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int j = i * 2;
            int c1 = Character.digit(s.charAt(j), 16);
            int c2 = Character.digit(s.charAt(j + 1), 16);
            b[i] = (byte) ((c1 << 4) | (c2));
        }
        return b;
    }

    /**
     * Produce a digest for the data available from multiple
     * <code>InputStream</code>s.
     *
     * @param in Data stream - must not be <code>null</code> or empty.
     * @return the message digest.
     * @throws java.io.IOException ConversionError reading the intput
     */
    public static byte[] digestNoClose(final InputStream[] in) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] b = new byte[8192];
            for (final InputStream anIn : in) for (int l; (l = anIn.read(b)) > 0; ) messageDigest.update(b, 0, l);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Produce a digest for the data available from multiple
     * <code>InputStream</code>s. This version will close all input
     * streams when done.
     *
     * @param in Data stream - must not be <code>null</code> or empty.
     * @return the message digest.
     * @throws java.io.IOException ConversionError reading input
     */
    public static byte[] digest(final InputStream[] in) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] b = new byte[8192];
            try {
                for (final InputStream anIn : in) for (int l; (l = anIn.read(b)) > 0; ) messageDigest.update(b, 0, l);
            } finally {
                for (InputStream anIn : in) try {
                    anIn.close();
                } catch (Exception e) {
                }
            }
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Password encrypt the given data using the given password.
     *
     * @param data     Data to be encrypted. Must not be <code>null</code>.
     * @param password Password to be used for encryption. Must not be
     *                 <code>null</code>.
     * @return The encrypted data.
     * @throws SecurityException An exception related to the security algorithm,
     *                           Such as NoSuchAlgorithmException. Will have the causal exception
     *                           set to the original exception.
     */
    public static byte[] encrypt(final byte[] data, final char[] password) throws SecurityException {
        try {
            SecretKey pbeKey = keyFactory.generateSecret(new PBEKeySpec(password));
            Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            SecurityException se = new SecurityException();
            se.initCause(e);
            throw se;
        }
    }

    /**
     * Password decrypt the given data using the given password.
     *
     * @param data     Data to be encrypted. Must not be <code>null</code>.
     * @param password Password to be used for encryption. Must not be
     *                 <code>null</code>.
     * @return The encrypted data.
     * @throws SecurityException An exception related to the security algorithm,
     *                           Such as NoSuchAlgorithmException. Will have the causal exception
     *                           set to the original exception.
     */
    public static byte[] decrypt(final byte[] data, final char[] password) throws SecurityException {
        try {
            SecretKey pbeKey = keyFactory.generateSecret(new PBEKeySpec(password));
            Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            SecurityException se = new SecurityException();
            se.initCause(e);
            throw se;
        }
    }

    /**
     * Perform standard password hashing.
     *
     * @param password Password to hash
     * @return Hashed password
     */
    public static String encodeMD5Base64(final String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(password.getBytes("UTF-8"));
            return base64Encode(digest);
        } catch (NoSuchAlgorithmException e) {
            RuntimeException re = new RuntimeException();
            re.initCause(e);
            throw re;
        } catch (UnsupportedEncodingException e) {
            RuntimeException re = new RuntimeException();
            re.initCause(e);
            throw re;
        }
    }

    static byte[] Base64EncMap = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/' };

    /**
     * Base64 encode data.
     * @param data If <code>null</code>, null is returned.
     * @return The encoded data, will be <code>null</code> if data is <code>null</code>.
     */
    public static String base64Encode(final byte[] data) {
        if (data == null) return null;
        int sidx, didx;
        byte dest[] = new byte[((data.length + 2) / 3) * 4];
        for (sidx = 0, didx = 0; sidx < data.length - 2; sidx += 3) {
            dest[didx++] = Base64EncMap[(data[sidx] >>> 2) & 0x3f];
            dest[didx++] = Base64EncMap[(data[sidx + 1] >>> 4) & 0xf | (data[sidx] << 4) & 0x3f];
            dest[didx++] = Base64EncMap[(data[sidx + 2] >>> 6) & 0x3 | (data[sidx + 1] << 2) & 0x3f];
            dest[didx++] = Base64EncMap[data[sidx + 2] & 0x3f];
        }
        if (sidx < data.length) {
            dest[didx++] = Base64EncMap[(data[sidx] >>> 2) & 0x3f];
            if (sidx < data.length - 1) {
                dest[didx++] = Base64EncMap[(data[sidx + 1] >>> 4) & 0xf | (data[sidx] << 4) & 0x3f];
                dest[didx++] = Base64EncMap[(data[sidx + 1] << 2) & 0x3f];
            } else dest[didx++] = Base64EncMap[(data[sidx] << 4) & 0x3f];
        }
        for (; didx < dest.length; didx++) dest[didx] = (byte) '=';
        try {
            return new String(dest, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
