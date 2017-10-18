import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * General purpose class with all kinds of useful methods
 * 
 * @author F. Kooman <F.Kooman@student.science.ru.nl>
 * 
 */
public class Utils {

    private Utils() {
    }

    /**
	 * Append a byte array to another byte array
	 * 
	 * @param first
	 *            the byte array to append to
	 * @param second
	 *            the byte array to append
	 * @return the appended array
	 */
    public static byte[] appendToByteArray(byte[] first, byte[] second) {
        int secondLength = (second != null) ? second.length : 0;
        return appendToByteArray(first, second, 0, secondLength);
    }

    /**
	 * Append a byte array to another byte array specifying which part of the
	 * second byte array should be appended to the first
	 * 
	 * @param first
	 *            the byte array to append to
	 * @param second
	 *            the byte array to append
	 * @param offset
	 *            offset in second array to start appending from
	 * @param length
	 *            number of bytes to append from second to first array
	 * @return the appended array
	 */
    public static byte[] appendToByteArray(byte[] first, byte[] second, int offset, int length) {
        if (second == null || second.length == 0) {
            return first;
        }
        int firstLength = (first != null) ? first.length : 0;
        if (length < 0 || offset < 0 || second.length < length + offset) throw new ArrayIndexOutOfBoundsException();
        byte[] result = new byte[firstLength + length];
        if (firstLength > 0) System.arraycopy(first, 0, result, 0, firstLength);
        System.arraycopy(second, offset, result, firstLength, length);
        return result;
    }

    /**
	 * Convert a (part of) a byte array to integer
	 * 
	 * @param data
	 *            the byte array
	 * @param offset
	 *            the offset in the byte array to start from
	 * @return the integer value represented by the byte array
	 */
    public static int byteArrayToInt(byte[] data, int offset) {
        return (int) ((data[offset] << 24 & 0xff000000) | (data[offset + 1] << 16 & 0x00ff0000) | (data[offset + 2] << 8 & 0x0000ff00) | (data[offset + 3] & 0x000000ff));
    }

    /**
	 * Convert a (part of) a byte array to integer assuming the byte array is
	 * Little Endian encoded
	 * 
	 * @param data
	 *            the byte array
	 * @param offset
	 *            the offset in the byte array to start from
	 * @return the integer value represented by the byte array
	 */
    public static int byteArrayToIntLE(byte[] data, int offset) {
        return (int) ((data[offset] & 0x000000ff) | (data[offset + 1] << 8 & 0x0000ff00) | (data[offset + 2] << 16 & 0x00ff0000) | (data[offset + 3] << 24 & 0xff000000));
    }

    /**
	 * Convert a (part of) a byte array to short
	 * 
	 * @param data
	 *            the byte array
	 * @param offset
	 *            the offset in the byte array to start from
	 * @return the short value represented by the byte array
	 */
    public static short byteArrayToShort(byte[] data, int offset) {
        return (short) (((data[offset] << 8)) | ((data[offset + 1] & 0xff)));
    }

    /**
	 * Convert a (part of) a byte array to short assuming the byte array is
	 * Little Endian encoded
	 * 
	 * @param data
	 *            the byte array
	 * @param offset
	 *            the offset in the byte array to start from
	 * @return the short value represented by the byte array
	 */
    public static short byteArrayToShortLE(byte[] data, int offset) {
        return (short) (((data[offset + 1] << 8)) | ((data[offset] & 0xff)));
    }

    /**
	 * Converts a byte array to readable string
	 * 
	 * @param a
	 *            array to print
	 * @return readable byte array string
	 */
    public static String byteArrayToString(byte[] a) {
        return byteArrayToString(a, 0, (a != null) ? a.length : 0);
    }

    public static String byteArrayToString(byte[] a, int offset, int length) {
        if (a == null) return "[null]";
        if (a.length == 0) return "[empty]";
        if (offset < 0 || length < 0 || length + offset > a.length) {
            throw new IndexOutOfBoundsException();
        }
        String result = "";
        for (int i = offset; i < offset + length; i++) {
            result += byteToString(a[i]);
        }
        return result;
    }

