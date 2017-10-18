package com.sshtools.j2ssh.util;

import com.sshtools.j2ssh.io.*;
import java.io.*;
import java.math.*;
import java.security.*;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class Hash {

    private MessageDigest hash;

    /**
     * Creates a new Hash object.
     *
     * @param algorithm
     *
     * @throws NoSuchAlgorithmException
     */
    public Hash(String algorithm) throws NoSuchAlgorithmException {
        hash = MessageDigest.getInstance(algorithm);
    }

    /**
     *
     *
     * @param bi
     */
    public void putBigInteger(BigInteger bi) {
        byte[] data = bi.toByteArray();
        putInt(data.length);
        hash.update(data);
    }

    /**
     *
     *
     * @param b
     */
    public void putByte(byte b) {
        hash.update(b);
    }

    /**
     *
     *
     * @param data
     */
    public void putBytes(byte[] data) {
        hash.update(data);
    }

    /**
     *
     *
     * @param i
     */
    public void putInt(int i) {
        ByteArrayWriter baw = new ByteArrayWriter();
        try {
            baw.writeInt(i);
        } catch (IOException ioe) {
        }
        hash.update(baw.toByteArray());
    }

    /**
     *
     *
     * @param str
     */
    public void putString(String str) {
        putInt(str.length());
        hash.update(str.getBytes());
    }

    /**
     *
     */
    public void reset() {
        hash.reset();
    }

    /**
     *
     *
     * @param data
     * @param algorithm
     *
     * @return
     *
     * @throws NoSuchAlgorithmException
     */
    public static byte[] simple(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest simp = MessageDigest.getInstance(algorithm);
        simp.update(data);
        return simp.digest();
    }

    /**
     *
     *
     * @return
     */
    public byte[] doFinal() {
        return hash.digest();
    }
}
