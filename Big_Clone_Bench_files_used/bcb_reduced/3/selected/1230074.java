package org.cyberaide.gridshell2.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A utility-class that provides random hash-generations.
 */
public final class RandomHashTools {

    private static final Logger logger = Logger.getLogger(RandomHashTools.class.getName());

    private static final Random random = new Random();

    /** Private constructor for utility class. */
    private RandomHashTools() {
    }

    /** Creates a session-key based on the SHA1 of a random value.
     * 
     * @return A session-key in it's hex-representation or null on error
     */
    public static String getRandomSHA1() {
        byte[] buffer = new byte[512];
        random.nextBytes(buffer);
        return getShaOf(buffer);
    }

    /** Get the SHA1 of the String.
     * 
     * @param data A String
     * @return The SHA1 of data or null on error.
     */
    public static String getShaOf(String data) {
        return getShaOf(data.getBytes());
    }

    /** Get the SHA1 of a byte-array.
     * 
     * Logs failures.
     * 
     * @param data
     * @return The SHA1 of the data or null on error
     */
    protected static String getShaOf(byte[] data) {
        final String hashType = "SHA-1";
        try {
            return getHashOf(data, hashType, 40);
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, hashType + " algorithm not found", ex);
            return null;
        }
    }

    /** Get the Hash of a byte-array
     * 
     * @param data Array with data.
     * @param hashType Name of the Hash
     * @param hashLength Length the Hash-String should have
     * @return The Hash
     * @throws java.security.NoSuchAlgorithmException
     */
    protected static String getHashOf(byte[] data, String hashType, int hashLength) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(hashType);
        BigInteger bigint = new BigInteger(1, md.digest(data));
        StringBuffer strbuf = new StringBuffer(bigint.toString(16));
        while (strbuf.length() < hashLength) {
            strbuf.insert(0, "0");
        }
        return strbuf.toString();
    }
}
