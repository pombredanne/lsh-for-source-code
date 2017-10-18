package net.anydigit.jiliu.hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author xingfei [xingfei0831 AT gmail.com]
 *
 */
public class FNV extends IntegerHash {

    static final long s_offsetBasis = 2166136261L;

    static final int s_prime = 16777619;

    static final long s_mask = 0xFFFFFFFFL;

    private int bits;

    private ByteArrayOutputStream srcBytes;

    private FNV(int bits) {
        this.srcBytes = new ByteArrayOutputStream();
        this.bits = bits;
    }

    public FNV() {
        this(32);
    }

    public int getBits() {
        return this.bits;
    }

    public void update(byte[] b) {
        try {
            this.srcBytes.write(b);
        } catch (IOException e) {
            throw new RuntimeException("cannot update bytes", e);
        }
    }

    public void reset() {
        srcBytes.reset();
    }

    public long digest() {
        byte[] src = this.srcBytes.toByteArray();
        long hash = s_offsetBasis;
        for (byte b : src) {
            hash *= s_prime;
            hash ^= b;
            hash &= s_mask;
        }
        reset();
        return hash;
    }

    public static void main(String[] args) {
        byte[] src = "s 1".getBytes();
        FNV fnv = new FNV();
        fnv.update(src);
        long h = fnv.digest();
        long H = 3647994781L;
        System.out.printf("expect %d got %d\n", H, h);
    }
}
