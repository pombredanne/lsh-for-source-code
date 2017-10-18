package org.mortbay.j2ee.session;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import org.jboss.logging.Logger;

public class GUIDGenerator implements IdGenerator {

    protected static final Logger _log = Logger.getLogger(GUIDGenerator.class);

    protected static final int SESSION_ID_BYTES = 16;

    protected static final String SESSION_ID_HASH_ALGORITHM = "MD5";

    protected static final String SESSION_ID_RANDOM_ALGORITHM = "SHA1PRNG";

    protected static final String SESSION_ID_RANDOM_ALGORITHM_ALT = "IBMSecureRandom";

    protected MessageDigest _digest = null;

    protected Random _random = null;

    /**
     * Generate a session-id that is not guessable
     * 
     * @return generated session-id
     */
    public synchronized String nextId(HttpServletRequest request) {
        if (_digest == null) {
            _digest = getDigest();
        }
        if (_random == null) {
            _random = getRandom();
        }
        byte[] bytes = new byte[SESSION_ID_BYTES];
        _random.nextBytes(bytes);
        bytes = _digest.digest(bytes);
        return encode(bytes);
    }

    /**
     * Encode the bytes into a String with a slightly modified Base64-algorithm
     * This code was written by Kevin Kelley <kelley@ruralnet.net> and adapted
     * by Thomas Peuss <jboss@peuss.de>
     * 
     * @param data The bytes you want to encode
     * @return the encoded String
     */
    protected String encode(byte[] data) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-*".toCharArray();
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;
            int val = (0xFF & (int) data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & (int) data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & (int) data[i + 2]);
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
                _log.error("Could not generate SecureRandom for session-id randomness", e);
                _log.error("Could not generate SecureRandom for session-id randomness", e_alt);
                return null;
            }
        }
        random.setSeed(seed);
        return random;
    }

    /**
     get a MessageDigest hash-generator
     @return a hash generator
     */
    protected synchronized MessageDigest getDigest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(SESSION_ID_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            _log.error("Could not generate MessageDigest for session-id hashing", e);
            return null;
        }
        return digest;
    }

    public synchronized Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            _log.warn("could not clone IdGenerator", e);
            return null;
        }
    }
}
