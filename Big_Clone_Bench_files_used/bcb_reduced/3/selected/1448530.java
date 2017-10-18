package com.myJava.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.myJava.configuration.FrameworkConfiguration;

public class HashInputStreamListener implements InputStreamListener {

    private static final String HASH_ALGORITHM = FrameworkConfiguration.getInstance().getFileHashAlgorithm();

    private boolean closed = false;

    private MessageDigest dg;

    public HashInputStreamListener() throws NoSuchAlgorithmException {
        reset();
    }

    public void reset() throws NoSuchAlgorithmException {
        dg = MessageDigest.getInstance(HASH_ALGORITHM);
    }

    public void close() {
        closed = true;
    }

    public void read(byte[] b, int off, int len, int read) {
        if (read > 0) {
            dg.update(b, off, read);
        }
    }

    public void read(int b) {
        if (b >= 0) {
            dg.update((byte) b);
        }
    }

    public byte[] getHash() {
        if (!closed) {
            throw new IllegalStateException("The stream is not closed");
        }
        return dg.digest();
    }
}
