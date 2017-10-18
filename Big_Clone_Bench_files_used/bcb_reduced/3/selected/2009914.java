package uk.ac.warwick.dcs.cokefolk.server.users;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * This class provides a centralised point from which to manage the hashing of
 * values as necessary in our system. This is primarily used to hash user's
 * passwords.
 * 
 * @author Tom
 * @designer Tom
 */
public class HashManager {

    private static final Logger LOG = Logger.getLogger(HashManager.class.getName());

    /**
	 * Returns the hash of a given string.
	 * 
	 * @param text
	 *            The string to hash.
	 * @return The hash value of the given string.
	 */
    public static String getHash(String text) {
        if (text == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
            byte[] hashedTextBytes = md.digest();
            BigInteger hashedTextBigInteger = new BigInteger(1, hashedTextBytes);
            String hashedTextString = hashedTextBigInteger.toString(16);
            return hashedTextString;
        } catch (NoSuchAlgorithmException e) {
            LOG.warning(e.toString());
            return null;
        }
    }
}
