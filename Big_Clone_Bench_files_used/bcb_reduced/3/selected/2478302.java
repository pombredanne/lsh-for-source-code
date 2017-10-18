package hanasu.encryption;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * @author Marc Miltenberger
 * Hashes a given password.
 */
public final class HashPassword {

    private static String SALT1 = "!\"$%&(/)=THIS is a salt.";

    private static String SALT2 = "for Hanasu";

    private static String SALT3 = "qwertzXYFYXFYXF";

    private static String SALT4 = "salted05330iqIAsfo";

    public static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdedfghijklmnopqrstuvwxyz0123456789!\"§$%&/)\\*_:";

    /**
     * Converts a given byte array to a hexdecimal string 
     * @param data the data byte array
     * @return the string
     */
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Generates a pseudo-random generated String
     * @param rng the random generator
     * @param characters a String containing the possible characters
     * @param length the length of the resulting string
     * @return resulting random string
     */
    public static String generateString(Random rng, String characters, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }

    /**
     * Hashes the given password using a generated salt
     * @param password the password to hash
     * @return the hashed password including salt
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String hashPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SecureRandom crandom = new SecureRandom();
        int length = 8;
        String customsalt = generateString(crandom, characters, length);
        return hashPassword(password, customsalt);
    }

    /**
     * Hashes the given password using a custom salt
     * @param password the password to hash
     * @param customsalt the custom salt
     * @return the hashed password including salt
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    private static String hashPassword(String password, String customsalt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        password = SALT1 + password;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(password.getBytes(), 0, password.length());
        password += convertToHex(md5.digest()) + SALT2 + customsalt;
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] sha1hash = new byte[40];
        md.update(password.getBytes("UTF-8"), 0, password.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash) + "|" + customsalt;
    }

    /**
     * You should avoid to use this method because it hashes a password using a constant salt.
     * @param password the password
     * @return the hashes password
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String unsecureHashConstantSalt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        password = SALT3 + password;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(password.getBytes(), 0, password.length());
        password += convertToHex(md5.digest()) + SALT4;
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] sha1hash = new byte[40];
        md.update(password.getBytes("UTF-8"), 0, password.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    /**
     * Verifies a password based on the given hash.
     * @param hash the hash
     * @param password the password to check
     * @return true if they are equal
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static boolean verifyPassword(String hash, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (!hash.contains("|")) return password.equals(hash) || unsecureHashConstantSalt(password).equals(hash);
        String[] parts = hash.split("\\|");
        String expect = parts[0] + "|" + parts[1];
        String actual = hashPassword(password, parts[1]);
        boolean result = actual.equals(expect);
        return result;
    }
}
