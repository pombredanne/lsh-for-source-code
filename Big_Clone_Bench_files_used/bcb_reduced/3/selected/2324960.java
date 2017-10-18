package org.brainypdm.modules.datastore.context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.brainypdm.modules.commons.log.BrainyLogger;

/**
 * Unique session id generator. This object is used by datastore for generate
 * session context ID
 * 
 * @author Ben Wang
 */
public class DSSessionIDGenerator {

    protected static final int SESSION_ID_BYTES = 16;

    protected static final String SESSION_ID_HASH_ALGORITHM = "MD5";

    protected static final String SESSION_ID_RANDOM_ALGORITHM = "SHA1PRNG";

    protected static final String SESSION_ID_RANDOM_ALGORITHM_ALT = "IBMSecureRandom";

    protected BrainyLogger log = new BrainyLogger(DSSessionIDGenerator.class);

    protected MessageDigest digest = null;

    protected Random random = null;

    protected static final DSSessionIDGenerator s_ = new DSSessionIDGenerator();

    protected String sessionIdAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-*";

    public static DSSessionIDGenerator getInstance() {
        return s_;
    }

    /**
	 * The SessionIdAlphabet is the set of characters used to create a session
	 * Id
	 */
    public void setSessionIdAlphabet(String sessionIdAlphabet) {
        if (sessionIdAlphabet.length() != 65) {
            throw new IllegalArgumentException("SessionIdAlphabet must be exactly 65 characters long");
        }
        checkDuplicateChars(sessionIdAlphabet);
        this.sessionIdAlphabet = sessionIdAlphabet;
    }

    protected void checkDuplicateChars(String sessionIdAlphabet) {
        char[] alphabet = sessionIdAlphabet.toCharArray();
        for (int i = 0; i < alphabet.length; i++) {
            if (!uniqueChar(alphabet[i], sessionIdAlphabet)) {
                throw new IllegalArgumentException("All chars in SessionIdAlphabet must be unique");
            }
        }
    }

    protected boolean uniqueChar(char c, String s) {
        int firstIndex = s.indexOf(c);
        if (firstIndex == -1) return false;
        return s.indexOf(c, firstIndex + 1) == -1;
    }

    /**
	 * The SessionIdAlphabet is the set of characters used to create a session
	 * Id
	 */
    public String getSessionIdAlphabet() {
        return this.sessionIdAlphabet;
    }

    public synchronized String getSessionId() {
        String id = generateSessionId();
        if (log.isDebugEnabled()) log.debug("getSessionId called: " + id);
        return id;
    }

    /**
	 * Generate a session-id that is not guessable
	 * 
	 * @return generated session-id
	 */
    protected synchronized String generateSessionId() {
        if (this.digest == null) {
            this.digest = getDigest();
        }
        if (this.random == null) {
            this.random = getRandom();
        }
        byte[] bytes = new byte[SESSION_ID_BYTES];
        this.random.nextBytes(bytes);
        bytes = this.digest.digest(bytes);
        return encode(bytes);
    }

    /**
	 * Encode the bytes into a String with a slightly modified Base64-algorithm
	 * This code was written by Kevin Kelley <kelley@ruralnet.net> and adapted
	 * by Thomas Peuss <jboss@peuss.de>
	 * 
	 * @param data
	 *            The bytes you want to encode
	 * @return the encoded String
	 */
    protected String encode(byte[] data) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        char[] alphabet = this.sessionIdAlphabet.toCharArray();
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;
            int val = (0xFF & data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & data[i + 2]);
                quad = true;
            }
            out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 1] = alphabet[val & 0x3F];
            val >>= 6;
            out[index + 0] = alphabet[val & 0x3F];
        }
        return new String(out);
    }

    /**
	 * get a random-number generator
	 * 
	 * @return a random-number generator
	 */
    protected synchronized Random getRandom() {
        long seed;
        Random random = null;
        seed = System.currentTimeMillis();
        seed ^= Runtime.getRuntime().freeMemory();
        try {
            random = SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            try {
                random = SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM_ALT);
            } catch (NoSuchAlgorithmException e_alt) {
                log.error("Could not generate SecureRandom for session-id randomness", e);
                log.error("Could not generate SecureRandom for session-id randomness", e_alt);
                return null;
            }
        }
        random.setSeed(seed);
        return random;
    }

    /**
	 * get a MessageDigest hash-generator
	 * 
	 * @return a hash generator
	 */
    protected synchronized MessageDigest getDigest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(SESSION_ID_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not generate MessageDigest for session-id hashing", e);
            return null;
        }
        return digest;
    }
}
