package org.jdonkey.proto;

/**
 * <p>An implementation of Ron Rivest's MD4 message digest algorithm.
 * MD4 was the precursor to the stronger MD5
 * algorithm, and while not considered cryptograpically secure itself,
 * MD4 is in use in various applications. It is slightly faster than
 * MD5.</p>
 *
 * <p>This implementation is derived from the GNU Classpath Extentions'
 * version.</p>
 *
 * <p>References:</p>
 *
 * <ol>
 *    <li>The <a href="http://www.ietf.org/rfc/rfc1320.txt">MD4</a> Message-
 *    Digest Algorithm.<br>
 *    R. Rivest.</li>
 * </ol>
 *
 * @version $Revision: 1.2 $
 */
public final class MD4 extends MessageDigest implements Cloneable {

    public static final String RCSID = "$Id: MD4.java,v 1.2 2002/10/13 15:55:41 ryl Exp $";

    /**
    * An MD4 message digest is always 128-bits long, or 16 bytes.
    */
    public static final int DIGEST_LENGTH = 16;

    /**
    * The MD4 algorithm operates on 512-bit blocks, or 64 bytes.
    */
    public static final int BLOCK_LENGTH = 64;

    private static final int A = 0x67452301;

    private static final int B = 0xefcdab89;

    private static final int C = 0x98badcfe;

    private static final int D = 0x10325476;

    private int a, b, c, d;

    /** Word buffer for transforming. */
    private final int[] X = new int[16];

    /** The output of this message digest when no data has been input. */
    private static final String DIGEST0 = "31d6cfe0d16ae931b73c59d7e0c089c0";

    /**
    * Trivial zero-argument constructor.
    */
    public MD4() {
        name = "md4";
        hashSize = DIGEST_LENGTH;
        blockSize = BLOCK_LENGTH;
        buffer = new byte[BLOCK_LENGTH];
        reset();
    }

    /**
    * Private constructor for cloning.
    */
    private MD4(MD4 that) {
        this();
        this.a = that.a;
        this.b = that.b;
        this.c = that.c;
        this.d = that.d;
        this.count = that.count;
        this.buffer = (byte[]) that.buffer.clone();
    }

    public Object clone() {
        return new MD4(this);
    }

    /**
    * Do a simple conformance test.
    *
    * @return true If the self-test suceeds.
    */
    public boolean selfTest() {
        return DIGEST0.equals(toString(new MD4().digest()));
    }

    /**
    * Pack the four chaining variables into a byte array.
    */
    protected byte[] getResult() {
        byte[] digest = { (byte) a, (byte) (a >>> 8), (byte) (a >>> 16), (byte) (a >>> 24), (byte) b, (byte) (b >>> 8), (byte) (b >>> 16), (byte) (b >>> 24), (byte) c, (byte) (c >>> 8), (byte) (c >>> 16), (byte) (c >>> 24), (byte) d, (byte) (d >>> 8), (byte) (d >>> 16), (byte) (d >>> 24) };
        return digest;
    }

    /** Reset the four chaining variables. */
    protected void resetContext() {
        a = A;
        b = B;
        c = C;
        d = D;
    }

    /**
    * Pad the buffer by appending the byte 0x80, then as many zero bytes
    * to fill the buffer 8 bytes shy of being a multiple of 64 bytes, then
    * append the length of the buffer, in bits, before padding.
    */
    protected byte[] padBuffer() {
        int n = (int) (count % BLOCK_LENGTH);
        int padding = (n < 56) ? (56 - n) : (120 - n);
        byte[] pad = new byte[padding + 8];
        pad[0] = (byte) 0x80;
        long bits = count << 3;
        pad[padding++] = (byte) bits;
        pad[padding++] = (byte) (bits >>> 8);
        pad[padding++] = (byte) (bits >>> 16);
        pad[padding++] = (byte) (bits >>> 24);
        pad[padding++] = (byte) (bits >>> 32);
        pad[padding++] = (byte) (bits >>> 40);
        pad[padding++] = (byte) (bits >>> 48);
        pad[padding] = (byte) (bits >>> 56);
        return pad;
    }

    /** Transform a 64-byte block. */
    protected void transform(byte[] in, int offset) {
        int aa, bb, cc, dd;
        for (int i = 0, n = 0; i < 16; i++) {
            X[i] = (in[offset++] & 0xff) | (in[offset++] & 0xff) << 8 | (in[offset++] & 0xff) << 16 | (in[offset++] & 0xff) << 24;
        }
        aa = a;
        bb = b;
        cc = c;
        dd = d;
        a += ((b & c) | ((~b) & d)) + X[0];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | ((~a) & c)) + X[1];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | ((~d) & b)) + X[2];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | ((~c) & a)) + X[3];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & c) | ((~b) & d)) + X[4];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | ((~a) & c)) + X[5];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | ((~d) & b)) + X[6];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | ((~c) & a)) + X[7];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & c) | ((~b) & d)) + X[8];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | ((~a) & c)) + X[9];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | ((~d) & b)) + X[10];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | ((~c) & a)) + X[11];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & c) | ((~b) & d)) + X[12];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | ((~a) & c)) + X[13];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | ((~d) & b)) + X[14];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | ((~c) & a)) + X[15];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & (c | d)) | (c & d)) + X[0] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[4] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[8] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[12] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += ((b & (c | d)) | (c & d)) + X[1] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[5] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[9] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[13] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += ((b & (c | d)) | (c & d)) + X[2] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[6] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[10] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[14] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += ((b & (c | d)) | (c & d)) + X[3] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[7] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[11] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[15] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += (b ^ c ^ d) + X[0] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[8] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[4] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[12] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += (b ^ c ^ d) + X[2] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[10] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[6] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[14] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += (b ^ c ^ d) + X[1] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[9] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[5] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[13] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += (b ^ c ^ d) + X[3] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[11] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[7] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[15] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += aa;
        b += bb;
        c += cc;
        d += dd;
    }
}
