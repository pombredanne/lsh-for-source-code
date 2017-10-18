package net.redlightning.jbittorrent;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * A set of utility methods used by the program
 * 
 * @author Baptiste Dubuis (baptiste.dubuis@gmail.com)
 * @author Michael Isaacson (michael@redlightning.net)
 * @version 11.4.6
 */
public final class Utils {

    protected static final Charset BYTE_CHARSET = Charset.forName("ISO-8859-1");

    protected static final Charset DEFAULT_CHARSET = Charset.forName("UTF8");

    private static final String[] HEX_SYMBOLS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    public static final String CLIENT_ID = "-RT0031-";

    public static String version;

    public static String name;

    /**
	 * Compare 2 byte arrays byte to byte
	 * 
	 * @param array1 byte[]
	 * @param array2 byte[]
	 * @return boolean
	 */
    public static boolean compareBytes(final byte[] array1, final byte[] array2) {
        boolean returnVal = true;
        if (array1.length != array2.length) {
            returnVal = false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                returnVal = false;
            }
        }
        return returnVal;
    }

    /**
	 * Concatenate the 2 byte arrays
	 * 
	 * @param array1 byte[]
	 * @param array2 byte[]
	 * @return byte[]
	 */
    public static byte[] concat(final byte[] array1, final byte[] array2) {
        byte[] returnVal;
        returnVal = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, returnVal, 0, array1.length);
        System.arraycopy(array2, 0, returnVal, array1.length, array2.length);
        return returnVal;
    }

    /**
	 * Generate the client id, which is a fixed string of length 8 concatenated
	 * with 12 random bytes
	 * 
	 * @return byte[]
	 */
    public static byte[] generateID() {
        byte[] value;
        value = new byte[12];
        new Random(System.currentTimeMillis()).nextBytes(value);
        return Utils.concat(CLIENT_ID.getBytes(), value);
    }

    /**
	 * Compute the SHA-1 hash of the given byte array
	 * 
	 * @param hashThis byte[]
	 * @return byte[]
	 */
    public static byte[] hash(final byte[] hashThis) {
        byte[] returnVal = new byte[0];
        try {
            returnVal = MessageDigest.getInstance("SHA-1").digest(hashThis);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return returnVal;
    }

    /**
	 * Return a subarray of the byte array in parameter.
	 * 
	 * @param input The original array
	 * @param offset Begin index of the subarray
	 * @param length Length of the subarray
	 * @return byte[]
	 */
    public static byte[] padArray(final byte[] input, final int length) {
        byte[] sub = new byte[length];
        if (input.length > length) {
            return input;
        } else {
            for (int i = 0; i < input.length; i++) {
                sub[i] = input[i];
            }
        }
        return sub;
    }

    /**
	 * Return a subarray of the byte array in parameter.
	 * 
	 * @param input The original array
	 * @param offset Begin index of the subarray
	 * @param length Length of the subarray
	 * @return byte[]
	 */
    public static byte[] subArray(final byte[] input, final int offset, final int length) {
        byte[] sub = new byte[length];
        for (int i = offset; i < offset + length; i++) {
            sub[i - offset] = input[i];
        }
        return sub;
    }

    /**
	 * Convert an integer value to its byte array representation
	 * 
	 * @param value the integer to convert
	 * @return byte[] the byte array representation of the integer
	 */
    public static byte[] toArray(final int value) {
        byte[] returnVal = new byte[4];
        for (int i = 0; i < 4; i++) {
            final int offset = (returnVal.length - 1 - i) * 8;
            returnVal[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return returnVal;
    }

    /**
	 * Convert an long value to its byte array representation
	 * 
	 * @param value the value to convert
	 * @return byte[] array holing the long
	 */
    public static byte[] toArray(final long value) {
        return new byte[] { (byte) ((value >> 56) & 0xff), (byte) ((value >> 48) & 0xff), (byte) ((value >> 40) & 0xff), (byte) ((value >> 32) & 0xff), (byte) ((value >> 24) & 0xff), (byte) ((value >> 16) & 0xff), (byte) ((value >> 8) & 0xff), (byte) ((value >> 0) & 0xff) };
    }

    /**
	 * Convert an long value to its byte array representation
	 * 
	 * @param value the value to convert
	 * @return byte[] array holing the short
	 */
    public static byte[] toArray(final short value) {
        return new byte[] { (byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF) };
    }

    /**
	 * Convert a byte array to a boolean array. Bit 0 is represented with false,
	 * Bit 1 is represented with 1
	 * 
	 * @param bytes byte[]
	 * @return boolean[]
	 */
    public static boolean[] toBitArray(final byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0) {
                bits[i] = true;
            }
        }
        return bits;
    }

    /**
	 * Convert a byte array integer (4 bytes) to its int value
	 * 
	 * @param input byte[]
	 * @return int the Integer this byte represents
	 */
    public static int toInt(final byte[] input) {
        int returnVal;
        try {
            returnVal = ByteBuffer.wrap(input).getInt();
        } catch (BufferUnderflowException e) {
            returnVal = -1;
        }
        return returnVal;
    }

    /**
	 * @param input
	 * @return
	 */
    public static char toChar(final byte[] input) {
        return ByteBuffer.wrap(input).getChar();
    }

    /**
	 * Convert a Byte Array to a Long
	 * 
	 * @param input the array to convert
	 * @return long the Long this byte represents
	 */
    public static long toLong(byte[] input) {
        return ByteBuffer.wrap(input).getLong();
    }

    /**
	 * Convert a Byte Array to a Short
	 * 
	 * @param input the array to convert
	 * @return short the short this byte represents
	 */
    public static short toShort(byte[] input) {
        return ByteBuffer.wrap(input).getShort();
    }

    public static String toString(final byte input[]) {
        final String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = null;
        String returnVal = null;
        byte character = 0x00;
        int position = 0;
        if (input != null && input.length > 0) {
            out = new StringBuffer();
            while (position < input.length) {
                character = (byte) (input[position] & 0xF0);
                character = (byte) (character >>> 4);
                character = (byte) (character & 0x0F);
                out.append(pseudo[(int) character]);
                character = (byte) (input[position] & 0x0F);
                out.append(pseudo[(int) character]);
                position++;
            }
        }
        if (out != null) {
            returnVal = out.toString();
        }
        return returnVal;
    }

    /**
	 * Convert a byte to an unsigned integer
	 * 
	 * @param input the byte to convert
	 * @return the converted unsigned integer
	 */
    public static int toUnsignedInt(final byte input) {
        return 0x00 << 24 | input & 0xff;
    }

    /**
	 * Convert a byte array to a URL encoded string
	 * 
	 * @param input byte[]
	 * @return String
	 */
    public static String toURLString(final byte input[]) {
        byte character = 0x00;
        int position = 0;
        StringBuffer out = null;
        String returnVal = null;
        if (input != null && input.length > 0) {
            out = new StringBuffer(input.length * 2);
            while (position < input.length) {
                if ((input[position] >= '0' && input[position] <= '9') || (input[position] >= 'a' && input[position] <= 'z') || (input[position] >= 'A' && input[position] <= 'Z') || input[position] == '$' || input[position] == '-' || input[position] == '_' || input[position] == '.' || input[position] == '!') {
                    out.append((char) input[position]);
                    position++;
                } else {
                    out.append('%');
                    character = (byte) (input[position] & 0xF0);
                    character = (byte) (character >>> 4);
                    character = (byte) (character & 0x0F);
                    out.append(HEX_SYMBOLS[(int) character]);
                    character = (byte) (input[position] & 0x0F);
                    out.append(HEX_SYMBOLS[(int) character]);
                    position++;
                }
            }
        }
        if (out != null) {
            returnVal = out.toString();
        }
        return returnVal;
    }

    /**
	 * Constructor to avoid instantiation
	 */
    private Utils() {
    }

    /**
	 * Convert a character array to a byte array
	 * 
	 * @param hex the character array to convert
	 * @return the character arrat converted to bytes
	 */
    public static byte[] toArray(char[] hex) {
        int length = hex.length / 2;
        byte[] raw = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127) value -= 256;
            raw[i] = (byte) value;
        }
        return raw;
    }

    /**
	 * Encodes hexidecimal string to its value byte array
	 * 
	 * @param hex a hexidecimal character string
	 * @return the string values as an array of bytes
	 */
    public static byte[] toArray(String hex) {
        return toArray(hex.toCharArray());
    }

    /**
	 * Convert an int to a xxx.xxx.xxx.xxx formated IPv4 address
	 * 
	 * @param i the address in integer form
	 * @return the address in xxx.xxx.xxx.xxx form
	 */
    public static String intToIp(int i) {
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }
}