    /**
	 * Decode a byte array which contains UTF-16 bytes to string.
	 * 
	 * @param data
	 *            the byte array containing the UTF-16 bytes NOT terminated by
	 *            0x00 0x00
	 * @param offset
	 *            the offset in the array
	 * @param length
	 *            the length of the array
	 * @return the decoded bytes
	 */
    public static String bytesToString(byte[] data, int offset, int length) {
        if (data == null) return null;
        String s = null;
        try {
            s = new String(data, offset, length, "UTF-16");
        } catch (UnsupportedEncodingException e) {
        }
        return s;
    }

    /**
	 * Decode a byte array which contains UTF-16 Little Endian bytes to string.
	 * We assume the byte array terminates with 0x00 0x00.
	 * 
	 * @param data
	 *            the byte array containing the UTF-16 LE bytes terminated by
	 *            0x00 0x00
	 * @return the decoded string
	 */
    public static String bytesToStringLE(byte[] data) {
        if (data != null && data.length >= 2) return bytesToStringLE(data, 0, data.length - 2); else return "";
    }

    /**
	 * Decode a byte array which contains UTF-16 Little Endian bytes to string.
	 * 
	 * @param data
	 *            the byte array containing the UTF-16 LE bytes NOT terminated
	 *            by 0x00 0x00
	 * @param offset
	 *            the offset in the array
	 * @param length
	 *            the length of the array
	 * @return the decoded bytes
	 */
    public static String bytesToStringLE(byte[] data, int offset, int length) {
        if (data == null) return null;
        String s = null;
        try {
            s = new String(data, offset, length, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
        }
        return s;
    }

    /**
	 * Convert a byte to a human readable representation
	 * 
	 * @param b
	 *            the byte
	 * @return the human readable representation
	 */
    public static String byteToString(int b) {
        String s = Integer.toHexString(b);
        if (s.length() == 1) s = "0" + s; else s = s.substring(s.length() - 2);
        return s;
    }

    /**
	 * Read a configuration value from a file
	 * 
	 * @param configFile
	 *            the file to read from
	 * @param requestKey
	 *            the configuration key to request
	 * @return the value belonging to the requested key
	 * @throws IOException
	 *             if reading from the configuration file fails or file is
	 *             broken
	 */
    public static String getConfigEntry(File configFile, String requestKey) throws IOException {
        if (configFile == null || !configFile.exists()) return null;
        FileReader f = new FileReader(configFile);
        BufferedReader b = new BufferedReader(f);
        while (b.ready()) {
            String line = b.readLine();
            if (line.trim().length() == 0) continue;
            if (line.trim().startsWith("#")) continue;
            String[] keyValue = line.split("=");
            if (keyValue.length != 2) throw new IOException("broken configuration file");
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            if (requestKey.equals(key)) return value;
        }
        return null;
    }

    /**
	 * Get the SHA1 hash of the specified file
	 * 
	 * @param f
	 *            the file
	 * @return the SHA1 hash
	 * @throws IOException
	 */
    public static byte[] getFileHash(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] data = new byte[1024];
        int bytesRead;
        MessageDigest hash = null;
        try {
            hash = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
        }
        while ((bytesRead = fis.read(data)) > 0) {
            hash.update(data, 0, bytesRead);
        }
        return hash.digest();
    }

    /**
	 * Look through data (hay stack) looking for a pattern that results in a
	 * certain SHA1 hash (needle).
	 * 
	 * This is a brute force search. For every possible (continuous) sub array
	 * of hayStack we calculate the SHA1 hash and match it with the hash we were
	 * looking for (needle).
	 * 
	 * @param hayStack
	 *            the data to analyze
	 * @param needle
	 *            the hash being looked for
	 * @return the {offset, length} of some sub array in hay stack resulting in
	 *         the hash specified by needle, or null if no match was found
	 */
    public static int[] hashCalculator(byte[] hayStack, byte[] needle) {
        MessageDigest hash = null;
        try {
            hash = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i <= hayStack.length; i++) {
            for (int j = 0; j <= hayStack.length - i; j++) {
                hash.update(hayStack, j, i);
                byte[] dig = hash.digest();
                if (Arrays.equals(dig, needle)) return new int[] { j, i };
            }
        }
        return null;
    }

    public static String hexDump(byte[] a) {
        return hexDump(a, 0, (a != null) ? a.length : 0);
    }

