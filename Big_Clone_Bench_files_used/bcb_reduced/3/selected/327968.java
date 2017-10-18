package com.volantis.mcs.utilities;

import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.mcs.localization.LocalizationFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating a fixed length hash of a string.
 */
public class StringHash {

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(StringHash.class);

    /**
     * The algorithm to use for getting the digest - currently MD5 for backwards
     * compatibility purposes.
     */
    private static final String ALGORITHM = "MD5";

    /**
     * The {@link MessageDigest} to use for calculating the hash.
     */
    private static MessageDigest digest;

    /**
     * An array of hex digits used to generate the output of this call (for
     * historical reasons we use upper case here).
     */
    private static final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    static {
        try {
            digest = MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("missing-hashing-algorithm", ALGORITHM);
        }
    }

    /**
     * Generates the MD5 digest of a string and returns it as a fixed-length
     * hex string.
     *
     * @param value The string we require the digest of
     * @return The digest of the string
     */
    public static String getDigestAsHex(String value) {
        byte[] digested = digest.digest(value.getBytes());
        char[] hexStringChars = new char[digested.length * 2];
        int j = 0;
        for (int i = 0; i < digested.length; i++) {
            hexStringChars[j] = hexDigits[(digested[i] >>> 4) & 0xf];
            hexStringChars[j + 1] = hexDigits[digested[i] & 0xf];
            j += 2;
        }
        return new String(hexStringChars);
    }
}
