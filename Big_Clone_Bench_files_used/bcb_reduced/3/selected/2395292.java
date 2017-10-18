package org.amlfilter.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The purpose of this class is to provide
 * some general purpose cryptographic utility functions
 *
 * @author Harish Seshadri
 * @version $Id: CryptographicUtils.java,v 1.1 2007/01/28 07:13:37 hseshadr Exp $
 */
public class CryptographicUtils {

    /**
     * Given the message digest and the input byte buffer
     * @param pMessageDigest The message digest of interest
     * @param pInputByteBuffer The input byte buffer
     * @return The generated hash
     */
    public static String generateHash(MessageDigest pMessageDigest, byte[] pInputByteBuffer) {
        pMessageDigest.reset();
        pMessageDigest.update(pInputByteBuffer);
        byte[] digest = pMessageDigest.digest();
        StringBuilder hexStringBuilder = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String hexString = Integer.toHexString(0xFF & digest[i]);
            hexStringBuilder.append(hexString);
        }
        return hexStringBuilder.toString();
    }

    /**
     * MD5 hash the given byte buffer
     * @param pStringToHash The string to hash
     * @return A string representing the hash
     * @throws NoSuchAlgorithmException
     */
    public static String generateMD5Hash(String pStringToHash) throws NoSuchAlgorithmException {
        if (null == pStringToHash) {
            throw new IllegalArgumentException("The string to hash is null");
        }
        byte[] inputByteBuffer = pStringToHash.getBytes();
        return generateMD5Hash(inputByteBuffer);
    }

    /**
     * MD5 hash the given byte buffer
     * @param pByteBuffer The byte buffer to hash
     * @return A string representing the hash
     * @throws NoSuchAlgorithmException
     */
    public static String generateMD5Hash(byte[] pInputByteBuffer) throws NoSuchAlgorithmException {
        if (null == pInputByteBuffer) {
            throw new IllegalArgumentException("The input byte buffer is null");
        }
        MessageDigest md5MessageDigest = MessageDigest.getInstance("MD5");
        return generateHash(md5MessageDigest, pInputByteBuffer);
    }

    /**
     * SHA hash the given byte buffer
     * @param pByteBuffer The byte buffer to hash
     * @return A string representing the hash
     * @throws NoSuchAlgorithmException
     */
    public static String generateSHAHash(String pStringToHash) throws NoSuchAlgorithmException {
        if (null == pStringToHash) {
            throw new IllegalArgumentException("The string to hash is null");
        }
        byte[] inputByteBuffer = pStringToHash.getBytes();
        return generateSHAHash(inputByteBuffer);
    }

    /**
     * SHA hash the given byte buffer
     * @param pByteBuffer The byte buffer to hash
     * @return A string representing the hash
     * @throws NoSuchAlgorithmException
     */
    public static String generateSHAHash(byte[] pInputByteBuffer) throws NoSuchAlgorithmException {
        if (null == pInputByteBuffer) {
            throw new IllegalArgumentException("The input byte buffer is null");
        }
        MessageDigest shaMessageDigest = MessageDigest.getInstance("SHA-1");
        return generateHash(shaMessageDigest, pInputByteBuffer);
    }

    public static void main(String[] args) throws Exception {
        String encryptThis = "MyPassword";
    }
}
