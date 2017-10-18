package org.plazmaforge.framework.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.net.BCodec;

/**
 * 
 * @author ohapon
 *
 */
public class CodecUtils {

    /**
     * Return digest by algorithm (MD5, SHA ...)
     * @param algorithm
     * @return
     */
    public static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Return MD5 digest hex of string
     * @param data
     * @return
     */
    public static String md5Hex(String data) {
        return digestHex(data, "MD5");
    }

    /**
     * Return SHA digest hex of string
     * @param data
     * @return
     */
    public static String shaHex(String data) {
        return digestHex(data, "SHA");
    }

    /**
     * Return digest hex of string by algorithm
     * @param data
     * @param algorithm
     * @return
     */
    public static String digestHex(String data, String algorithm) {
        return Hex.encodeHexString(digest(data, algorithm));
    }

    /**
     * Return digest data of string by algorithm
     * @param data
     * @param algorithm
     * @return
     */
    public static byte[] digest(String data, String algorithm) {
        return digest(getBytesUtf8(data), algorithm);
    }

    /**
     * Return digest data of array of byte by algorithm
     * @param data
     * @param algorithm
     * @return
     */
    public static byte[] digest(byte data[], String algorithm) {
        return getDigest(algorithm).digest(data);
    }

    /**
     * Encrypt password by default algorithm (MD5)
     * @param password
     * @return
     */
    public static String encryptPassword(String password) {
        return encryptPassword(password, "MD5");
    }

    /**
     * Encrypt password by algorithm 
     * @param password
     * @param algorithm
     * @return
     */
    public static String encryptPassword(String password, String algorithm) {
        return digestHex(password, algorithm);
    }

    /**
     * Encode string
     * @param str
     * @return
     */
    public static String encode(String str) {
        try {
            return new BCodec().encode(str);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decode string
     * @param str
     * @return
     */
    public static String decode(String str) {
        try {
            return new BCodec().decode(str);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static byte[] getBytesUtf8(String data) {
        return org.apache.commons.codec.binary.StringUtils.getBytesUtf8(data);
    }
}
