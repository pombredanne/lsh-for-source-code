package util;

import java.security.*;

/**
 * Use String toMD5(String text) to get the digest of the String.
 * 
 * @author Xuefeng
 */
public final class HashUtil {

    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String toHexString(byte[] bytes) {
        int length = bytes.length;
        StringBuffer sb = new StringBuffer(length * 2);
        int x = 0;
        int n1 = 0, n2 = 0;
        for (int i = 0; i < length; i++) {
            if (bytes[i] >= 0) x = bytes[i]; else x = 256 + bytes[i];
            n1 = x >> 4;
            n2 = x & 0x0f;
            sb = sb.append(HEX[n1]);
            sb = sb.append(HEX[n2]);
        }
        return sb.toString();
    }

    /**
     * Make MD5 diaguest. The same as <code>toMD5(text.getBytes())</code>
     */
    public static String toMD5(String text) {
        return toMD5(text.getBytes());
    }

    /**
     * Make MD5 diaguest.
     */
    public static String toMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = md.digest(data);
            return toHexString(buf);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert hash code to dirs, such as "83/91/".
     * 
     * @param hash
     * @param levels
     */
    public static String toDirs(int hash, int levels) {
        int dir1 = hash & 0xff;
        if (levels == 1) return dir1 + "/";
        int dir2 = (hash & 0xff00) >>> 8;
        if (levels == 2) return new StringBuilder(20).append(dir1).append('/').append(dir2).append('/').toString();
        int dir3 = (hash & 0xff0000) >>> 16;
        return new StringBuilder(30).append(dir1).append('/').append(dir2).append('/').append(dir3).append('/').toString();
    }
}
