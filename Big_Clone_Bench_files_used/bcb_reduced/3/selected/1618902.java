package com.noahsloan.nutils.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.noahsloan.nutils.InToOut;

/**
 * Writes data to a {@link MessageDigest} to determine the hash. Useful with
 * {@link InToOut}.
 * 
 * @author noah
 * 
 */
public class HashOutputStream extends OutputStream {

    private final MessageDigest hash;

    private byte[] digest;

    /**
     * Creates a new HashOutputStream with SHA-1 has the MessageDigest
     * algorithm.
     */
    public HashOutputStream() {
        this(getSHA1());
    }

    public static MessageDigest getSHA1() {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public HashOutputStream(final MessageDigest hash) {
        super();
        this.hash = hash;
    }

    @Override
    public void write(int b) {
        hash.update((byte) b);
    }

    @Override
    public void write(byte[] b) {
        hash.update(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        hash.update(b, off, len);
    }

    @Override
    public void close() {
        if (digest == null) {
            digest = hash.digest();
        }
    }

    /**
     * 
     * @return the message digest
     * 
     * @throws IOException
     */
    public byte[] getDigest() {
        close();
        return digest;
    }
}
