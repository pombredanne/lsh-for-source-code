package com.bytetranslation.boltran.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.security.SecureRandom;
import org.jboss.seam.util.Base64;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.AutoCreate;

/**
 * Not super safe, should use a random salt, prepended later on the digest.
 * Should also iterate the hashing a few thousand times to make brute force
 * attacks more difficult. Oh well, probably good enough for storing things
 * in an internal database.
 * <p/>
 */
@Name("hashUtil")
@AutoCreate
public class Hash {

    private static final String HASH_FUNCTION = "SHA-1";

    private static final String PRNG_FUNCTION = "SHA1PRNG";

    private static final String CHARSET = "UTF-8";

    /** The number of iterations of the algorithm */
    private static final int ITERATION_NUMBER = 1000;

    /**
    * Checks a password against the corresponding hash and salt
    * @param password String The password to verify
    * @param salt String The salt
    * @param passwordHash String The password hash
    * @throws NoSuchAlgorithmException If SHA-1 is not supported by the JVM
    * @throws UnsupportedEncodingException
    */
    public static boolean isHashOk(String password, String salt, String passwordHash) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        boolean missingCredentials = false;
        if (password == null || salt == null || passwordHash == null) {
            missingCredentials = true;
            password = "";
            passwordHash = "000000000000000000000000000=";
            salt = "00000000000=";
        }
        String proposedDigest = getPasswordHash(password, salt);
        boolean isOk = proposedDigest.equals(passwordHash) && (!missingCredentials);
        return (isOk);
    }

    /**
    * Generate random salt
    * @return String The random salt
    * @throws NoSuchAlgorithmException If SecureRandom is not supported by the JVM
    */
    public static String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        byte[] bSalt = new byte[8];
        random.nextBytes(bSalt);
        String sSalt = byteToBase64(bSalt);
        return (sSalt);
    }

    /**
    * From password and salt returns
    * the corresponding hash
    * @param password String The password to hash
    * @param salt String The salt
    * @return String The password hash
    * @throws NoSuchAlgorithmException If SHA-1 is not supported by the JVM
    * @throws UnsupportedEncodingException
    */
    public static String getPasswordHash(String password, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bSalt = base64ToByte(salt);
        MessageDigest digest = MessageDigest.getInstance(HASH_FUNCTION);
        digest.reset();
        digest.update(bSalt);
        byte[] input = digest.digest(password.getBytes(CHARSET));
        for (int i = 0; i < ITERATION_NUMBER; i++) {
            digest.reset();
            input = digest.digest(input);
        }
        String passwordHash = byteToBase64(input);
        return passwordHash;
    }

    /**
    * From a base 64 representation, returns the corresponding byte[] 
    * @param data String The base64 representation
    * @return byte[]
    */
    public static byte[] base64ToByte(String data) {
        return Base64.decode(data);
    }

    /**
    * From a byte[] returns a base 64 representation
    * @param data byte[]
    * @return String
    */
    public static String byteToBase64(byte[] data) {
        return Base64.encodeBytes(data);
    }
}
