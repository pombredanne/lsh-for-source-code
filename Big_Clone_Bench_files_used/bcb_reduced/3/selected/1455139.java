package org.tamacat.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestInputStream extends FilterInputStream {

    private MessageDigest digest;

    public MessageDigestInputStream(InputStream in) {
        super(in);
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new MessageDigestException(e);
        }
    }

    public MessageDigestInputStream(InputStream in, String algorithm) {
        super(in);
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new MessageDigestException(e);
        }
    }

    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            digest.update((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        len = in.read(buf, off, len);
        if (len != -1) {
            digest.update(buf, off, len);
        }
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        byte[] buf = new byte[512];
        long total = 0;
        while (total < n) {
            long len = n - total;
            len = read(buf, 0, len < buf.length ? (int) len : buf.length);
            if (len == -1) {
                return total;
            }
            total += len;
        }
        return total;
    }

    public String getDigest() {
        byte[] hash = digest.digest();
        BigInteger bigInt = new BigInteger(1, hash);
        return bigInt.toString(16);
    }
}
