package net.sf.easyweb4j.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import net.sf.easyweb4j.exceptions.HashingException;

/**
 * Utility class to handling hashing requirements of web applications,
 * especially while storing passwords. Please don't store passwords as clear
 * text, hashing them up is really simple.
 * <p>
 * Here is a simple code to hash a user's password.
 * 
 * <pre>
 * public class User extends Model {
 *     private String passwordHash;
 *     private byte[] salt;
 * 
 *     private void hashPassword(String password) {
 *         salt = HashUtil.generateSalt(10);
 *         passwordHash = HashUtil.hash(password, salt, &quot;md5&quot;, &quot;UTF-8&quot;);
 *     }
 * }
 * </pre>
 * 
 * @author Chandra Sekar S
 */
public class HashUtil {

    /**
     * Same as <code>hash(input, new byte[0], algorithm, null);</code>
     * 
     * @see HashUtil#hash(String, byte[], String, String)
     */
    public static String hash(String input, String algorithm) {
        return hash(input, new byte[0], algorithm, null);
    }

    /**
     * Same as <code>hash(input, salt, algorithm, null);</code>
     * 
     * @see HashUtil#hash(String, byte[], String, String)
     */
    public static String hash(String input, byte[] salt, String algorithm) {
        return hash(input, salt, algorithm, null);
    }

    /**
     * Same as <code>hash(input, new byte[0], algorithm, charsetName);</code>
     * 
     * @see HashUtil#hash(String, byte[], String, String)
     */
    public static String hash(String input, String algorithm, String charsetName) {
        return hash(input, new byte[0], algorithm, charsetName);
    }

    /**
     * Generates a has of the given input String by appending the given salt
     * bytes. The given algorithm is passed to MessageDigest and the given
     * charSetName is used to extract independent bytes from the input String.
     * 
     * @param input
     *            The String to be hashed.
     * @param salt
     *            The salt to be added to the input.
     * @param algorithm
     *            The hashing algorithm to be used.
     * @param charsetName
     *            The character set to be used for extracting bytes from the
     *            String.
     * @return The generated hash.
     */
    public static String hash(String input, byte[] salt, String algorithm, String charsetName) {
        try {
            byte[] inputBytes;
            if (charsetName == null) inputBytes = input.getBytes(); else inputBytes = input.getBytes(charsetName);
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(salt);
            md.update(inputBytes);
            byte[] digest = md.digest();
            return digestToHash(digest);
        } catch (Exception e) {
            throw new HashingException("Hashing of input failed.", e);
        }
    }

    /**
     * Generates a random byte array of specified length. This can be passed to
     * the {@link HashUtil#hash(String, byte[], String, String)} method. It uses
     * {@link SecureRandom} with "SHA1PRNG" algorithm to generate the salt
     * bytes.
     * 
     * @param length
     *            The length of the generated salt.
     * @return The salt.
     */
    public static byte[] generateSalt(int length) {
        try {
            byte[] salt = new byte[length];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);
            return salt;
        } catch (Exception e) {
            throw new HashingException("Generation of salt failed.", e);
        }
    }

    /**
     * Converts a digest byte array to it corresponding Hexa-Decimal String.
     * 
     * @param digest
     *            The digest to be converted.
     * @return The hash String.
     */
    private static String digestToHash(byte[] digest) {
        StringBuilder builder = new StringBuilder();
        for (byte value : digest) builder.append(String.format("%02x", value));
        return builder.toString();
    }
}
