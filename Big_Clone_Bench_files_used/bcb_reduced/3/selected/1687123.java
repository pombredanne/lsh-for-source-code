package users;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An class for generating a securly encoded passwort string using the MD5 algorithm 
 * @author Alexander Herrmann
 */
public final class MD5 {

    /**
     * The MD5 generator.
     */
    private static MessageDigest md5;

    /**
     * An Array with all hexadecimal characters.
     * Needed for converting byte[] to hexadecimal
     */
    private static final char chHexchar[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Retrieves a hexadecimal character sequence representing the MD5
     * digest of the specified character sequence by using ISO-8859-1 encoding
     *
     * @param sPhrase the string to encode.
     * @return a hexadecimal character sequence representing the MD5 digest of the specified string
     * @throws RuntimeException if an MD5 digest algorithm is not available through the java.security.MessageDigest spi
     */
    public static final String encodeString(String sPhrase) throws RuntimeException {
        return byteToHex(digestString(sPhrase));
    }

    /**
     * Converts a byte-Array to hexadecimal string
     * 
     * @param b the byte[] to be converted
     * @return hexadecimal string
     */
    public static String byteToHex(byte b[]) {
        int len = b.length;
        char[] s = new char[len * 2];
        for (int i = 0, j = 0; i < len; i++) {
            int c = ((int) b[i]) & 0xff;
            s[j++] = chHexchar[c >> 4 & 0xf];
            s[j++] = chHexchar[c & 0xf];
        }
        return new String(s);
    }

    /**
     * Retrieves a byte sequence that represents the MD5 digest string.
     * Used encoding is ISO-8859-1
     *
     * @param sPhrase the string to digest.
     * @return the digest as an array of 16 bytes.
     * @throws RuntimeException if an MD5 digest algorithm is not available through the java.security.MessageDigest spi 
     */
    public static byte[] digestString(String sPhrase) throws RuntimeException {
        byte[] bData;
        String sEncoding = "ISO-8859-1";
        try {
            bData = sPhrase.getBytes(sEncoding);
        } catch (UnsupportedEncodingException ue) {
            throw new RuntimeException(ue.toString());
        }
        return digestBytes(bData);
    }

    /**
     * Retrieves a byte sequence representing the MD5 digest of the
     * specified byte sequence.
     *
     * @param bData the data to digest.
     * @return the MD5 digest as an array of 16 bytes.
     * @throws RuntimeException if an MD5 digest algorithm is not available through the java.security.MessageDigest spi
     */
    public static final byte[] digestBytes(byte[] bData) throws RuntimeException {
        synchronized (MD5.class) {
            if (md5 == null) {
                try {
                    md5 = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e.toString());
                }
            }
            return md5.digest(bData);
        }
    }
}
