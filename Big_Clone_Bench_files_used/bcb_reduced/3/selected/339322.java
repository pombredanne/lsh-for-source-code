package ow.dht.memcached;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Item implements Externalizable {

    private static MessageDigest md = null;

    private static final String mdAlgoName = "SHA1";

    static {
        try {
            md = MessageDigest.getInstance(mdAlgoName);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    private byte[] data;

    private long flag;

    private volatile int cachedHashCode;

    public Item(byte[] data, long flag) {
        this.data = data;
        this.flag = flag;
        this.init();
    }

    private void init() {
        byte[] hash;
        synchronized (md) {
            md.update(data);
            for (int i = 56; i >= 0; i -= 8) md.update((byte) (flag >>> i));
            hash = md.digest();
        }
        int hashCode = 0;
        int index = 24;
        for (int i = 0; i < hash.length; i++) {
            hashCode ^= (((int) hash[i]) << index);
            index -= 8;
            if (index < 0) index = 24;
        }
        cachedHashCode = hashCode;
    }

    public byte[] getData() {
        return this.data;
    }

    public long getFlag() {
        return this.flag;
    }

    public long getCasUnique() {
        long uniq = this.hashCode();
        if (uniq < 0) uniq += (1L << 32);
        return uniq;
    }

    public int hashCode() {
        return this.cachedHashCode;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Item)) return false;
        Item other = (Item) o;
        if (this.hashCode() != other.hashCode()) return false;
        byte[] d0 = this.getData();
        byte[] d1 = other.getData();
        if (d0.length != d1.length) return false;
        for (int i = 0; i < d0.length; i++) {
            if (d0[i] != d1[i]) return false;
        }
        if (this.getFlag() != other.getFlag()) return false;
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("{ data: ").append(new String(this.data, Memcached.ENCODING));
        } catch (UnsupportedEncodingException e) {
        }
        sb.append(", flag:").append(this.flag);
        sb.append(" }");
        return sb.toString();
    }

    /**
	 * A public constructor with no argument required to implement Externalizable interface.
	 */
    public Item() {
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(this.data.length);
        out.write(this.data);
        out.writeLong(this.flag);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int len = in.readInt();
        this.data = new byte[len];
        in.readFully(this.data);
        this.flag = in.readLong();
        this.init();
    }
}
