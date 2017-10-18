package de.iteratec.turm.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class that takes a plain-text user supplied password, and applies a one-way message digest
 * algorithm to generate an encrypted password that will be compared against an already encrypted
 * password record, most likely held in a database. This class will typically be used by a servlet
 * or struts action class that needs to enforce programmatic security.
 * 
 * This class was copied from the iteraplan project.
 */
public final class PasswordEncryption {

    public static final String MESSAGEDIGEST_SHA = "SHA";

    public static final String MESSAGEDIGEST_MD2 = "MD2";

    public static final String MESSAGEDIGEST_MD5 = "MD5";

    public static final byte BYTE_MASK = 0x0f;

    private static PasswordEncryption instance;

    /** Private modifier to prevent instantiation */
    private PasswordEncryption() {
    }

    /**
   * Encrypts the supplied plaintext password with the default message digest MD5 algorithm.
   * 
   * @throws UnsupportedEncodingException 
   * @throws NoSuchAlgorithmException 
   */
    public synchronized String getEncryptedPassword(String plaintext) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return getEncryptedPassword(plaintext, MESSAGEDIGEST_MD5);
    }

    /**
   * Encrypts the supplied plaintext password with the supplied algorithm.
   */
    public synchronized String getEncryptedPassword(String plaintext, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = null;
        md = MessageDigest.getInstance(algorithm);
        md.update(plaintext.getBytes("UTF-8"));
        return bytesToHexString(md.digest());
    }

    /**
   * Utilises the Singleton pattern as there is no need to create separate instances
   */
    public static synchronized PasswordEncryption getInstance() {
        if (instance == null) {
            instance = new PasswordEncryption();
        }
        return instance;
    }

    private static final char[] HEXARRAY = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
   * Converts a byte[] array into a Hex string
   * 
   * @param inByteArray
   * @return string
   */
    public static String bytesToHexString(byte[] inByteArray) {
        return bytesToHexString(inByteArray, 0, inByteArray.length);
    }

    /**
   * Converts the given byte array into a hex string representation.
   * 
   * @param inByteArray
   * @param offset
   * @param len
   * @return a hex String representation of the byte array.
   */
    public static String bytesToHexString(byte[] inByteArray, int offset, int len) {
        if (inByteArray == null) {
            return null;
        }
        int position;
        StringBuffer returnBuffer = new StringBuffer();
        for (position = offset; position < len; position++) {
            returnBuffer.append(HEXARRAY[((inByteArray[position] >> 4) & BYTE_MASK)]);
            returnBuffer.append(HEXARRAY[(inByteArray[position] & BYTE_MASK)]);
        }
        return returnBuffer.toString();
    }
}
