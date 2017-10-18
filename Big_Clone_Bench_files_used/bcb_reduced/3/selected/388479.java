package net.sf.exorcist.api;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An output stream that calculates the SHA-1 content hash of the
 * written data.
 */
public class HashStream extends OutputStream {

    /** The name of the algorithm used to calculate the content hash. */
    private static final String ALGORITHM = "SHA-1";

    /** The lowercase hexadecimal digits used to encode the content hash. */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** The message digest instance. */
    private final MessageDigest digest;

    /** The content hash. Is <code>null</code> until the stream gets closed. */
    private String contentHash;

    /**
     * Creates a new hash stream.
     *
     * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available
     */
    public HashStream() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(ALGORITHM);
        this.contentHash = null;
    }

    /**
     * Returns the hex-encoded content hash of the data written to this stream.
     * The content hash is only available once the stream has been closed.
     *
     * @return content hash
     * @throws IllegalStateException if this stream has not yet been closed
     */
    public String getContentHash() throws IllegalStateException {
        if (contentHash != null) {
            return contentHash;
        } else {
            throw new IllegalStateException("Content hash is not available");
        }
    }

    /**
     * Updates the content digest using the given byte array slice.
     *
     * @param b byte array containing the slice
     * @param off start offset of the slice
     * @param len length of the slice
     * @throws IOException never thrown, kept to allow throwing in subclasses
     * @see OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        digest.update(b, off, len);
    }

    /**
     * Updates the content digest using the given byte array.
     *
     * @param b byte array
     * @throws IOException never thrown, kept to allow throwing in subclasses
     * @see OutputStream#write(byte[])
     */
    public void write(byte[] b) throws IOException {
        digest.update(b);
    }

    /**
     * Updates the content digest using the given byte.
     *
     * @param b byte
     * @throws IOException never thrown, kept to allow throwing in subclasses
     * @see OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        digest.update((byte) b);
    }

    /**
     * Does nothing. All the written data has already been fed to the
     * digest algorithm.
     *
     * @throws IOException never thrown, kept to allow throwing in subclasses
     * @see OutputStream#flush()
     */
    public void flush() throws IOException {
    }

    /**
     * Calculates the hex-encoded content hash of all the data written to
     * this stream.
     *
     * @throws IOException never thrown, kept to allow throwing in subclasses
     * @see OutputStream#close()
     */
    public void close() throws IOException {
        byte[] hash = digest.digest();
        char[] hex = new char[2 * hash.length];
        for (int i = 0; i < hash.length; i++) {
            hex[2 * i] = HEX[hash[i] / 16];
            hex[2 * i + 1] = HEX[hash[i] % 16];
        }
        contentHash = new String(hex);
    }
}
