package gnu.javax.crypto.mac;

import gnu.java.security.Registry;
import gnu.java.security.prng.IRandom;
import gnu.java.security.prng.LimitReachedException;
import java.security.InvalidKeyException;
import java.util.Map;

/**
 * <i>TMMH</i> is a <i>universal</i> hash function suitable for message
 * authentication in the Wegman-Carter paradigm, as in the Stream Cipher
 * Security Transform. It is simple, quick, and especially appropriate for
 * Digital Signal Processors and other processors with a fast multiply
 * operation, though a straightforward implementation requires storage equal in
 * length to the largest message to be hashed.
 * <p>
 * <i>TMMH</i> is a simple hash function which maps a key and a message to a
 * hash value. There are two versions of TMMH: TMMH/16 and TMMH/32. <i>TMMH</i>
 * can be used as a message authentication code, as described in Section 5 (see
 * References).
 * <p>
 * The key, message, and hash value are all octet strings, and the lengths of
 * these quantities are denoted as <code>KEY_LENGTH</code>,
 * <code>MESSAGE_LENGTH</code>, and <code>TAG_LENGTH</code>, respectively.
 * The values of <code>KEY_LENGTH</code> and <code>TAG_LENGTH</code>
 * <bold>MUST</bold> be fixed for any particular fixed value of the key, and
 * must obey the alignment restrictions described below.
 * <p>
 * The parameter <code>MAX_HASH_LENGTH</code>, which denotes the maximum
 * value which <code>MESSAGE_LENGTH</code> may take, is equal to
 * <code>KEY_LENGTH - TAG_LENGTH</code>.
 * <p>
 * References:
 * <ol>
 * <li><a
 * href="http://www.ietf.org/internet-drafts/draft-mcgrew-saag-tmmh-01.txt"> The
 * Truncated Multi-Modular Hash Function (TMMH)</a>, David A. McGrew.</li>
 * </ol>
 */
public class TMMH16 extends BaseMac implements Cloneable {

    public static final String TAG_LENGTH = "gnu.crypto.mac.tmmh.tag.length";

    public static final String KEYSTREAM = "gnu.crypto.mac.tmmh.keystream";

    public static final String PREFIX = "gnu.crypto.mac.tmmh.prefix";

    private static final int P = (1 << 16) + 1;

    /** caches the result of the correctness test, once executed. */
    private static Boolean valid;

    private int tagWords = 0;

    private IRandom keystream = null;

    private byte[] prefix;

    private long keyWords;

    private long msgLength;

    private long msgWords;

    private int[] context;

    private int[] K0;

    private int[] Ki;

    private int Mi;

    /** Trivial 0-arguments constructor. */
    public TMMH16() {
        super(Registry.TMMH16);
    }

    public int macSize() {
        return tagWords * 2;
    }

    public void init(Map attributes) throws InvalidKeyException, IllegalStateException {
        int wantTagLength = 0;
        Integer tagLength = (Integer) attributes.get(TAG_LENGTH);
        if (tagLength == null) {
            if (tagWords == 0) throw new IllegalArgumentException(TAG_LENGTH);
        } else {
            wantTagLength = tagLength.intValue();
            if (wantTagLength < 2 || (wantTagLength % 2 != 0)) throw new IllegalArgumentException(TAG_LENGTH); else if (wantTagLength > (512 / 8)) throw new IllegalArgumentException(TAG_LENGTH);
            tagWords = wantTagLength / 2;
            K0 = new int[tagWords];
            Ki = new int[tagWords];
            context = new int[tagWords];
        }
        prefix = (byte[]) attributes.get(PREFIX);
        if (prefix == null) prefix = new byte[tagWords * 2]; else {
            if (prefix.length != tagWords * 2) throw new IllegalArgumentException(PREFIX);
        }
        IRandom prng = (IRandom) attributes.get(KEYSTREAM);
        if (prng == null) {
            if (keystream == null) throw new IllegalArgumentException(KEYSTREAM);
        } else keystream = prng;
        reset();
        for (int i = 0; i < tagWords; i++) Ki[i] = K0[i] = getNextKeyWord(keystream);
    }

