package org.simpleframework.http.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PerformanceUtil {

    static final int MD5_BUFFER_SIZE = 1024 * 16;

    /**  
     * Get the MD5 hash of the given input stream.  
     *   
     * @param ins The input stream to read from  
     * @return Returns the MD5 hash for the given input stream  
     */
    public static byte[] getMD5Hash(InputStream ins) throws IOException {
        return getMD5Hash(ins, 0, 0);
    }

    /**  
     * Get the MD5 hash of the contents of the given input stream starting at  
     * the given offset and going until length bytes are read  
     *   
     * @param ins The input stream to pull bytes from  
     * @param offset How far from the beginning to start  
     * @param length Number of bytes to read. If zero or smaller read to the end  
     * @return Returns a byte array containing 16 bytes corresponding to the MD5  
     *         hash code for the input.  
     */
    public static byte[] getMD5Hash(InputStream ins, long offset, long length) throws IOException {
        if (ins == null) {
            throw new IllegalArgumentException("ins should not be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset should not be negative");
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        ins.skip(offset);
        int bytesRead = 0;
        if (length > 0) {
            byte[] bytes = new byte[(int) length];
            bytesRead = ins.read(bytes);
            if (bytesRead < length) {
                byte[] lastBytes = new byte[bytesRead];
                System.arraycopy(bytes, 0, lastBytes, 0, lastBytes.length);
                md.update(lastBytes);
                return md.digest();
            }
            md.update(bytes);
        } else {
            byte[] bytes = new byte[MD5_BUFFER_SIZE];
            while (bytesRead > -1) {
                bytesRead = ins.read(bytes);
                if (bytesRead < MD5_BUFFER_SIZE) {
                    byte[] lastBytes = new byte[bytesRead];
                    System.arraycopy(bytes, 0, lastBytes, 0, lastBytes.length);
                    md.update(lastBytes);
                    return md.digest();
                } else {
                    md.update(bytes);
                }
            }
        }
        return md.digest();
    }

    /**  
     * Get a String representation of the byte array in hex format  
     *   
     * @param data The byte array containing the data to convert  
     * @return Returns a string representation of the byte array in hex format  
     */
    public static String toHexString(byte[] data) {
        return toHexString(data, 0, -1);
    }

    /**  
     * Get a String representation of the byte array in hex format  
     *   
     * @param data The byte array containing the data to convert  
     * @param offset How far from the beginning to start  
     * @param length Number of bytes to read; if negative, reads to the end  
     * @return Returns a string representation of the byte array in hex format  
     */
    public static String toHexString(byte[] data, int offset, int length) {
        if (offset < 0 || offset > data.length) {
            throw new IllegalArgumentException("offset outside of valid range");
        }
        if (length > (data.length - offset)) {
            throw new IllegalArgumentException("invalid length");
        }
        int i = 0;
        int len = (length < 0) ? data.length - offset : length;
        char[] ch = new char[len * 2];
        while (len-- > 0) {
            int b = data[offset++] & 0xff;
            int d = b >> 4;
            d = (d < 0xA) ? d + '0' : d - 0xA + 'a';
            ch[i++] = (char) d;
            d = b & 0xF;
            d = (d < 0xA) ? d + '0' : d - 0xA + 'a';
            ch[i++] = (char) d;
        }
        return new String(ch);
    }
}
