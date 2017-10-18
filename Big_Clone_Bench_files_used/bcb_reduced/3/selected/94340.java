package gnu.javax.crypto.sasl.srp;

import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.util.Util;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * A Factory class that returns SRP Singletons that know all SRP-related
 * mathematical computations and protocol-related operations for both the
 * client- and server-sides.
 */
public final class SRP {

    /** The map of already instantiated SRP algorithm instances. */
    private static final HashMap algorithms = new HashMap();

    private static final byte COLON = (byte) 0x3A;

    /** The underlying message digest algorithm used for all SRP calculations. */
    private IMessageDigest mda;

    /** Trivial private constructor to enforce Singleton pattern. */
    private SRP(final IMessageDigest mda) {
        super();
        this.mda = mda;
    }

    /**
   * Returns an instance of this object that uses the designated message digest
   * algorithm as its digest function.
   * 
   * @return an instance of this object for the designated digest name.
   */
    public static synchronized SRP instance(String mdName) {
        if (mdName != null) mdName = mdName.trim().toLowerCase();
        if (mdName == null || mdName.equals("")) mdName = SRPRegistry.SRP_DEFAULT_DIGEST_NAME;
        SRP result = (SRP) algorithms.get(mdName);
        if (result == null) {
            final IMessageDigest mda = HashFactory.getInstance(mdName);
            result = new SRP(mda);
            algorithms.put(mdName, result);
        }
        return result;
    }

    private static final byte[] xor(final byte[] b1, final byte[] b2, final int length) {
        final byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) result[i] = (byte) (b1[i] ^ b2[i]);
        return result;
    }

    /** @return the message digest algorithm name used by this instance. */
    public String getAlgorithm() {
        return mda.name();
    }

    /**
   * Returns a new instance of the SRP message digest algorithm --which is
   * SHA-160 by default, but could be anything else provided the proper
   * conditions as specified in the SRP specifications.
   * 
   * @return a new instance of the underlying SRP message digest algorithm.
   * @throws RuntimeException if the implementation of the message digest
   *           algorithm does not support cloning.
   */
    public IMessageDigest newDigest() {
        return (IMessageDigest) mda.clone();
    }

    /**
   * Convenience method to return the result of digesting the designated input
   * with a new instance of the SRP message digest algorithm.
   * 
   * @param src some bytes to digest.
   * @return the bytes constituting the result of digesting the designated input
   *         with a new instance of the SRP message digest algorithm.
   */
    public byte[] digest(final byte[] src) {
        final IMessageDigest hash = (IMessageDigest) mda.clone();
        hash.update(src, 0, src.length);
        return hash.digest();
    }

    /**
   * Convenience method to return the result of digesting the designated input
   * with a new instance of the SRP message digest algorithm.
   * 
   * @param src a String whose bytes (using US-ASCII encoding) are to be
   *          digested.
   * @return the bytes constituting the result of digesting the designated input
   *         with a new instance of the SRP message digest algorithm.
   * @throws UnsupportedEncodingException if US-ASCII charset is not found.
   */
    public byte[] digest(final String src) throws UnsupportedEncodingException {
        return digest(src.getBytes("US-ASCII"));
    }

    /**
   * Convenience method to XOR N bytes from two arrays; N being the output size
   * of the SRP message digest algorithm.
   * 
   * @param a the first byte array.
   * @param b the second one.
   * @return N bytes which are the result of the XOR operations on the first N
   *         bytes from the designated arrays. N is the size of the SRP message
   *         digest algorithm; eg. 20 for SHA-160.
   */
    public byte[] xor(final byte[] a, final byte[] b) {
        return xor(a, b, mda.hashSize());
    }

    public byte[] generateM1(final BigInteger N, final BigInteger g, final String U, final byte[] s, final BigInteger A, final BigInteger B, final byte[] K, final String I, final String L, final byte[] cn, final byte[] cCB) throws UnsupportedEncodingException {
        final IMessageDigest hash = (IMessageDigest) mda.clone();
        byte[] b;
        b = xor(digest(Util.trim(N)), digest(Util.trim(g)));
        hash.update(b, 0, b.length);
        b = digest(U);
        hash.update(b, 0, b.length);
        hash.update(s, 0, s.length);
        b = Util.trim(A);
        hash.update(b, 0, b.length);
        b = Util.trim(B);
        hash.update(b, 0, b.length);
        hash.update(K, 0, K.length);
        b = digest(I);
        hash.update(b, 0, b.length);
        b = digest(L);
        hash.update(b, 0, b.length);
        hash.update(cn, 0, cn.length);
        hash.update(cCB, 0, cCB.length);
        return hash.digest();
    }

    public byte[] generateM2(final BigInteger A, final byte[] M1, final byte[] K, final String U, final String I, final String o, final byte[] sid, final int ttl, final byte[] cIV, final byte[] sIV, final byte[] sCB) throws UnsupportedEncodingException {
        final IMessageDigest hash = (IMessageDigest) mda.clone();
        byte[] b;
        b = Util.trim(A);
        hash.update(b, 0, b.length);
        hash.update(M1, 0, M1.length);
        hash.update(K, 0, K.length);
        b = digest(U);
        hash.update(b, 0, b.length);
        b = digest(I);
        hash.update(b, 0, b.length);
        b = digest(o);
        hash.update(b, 0, b.length);
        hash.update(sid, 0, sid.length);
        hash.update((byte) (ttl >>> 24));
        hash.update((byte) (ttl >>> 16));
        hash.update((byte) (ttl >>> 8));
        hash.update((byte) ttl);
        hash.update(cIV, 0, cIV.length);
        hash.update(sIV, 0, sIV.length);
        hash.update(sCB, 0, sCB.length);
        return hash.digest();
    }

    public byte[] generateKn(final byte[] K, final byte[] cn, final byte[] sn) {
        final IMessageDigest hash = (IMessageDigest) mda.clone();
        hash.update(K, 0, K.length);
        hash.update(cn, 0, cn.length);
        hash.update(sn, 0, sn.length);
        return hash.digest();
    }

    public byte[] computeX(final byte[] s, final String user, final String password) throws UnsupportedEncodingException {
        return computeX(s, user.getBytes("US-ASCII"), password.getBytes("US-ASCII"));
    }

    public byte[] computeX(final byte[] s, final String user, final byte[] p) throws UnsupportedEncodingException {
        return computeX(s, user.getBytes("US-ASCII"), p);
    }

    private byte[] computeX(final byte[] s, final byte[] user, final byte[] p) {
        final IMessageDigest hash = (IMessageDigest) mda.clone();
        hash.update(user, 0, user.length);
        hash.update(COLON);
        hash.update(p, 0, p.length);
        final byte[] up = hash.digest();
        hash.update(s, 0, s.length);
        hash.update(up, 0, up.length);
        return hash.digest();
    }
}
