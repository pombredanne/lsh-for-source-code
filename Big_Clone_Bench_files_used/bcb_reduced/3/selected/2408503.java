package org.apache.xml.security.utils;

import java.io.ByteArrayOutputStream;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;

/**
 * @author raul
 *
 */
public class DigesterOutputStream extends ByteArrayOutputStream {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DigesterOutputStream.class);

    final MessageDigestAlgorithm mda;

    /**
     * @param mda
     */
    public DigesterOutputStream(MessageDigestAlgorithm mda) {
        this.mda = mda;
    }

    /** @inheritDoc */
    public void write(byte[] arg0) {
        write(arg0, 0, arg0.length);
    }

    /** @inheritDoc */
    public void write(int arg0) {
        mda.update((byte) arg0);
    }

    /** @inheritDoc */
    public void write(byte[] arg0, int arg1, int arg2) {
        if (log.isDebugEnabled()) {
            log.debug("Pre-digested input:");
            StringBuilder sb = new StringBuilder(arg2);
            for (int i = arg1; i < (arg1 + arg2); i++) {
                sb.append((char) arg0[i]);
            }
            log.debug(sb.toString());
        }
        mda.update(arg0, arg1, arg2);
    }

    /**
     * @return the digest value 
     */
    public byte[] getDigestValue() {
        return mda.digest();
    }
}
