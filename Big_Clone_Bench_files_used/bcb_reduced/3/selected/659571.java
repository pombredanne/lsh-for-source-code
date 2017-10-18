package database;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import log.SystemLog;

/**
 * The Cryptography Class.
 * 
 * @author Fraser P. Newton
 * @version 1.0.0
 */
public class Crypto {

    /** The String digest. */
    private static MessageDigest StringDigest = null;

    /** The Digest bytes. */
    private static byte DigestBytes[] = null;

    /** The Digest number. */
    private static BigInteger DigestNumber = null;

    /** The Constant HASH_LIMIT. */
    private static final int HASH_LIMIT = 16;

    /** The Constant HASH_LENGTH. */
    private static final int HASH_LENGTH = 32;

    /**
	 * Gets the sha1 hash of a string.
	 * 
	 * @param data
	 *            The data to digest
	 * @return The sha1 hash of the data
	 */
    public static String getSha1Hash(String data) {
        try {
            StringDigest = MessageDigest.getInstance("SHA-1");
            DigestBytes = StringDigest.digest(data.getBytes());
            DigestNumber = new BigInteger(1, DigestBytes);
            final StringBuilder hashText = new StringBuilder();
            hashText.append(DigestNumber.toString(HASH_LIMIT));
            while (hashText.length() < HASH_LENGTH) {
                hashText.append("0" + hashText);
            }
            return hashText.toString();
        } catch (NoSuchAlgorithmException e) {
            if (!SystemLog.canLogMessage(e.getMessage(), Level.SEVERE)) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
	 * toString() Override.
	 * 
	 * @return An empty string
	 */
    public String toString() {
        return "";
    }
}
