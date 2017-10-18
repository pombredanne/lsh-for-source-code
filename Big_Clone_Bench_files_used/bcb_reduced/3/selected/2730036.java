package org.zamia.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.zamia.ExceptionLogger;
import org.zamia.ZamiaLogger;

/**
 * Simple md5 based convenience hash class
 * 
 * @author Guenter Bartsch
 * 
 */
public class ZHash {

    public static final ZamiaLogger logger = ZamiaLogger.getInstance();

    public static final ExceptionLogger el = ExceptionLogger.getInstance();

    private MessageDigest md = null;

    private static final char[] hex = "0123456789ABCDEF".toCharArray();

    private static ZHash instance = null;

    private ZHash() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            el.logException(e);
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b < 0) b += 256;
            buf.append(hex[b >> 4]);
            buf.append(hex[b & 0xF]);
        }
        return buf.toString();
    }

    private synchronized String encodeString(String msg) {
        md.update(msg.getBytes());
        return toHexString(md.digest());
    }

    private static ZHash getInstance() {
        if (instance == null) {
            instance = new ZHash();
        }
        return instance;
    }

    public static String encodeZ(String msg) {
        ZHash instance = getInstance();
        return instance.encodeString(msg);
    }
}
