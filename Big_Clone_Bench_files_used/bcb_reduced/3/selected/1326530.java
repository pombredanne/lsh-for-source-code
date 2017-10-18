package peer.util;

import java.security.MessageDigest;

/**
 * Utility class to perform hash computation.
 * @author Thomas
 */
public class Thumbprint {

    /** Default hash computation algorithm */
    public static final String ALGORITHM = "SHA-1";

    /**
     * Performs the hash computation using the default algorithm method defined
     * in {@link org.reddant.util.Thumbprint}{@code .METHOD}.
     * @param value the value to be hashed
     * @return
     * @throws Exception
     */
    public static String digest(String value) throws Exception {
        return Thumbprint.digest(value, ALGORITHM);
    }

    /**
     * Returns the hash computation using the algorithm supplied. The list of
     * standard algorithm names can be found here:
     * http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest
     *
     * @param value the value to be hashed
     * @param algorithm the algorithm to be used for the hash computation
     * @return
     * @throws Exception
     * @see java.security.MessageDigest
     */
    public static String digest(String value, String algorithm) throws Exception {
        MessageDigest algo = MessageDigest.getInstance(algorithm);
        algo.reset();
        algo.update(value.getBytes("UTF-8"));
        return StringTool.byteArrayToString(algo.digest());
    }

    /**
     * Return the hash computation of a byte array using the default algorithm
     * method defined in {@link org.reddant.util.Thumbprint}{@code .METHOD}.
     * @param b_value the byte array to be hashed
     * @return
     * @throws Exception
     */
    public static byte[] digest(byte[] b_value) throws Exception {
        return Thumbprint.digest(b_value, ALGORITHM);
    }

    /**
     * Returns the hash computation using the algorithm supplied. The list of
     * standard algorithm names can be found here:
     * http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest
     * @param b_value the byte array to be hashed
     * @param algorithm the algorithm to be used for the hash computation
     * @return
     * @throws Exception
     */
    public static byte[] digest(byte[] b_value, String algorithm) throws Exception {
        MessageDigest algo = MessageDigest.getInstance(algorithm);
        algo.reset();
        algo.update(b_value);
        return algo.digest();
    }

    /**
     * Return the hash computation from a byte array as a string using the
     * default algorithm method defined in
     * {@link org.reddant.util.Thumbprint}{@code .METHOD}.
     * @param b_value
     * @return
     * @throws Exception
     */
    public static String digestBytesToString(byte[] b_value) throws Exception {
        return Thumbprint.digestBytesToString(b_value, ALGORITHM);
    }

    /**
     * Return the hash computation from a byte array as a string.
     * The list of standard algorithm names can be found here:
     * http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest
     * @param b_value the byte array to be hashed
     * @param algorithm the algorithm to be used for the hash computation
     * @return
     * @throws Exception
     */
    public static String digestBytesToString(byte[] b_value, String algorithm) throws Exception {
        MessageDigest algo = MessageDigest.getInstance(algorithm);
        algo.reset();
        algo.update(b_value);
        return StringTool.byteArrayToString(algo.digest());
    }
}