    public void update(byte b) {
        this.update(b, keystream);
    }

    public void update(byte[] b, int offset, int len) {
        for (int i = 0; i < len; i++) this.update(b[offset + i], keystream);
    }

    public byte[] digest() {
        return this.digest(keystream);
    }

    public void reset() {
        msgLength = msgWords = keyWords = 0L;
        Mi = 0;
        for (int i = 0; i < tagWords; i++) context[i] = 0;
    }

    public boolean selfTest() {
        if (valid == null) {
            valid = Boolean.TRUE;
        }
        return valid.booleanValue();
    }

    public Object clone() throws CloneNotSupportedException {
        TMMH16 result = (TMMH16) super.clone();
        if (this.keystream != null) result.keystream = (IRandom) this.keystream.clone();
        if (this.prefix != null) result.prefix = (byte[]) this.prefix.clone();
        if (this.context != null) result.context = (int[]) this.context.clone();
        if (this.K0 != null) result.K0 = (int[]) this.K0.clone();
        if (this.Ki != null) result.Ki = (int[]) this.Ki.clone();
        return result;
    }

    /**
   * Similar to the same method with one argument, but uses the designated
   * random number generator to compute needed keying material.
   * 
   * @param b the byte to process.
   * @param prng the source of randomness to use.
   */
    public void update(byte b, IRandom prng) {
        Mi <<= 8;
        Mi |= b & 0xFF;
        msgLength++;
        if (msgLength % 2 == 0) {
            msgWords++;
            System.arraycopy(Ki, 1, Ki, 0, tagWords - 1);
            Ki[tagWords - 1] = getNextKeyWord(prng);
            long t;
            for (int i = 0; i < tagWords; i++) {
                t = context[i] & 0xFFFFFFFFL;
                t += Ki[i] * Mi;
                context[i] = (int) t;
            }
            Mi = 0;
        }
    }

    /**
   * Similar to the same method with three arguments, but uses the designated
   * random number generator to compute needed keying material.
   * 
   * @param b the byte array to process.
   * @param offset the starting offset in <code>b</code> to start considering
   *          the bytes to process.
   * @param len the number of bytes in <code>b</code> starting from
   *          <code>offset</code> to process.
   * @param prng the source of randomness to use.
   */
    public void update(byte[] b, int offset, int len, IRandom prng) {
        for (int i = 0; i < len; i++) this.update(b[offset + i], prng);
    }

    /**
   * Similar to the same method with no arguments, but uses the designated
   * random number generator to compute needed keying material.
   * 
   * @param prng the source of randomness to use.
   * @return the final result of the algorithm.
   */
    public byte[] digest(IRandom prng) {
        doFinalRound(prng);
        byte[] result = new byte[tagWords * 2];
        for (int i = 0, j = 0; i < tagWords; i++) {
            result[j] = (byte) ((context[i] >>> 8) ^ prefix[j]);
            j++;
            result[j] = (byte) (context[i] ^ prefix[j]);
            j++;
        }
        reset();
        return result;
    }

    private int getNextKeyWord(IRandom prng) {
        int result = 0;
        try {
            result = (prng.nextByte() & 0xFF) << 8 | (prng.nextByte() & 0xFF);
        } catch (LimitReachedException x) {
            throw new RuntimeException(String.valueOf(x));
        }
        keyWords++;
        return result;
    }

    private void doFinalRound(IRandom prng) {
        long limit = msgLength;
        while (msgLength % 2 != 0) update((byte) 0x00, prng);
        long t;
        for (int i = 0; i < tagWords; i++) {
            t = context[i] & 0xFFFFFFFFL;
            t += K0[i] * limit;
            t %= P;
            context[i] = (int) t;
        }
    }
}
