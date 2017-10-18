package org.furthurnet.md5;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of the MD5 hash algorithm
 */
public class MD5 {

    private MessageDigest state;

    /**
     * Constructs a stateful instance of the MD5 digest
     */
    public MD5() {
        try {
            state = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not implemented in this JRE\n");
        }
    }

    /**
     * Updates the digest with the specified number of bytes
     * from the specified array of bytes, starting at the specified offset.
     *
     * @param input   the array of bytes.
     * @param offset  the offset to start from in the array.
     * @param length  the number of bytes to use from the array.
     */
    public void update(byte input[], int offset, int length) {
        state.update(input, offset, length);
    }

    /**
     * Updates the digest with the specified number of bytes
     * from the specified array of bytes.
     *
     * @param input   the array of bytes.
     * @param length  the number of bytes to use from the array.
     */
    public void update(byte input[], int length) {
        state.update(input, 0, length);
    }

    /**
     * Updates the digest with the entire contents of
     * the specified array of bytes.
     *
     * @param input   the array of bytes.
     */
    public void update(byte input[]) {
        state.update(input, 0, input.length);
    }

    /**
     * Updates the digest with a single byte
     *
     * @param input   the byte with which to update the digest.
     */
    public void update(byte input) {
        state.update(input);
    }

    /**
     * Updates the digest with the given string
     *
     * @param input   string with which to update the digest. ASCII
     *            is used for conversion to a byte array.
     */
    public void update(String input) {
        try {
            state.update(input.getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("ASCII encoding not supported\n");
        }
    }

    /**
     * Re-initialize the internal digest state (object can be reused just by
     * calling reset() after every digest())
     */
    public void reset() {
        state.reset();
    }

    /**
     * Returns the MD5 digest of the things previously fed
     * to this MD5 object (through update() calls).
     *
     * @return    the MD5 digest of all inputs.
     */
    public byte[] digest() {
        MessageDigest tmp;
        try {
            tmp = (MessageDigest) state.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("MD5 not clonable in this JRE\n");
        }
        return tmp.digest();
    }

    /**
     * Returns the MD5 digest of the things previously fed
     * to this MD5 object (through update() calls).
     * This method corrupts the state of this MD5 object.
     *
     * @return    the MD5 digest of all inputs.
     */
    public byte[] digestNoCopy() {
        return state.digest();
    }

    /**
     * Turns array of bytes into string representing each byte as
     * unsigned hex number.
     *
     * @param hash    Array of bytes to convert to hex-string
     * @return    Generated hex string
     */
    public static String asHex(byte hash[]) {
        StringBuffer buf = new StringBuffer(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            if ((((int) hash[i]) & 0xff) < 0x10) buf.append("0");
            buf.append(Integer.toString(((int) hash[i]) & 0xff, 16));
        }
        return new String(buf);
    }

    /**
     * Returns 32-character hex string representation of this objects hash
     *
     * @return String of this object's hash
     */
    public String asHex() {
        return asHex(digest());
    }

    /**
     * Returns 32-character hex string representation of this objects hash
     *
     * @return String of this object's hash
     */
    public String asHexNoCopy() {
        return asHex(digestNoCopy());
    }

    public static String getMD5(File f) throws IOException {
        return getMD5(f, null, false);
    }

    public static String getMD5(File f, MD5StatusListener listener, boolean yield) throws IOException {
        MD5 md5 = new MD5();
        BufferedInputStream in;
        byte buf[] = new byte[4096];
        int l, count = 0;
        long total = 0;
        in = new BufferedInputStream(new FileInputStream(f));
        do {
            try {
                l = in.read(buf, 0, 4096);
                count++;
                total += l;
                if (l >= 0) {
                    md5.update(buf, 0, l);
                    if (listener != null && (count % 500 == 0)) listener.updateStatus(total);
                    if (yield) Thread.yield();
                }
            } catch (IOException e) {
                throw new IOException("IOException reading " + f.toString());
            }
            ;
        } while (l >= 0);
        in.close();
        if (listener != null) listener.completed(total);
        return md5.asHexNoCopy();
    }

    public static String getMD5(String s) {
        MD5 md5 = new MD5();
        md5.update(s);
        return md5.asHexNoCopy();
    }
}
