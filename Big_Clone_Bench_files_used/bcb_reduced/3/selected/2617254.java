package ast.common.util;

import ast.common.error.ASTError;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Some methods to generate hash values.
 *
 * @author Chrissyx
 * @see java.security.MessageDigest
 */
public class HashUtils {

    /**
     * Hidden constructor to prevent instances of this class.
     */
    private HashUtils() {
    }

    /**
     * Generates an hash value from the given text by using SHA-3 or lower SHA
     * algorithms.
     *
     * @param sText Text to calculate the SHA hash value from
     * @return SHA hash string of text
     */
    public static String generateHash(final String sText) {
        String hash = null;
        try {
            hash = HashUtils.generateHash(sText, "SHA-2");
        } catch (final NoSuchAlgorithmException ex) {
            try {
                new ASTError("Can't generate SHA-2 hash!", ex).warning(false);
                hash = HashUtils.generateHash(sText, "SHA-1");
            } catch (final NoSuchAlgorithmException exc) {
                new ASTError("Can't generate any SHA hash!", exc).severe();
            }
        }
        return hash;
    }

    /**
     * Generates an hash value from the given text by using stated algorithm,
     * e.g. SHA-1 or MD5.
     *
     * @param sText Text to calculate an hash value from
     * @param sAlgo Algorithm used for calculation
     * @return Hash string of text
     * @throws NoSuchAlgorithmException If requested algorithm does not exist
     */
    public static String generateHash(final String sText, final String sAlgo) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance(sAlgo);
        md.update(sText.getBytes());
        final Formatter formatter = new Formatter();
        for (final Byte curByte : md.digest()) formatter.format("%x", curByte);
        return formatter.toString();
    }
}
