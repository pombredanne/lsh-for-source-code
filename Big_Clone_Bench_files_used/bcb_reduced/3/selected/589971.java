package org.pustefixframework.webservices.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author mleidig@schlund.de
 */
public class FileCacheData {

    String md5;

    byte[] bytes;

    public FileCacheData(byte[] bytes) {
        this.bytes = bytes;
        md5 = getMD5Digest(bytes);
    }

    public FileCacheData(String md5, byte[] bytes) {
        this.md5 = md5;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getMD5() {
        return md5;
    }

    private static String getMD5Digest(byte[] bytes) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException x) {
            throw new RuntimeException("MD5 algorithm not supported.", x);
        }
        digest.reset();
        digest.update(bytes);
        String md5sum = bytesToString(digest.digest());
        return md5sum;
    }

    private static String bytesToString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) sb.append(byteToString(b[i]));
        return sb.toString();
    }

    private static String byteToString(byte b) {
        int b1 = b & 0xF;
        int b2 = (b & 0xF0) >> 4;
        return Integer.toHexString(b2) + Integer.toHexString(b1);
    }
}
