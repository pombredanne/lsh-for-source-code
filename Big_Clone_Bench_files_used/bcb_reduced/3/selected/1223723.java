package com.hanhuy.scurp;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Digest {

    private static MessageDigest sha1Digest;

    private static MessageDigest md5Digest;

    private static char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    static {
        try {
            sha1Digest = MessageDigest.getInstance("SHA1");
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String md5(byte[] data) {
        md5Digest.reset();
        return toHexString(md5Digest.digest(data));
    }

    public static String md5(char[] data) {
        CharBuffer cb = CharBuffer.wrap(data);
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer b = cs.encode(cb);
        md5Digest.reset();
        md5Digest.update(b.array(), b.position(), b.limit());
        Arrays.fill(b.array(), (byte) 0);
        byte[] digest = md5Digest.digest();
        return toHexString(digest);
    }

    public static String sha1(byte[] data) {
        sha1Digest.reset();
        return toHexString(sha1Digest.digest(data));
    }

    static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(HEX_DIGITS[(b & 0xf0) >> 4]);
            sb.append(HEX_DIGITS[b & 0xf]);
            sb.append(':');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
