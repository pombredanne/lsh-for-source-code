package jonelo.jacksum.adapt.gnu.crypto.hash;

import jonelo.jacksum.adapt.gnu.crypto.Registry;
import jonelo.jacksum.adapt.gnu.crypto.util.Util;

/**
 * <p>Whirlpool2000, a new 512-bit hashing function operating on messages less than
 * 2 ** 256 bits in length. The function structure is designed according to the
 * Wide Trail strategy and permits a wide variety of implementation trade-offs.
 * </p>
 *
 * <p><b>IMPORTANT</b>: This implementation is not thread-safe.</p>
 *
 * <p>References:</p>
 *
 * <ol>
 *    <li><a href="http://planeta.terra.com.br/informatica/paulobarreto/WhirlpoolPage.html">
 *    The Whirlpool2000 Hashing Function</a>.<br>
 *    <a href="mailto:paulo.barreto@terra.com.br">Paulo S.L.M. Barreto</a> and
 *    <a href="mailto:vincent.rijmen@esat.kuleuven.ac.be">Vincent Rijmen</a>.</li>
 * </ol>
 *
 * @version $Revision: 1.9 $
 */
public final class Whirlpool2000 extends BaseHash {

    private static final int BLOCK_SIZE = 64;

    /** The digest of the 0-bit long message. */
    private static final String DIGEST0 = "B3E1AB6EAF640A34F784593F2074416ACCD3B8E62C620175FCA0997B1BA23473" + "39AA0D79E754C308209EA36811DFA40C1C32F1A2B9004725D987D3635165D3C8";

    private static final int R = 10;

    private static final String Sd = "棐䢝櫤嚁緱薞" + "Ⲏ磊ឩ懕崋谼睑≂" + "㽔䆀첆댘⹗٢텫" + "᭥甐?⛹쭦깐劫" + "װ൳㬄⃾?둟વ삠" + "熥ⵠ犓㤈茡岇뇠Ã" + "ኑ訂᳦䗂쓽뽄ꅌ㏅" + "萣粰┕㕩ﾔ䵰ꊯ췖" + "沷ꓪ퓒ᐞ" + "㣆?稺??ﲪퟎ܏" + "㵘骘鳲꜑纋䌃" + "仇淩❀?銏ĝ匾姁" + "伲᛺瓻掟㐚⩚跉쿶" + "逨袛ㄎ뵊ꘌ졹벾" + "䚗寭᧙겙꠩搟굕" + "Ꮋ륇⿮롻褰퍿皂";

    private static final long[] T0 = new long[256];

    private static final long[] T1 = new long[256];

    private static final long[] T2 = new long[256];

    private static final long[] T3 = new long[256];

    private static final long[] T4 = new long[256];

    private static final long[] T5 = new long[256];

    private static final long[] T6 = new long[256];

    private static final long[] T7 = new long[256];

    private static final long[] rc = new long[R];

    /** caches the result of the correctness test, once executed. */
    private static Boolean valid;

    /** The 512-bit context as 8 longs. */
    private long H0, H1, H2, H3, H4, H5, H6, H7;

    /** Work area for computing the round key schedule. */
    private long k00, k01, k02, k03, k04, k05, k06, k07;

    private long Kr0, Kr1, Kr2, Kr3, Kr4, Kr5, Kr6, Kr7;

    /** work area for transforming the 512-bit buffer. */
    private long n0, n1, n2, n3, n4, n5, n6, n7;

    private long nn0, nn1, nn2, nn3, nn4, nn5, nn6, nn7;

    /** work area for holding block cipher's intermediate values. */
    private long w0, w1, w2, w3, w4, w5, w6, w7;

