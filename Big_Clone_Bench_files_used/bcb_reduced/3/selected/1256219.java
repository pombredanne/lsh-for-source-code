package org.ideenmanufaktur.games.threedriving.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Build hash codes
 * 
 * @author Juergen
 */
public final class Hash {

    /**
     * Geneate hash
     * 
     * @param input string to hash
     * @param algorithm algorithm to use
     * @return hash
     */
    private static final String hash(String input, String algorithm) {
        try {
            MessageDigest dig = MessageDigest.getInstance(algorithm);
            dig.update(input.getBytes());
            StringBuffer result = new StringBuffer();
            byte[] digest = dig.digest();
            String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
            for (int i = 0; i < digest.length; i++) {
                int u = digest[i];
                u &= 0x000000FF;
                int highCount = u / 16;
                int lowCount = u - (highCount * 16);
                result.append(hex[highCount]);
                result.append(hex[lowCount]);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Create an md2 hash code
     * 
     * @param input String to hash
     * @return the hash
     */
    public static final String md2(String input) {
        return Hash.hash(input, "MD2");
    }

    /**
     * Create an md5 hash code
     * 
     * @param input String to hash
     * @return the hash
     */
    public static final String md5(String input) {
        return Hash.hash(input, "MD5");
    }

    /**
     * Create an sha1 hash code
     * 
     * @param input String to hash
     * @return the hash
     */
    public static final String sha1(String input) {
        return Hash.hash(input, "SHA-1");
    }

    /**
     * Create an sha256 hash code
     * 
     * @param input String to hash
     * @return the hash
     */
    public static final String sha256(String input) {
        return Hash.hash(input, "SHA-256");
    }

    /**
     * Create an sha384 hash code
     * 
     * @param input String to hash
     * @return the hash
     */
    public static final String sha384(String input) {
        return Hash.hash(input, "SHA-384");
    }

    /**
     * Create an sha512 hash code
     * 
     * @param input String to hash
     * @return the hash
     */
    public static final String sha512(String input) {
        return Hash.hash(input, "SHA-512");
    }
}
