package threshsig;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Signature Shares Class<BR>
 * Associates a signature share with an id & wraps a static verifier
 * 
 * Reference: "Practical Threshold Signatures",<br>
 * Victor Shoup (sho@zurich.ibm.com), IBM Research Paper RZ3121, 4/30/99<BR>
 * 
 * @author Steve Weis <sweis@mit.edu>
 */
public class SigShare {

    private static final boolean CHECKVERIFIER = true;

    private int id;

    private BigInteger sig;

    private Verifier sigVerifier;

    public SigShare(final int id, final BigInteger sig, final Verifier sigVerifier) {
        this.id = id;
        this.sig = sig;
        this.sigVerifier = sigVerifier;
    }

    public SigShare(final int id, final byte[] sig) {
        this.id = id;
        this.sig = new BigInteger(sig);
    }

    /**
   * Return this share's id. Needed for Lagrange interpolation
   * 
   * @return the id of this key share
   */
    public int getId() {
        return id;
    }

    /**
   * Return a BigInteger representation of this signature
   * 
   * @return a BigInteger representation of this signature
   */
    public BigInteger getSig() {
        return sig;
    }

    /**
   * Return this signature's verifier
   * 
   * @return A verifier for this signaute
   */
    public Verifier getSigVerifier() {
        return sigVerifier;
    }

    /**
   * Return a byte array representation of this signature
   * 
   * @return a byte array representation of this signature
   */
    public byte[] getBytes() {
        return sig.toByteArray();
    }

    @Override
    public String toString() {
        return "Sig[" + id + "]: " + sig.toString();
    }

    public static boolean verify(final byte[] data, final SigShare[] sigs, final int k, final int l, final BigInteger n, final BigInteger e) throws ThresholdSigException {
        final boolean[] haveSig = new boolean[l];
        for (int i = 0; i < k; i++) {
            if (sigs[i] == null) {
                throw new ThresholdSigException("Null signature");
            }
            if (haveSig[sigs[i].getId() - 1]) {
                throw new ThresholdSigException("Duplicate signature: " + sigs[i].getId());
            }
            haveSig[sigs[i].getId() - 1] = true;
        }
        final BigInteger x = (new BigInteger(data)).mod(n);
        final BigInteger delta = SigShare.factorial(l);
        if (CHECKVERIFIER) {
            final BigInteger FOUR = BigInteger.valueOf(4l);
            final BigInteger TWO = BigInteger.valueOf(2l);
            final BigInteger xtilde = x.modPow(FOUR.multiply(delta), n);
            try {
                final MessageDigest md = MessageDigest.getInstance("SHA");
                for (int i = 0; i < k; i++) {
                    md.reset();
                    final Verifier ver = sigs[i].getSigVerifier();
                    final BigInteger v = ver.getGroupVerifier();
                    final BigInteger vi = ver.getShareVerifier();
                    md.update(v.toByteArray());
                    md.update(xtilde.toByteArray());
                    md.update(vi.toByteArray());
                    final BigInteger xi = sigs[i].getSig();
                    md.update(xi.modPow(TWO, n).toByteArray());
                    final BigInteger vz = v.modPow(ver.getZ(), n);
                    final BigInteger vinegc = vi.modPow(ver.getC(), n).modInverse(n);
                    md.update(vz.multiply(vinegc).mod(n).toByteArray());
                    final BigInteger xtildez = xtilde.modPow(ver.getZ(), n);
                    final BigInteger xineg2c = xi.modPow(ver.getC(), n).modInverse(n);
                    md.update(xineg2c.multiply(xtildez).mod(n).toByteArray());
                    final BigInteger result = new BigInteger(md.digest()).mod(n);
                    if (!result.equals(ver.getC())) {
                        debug("Share verifier is not OK");
                        return false;
                    }
                }
            } catch (final java.security.NoSuchAlgorithmException ex) {
                debug("Provider could not locate SHA message digest .");
                ex.printStackTrace();
            }
        }
        BigInteger w = BigInteger.valueOf(1l);
        for (int i = 0; i < k; i++) {
            w = w.multiply(sigs[i].getSig().modPow(SigShare.lambda(sigs[i].getId(), sigs, delta), n));
        }
        final BigInteger eprime = delta.multiply(delta).shiftLeft(2);
        w = w.mod(n);
        final BigInteger xeprime = x.modPow(eprime, n);
        final BigInteger we = w.modPow(e, n);
        return (xeprime.compareTo(we) == 0);
    }

    /**
   * Returns the factorial of the given integer as a BigInteger
   * 
   * @return l!
   */
    private static BigInteger factorial(final int l) {
        BigInteger x = BigInteger.valueOf(1l);
        for (int i = 1; i <= l; i++) {
            x = x.multiply(BigInteger.valueOf(i));
        }
        return x;
    }

    /**
   * Compute lagarange interpolation points Reference: Shoup, pg 7.
   * 
   * @param ik - a point in S
   * @param S - a set of k points in {0...l}
   * @param delta - the factorial of the group size
   * 
   * @return the Lagarange interpolation of these points at 0
   */
    private static BigInteger lambda(final int ik, final SigShare[] S, final BigInteger delta) {
        BigInteger value = delta;
        for (final SigShare element : S) {
            if (element.getId() != ik) {
                value = value.multiply(BigInteger.valueOf(element.getId()));
            }
        }
        for (final SigShare element : S) {
            if (element.getId() != ik) {
                value = value.divide(BigInteger.valueOf((element.getId() - ik)));
            }
        }
        return value;
    }

    private static void debug(final String s) {
        System.err.println("SigShare: " + s);
    }
}
