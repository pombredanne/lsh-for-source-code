package gnu.javax.crypto.key.dh;

import gnu.java.security.hash.Sha160;
import gnu.java.security.util.PRNG;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * An implementation of the Diffie-Hellman parameter generation as defined in
 * RFC-2631.
 * <p>
 * Reference:
 * <ol>
 * <li><a href="http://www.ietf.org/rfc/rfc2631.txt">Diffie-Hellman Key
 * Agreement Method</a><br>
 * Eric Rescorla.</li>
 * </ol>
 */
public class RFC2631 {

    public static final int DH_PARAMS_SEED = 0;

    public static final int DH_PARAMS_COUNTER = 1;

    public static final int DH_PARAMS_Q = 2;

    public static final int DH_PARAMS_P = 3;

    public static final int DH_PARAMS_J = 4;

    public static final int DH_PARAMS_G = 5;

    private static final BigInteger TWO = BigInteger.valueOf(2L);

    /** The SHA instance to use. */
    private Sha160 sha = new Sha160();

    /** Length of private modulus and of q. */
    private int m;

    /** Length of public modulus p. */
    private int L;

    /** The optional {@link SecureRandom} instance to use. */
    private SecureRandom rnd = null;

    /** Our default source of randomness. */
    private PRNG prng = null;

    public RFC2631(int m, int L, SecureRandom rnd) {
        super();
        this.m = m;
        this.L = L;
        this.rnd = rnd;
    }

    public BigInteger[] generateParameters() {
        int i, j, counter;
        byte[] u1, u2, v;
        byte[] seedBytes = new byte[m / 8];
        BigInteger SEED, U, q, R, V, W, X, p, g;
        int m_ = (m + 159) / 160;
        int L_ = (L + 159) / 160;
        int N_ = (L + 1023) / 1024;
        algorithm: while (true) {
            step4: while (true) {
                nextRandomBytes(seedBytes);
                SEED = new BigInteger(1, seedBytes).setBit(m - 1).setBit(0);
                U = BigInteger.ZERO;
                for (i = 0; i < m_; i++) {
                    u1 = SEED.add(BigInteger.valueOf(i)).toByteArray();
                    u2 = SEED.add(BigInteger.valueOf(m_ + i)).toByteArray();
                    sha.update(u1, 0, u1.length);
                    u1 = sha.digest();
                    sha.update(u2, 0, u2.length);
                    u2 = sha.digest();
                    for (j = 0; j < u1.length; j++) u1[j] ^= u2[j];
                    U = U.add(new BigInteger(1, u1).multiply(TWO.pow(160 * i)));
                }
                q = U.setBit(m - 1).setBit(0);
                if (q.isProbablePrime(80)) break step4;
            }
            counter = 0;
            step9: while (true) {
                R = SEED.add(BigInteger.valueOf(2 * m_)).add(BigInteger.valueOf(L_ * counter));
                V = BigInteger.ZERO;
                for (i = 0; i < L_; i++) {
                    v = R.toByteArray();
                    sha.update(v, 0, v.length);
                    v = sha.digest();
                    V = V.add(new BigInteger(1, v).multiply(TWO.pow(160 * i)));
                }
                W = V.mod(TWO.pow(L));
                X = W.setBit(L - 1);
                p = X.add(BigInteger.ONE).subtract(X.mod(TWO.multiply(q)));
                if (p.isProbablePrime(80)) {
                    break algorithm;
                }
                counter++;
                if (counter >= 4096 * N_) continue algorithm;
            }
        }
        BigInteger e = p.subtract(BigInteger.ONE).divide(q);
        BigInteger h = TWO;
        BigInteger p_minus_1 = p.subtract(BigInteger.ONE);
        g = TWO;
        for (; h.compareTo(p_minus_1) < 0; h = h.add(BigInteger.ONE)) {
            g = h.modPow(e, p);
            if (!g.equals(BigInteger.ONE)) break;
        }
        return new BigInteger[] { SEED, BigInteger.valueOf(counter), q, p, e, g };
    }

    /**
   * Fills the designated byte array with random data.
   *
   * @param buffer the byte array to fill with random data.
   */
    private void nextRandomBytes(byte[] buffer) {
        if (rnd != null) rnd.nextBytes(buffer); else getDefaultPRNG().nextBytes(buffer);
    }

    private PRNG getDefaultPRNG() {
        if (prng == null) prng = PRNG.getInstance();
        return prng;
    }
}
