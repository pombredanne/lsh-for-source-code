package net.jtaskman.helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Helper {

    public static String getMD5(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        md.update(str.getBytes());
        return bytesToString(md.digest());
    }

    private static String bytesToString(byte[] digest) {
        char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] result = new char[2 * digest.length];
        for (int i = 0; i < digest.length; i++) {
            result[2 * i] = hexdigits[(digest[i] & 0xf0) / 16];
            result[2 * i + 1] = hexdigits[digest[i] & 0x0f];
        }
        return new String(result);
    }
}
