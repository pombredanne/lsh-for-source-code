package rmi.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wrapper class for basic MD5 hashing functions.
 * May be used for HTTP digest authentication.
 * 
 * @author Rudolf Scheurer (EIA-FR)
 * @version 0.9 (11.2008)
 *
 */
public class MD5Digest {

    MessageDigest md;

    /**
   * Initializes the MD5 hash utility class.
   * 
   * @throws NoSuchAlgorithmException if initialization fails
   */
    public MD5Digest() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
    }

    /**
   * Returns a MD5 hash value in the form of an array of bytes.
   * 
   * @param  str  the string to be hashed
   * @return    array of bytes containing hash value      
   */
    byte[] doHash(String str) {
        md.reset();
        md.update(str.getBytes());
        return md.digest();
    }

    /**
   * Convenience method allowing to compute a hash value over
   * two (concatenated) byte arrays.
   * 
   * @param byteArray1 first byte array
   * @param byteArray2 second byte array
   * @return hash value for the two byte arrays
   */
    byte[] doHash(byte[] byteArray1, byte[] byteArray2) {
        md.reset();
        md.update(byteArray1);
        md.update(byteArray2);
        return md.digest();
    }

    /**
   * Returns a hexadecimal String representation for a given byte array.
   * <p>May be used for debugging purposes.
   * 
   * @param bytes the byte array to be represented 
   * @return a hexadecimal String representation
   */
    static String toHexString(byte[] bytes) {
        char[] ret = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int c = (int) bytes[i];
            if (c < 0) {
                c += 0x100;
            }
            ret[j++] = Character.forDigit(c / 0x10, 0x10);
            ret[j++] = Character.forDigit(c % 0x10, 0x10);
        }
        return new String(ret);
    }

    /**
   * Returns a byte array corresponding to the given hexadecimal String.
   * <p>May be used for debugging purposes.
   * 
   * @param the hexadecimal String
   * @return the corresponding byte array
   */
    static byte[] toByteArray(String hex) {
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }
}
