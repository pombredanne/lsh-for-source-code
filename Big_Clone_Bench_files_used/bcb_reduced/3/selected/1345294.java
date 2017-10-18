package eu.wc.snippets.hashes;

import eu.wc.snippets.conversion.ByteArrayToAny;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class to generate hashes and compare hashes<br>
 * A few algorithms are predefined as public variables.
 * @author Francisco Dominguez Santos
 */
public class HashCheck {

    /**
     * MD5 hashing algorithm
     */
    public static final String MD5 = "MD5";

    /**
     * SHA 256 hashing algorithm
     */
    public static final String SHA256 = "SHA-256";

    /**
     * SHA 512 hashing algorithm
     */
    public static final String SHA512 = "SHA-512";

    /** Insantiation not allowed */
    private HashCheck() {
    }

    /**
     * Generates a hash from the given String,using the given algorithm
     * @param data The String to be hashed, it is converted to byte[] according to UTF-8
     * @param algorithm The algorithm to use
     * @throws java.security.NoSuchAlgorithmException I the specified algorithm does not exist
     * @throws java.io.UnsupportedEncodingException if utf-8 conversion is not supported
     * @return The generated hash as a hexadecimal String
     */
    public static String generateHash(String data, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bData = data.getBytes("UTF-8");
        String hash = ByteArrayToAny.byteArrayToHex(generateHash(bData, algorithm));
        return hash;
    }

    /**
     * Generates a hash from the given byte array,using the given algorithm
     * @param data The byte array to be hashed
     * @param algorithm The algorithm to use
     * @throws java.security.NoSuchAlgorithmException If the algorithm does not exist
     * @return a byte array representing the hash
     */
    public static byte[] generateHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data);
        return md.digest();
    }

    /**
     * Compares the given hash to a hash generated from the given data
     * @param data the data to be hashed
     * @param hash The hash to which it will be compared
     * @param algorithm The algorithm to use
     * @throws java.security.NoSuchAlgorithmException If the algorithm does not exist
     * @return boolean indicating if the hashes are the same
     */
    public static boolean isSameHash(byte[] data, String hash, String algorithm) throws NoSuchAlgorithmException {
        byte[] result = generateHash(data, algorithm);
        String sResult = ByteArrayToAny.byteArrayToHex(result);
        return sResult.equalsIgnoreCase(hash);
    }

    /**
     * Compares the given hash to a hash generated from the given data
     * @param data the data to be hashed
     * @param hash The hash to which it will be compared
     * @param algorithm The algorithm to use
     * @throws java.security.NoSuchAlgorithmException If the algorithm does not exist
     * @return boolean indicating if the hashes are the same
     */
    public static boolean isSameHash(String data, String hash, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bData = data.getBytes("UTF-8");
        return isSameHash(bData, hash, algorithm);
    }
}
