package com.sun.org.apache.xml.internal.security.utils;

import java.io.ByteArrayOutputStream;
import com.sun.org.apache.xml.internal.security.algorithms.MessageDigestAlgorithm;

/**
 * @author raul
 *
 */
public class DigesterOutputStream extends ByteArrayOutputStream {

    static final byte none[] = "error".getBytes();

    final MessageDigestAlgorithm mda;

    /**
	 * @param mda
	 */
    public DigesterOutputStream(MessageDigestAlgorithm mda) {
        this.mda = mda;
    }

    /** @inheritDoc */
    public byte[] toByteArray() {
        return none;
    }

    /** @inheritDoc */
    public void write(byte[] arg0) {
        mda.update(arg0);
    }

    /** @inheritDoc */
    public void write(int arg0) {
        mda.update((byte) arg0);
    }

    /** @inheritDoc */
    public void write(byte[] arg0, int arg1, int arg2) {
        mda.update(arg0, arg1, arg2);
    }

    /**
     * @return the digest value 
     */
    public byte[] getDigestValue() {
        return mda.digest();
    }
}
