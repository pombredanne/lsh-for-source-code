package org.jgenesis.helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Digest {

    private static Log log = LogFactory.getLog(Digest.class);

    public static String md5(String str) {
        return digest(str, "MD5");
    }

    public static String sha(String str) {
        return digest(str, "SHA");
    }

    public static String digest(String input, String algorithm) {
        return ByteUtils.bytesToString(digest(input.getBytes(), algorithm));
    }

    public static byte[] digest(byte[] input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(input);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            log.warn(e.getMessage());
        }
        return null;
    }
}