    static {
        int ROOT = 0x11d;
        int i, r, j;
        long s, s2, s3, s4, s5, s8, s9, t;
        char c;
        final byte[] S = new byte[256];
        for (i = 0; i < 256; i++) {
            c = Sd.charAt(i >>> 1);
            s = ((i & 1) == 0 ? c >>> 8 : c) & 0xFFL;
            s2 = s << 1;
            if (s2 > 0xFFL) {
                s2 ^= ROOT;
            }
            s3 = s2 ^ s;
            s4 = s2 << 1;
            if (s4 > 0xFFL) {
                s4 ^= ROOT;
            }
            s5 = s4 ^ s;
            s8 = s4 << 1;
            if (s8 > 0xFFL) {
                s8 ^= ROOT;
            }
            s9 = s8 ^ s;
            S[i] = (byte) s;
            T0[i] = t = s << 56 | s << 48 | s3 << 40 | s << 32 | s5 << 24 | s8 << 16 | s9 << 8 | s5;
            T1[i] = t >>> 8 | t << 56;
            T2[i] = t >>> 16 | t << 48;
            T3[i] = t >>> 24 | t << 40;
            T4[i] = t >>> 32 | t << 32;
            T5[i] = t >>> 40 | t << 24;
            T6[i] = t >>> 48 | t << 16;
            T7[i] = t >>> 56 | t << 8;
        }
        for (r = 1, i = 0, j = 0; r < R + 1; r++) {
            rc[i++] = (S[j++] & 0xFFL) << 56 | (S[j++] & 0xFFL) << 48 | (S[j++] & 0xFFL) << 40 | (S[j++] & 0xFFL) << 32 | (S[j++] & 0xFFL) << 24 | (S[j++] & 0xFFL) << 16 | (S[j++] & 0xFFL) << 8 | (S[j++] & 0xFFL);
        }
    }

    /** Trivial 0-arguments constructor. */
    public Whirlpool2000() {
        super(Registry.WHIRLPOOL2000_HASH, 20, BLOCK_SIZE);
    }

    /**
    * <p>Private constructor for cloning purposes.</p>
    *
    * @param md the instance to clone.
    */
    private Whirlpool2000(Whirlpool2000 md) {
        this();
        this.H0 = md.H0;
        this.H1 = md.H1;
        this.H2 = md.H2;
        this.H3 = md.H3;
        this.H4 = md.H4;
        this.H5 = md.H5;
        this.H6 = md.H6;
        this.H7 = md.H7;
        this.count = md.count;
        this.buffer = (byte[]) md.buffer.clone();
    }

    public Object clone() {
        return (new Whirlpool2000(this));
    }

