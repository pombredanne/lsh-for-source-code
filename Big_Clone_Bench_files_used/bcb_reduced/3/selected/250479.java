package com.google.gwt.util.tools.shared;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to generate MD5 hashes using per-thread MD5
 * {@link MessageDigest} instance.
 */
public class Md5Utils {

    /**
   * Per thread MD5 instance.
   */
    private static final ThreadLocal<MessageDigest> perThreadMd5 = new ThreadLocal<MessageDigest>() {

        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 implementation not found", e);
            }
        }

        ;
    };

    /**
   * Generate MD5 digest.
   *
   * @param input input data to be hashed.
   * @return MD5 digest.
   */
    public static byte[] getMd5Digest(byte[] input) {
        MessageDigest md5 = perThreadMd5.get();
        md5.reset();
        md5.update(input);
        return md5.digest();
    }
}
