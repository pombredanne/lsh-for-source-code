package jBittorrentAPI;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.nio.*;

/**
 * A set of utility methods used by the program
 *
 * @author Baptiste Dubuis
 * @version 0.1
 */
public class Utils {

    /**
     * Convenience method to convert a byte to a hex string.
     *
     * @param data the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }

    /**
     * Convenience method to convert a byte array to a hex string.
     *
     * @param data the byte[] to convert
     * @return String the converted byte[]
     */
    public static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]));
        }
        return buf.toString();
    }

    /**
     * Convenience method to convert an int to a hex char.
     *
     * @param i the int to convert
     * @return char the converted char
     */
    public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) return (char) ('0' + i); else return (char) ('a' + (i - 10));
    }

    /**
     * Convert a byte array to a URL encoded string
     * @param in byte[]
     * @return String
     */
    public static String byteArrayToURLString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);
        while (i < in.length) {
            if ((in[i] >= '0' && in[i] <= '9') || (in[i] >= 'a' && in[i] <= 'z') || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$' || in[i] == '-' || in[i] == '_' || in[i] == '.' || in[i] == '!') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0);
                ch = (byte) (ch >>> 4);
                ch = (byte) (ch & 0x0F);
                out.append(pseudo[(int) ch]);
                ch = (byte) (in[i] & 0x0F);
                out.append(pseudo[(int) ch]);
                i++;
            }
        }
        String rslt = new String(out);
        return rslt;
    }

    public static String byteArrayToByteString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);
        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            out.append(pseudo[(int) ch]);
            ch = (byte) (in[i] & 0x0F);
            out.append(pseudo[(int) ch]);
            i++;
        }
        String rslt = new String(out);
        return rslt;
    }

    /**
     * Convert an integer value to its byte array representation
     * @param value int
     * @return byte[]
     */
    public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    /**
     * Convert a byte array integer (4 bytes) to its int value
     * @param b byte[]
     * @return int
     */
    public static int byteArrayToInt(byte[] b) {
        if (b.length == 4) return b[0] << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff); else if (b.length == 2) return 0x00 << 24 | 0x00 << 16 | (b[0] & 0xff) << 8 | (b[1] & 0xff);
        return 0;
    }

    public static int byteToUnsignedInt(byte b) {
        return 0x00 << 24 | b & 0xff;
    }

    public static byte[] intToByteArray4(int i) {
        return new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i };
    }

    /**
     * Compute the SHA-1 hash of the bytes in the given buffer
     * @param hashThis ByteBuffer
     * @return byte[]
     */
    public static byte[] hash(ByteBuffer hashThis) {
        try {
            byte[] hash = new byte[20];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(hashThis);
            return md.digest();
        } catch (NoSuchAlgorithmException nsae) {
        }
        return null;
    }

    /**
     * Compute the SHA-1 hash of the given byte array
     * @param hashThis byte[]
     * @return byte[]
     */
    public static byte[] hash(byte[] hashThis) {
        try {
            byte[] hash = new byte[20];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            hash = md.digest(hashThis);
            return hash;
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("SHA-1 algorithm is not available...");
            System.exit(2);
        }
        return null;
    }

    /**
     * Generate the client id, which is a fixed string of length 8 concatenated with 12 random bytes
     * @return byte[]
     */
    public static byte[] generateID() {
        byte[] id = new byte[12];
        Random r = new Random(System.currentTimeMillis());
        r.nextBytes(id);
        return Utils.concat("-BE0001-".getBytes(), id);
    }

    /**
     * Concatenate the 2 byte arrays
     * @param a byte[]
     * @param b byte[]
     * @return byte[]
     */
    public static byte[] concat2(byte[] a, byte[] b) {
        ByteBuffer bb = ByteBuffer.allocate(a.length + b.length);
        bb.put(a);
        bb.put(b);
        return bb.array();
    }

    /**
     * Concatenate the 2 byte arrays
     * @param b1 byte[]
     * @param b2 byte[]
     * @return byte[]
     */
    public static byte[] concat(byte[] b1, byte[] b2) {
        byte[] b3 = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, b3, 0, b1.length);
        System.arraycopy(b2, 0, b3, b1.length, b2.length);
        return b3;
    }

    /**
     * Concatenate the byte array and the byte
     * @param b1 byte[]
     * @param b2 byte
     * @return byte[]
     */
    public static byte[] concat(byte[] b1, byte b2) {
        byte[] b3 = new byte[b1.length + 1];
        byte[] temp = new byte[] { b2 };
        System.arraycopy(b1, 0, b3, 0, b1.length);
        System.arraycopy(temp, 0, b3, b1.length, 1);
        return b3;
    }

    /**
     * Convert a byte array representing an unsigned integer (4bytes) to its long value
     * @param b byte[]
     * @return long
     */
    public static final long unsignedIntToLong(byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;
        return l;
    }

    /**
     * Convert a byte array to a boolean array. Bit 0 is represented with false, Bit 1 is represented with 1
     * @param bytes byte[]
     * @return boolean[]
     */
    public static boolean[] byteArray2BitArray(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0) bits[i] = true;
        }
        return bits;
    }

    /**
     * Return a subarray of the byte array in parameter.
     * @param b The original array
     * @param offset Begin index of the subarray
     * @param length Length of the subarray
     * @return byte[]
     */
    public static byte[] subArray(byte[] b, int offset, int length) {
        byte[] sub = new byte[length];
        for (int i = offset; i < offset + length; i++) sub[i - offset] = b[i];
        return sub;
    }

    /**
     * Compare 2 byte arrays byte to byte
     * @param a byte[]
     * @param b byte[]
     * @return boolean
     */
    public static boolean bytesCompare(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    /**
     * Copy the input byte array to the output byte array
     * @param in byte[]
     * @param out byte[]
     */
    public static void copy(byte[] in, byte[] out) {
        for (int i = 0; i < out.length && i < in.length; i++) out[i] = in[i];
    }

    public static byte[] toByteArray(BitSet bits) {
        byte[] bytes = new byte[bits.length() / 8 + 1];
        for (int i = 0; i < bits.length(); i++) {
            if (bits.get(i)) {
                bytes[i / 8] |= 1 << (7 - i % 8);
            }
        }
        return bytes;
    }
}
