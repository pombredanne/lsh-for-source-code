package edu.rice.cs.plt.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A stream that accumulates its bytes in a {@link MessageDigest} object. */
public class MessageDigestOutputStream extends DirectOutputStream {

    private final MessageDigest _messageDigest;

    /**
   * Instantiate with the given MessageDigest.  {@code messageDigest} will not be reset, and
   * may contain a partially-computed digest.
   */
    public MessageDigestOutputStream(MessageDigest messageDigest) {
        _messageDigest = messageDigest;
    }

    /** Return {@code messageDigest.digest()}. */
    public byte[] digest() {
        return _messageDigest.digest();
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void write(byte[] bbuf) {
        _messageDigest.update(bbuf);
    }

    @Override
    public void write(byte[] bbuf, int offset, int len) {
        _messageDigest.update(bbuf, offset, len);
    }

    @Override
    public void write(int b) {
        _messageDigest.update((byte) b);
    }

    /**
   * Create a stream for computing MD5 hashes.  Throws a {@code RuntimeException} with a
   * {@link NoSuchAlgorithmException} cause if the MD5 algorithm implementation cannot be located.
   */
    public static MessageDigestOutputStream makeMD5() {
        try {
            return new MessageDigestOutputStream(MessageDigest.getInstance("MD5"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
   * Create a stream for computing SHA-1 hashes.  Throws a {@code RuntimeException} with a
   * {@link NoSuchAlgorithmException} cause if the SHA-1 algorithm implementation cannot be located.
   */
    public static MessageDigestOutputStream makeSHA1() {
        try {
            return new MessageDigestOutputStream(MessageDigest.getInstance("SHA-1"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
   * Create a stream for computing SHA-256 hashes.  Throws a {@code RuntimeException} with a
   * {@link NoSuchAlgorithmException} cause if the SHA-256 algorithm implementation cannot be located.
   */
    public static MessageDigestOutputStream makeSHA256() {
        try {
            return new MessageDigestOutputStream(MessageDigest.getInstance("SHA-256"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
