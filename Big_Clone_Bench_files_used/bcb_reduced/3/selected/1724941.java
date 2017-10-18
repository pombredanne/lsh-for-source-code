package ow.dht;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A byte array with utility methods.
 */
public final class ByteArray implements java.io.Externalizable {

    private static MessageDigest md = null;

    private static final String mdAlgoName = "SHA1";

    static {
        try {
            md = MessageDigest.getInstance(mdAlgoName);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    private byte[] barray;

    private volatile int hashCode;

    public ByteArray(byte[] bytes) {
        this.barray = bytes;
        this.init();
    }

    private void init() {
        int h = 0;
        for (int i = 0; i < this.barray.length; i++) {
            h ^= this.barray[i] << ((i % 4) * 8);
        }
        this.hashCode = h;
    }

    public byte[] getBytes() {
        return this.barray;
    }

    /**
	 * Returns a ByteArray instance based on the specified String.
	 */
    public static ByteArray valueOf(String str, String encoding) throws UnsupportedEncodingException {
        return new ByteArray(str.getBytes(encoding));
    }

    /**
	 * Returns a newly generated instance
	 * with the hashed value of the original instance.
	 */
    public ByteArray hashWithSHA1() {
        byte[] hashed;
        synchronized (md) {
            hashed = md.digest(this.barray);
        }
        return new ByteArray(hashed);
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof ByteArray)) return false;
        ByteArray other = (ByteArray) o;
        if (this.barray.length != other.barray.length) return false;
        for (int i = 0; i < this.barray.length; i++) {
            if (this.barray[i] != other.barray[i]) return false;
        }
        return true;
    }

    public int hashCode() {
        return this.hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        for (int i = 0; i < this.barray.length; i++) {
            int b = this.barray[i] & 0xff;
            if (b < 16) sb.append("0");
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    /**
	 * A public constructor with no argument required to implement Externalizable interface.
	 */
    public ByteArray() {
    }

    public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
        int len = barray.length;
        if (len < 255) {
            out.writeByte(len);
        } else {
            out.writeByte(0xff);
            out.writeInt(len);
        }
        out.write(barray);
    }

    public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
        int len = in.readByte() & 0xff;
        if (len == 0xff) {
            len = in.readInt();
        }
        this.barray = new byte[len];
        in.readFully(this.barray);
        this.init();
    }
}
