package net.redlightning.dht.kad;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import net.redlightning.dht.kad.utils.BitVector;
import static java.lang.Math.*;

public class BloomFilterBEP33 implements Comparable<BloomFilterBEP33>, Cloneable {

    private static final int m = 256 * 8;

    private static final int k = 2;

    MessageDigest sha1;

    BitVector filter;

    public BloomFilterBEP33() {
        filter = new BitVector(m);
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public BloomFilterBEP33(byte[] serializedFilter) {
        filter = new BitVector(m, serializedFilter);
    }

    public void insert(InetAddress addr) {
        byte[] hash = sha1.digest(addr.getAddress());
        int index1 = (hash[0] & 0xFF) | (hash[1] & 0xFF) << 8;
        int index2 = (hash[2] & 0xFF) | (hash[3] & 0xFF) << 8;
        index1 %= m;
        index2 %= m;
        filter.set(index1);
        filter.set(index2);
    }

    protected BloomFilterBEP33 clone() {
        BloomFilterBEP33 newFilter = null;
        try {
            newFilter = (BloomFilterBEP33) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        newFilter.filter = new BitVector(filter);
        return newFilter;
    }

    public int compareTo(BloomFilterBEP33 o) {
        return (int) (size() - o.size());
    }

    public int size() {
        double c = filter.bitcount();
        double size = log1p(-c / m) / (k * logB());
        return (int) size;
    }

    public static int unionSize(Collection<BloomFilterBEP33> filters) {
        BitVector[] vectors = new BitVector[filters.size()];
        int i = 0;
        for (BloomFilterBEP33 f : filters) vectors[i++] = f.filter;
        double c = BitVector.unionAndCount(vectors);
        return (int) (log1p(-c / m) / (k * logB()));
    }

    public byte[] serialize() {
        return filter.getSerializedFormat();
    }

    private static double logB() {
        return log1p(-1.0 / m);
    }

    public static void main(String[] args) throws Exception {
        BloomFilterBEP33 bf = new BloomFilterBEP33();
        for (int i = 0; i < 1000; i++) {
            bf.insert(InetAddress.getByAddress(new byte[] { 0x20, 0x01, 0x0D, (byte) 0xB8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) (i >> 8 & 0xFF), (byte) (i & 0xFF) }));
        }
        for (int i = 0; i < 256; i++) {
            bf.insert(InetAddress.getByAddress(new byte[] { (byte) 192, 0, 2, (byte) i }));
        }
        System.out.println(bf.filter.toString());
        System.out.println(bf.filter.bitcount());
        System.out.println(bf.size());
    }
}
