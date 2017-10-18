package imi.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * This class provides easy convenience methods to interact with the MD5 support
 * build into java.
 * @author Ronald E Dahlgren
 */
public class MD5HashUtils {

    /** Logger ref **/
    private static final Logger logger = Logger.getLogger(MD5HashUtils.class.getName());

    /** Characters we use in the output string for hex **/
    private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static MessageDigest data = null;

    static {
        try {
            data = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsa) {
            logger.severe(nsa.getMessage());
        }
    }

    /**
     * Hash the provided byte array.
     * @param dataToHash
     * @return
     */
    private static byte[] getBytesFromHash(byte[] dataToHash) {
        byte[] result = null;
        synchronized (data) {
            data.update(dataToHash, 0, dataToHash.length);
            result = data.digest();
        }
        return result;
    }

    /**
     * Hash the provided data and return it in a friendly string.
     * @param dataToHash
     * @return
     */
    public static String getStringFromHash(byte[] dataToHash) {
        String result = null;
        byte[] byteResult = null;
        synchronized (data) {
            data.update(dataToHash, 0, dataToHash.length);
            byteResult = data.digest();
        }
        result = hexStringFromByteArray(byteResult);
        return result;
    }

    /**
     * Convert a byte array into a hexadecimal string. This is a good candidate to
     * head into another utils file some day.
     * @param data
     * @return
     */
    public static String hexStringFromByteArray(byte[] data) {
        StringBuilder result = new StringBuilder();
        int highOrderByte = 0;
        int lowOrderByte = 0;
        for (int i = 0; i < data.length; i++) {
            highOrderByte = ((int) data[i] & 0x000000FF) / 16;
            lowOrderByte = ((int) data[i] & 0x000000FF) % 16;
            result.append(hexChars[highOrderByte] + hexChars[lowOrderByte]);
        }
        return result.toString();
    }
}