    protected void transform(byte[] in, int offset) {
        n0 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n1 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n2 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n3 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n4 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n5 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n6 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        n7 = (in[offset++] & 0xFFL) << 56 | (in[offset++] & 0xFFL) << 48 | (in[offset++] & 0xFFL) << 40 | (in[offset++] & 0xFFL) << 32 | (in[offset++] & 0xFFL) << 24 | (in[offset++] & 0xFFL) << 16 | (in[offset++] & 0xFFL) << 8 | (in[offset++] & 0xFFL);
        k00 = H0;
        k01 = H1;
        k02 = H2;
        k03 = H3;
        k04 = H4;
        k05 = H5;
        k06 = H6;
        k07 = H7;
        nn0 = n0 ^ k00;
        nn1 = n1 ^ k01;
        nn2 = n2 ^ k02;
        nn3 = n3 ^ k03;
        nn4 = n4 ^ k04;
        nn5 = n5 ^ k05;
        nn6 = n6 ^ k06;
        nn7 = n7 ^ k07;
        w0 = w1 = w2 = w3 = w4 = w5 = w6 = w7 = 0L;
        for (int r = 0; r < R; r++) {
            Kr0 = T0[(int) ((k00 >> 56) & 0xFFL)] ^ T1[(int) ((k07 >> 48) & 0xFFL)] ^ T2[(int) ((k06 >> 40) & 0xFFL)] ^ T3[(int) ((k05 >> 32) & 0xFFL)] ^ T4[(int) ((k04 >> 24) & 0xFFL)] ^ T5[(int) ((k03 >> 16) & 0xFFL)] ^ T6[(int) ((k02 >> 8) & 0xFFL)] ^ T7[(int) (k01 & 0xFFL)] ^ rc[r];
            Kr1 = T0[(int) ((k01 >> 56) & 0xFFL)] ^ T1[(int) ((k00 >> 48) & 0xFFL)] ^ T2[(int) ((k07 >> 40) & 0xFFL)] ^ T3[(int) ((k06 >> 32) & 0xFFL)] ^ T4[(int) ((k05 >> 24) & 0xFFL)] ^ T5[(int) ((k04 >> 16) & 0xFFL)] ^ T6[(int) ((k03 >> 8) & 0xFFL)] ^ T7[(int) (k02 & 0xFFL)];
            Kr2 = T0[(int) ((k02 >> 56) & 0xFFL)] ^ T1[(int) ((k01 >> 48) & 0xFFL)] ^ T2[(int) ((k00 >> 40) & 0xFFL)] ^ T3[(int) ((k07 >> 32) & 0xFFL)] ^ T4[(int) ((k06 >> 24) & 0xFFL)] ^ T5[(int) ((k05 >> 16) & 0xFFL)] ^ T6[(int) ((k04 >> 8) & 0xFFL)] ^ T7[(int) (k03 & 0xFFL)];
            Kr3 = T0[(int) ((k03 >> 56) & 0xFFL)] ^ T1[(int) ((k02 >> 48) & 0xFFL)] ^ T2[(int) ((k01 >> 40) & 0xFFL)] ^ T3[(int) ((k00 >> 32) & 0xFFL)] ^ T4[(int) ((k07 >> 24) & 0xFFL)] ^ T5[(int) ((k06 >> 16) & 0xFFL)] ^ T6[(int) ((k05 >> 8) & 0xFFL)] ^ T7[(int) (k04 & 0xFFL)];
            Kr4 = T0[(int) ((k04 >> 56) & 0xFFL)] ^ T1[(int) ((k03 >> 48) & 0xFFL)] ^ T2[(int) ((k02 >> 40) & 0xFFL)] ^ T3[(int) ((k01 >> 32) & 0xFFL)] ^ T4[(int) ((k00 >> 24) & 0xFFL)] ^ T5[(int) ((k07 >> 16) & 0xFFL)] ^ T6[(int) ((k06 >> 8) & 0xFFL)] ^ T7[(int) (k05 & 0xFFL)];
            Kr5 = T0[(int) ((k05 >> 56) & 0xFFL)] ^ T1[(int) ((k04 >> 48) & 0xFFL)] ^ T2[(int) ((k03 >> 40) & 0xFFL)] ^ T3[(int) ((k02 >> 32) & 0xFFL)] ^ T4[(int) ((k01 >> 24) & 0xFFL)] ^ T5[(int) ((k00 >> 16) & 0xFFL)] ^ T6[(int) ((k07 >> 8) & 0xFFL)] ^ T7[(int) (k06 & 0xFFL)];
            Kr6 = T0[(int) ((k06 >> 56) & 0xFFL)] ^ T1[(int) ((k05 >> 48) & 0xFFL)] ^ T2[(int) ((k04 >> 40) & 0xFFL)] ^ T3[(int) ((k03 >> 32) & 0xFFL)] ^ T4[(int) ((k02 >> 24) & 0xFFL)] ^ T5[(int) ((k01 >> 16) & 0xFFL)] ^ T6[(int) ((k00 >> 8) & 0xFFL)] ^ T7[(int) (k07 & 0xFFL)];
            Kr7 = T0[(int) ((k07 >> 56) & 0xFFL)] ^ T1[(int) ((k06 >> 48) & 0xFFL)] ^ T2[(int) ((k05 >> 40) & 0xFFL)] ^ T3[(int) ((k04 >> 32) & 0xFFL)] ^ T4[(int) ((k03 >> 24) & 0xFFL)] ^ T5[(int) ((k02 >> 16) & 0xFFL)] ^ T6[(int) ((k01 >> 8) & 0xFFL)] ^ T7[(int) (k00 & 0xFFL)];
            k00 = Kr0;
            k01 = Kr1;
            k02 = Kr2;
            k03 = Kr3;
            k04 = Kr4;
            k05 = Kr5;
            k06 = Kr6;
            k07 = Kr7;
            w0 = T0[(int) ((nn0 >> 56) & 0xFFL)] ^ T1[(int) ((nn7 >> 48) & 0xFFL)] ^ T2[(int) ((nn6 >> 40) & 0xFFL)] ^ T3[(int) ((nn5 >> 32) & 0xFFL)] ^ T4[(int) ((nn4 >> 24) & 0xFFL)] ^ T5[(int) ((nn3 >> 16) & 0xFFL)] ^ T6[(int) ((nn2 >> 8) & 0xFFL)] ^ T7[(int) (nn1 & 0xFFL)] ^ Kr0;
            w1 = T0[(int) ((nn1 >> 56) & 0xFFL)] ^ T1[(int) ((nn0 >> 48) & 0xFFL)] ^ T2[(int) ((nn7 >> 40) & 0xFFL)] ^ T3[(int) ((nn6 >> 32) & 0xFFL)] ^ T4[(int) ((nn5 >> 24) & 0xFFL)] ^ T5[(int) ((nn4 >> 16) & 0xFFL)] ^ T6[(int) ((nn3 >> 8) & 0xFFL)] ^ T7[(int) (nn2 & 0xFFL)] ^ Kr1;
            w2 = T0[(int) ((nn2 >> 56) & 0xFFL)] ^ T1[(int) ((nn1 >> 48) & 0xFFL)] ^ T2[(int) ((nn0 >> 40) & 0xFFL)] ^ T3[(int) ((nn7 >> 32) & 0xFFL)] ^ T4[(int) ((nn6 >> 24) & 0xFFL)] ^ T5[(int) ((nn5 >> 16) & 0xFFL)] ^ T6[(int) ((nn4 >> 8) & 0xFFL)] ^ T7[(int) (nn3 & 0xFFL)] ^ Kr2;
            w3 = T0[(int) ((nn3 >> 56) & 0xFFL)] ^ T1[(int) ((nn2 >> 48) & 0xFFL)] ^ T2[(int) ((nn1 >> 40) & 0xFFL)] ^ T3[(int) ((nn0 >> 32) & 0xFFL)] ^ T4[(int) ((nn7 >> 24) & 0xFFL)] ^ T5[(int) ((nn6 >> 16) & 0xFFL)] ^ T6[(int) ((nn5 >> 8) & 0xFFL)] ^ T7[(int) (nn4 & 0xFFL)] ^ Kr3;
            w4 = T0[(int) ((nn4 >> 56) & 0xFFL)] ^ T1[(int) ((nn3 >> 48) & 0xFFL)] ^ T2[(int) ((nn2 >> 40) & 0xFFL)] ^ T3[(int) ((nn1 >> 32) & 0xFFL)] ^ T4[(int) ((nn0 >> 24) & 0xFFL)] ^ T5[(int) ((nn7 >> 16) & 0xFFL)] ^ T6[(int) ((nn6 >> 8) & 0xFFL)] ^ T7[(int) (nn5 & 0xFFL)] ^ Kr4;
            w5 = T0[(int) ((nn5 >> 56) & 0xFFL)] ^ T1[(int) ((nn4 >> 48) & 0xFFL)] ^ T2[(int) ((nn3 >> 40) & 0xFFL)] ^ T3[(int) ((nn2 >> 32) & 0xFFL)] ^ T4[(int) ((nn1 >> 24) & 0xFFL)] ^ T5[(int) ((nn0 >> 16) & 0xFFL)] ^ T6[(int) ((nn7 >> 8) & 0xFFL)] ^ T7[(int) (nn6 & 0xFFL)] ^ Kr5;
            w6 = T0[(int) ((nn6 >> 56) & 0xFFL)] ^ T1[(int) ((nn5 >> 48) & 0xFFL)] ^ T2[(int) ((nn4 >> 40) & 0xFFL)] ^ T3[(int) ((nn3 >> 32) & 0xFFL)] ^ T4[(int) ((nn2 >> 24) & 0xFFL)] ^ T5[(int) ((nn1 >> 16) & 0xFFL)] ^ T6[(int) ((nn0 >> 8) & 0xFFL)] ^ T7[(int) (nn7 & 0xFFL)] ^ Kr6;
            w7 = T0[(int) ((nn7 >> 56) & 0xFFL)] ^ T1[(int) ((nn6 >> 48) & 0xFFL)] ^ T2[(int) ((nn5 >> 40) & 0xFFL)] ^ T3[(int) ((nn4 >> 32) & 0xFFL)] ^ T4[(int) ((nn3 >> 24) & 0xFFL)] ^ T5[(int) ((nn2 >> 16) & 0xFFL)] ^ T6[(int) ((nn1 >> 8) & 0xFFL)] ^ T7[(int) (nn0 & 0xFFL)] ^ Kr7;
            nn0 = w0;
            nn1 = w1;
            nn2 = w2;
            nn3 = w3;
            nn4 = w4;
            nn5 = w5;
            nn6 = w6;
            nn7 = w7;
        }
        H0 ^= w0 ^ n0;
        H1 ^= w1 ^ n1;
        H2 ^= w2 ^ n2;
        H3 ^= w3 ^ n3;
        H4 ^= w4 ^ n4;
        H5 ^= w5 ^ n5;
        H6 ^= w6 ^ n6;
        H7 ^= w7 ^ n7;
    }

