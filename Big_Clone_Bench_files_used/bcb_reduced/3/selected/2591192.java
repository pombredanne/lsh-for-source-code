package org.jcp.xml.dsig.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream;

/**
 * This class has been modified slightly to use java.security.MessageDigest
 * objects as input, rather than
 * com.sun.org.apache.xml.internal.security.algorithms.MessageDigestAlgorithm objects.
 * It also optionally caches the input bytes.
 *
 * @author raul
 * @author Sean Mullan
 */
public class DigesterOutputStream extends OutputStream {

    private boolean buffer = false;

    private UnsyncByteArrayOutputStream bos;

    private final MessageDigest md;

    private static Logger log = Logger.getLogger("org.jcp.xml.dsig.internal");

    /**
     * Creates a DigesterOutputStream.
     *
     * @param md the MessageDigest
     */
    public DigesterOutputStream(MessageDigest md) {
        this(md, false);
    }

    /**
     * Creates a DigesterOutputStream.
     *
     * @param md the MessageDigest
     * @param buffer if true, caches the input bytes
     */
    public DigesterOutputStream(MessageDigest md, boolean buffer) {
        this.md = md;
        this.buffer = buffer;
        if (buffer) {
            bos = new UnsyncByteArrayOutputStream();
        }
    }

    /** @inheritDoc */
    public void write(byte[] input) {
        write(input, 0, input.length);
    }

    /** @inheritDoc */
    public void write(int input) {
        if (buffer) {
            bos.write(input);
        }
        md.update((byte) input);
    }

    /** @inheritDoc */
    public void write(byte[] input, int offset, int len) {
        if (buffer) {
            bos.write(input, offset, len);
        }
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "Pre-digested input:");
            StringBuffer sb = new StringBuffer(len);
            for (int i = offset; i < (offset + len); i++) {
                sb.append((char) input[i]);
            }
            log.log(Level.FINER, sb.toString());
        }
        md.update(input, offset, len);
    }

    /**
     * @return the digest value
     */
    public byte[] getDigestValue() {
        return md.digest();
    }

    /**
     * @return an input stream containing the cached bytes, or
     *    null if not cached
     */
    public InputStream getInputStream() {
        if (buffer) {
            return new ByteArrayInputStream(bos.toByteArray());
        } else {
            return null;
        }
    }
}
