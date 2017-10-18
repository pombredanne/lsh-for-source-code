package au.edu.uq.itee.maenad.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class to generate different cryptographic hashes.
 */
public class HashGenerator {

    private static final Logger LOGGER = Logger.getLogger(HashGenerator.class.getName());

    /**
     * Generates an MD5 hash from the input string.
     * 
     * @param input A string to be encoded.
     * @return The MD5 hash for the input string.
     */
    public static String createMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            return StringHelper.toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to find MD5 codec", e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Failed to find UTF-8 encoding", e);
        }
        assert false : "We failed to MD5 encode a string.";
        return null;
    }
}
