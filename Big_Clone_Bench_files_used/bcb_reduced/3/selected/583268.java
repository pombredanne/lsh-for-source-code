package aoetec.util.other;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

    public static final String MD5 = "MD5";

    public static final String SHA = "SHA-1";

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /**
     * Convert byte array to hex string
     * 
     * @param b byte array
     * @return hex string
     */
    public static String byteArrayToHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            buf.append(byteToHexString(b[i]));
        }
        return buf.toString();
    }

    /**
     * Convert byte to hex String
     * 
     * @param b byte
     * @return hex String
     */
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n >> 4;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    /**
     * Encode Message Digest with algorithm
     * 
     * @param origin information string
     * @param algorithm algorithm used in digest
     * @return byte array of message digest
     * @exception NoSuchAlgorithmException throws when
     *                algorithm does not support
     */
    public static byte[] encode(String origin, String algorithm) throws NoSuchAlgorithmException {
        String resultStr = null;
        resultStr = new String(origin);
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(resultStr.getBytes());
        return md.digest();
    }

    /**
     * Encode Message Digest with MD5
     * 
     * @param origin information string
     * @return byte array of message digest
     * @exception NoSuchAlgorithmException throws when
     *                MD5 does not support.
     */
    public static byte[] encodeWithMD5(String origin) throws NoSuchAlgorithmException {
        return encode(origin, MD5Utils.MD5);
    }

    /**
     * Encode Message Digest with SHA,
     * 
     * @param origin information string
     * @return byte array of message digest
     * @exception NoSuchAlgorithmException throws when
     *                SHA does not support
     */
    public static byte[] encodeWithSHA(String origin) throws NoSuchAlgorithmException {
        return encode(origin, MD5Utils.SHA);
    }

    /**
     * Encode Message Digest with algorithm
     * 
     * @param origin information string
     * @param algorithm algorithm used in digest
     * @return message digest( in string ) or null
     */
    public static String encode2String(String origin, String algorithm) {
        try {
            return byteArrayToHexString(encode(origin, algorithm));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    /**
     * Encode Message Digest with MD5
     * 
     * @param origin information string
     * @return message digest( in string ) or null
     */
    public static String encode2StringWithMD5(String origin) {
        return encode2String(origin, MD5Utils.MD5);
    }

    /**
     * Encode Message Digest with SHA
     * 
     * @param origin information string
     * @return message digest( in string ) or null
     */
    public static String encode2StringWithSHA(String origin) {
        return encode2String(origin, MD5Utils.SHA);
    }

    /**
     * Check encode to ensure match of message digest
     * 
     * @param origin information string
     * @param mDigest digest information
     * @param algorithm algorithm used in digest
     * @return true if match, or else return false
     * @exception NoSuchAlgorithmException throws when
     *                algorithm does not support.
     */
    public static boolean checkEncode(String origin, byte[] mDigest, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(origin.getBytes());
        if (MessageDigest.isEqual(mDigest, md.digest())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check encode to ensure match of message digest
     * encoded by MD5
     * 
     * @param origin information string
     * @param mDigest digest information encoded by MD5
     * @return true if match or else return false
     */
    public static boolean checkEncodeWithMD5(String origin, byte[] mDigest) {
        try {
            return checkEncode(origin, mDigest, MD5Utils.MD5);
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }

    /**
     * Check encode to ensure match of message digest
     * encoded by SHA
     * 
     * @param origin information string
     * @param mDigest digest information encoded in SHA
     * @return true if match or else return false
     */
    public static boolean checkEncodeWithSHA(String origin, byte[] mDigest) {
        try {
            return checkEncode(origin, mDigest, MD5Utils.SHA);
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(encode2StringWithMD5("0"));
        try {
            System.out.println(checkEncodeWithMD5("0000", encodeWithMD5("0000")));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }
}
