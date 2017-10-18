package slojj.dotsbox.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CodecUtils {

    public static String toHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            buf.append(Integer.toHexString((b[i] >> 4) & 0x0f));
            buf.append(Integer.toHexString(b[i] & 0x0f));
        }
        return buf.toString();
    }

    public static String md5Encode(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes());
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return s;
        }
    }
}
