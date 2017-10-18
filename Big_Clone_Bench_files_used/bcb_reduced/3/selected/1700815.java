package ch.ethz.dcg.spamato.base.common.util.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ch.ethz.dcg.spamato.base.common.util.StringUtils;

/**
 * @author simon
 * This class provides common functionality for wrappers for all hashing algorithms supported by the JRE 1.4.0 or higher.
 */
public abstract class HashEngine {

    protected MessageDigest digest = null;

    /**
		 * Default Constructor to create a Hashing Engine. 
		 * Throws a <code>RuntimeException</code> if
		 * used JRE doesn't provide the specified algorithm.
		 * 
		 */
    public HashEngine() {
        try {
            this.digest = MessageDigest.getInstance(getHashAlgorithmName());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("System could not initialize " + getHashAlgorithmName() + "-Engine. May be your JRE doesnt provide one. Please use JRE 1.4.0 or higher.", e);
        }
    }

    /**
		 * The Hash algorithm that a subclass uses.
		 * @return the name of the HashAlgorithm to use.
		 */
    protected abstract String getHashAlgorithmName();

    /**
		 * Reset the state of the Hashengine. Calling this method is the
		 * same as creating a new Instance, but is more efficient.
		 */
    public void init() {
        this.digest.reset();
    }

    /**
		 * Add a String to the existing Data to be hashed.
		 * @param string the string to be hashed.
		 */
    public void updateASCII(String string) {
        if (string == null) throw new IllegalArgumentException("The String to hash may not be null.");
        this.digest.update(string.getBytes());
    }

    /**
		 * Get The Hash value as a Hex-String. Calling this method resets the object's state to initial state.
		 * @return String representing the Hashvalue in hexadecimal format.
		 */
    public String digout() {
        byte[] digest = this.digest.digest();
        if (digest != null) return StringUtils.hexEncode(digest); else return null;
    }

    /**
		 * Get the hash value as a <code>byte</code>-array.
		 * Calling this method resets the object's state to initial state.
		 * @return the hash value.
		 */
    public byte[] digest() {
        return digest.digest();
    }

    /**
		 * Performs a final update on the engine and calculates hash value afterwards.
		 * In detail: method first calls <code>update(input)</code> and afterwards
		 * <code>digest()</code>. Calling this method resets the object's state to initial state.
		 * @param input the final value to update the hash engine with.
		 * @return the final hash value of the engine.
		 */
    public byte[] digest(byte[] input) {
        return digest.digest(input);
    }

    /**
		 * Update the hash engine with the input provided.
		 * @param input a single byte to append to the data that will be hashed.
		 */
    public void update(byte input) {
        digest.update(input);
    }

    /**
		 * Updates the hash engine with the input provided.
		 * @param input an array of bytes to be appended to the data that will be hashed.
		 */
    public void update(byte[] input) {
        digest.update(input);
    }

    /**
		 * Updates the hash engine with the input provided.
		 * @param input the byte array to read the data from, that will be appended to the data that will be hashed.
		 * @param offset where to start reading.
		 * @param len how much to read.
		 */
    public void update(byte[] input, int offset, int len) {
        digest.update(input, offset, len);
    }
}
