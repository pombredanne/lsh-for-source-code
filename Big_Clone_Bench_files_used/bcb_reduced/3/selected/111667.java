package com.usoog.commons.util.various;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class will create an MD5 sum of any String provided.
 *
 * @author Jimmy Axenhus
 * @author Hylke van der Schaaf
 */
public final class MD5 {

    /**
	 * The MD5 instance.
	 */
    private static MD5 instance = null;

    /**
	 * Characters allowed in the final sum.
	 */
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
	 * The MEssageDigest instance.
	 */
    private final MessageDigest md;

    /**
	 * Constructor is private so you must use the getInstance method.
	 *
	 * @throws NoSuchAlgorithmException If it could not find an MD5 hashing
	 *	algorithm to use.
	 */
    private MD5() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
    }

    /**
	 * This returns the singleton instance.
	 *
	 * @return This MD5 instance.
	 * @throws NoSuchAlgorithmException If the MD5 "engine" couldn't be found.
	 */
    public static synchronized MD5 getInstance() throws NoSuchAlgorithmException {
        if (instance == null) {
            instance = new MD5();
        }
        return instance;
    }

    /**
	 * This will hash the specific String to an unique checksum.
	 *
	 * @param dataToHash The data to be hashed.
	 * @return The checksum.
	 */
    public synchronized String hashData(String dataToHash) {
        return hexStringFromBytes(calculateHash(dataToHash.getBytes(Charset.forName("UTF-8"))));
    }

    /**
	 * This will hash the specific String to an unique checksum.
	 *
	 * @param dataToHash The data to be hashed.
	 * @return The checksum.
	 */
    public synchronized String hashData(byte[] dataToHash) {
        return hexStringFromBytes(calculateHash(dataToHash));
    }

    /**
	 * Calculate the hash of the given byte array.
	 *
	 * @param dataToHash The data to hash.
	 * @return The hash of the data.
	 */
    private synchronized byte[] calculateHash(byte[] dataToHash) {
        md.update(dataToHash, 0, dataToHash.length);
        return md.digest();
    }

    /**
	 * Converts a byte array to a string of hexadecimal characters (a string
	 * containing only 0-9A-F).
	 *
	 * @param b The byte array to convert.
	 * @return A string containing only 0-9A-F.
	 */
    public synchronized String hexStringFromBytes(byte[] b) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int msb = ((int) b[i] & 0x000000FF) / 16;
            int lsb = ((int) b[i] & 0x000000FF) % 16;
            hex.append(HEX_CHARS[msb]);
            hex.append(HEX_CHARS[lsb]);
        }
        return hex.toString();
    }
}