    protected byte[] padBuffer() {
        int n = (int) ((count + 33) % BLOCK_SIZE);
        int padding = n == 0 ? 33 : BLOCK_SIZE - n + 33;
        byte[] result = new byte[padding];
        result[0] = (byte) 0x80;
        long bits = count * 8;
        int i = padding - 8;
        result[i++] = (byte) (bits >>> 56);
        result[i++] = (byte) (bits >>> 48);
        result[i++] = (byte) (bits >>> 40);
        result[i++] = (byte) (bits >>> 32);
        result[i++] = (byte) (bits >>> 24);
        result[i++] = (byte) (bits >>> 16);
        result[i++] = (byte) (bits >>> 8);
        result[i] = (byte) bits;
        return result;
    }

    protected byte[] getResult() {
        byte[] result = new byte[] { (byte) (H0 >>> 56), (byte) (H0 >>> 48), (byte) (H0 >>> 40), (byte) (H0 >>> 32), (byte) (H0 >>> 24), (byte) (H0 >>> 16), (byte) (H0 >>> 8), (byte) H0, (byte) (H1 >>> 56), (byte) (H1 >>> 48), (byte) (H1 >>> 40), (byte) (H1 >>> 32), (byte) (H1 >>> 24), (byte) (H1 >>> 16), (byte) (H1 >>> 8), (byte) H1, (byte) (H2 >>> 56), (byte) (H2 >>> 48), (byte) (H2 >>> 40), (byte) (H2 >>> 32), (byte) (H2 >>> 24), (byte) (H2 >>> 16), (byte) (H2 >>> 8), (byte) H2, (byte) (H3 >>> 56), (byte) (H3 >>> 48), (byte) (H3 >>> 40), (byte) (H3 >>> 32), (byte) (H3 >>> 24), (byte) (H3 >>> 16), (byte) (H3 >>> 8), (byte) H3, (byte) (H4 >>> 56), (byte) (H4 >>> 48), (byte) (H4 >>> 40), (byte) (H4 >>> 32), (byte) (H4 >>> 24), (byte) (H4 >>> 16), (byte) (H4 >>> 8), (byte) H4, (byte) (H5 >>> 56), (byte) (H5 >>> 48), (byte) (H5 >>> 40), (byte) (H5 >>> 32), (byte) (H5 >>> 24), (byte) (H5 >>> 16), (byte) (H5 >>> 8), (byte) H5, (byte) (H6 >>> 56), (byte) (H6 >>> 48), (byte) (H6 >>> 40), (byte) (H6 >>> 32), (byte) (H6 >>> 24), (byte) (H6 >>> 16), (byte) (H6 >>> 8), (byte) H6, (byte) (H7 >>> 56), (byte) (H7 >>> 48), (byte) (H7 >>> 40), (byte) (H7 >>> 32), (byte) (H7 >>> 24), (byte) (H7 >>> 16), (byte) (H7 >>> 8), (byte) H7 };
        return result;
    }

    protected void resetContext() {
        H0 = H1 = H2 = H3 = H4 = H5 = H6 = H7 = 0L;
    }

    public boolean selfTest() {
        if (valid == null) {
            valid = new Boolean(DIGEST0.equals(Util.toString(new Whirlpool2000().digest())));
        }
        return valid.booleanValue();
    }
}
