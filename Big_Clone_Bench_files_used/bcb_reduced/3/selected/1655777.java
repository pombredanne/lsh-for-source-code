package org.via.utils.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class contains a set of convenience methods to digest a binary array.
 * 
 * @author Zé Carioca
 */
public final class Digester {

    /**
     * Gets the SHA1 hash of the provided data.
     * 
     * @param data The data to hash.
     * 
     * @return Returns a checksum of the data provided.
     * 
     * @throws NoSuchAlgorithmException
     *             if the JVM cannot locate the SHA1 algorithm.
     */
    public static String sha1(byte[] data) throws NoSuchAlgorithmException {
        return digest(data, MessageDigest.getInstance("SHA1"));
    }

    /**
     * Gets the MD5 hash of the provided data.
     * 
     * @param data The data to hash.
     * @return Returns a checksum of the data provided.
     * 
     * @throws NoSuchAlgorithmException
     *             if the JVM cannot locate the MD5 algorithm.
     */
    public static String md5(byte[] data) throws NoSuchAlgorithmException {
        return digest(data, MessageDigest.getInstance("MD5"));
    }

    private static String digest(byte[] data, MessageDigest digest) {
        byte[] enc = digest.digest(data);
        StringBuffer sb = new StringBuffer(enc.length * 2);
        for (byte b : enc) {
            String t = Integer.toString(b & 0xff, 16);
            if (t.length() == 1) sb.append('0');
            sb.append(t);
        }
        return sb.toString();
    }
}
