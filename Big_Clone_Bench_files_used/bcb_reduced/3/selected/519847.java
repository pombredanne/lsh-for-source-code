package gnu.java.security.hash;

import gnu.java.security.Registry;
import gnu.java.security.util.Util;

/**
 * RIPEMD-160 is a 160-bit message digest.
 * <p>
 * References:
 * <ol>
 *    <li><a href="http://www.esat.kuleuven.ac.be/~bosselae/ripemd160.html">
 *    RIPEMD160</a>: A Strengthened Version of RIPEMD.<br>
 *    Hans Dobbertin, Antoon Bosselaers and Bart Preneel.</li>
 * </ol>
 */
public class RipeMD160 extends BaseHash {

    private static final int BLOCK_SIZE = 64;

    private static final String DIGEST0 = "9C1185A5C5E9FC54612808977EE8F548B2258D31";

    private static final int[] R = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8, 3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12, 1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2, 4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13 };

    private static final int[] Rp = { 5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12, 6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2, 15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13, 8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14, 12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11 };

    private static final int[] S = { 11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8, 7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12, 11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5, 11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12, 9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6 };

    private static final int[] Sp = { 8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6, 9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11, 9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5, 15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8, 8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11 };

    /** caches the result of the correctness test, once executed. */
    private static Boolean valid;

    /** 160-bit h0, h1, h2, h3, h4 (interim result) */
    private int h0, h1, h2, h3, h4;

    /** 512 bits work buffer = 16 x 32-bit words */
    private int[] X = new int[16];

    /** Trivial 0-arguments constructor. */
    public RipeMD160() {
        super(Registry.RIPEMD160_HASH, 20, BLOCK_SIZE);
    }

    /**
   * Private constructor for cloning purposes.
   *
   * @param md the instance to clone.
   */
    private RipeMD160(RipeMD160 md) {
        this();
        this.h0 = md.h0;
        this.h1 = md.h1;
        this.h2 = md.h2;
        this.h3 = md.h3;
        this.h4 = md.h4;
        this.count = md.count;
        this.buffer = (byte[]) md.buffer.clone();
    }

    public Object clone() {
        return (new RipeMD160(this));
    }

    protected void transform(byte[] in, int offset) {
        int A, B, C, D, E, Ap, Bp, Cp, Dp, Ep, T, s, i;
        for (i = 0; i < 16; i++) X[i] = (in[offset++] & 0xFF) | (in[offset++] & 0xFF) << 8 | (in[offset++] & 0xFF) << 16 | in[offset++] << 24;
        A = Ap = h0;
        B = Bp = h1;
        C = Cp = h2;
        D = Dp = h3;
        E = Ep = h4;
        for (i = 0; i < 16; i++) {
            s = S[i];
            T = A + (B ^ C ^ D) + X[i];
            A = E;
            E = D;
            D = C << 10 | C >>> 22;
            C = B;
            B = (T << s | T >>> (32 - s)) + A;
            s = Sp[i];
            T = Ap + (Bp ^ (Cp | ~Dp)) + X[Rp[i]] + 0x50A28BE6;
            Ap = Ep;
            Ep = Dp;
            Dp = Cp << 10 | Cp >>> 22;
            Cp = Bp;
            Bp = (T << s | T >>> (32 - s)) + Ap;
        }
        for (; i < 32; i++) {
            s = S[i];
            T = A + ((B & C) | (~B & D)) + X[R[i]] + 0x5A827999;
            A = E;
            E = D;
            D = C << 10 | C >>> 22;
            C = B;
            B = (T << s | T >>> (32 - s)) + A;
            s = Sp[i];
            T = Ap + ((Bp & Dp) | (Cp & ~Dp)) + X[Rp[i]] + 0x5C4DD124;
            Ap = Ep;
            Ep = Dp;
            Dp = Cp << 10 | Cp >>> 22;
            Cp = Bp;
            Bp = (T << s | T >>> (32 - s)) + Ap;
        }
        for (; i < 48; i++) {
            s = S[i];
            T = A + ((B | ~C) ^ D) + X[R[i]] + 0x6ED9EBA1;
            A = E;
            E = D;
            D = C << 10 | C >>> 22;
            C = B;
            B = (T << s | T >>> (32 - s)) + A;
            s = Sp[i];
            T = Ap + ((Bp | ~Cp) ^ Dp) + X[Rp[i]] + 0x6D703EF3;
            Ap = Ep;
            Ep = Dp;
            Dp = Cp << 10 | Cp >>> 22;
            Cp = Bp;
            Bp = (T << s | T >>> (32 - s)) + Ap;
        }
        for (; i < 64; i++) {
            s = S[i];
            T = A + ((B & D) | (C & ~D)) + X[R[i]] + 0x8F1BBCDC;
            A = E;
            E = D;
            D = C << 10 | C >>> 22;
            C = B;
            B = (T << s | T >>> (32 - s)) + A;
            s = Sp[i];
            T = Ap + ((Bp & Cp) | (~Bp & Dp)) + X[Rp[i]] + 0x7A6D76E9;
            Ap = Ep;
            Ep = Dp;
            Dp = Cp << 10 | Cp >>> 22;
            Cp = Bp;
            Bp = (T << s | T >>> (32 - s)) + Ap;
        }
        for (; i < 80; i++) {
            s = S[i];
            T = A + (B ^ (C | ~D)) + X[R[i]] + 0xA953FD4E;
            A = E;
            E = D;
            D = C << 10 | C >>> 22;
            C = B;
            B = (T << s | T >>> (32 - s)) + A;
            s = Sp[i];
            T = Ap + (Bp ^ Cp ^ Dp) + X[Rp[i]];
            Ap = Ep;
            Ep = Dp;
            Dp = Cp << 10 | Cp >>> 22;
            Cp = Bp;
            Bp = (T << s | T >>> (32 - s)) + Ap;
        }
        T = h1 + C + Dp;
        h1 = h2 + D + Ep;
        h2 = h3 + E + Ap;
        h3 = h4 + A + Bp;
        h4 = h0 + B + Cp;
        h0 = T;
    }

    protected byte[] padBuffer() {
        int n = (int) (count % BLOCK_SIZE);
        int padding = (n < 56) ? (56 - n) : (120 - n);
        byte[] result = new byte[padding + 8];
        result[0] = (byte) 0x80;
        long bits = count << 3;
        result[padding++] = (byte) bits;
        result[padding++] = (byte) (bits >>> 8);
        result[padding++] = (byte) (bits >>> 16);
        result[padding++] = (byte) (bits >>> 24);
        result[padding++] = (byte) (bits >>> 32);
        result[padding++] = (byte) (bits >>> 40);
        result[padding++] = (byte) (bits >>> 48);
        result[padding] = (byte) (bits >>> 56);
        return result;
    }

    protected byte[] getResult() {
        return new byte[] { (byte) h0, (byte) (h0 >>> 8), (byte) (h0 >>> 16), (byte) (h0 >>> 24), (byte) h1, (byte) (h1 >>> 8), (byte) (h1 >>> 16), (byte) (h1 >>> 24), (byte) h2, (byte) (h2 >>> 8), (byte) (h2 >>> 16), (byte) (h2 >>> 24), (byte) h3, (byte) (h3 >>> 8), (byte) (h3 >>> 16), (byte) (h3 >>> 24), (byte) h4, (byte) (h4 >>> 8), (byte) (h4 >>> 16), (byte) (h4 >>> 24) };
    }

    protected void resetContext() {
        h0 = 0x67452301;
        h1 = 0xEFCDAB89;
        h2 = 0x98BADCFE;
        h3 = 0x10325476;
        h4 = 0xC3D2E1F0;
    }

    public boolean selfTest() {
        if (valid == null) {
            String d = Util.toString(new RipeMD160().digest());
            valid = Boolean.valueOf(DIGEST0.equals(d));
        }
        return valid.booleanValue();
    }
}
