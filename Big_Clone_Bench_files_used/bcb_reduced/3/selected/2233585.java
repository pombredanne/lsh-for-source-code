package com.volantis.synergetics.descriptorstore.impl;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.lang.reflect.UndeclaredThrowableException;
import java.io.UnsupportedEncodingException;
import com.volantis.synergetics.utilities.Base64;

/**
 * Generates externalID value using secure random generaton and applying MD5 hash to it.
 * The assumption is that due to the transient nature of stored data and used algorithm
 * it is very unlikely for generated value to be non unique.
 */
public class ExternalIDGenerator {

    private static SecureRandom secureRnd = new SecureRandom();

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * Returns identifier value supposed to be unique in practice 
     *
     * @return unique identifier value
     */
    public static String getNextID() {
        try {
            String str = String.valueOf(secureRnd.nextLong());
            digest.update(str.getBytes("UTF8"));
            byte[] hash = digest.digest();
            return Base64.encodeBytes(hash);
        } catch (UnsupportedEncodingException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
