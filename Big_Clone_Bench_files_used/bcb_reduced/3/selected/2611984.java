package de.spotnik.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * Calculates a MD5 value for a given text.
 * 
 * @author Jens Rehp�hler
 * @since 21.08.2006
 */
public final class Digester {

    /** the MD5 digester. */
    private static MessageDigest digest;

    /** the class logger. */
    private static final Logger LOG = Logger.getLogger(Digester.class);

    static {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            LOG.error(ex);
        }
    }

    /**
     * Creates a new instance of <code>Digester</code>.
     */
    private Digester() {
    }

    /**
     * calculates the MD5 hash to the given text.
     * 
     * @param text the text
     * @return the hash
     */
    public static synchronized String calculateHash(String text) {
        byte[] hash = digest.digest(text.getBytes());
        return bytesToHex(hash);
    }

    /**
     * Convenience method to convert a byte array to a hex string.
     * 
     * @param data the byte[] to convert
     * @return String the converted byte[]
     */
    private static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (byte element : data) {
            buf.append(byteToHex(element));
        }
        return buf.toString();
    }

    /**
     * Convenience method to convert a byte to a hex string.
     * 
     * @param data the byte to convert
     * @return String the converted byte
     */
    private static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar(data >>> 4 & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }

    /**
     * Convenience method to convert an int to a hex char.
     * 
     * @param i the int to convert
     * @return char the converted char
     */
    private static char toHexChar(int i) {
        if (0 <= i && i <= 9) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + i - 10);
        }
    }
}
