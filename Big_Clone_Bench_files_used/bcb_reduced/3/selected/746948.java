package de.beas.explicanto;

import java.security.MessageDigest;

/**
 * MessageDigest generic class used to obtain the hash (digest) of a message.
 *
 * @author marius.staicu
 * @version 1.0
 *
 */
public class MessageHash {

    public static final String ALGORITHM = "SHA-1";

    public static byte[] getHash(String message) {
        return getHash(message.getBytes());
    }

    public static byte[] getHash(byte[] message) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            return md.digest(message);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return null;
    }
}
