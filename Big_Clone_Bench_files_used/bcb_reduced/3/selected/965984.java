package jm.lib.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import jm.lib.util.other.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Jiming Liu
 */
public final class CodeUtil {

    private CodeUtil() {
    }

    private static final Log logger = LogFactory.getLog(CodeUtil.class);

    public static String base64Encode(String content) {
        return new String(base64Encode(content.getBytes()));
    }

    public static String base64Encode(byte[] content) {
        return new String(Base64.encode(content));
    }

    public static String base64Decode(String content) {
        return new String(base64Decode(content.getBytes()));
    }

    public static String base64Decode(byte[] content) {
        return new String(Base64.decode(content));
    }

    public static String hash(String content) {
        if (null == content) return "";
        byte key[] = content.getBytes();
        int result = 0x238F13AF * key.length;
        for (int i = 0; i < key.length; i++) {
            result = (result + (key[i] << (i * 5 % 24))) & 0x7FFFFFFF;
        }
        result = (1103515243 * result + 12345) & 0x7FFFFFFF;
        return Integer.toString(result, 36);
    }

    public static byte[] md5(String content) {
        return md5(content.getBytes());
    }

    public static byte[] md5(String content, String charsetName) throws UnsupportedEncodingException {
        return md5(content.getBytes(charsetName));
    }

    public static byte[] md5(byte[] content) {
        if (null == content) return null;
        byte[] result = null;
        try {
            result = MessageDigest.getInstance("MD5").digest(content);
        } catch (NoSuchAlgorithmException ex) {
            logger.warn("Cannot find MD5 algorithm!", ex);
        }
        return result;
    }

    public static String md5Str(String content) {
        return byte2String(md5(content));
    }

    public static String md5Str(byte[] content) {
        return byte2String(md5(content));
    }

    public static byte[] sha(String content) {
        return sha(content.getBytes());
    }

    public static byte[] sha(byte[] content) {
        if (null == content) return null;
        byte[] result = null;
        try {
            result = MessageDigest.getInstance("SHA").digest(content);
        } catch (NoSuchAlgorithmException ex) {
            logger.warn("Cannot find SHA algorithm!", ex);
        }
        return result;
    }

    public static String shaStr(String content) {
        return byte2String(sha(content));
    }

    public static String shaStr(byte[] content) {
        return byte2String(sha(content));
    }

    static final char[] charArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String byte2String(byte[] content) {
        StringBuilder buffer = new StringBuilder(content.length * 2);
        for (int i = 0; i < content.length; i++) {
            buffer.append(charArray[(0xF0 & content[i]) >> 4]);
            buffer.append(charArray[(0xF & content[i])]);
        }
        return buffer.toString();
    }

    public static int getInt(char c) {
        return Arrays.binarySearch(charArray, c);
    }

    public static byte[] string2Byte(String s) {
        byte[] result = new byte[s.length() / 2];
        String str = s.toUpperCase();
        for (int i = 0; i < str.length(); i++, i++) {
            result[i / 2] = (byte) ((getInt(str.charAt(i)) << 4) + getInt(str.charAt(i + 1)));
        }
        return result;
    }

    static final byte ASCII_0 = 0x30;

    static final byte ASCII_9 = 0x39;

    static final byte ASCII_A = 0x41;

    static final byte ASCII_F = 0x46;

    static final byte ASCII_a = 0x61;

    static final byte ASCII_f = 0x66;

    public static byte getByte(byte b1, byte b2) {
        byte b = 0;
        if ((b1 >= ASCII_0) && (b1 <= ASCII_9)) {
            b += ((b1 - ASCII_0) * 16);
        } else if ((b1 >= ASCII_a) && (b1 <= ASCII_f)) {
            b += ((b1 - ASCII_a + 10) * 16);
        } else if ((b1 >= ASCII_a) && (b1 <= ASCII_f)) {
            b += ((b1 - ASCII_a + 10) * 16);
        } else {
            return 0;
        }
        if ((b2 >= ASCII_0) && (b2 <= ASCII_9)) {
            b += (b2 - ASCII_0);
        } else if ((b2 >= ASCII_a) && (b2 <= ASCII_f)) {
            b += (b2 - ASCII_a + 10);
        } else if ((b2 >= ASCII_a) && (b2 <= ASCII_f)) {
            b += (b2 - ASCII_a + 10);
        } else {
            return 0;
        }
        return b;
    }

    public static void main(String[] args) {
        System.out.println(base64Encode("����good"));
        System.out.println(base64Decode(base64Encode("����good")));
        System.out.println(byte2String(md5("a")));
        System.out.println(byte2String(sha("a")));
        System.out.println(hash("a"));
    }
}
