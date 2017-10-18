package util;

import java.security.MessageDigest;
import java.util.zip.CRC32;

public class CryptoUtil {

    public static String SHA1(String str) {
        final StringBuilder buffer = new StringBuilder();
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha.digest(str.getBytes("UTF-8"));
            for (int i = 0; i < digest.length; ++i) {
                if ((digest[i] & 0xff) < 16) {
                    buffer.append("0");
                }
                buffer.append(Integer.toHexString(digest[i] & 0xff).toUpperCase());
            }
        } catch (Exception e) {
        }
        return buffer.toString();
    }

    public static int CRC32(final byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }
}