    public static String hexDump(byte[] a, int offset, int length) {
        int WIDTH = 16;
        if (a == null) return "[null]";
        if (a.length == 0) return "[empty]";
        String result = "";
        if (offset < 0 || length < 0 || length + offset > a.length) {
            throw new IndexOutOfBoundsException();
        }
        int rows = length / WIDTH;
        int lastRow = length % WIDTH;
        if (lastRow != 0) rows++;
        for (int i = 0; i < rows; i++) {
            int m = (lastRow != 0 && i == rows - 1) ? lastRow : WIDTH;
            for (int j = 0; j < m; j++) {
                result += byteToString(a[offset + i * WIDTH + j]) + " ";
            }
            for (int z = 0; z < WIDTH - m; z++) {
                result += "   ";
            }
            result += " |";
            for (int j = 0; j < m; j++) {
                if (a[offset + i * WIDTH + j] >= 0x20 && a[offset + i * WIDTH + j] < 0x7f) result += (char) a[offset + i * WIDTH + j]; else result += ".";
            }
            result += "|\n";
        }
        return result;
    }

    /**
	 * Convert an integer to byte array
	 * 
	 * @param v
	 *            the integer
	 * @return the byte array representing the integer
	 */
    public static byte[] intToByteArray(int v) {
        return new byte[] { (byte) ((v & 0xFF000000) >> 24), (byte) ((v & 0x00FF0000) >> 16), (byte) ((v & 0x0000FF00) >> 8), (byte) ((v & 0x000000FF)) };
    }

    /**
	 * Convert an integer to byte array using Little Endian representation
	 * 
	 * @param v
	 *            the integer
	 * @return the byte array representing the integer
	 */
    public static byte[] intToByteArrayLE(int v) {
        return new byte[] { (byte) ((v & 0x000000FF)), (byte) ((v & 0x0000FF00) >> 8), (byte) ((v & 0x00FF0000) >> 16), (byte) ((v & 0xFF000000) >> 24) };
    }

    /**
	 * Convert a short to byte array
	 * 
	 * @param s
	 *            the short
	 * @return the byte array representing the short
	 */
    public static byte[] shortToByteArray(short s) {
        return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
    }

    /**
	 * Convert a short to byte array using Little Endian representation
	 * 
	 * @param s
	 *            the short
	 * @return the byte array representing the short
	 */
    public static byte[] shortToByteArrayLE(short s) {
        return new byte[] { (byte) (s & 0x00FF), (byte) ((s & 0xFF00) >> 8) };
    }

    /**
	 * Convert a short to string and pad it so it is always at least of length
	 * two.
	 * 
	 * @param s
	 *            the short to convert
	 * @return the (padded) string
	 */
    public static String shortToTwoDigitString(short s) {
        return ((s < 10) ? "0" + s : "" + s);
    }

    /**
	 * Convert a string to a UTF-16 Little Endian byte array terminated with
	 * 0x00 0x00
	 * 
	 * @param s
	 *            the string to convert
	 * @param terminator
	 *            whether or not to add String terminator bytes
	 * @return the UTF-16 LE byte array terminated with 0x00 0x00
	 */
    public static byte[] stringToBytes(String s, boolean terminator) {
        if (s == null && terminator) return new byte[] { 0x00, 0x00 }; else if (s == null) return null;
        byte[] stringBytes = new byte[s.length() * 2 + ((terminator) ? 2 : 0)];
        try {
            byte[] bA = s.getBytes("UTF-16LE");
            System.arraycopy(bA, 0, stringBytes, 0, bA.length);
        } catch (UnsupportedEncodingException e) {
        }
        return stringBytes;
    }

    /**
	 * Return a specific part of a byte array starting from <code>offset</code>
	 * with <code>length</code>
	 * 
	 * @param array
	 *            the byte array
	 * @param offset
	 *            the offset in the array from where to start in bytes
	 * @param length
	 *            the number of bytes to get
	 * @return the sub byte array
	 */
    public static byte[] subByteArray(byte[] array, int offset, int length) {
        return appendToByteArray(null, array, offset, length);
    }

    public static int min(int size, int size2) {
        if (size <= size2) return size; else return size2;
    }
}
