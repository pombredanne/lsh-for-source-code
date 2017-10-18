package net.sourceforge.xconf.toolbox;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Convenience methods to perform cryptographic digests.
 *
 * @author  Tom Czarniecki
 */
public class DigestUtils {

    /**
     * Returns the MD5 digest of the given byte array.
     */
    public static byte[] md5(byte[] ba) {
        return digest("MD5", ba);
    }

    /**
     * Convenience method to perform a MD5 digest on the bytes of the given value,
     * and return the result as a hexadecimal string.
     */
    public static String md5Hex(String value) {
        byte[] digest = md5(value.getBytes());
        return Hex.encodeHex(digest).toUpperCase();
    }

    /**
     * Returns the SHA1 digest of the given byte array.
     */
    public static byte[] sha1(byte[] ba) {
        return digest("SHA1", ba);
    }

    /**
     * Convenience method to perform a SHA1 digest on the bytes of the given value,
     * and return the result as a hexadecimal string.
     */
    public static String sha1Hex(String value) {
        byte[] digest = sha1(value.getBytes());
        return Hex.encodeHex(digest).toUpperCase();
    }

    /**
     * Returns the results of performing a digest on the given byte array
     * using the given digest algorithm.
     *
     * @throws IllegalArgumentException
     *      If the digest algorithm is not known.
     */
    public static byte[] digest(String algorithm, byte[] ba) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return digest.digest(ba);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
}
