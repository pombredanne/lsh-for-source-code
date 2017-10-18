package org.opennms.acl.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Util class to encript a password
 * 
 * @author Massimiliano Dess&igrave; (desmax74@yahoo.it)
 * @since jdk 1.5.0
 */
public class Cripto {

    public static String stringToSHA(String buffer) {
        try {
            MessageDigest shaDigest = MessageDigest.getInstance(Constants.ALGORITHM_SHA);
            byte[] mybytes = buffer.getBytes();
            byte[] byteResult = shaDigest.digest(mybytes);
            return bytesToHex(byteResult);
        } catch (NoSuchAlgorithmException shaex) {
            return null;
        }
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; ++i) {
            sb.append((Integer.toHexString((b[i] & 0xFF) | 0x100)).substring(1, 3));
        }
        return sb.toString();
    }
}
