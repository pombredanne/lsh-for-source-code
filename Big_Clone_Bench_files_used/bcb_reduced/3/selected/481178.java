package migool.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author Denis Migol
 * 
 */
public final class CryptoUtil {

    public static final String MD5 = "MD5";

    public static final String HEX_STRING = "0123456789abcdef";

    public static final char[] HEX_CHARS = HEX_STRING.toCharArray();

    private static MessageDigest MD5D;

    static {
        try {
            MD5D = MessageDigest.getInstance(MD5);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
        }
    }

    private CryptoUtil() {
    }

    /**
	 * 
	 * @param source
	 * @return
	 */
    public static final String getMD5hash(String source) {
        byte[] dataToHash = source.getBytes();
        MD5D.update(dataToHash, 0, dataToHash.length);
        return toHexString(MD5D.digest());
    }

    /**
	 * 
	 * @param b
	 * @return
	 */
    public static final byte[] getMD5hash(byte[] b) {
        MD5D.update(b, 0, b.length);
        return MD5D.digest();
    }

    /**
	 * 
	 * @param b
	 * @return
	 */
    public static final String toHexString(byte[] b) {
        final StringBuffer sb = new StringBuffer();
        int msb;
        int lsb = 0;
        int i;
        for (i = 0; i < b.length; i++) {
            msb = ((int) b[i] & 0x000000FF) / 16;
            lsb = ((int) b[i] & 0x000000FF) % 16;
            sb.append(HEX_CHARS[msb]);
            sb.append(HEX_CHARS[lsb]);
        }
        return sb.toString();
    }

    /**
	 * 
	 * @param value
	 * @return
	 */
    public static byte[] fromHexString(String value) {
        final byte[] bytes = new byte[value.length() / 2];
        final String mapping = HEX_STRING;
        for (int i = 0; i < bytes.length; i++) {
            String high = value.substring(2 * i, 2 * i + 1);
            String low = value.substring(2 * i + 1, 2 * i + 2);
            bytes[i] = (byte) ((mapping.indexOf(high) << 4) + mapping.indexOf(low));
        }
        return bytes;
    }
}
