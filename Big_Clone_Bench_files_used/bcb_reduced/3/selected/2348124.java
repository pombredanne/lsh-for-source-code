package alto.hash;

import java.math.BigInteger;

/**
 * 
 *
 * @author John Pritchard
 * @since 1.2
 */
public abstract class Tools extends alto.io.u.Hex {

    /**
     * The integer value zero.
     */
    public static final java.math.BigInteger ZERO = java.math.BigInteger.valueOf(0);

    /**
     * The integer value one.
     */
    public static final java.math.BigInteger ONE = java.math.BigInteger.valueOf(1);

    /**
     * The integer value two.
     */
    public static final java.math.BigInteger TWO = java.math.BigInteger.valueOf(2);

    /**
     * The integer value three.
     */
    public static final java.math.BigInteger THREE = java.math.BigInteger.valueOf(3);

    /**
     * The integer value four.
     */
    public static final java.math.BigInteger FOUR = java.math.BigInteger.valueOf(4);

    /**
     * @since 1.1
     */
    public static final int DigestLength(java.security.MessageDigest md) {
        try {
            return md.getDigestLength();
        } catch (Exception exc) {
            String algorithm = md.getAlgorithm();
            return DigestLength(algorithm);
        }
    }

    private static final java.util.Hashtable DIGLEN = new java.util.Hashtable();

    /**
     * @since Java 1.1
     */
    public static final int DigestLength(String algorithm) {
        Object test = DIGLEN.get(algorithm);
        if (test instanceof Integer) return ((Integer) test).intValue(); else {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
                byte[] digest = md.digest();
                int digest_len = digest.length;
                DIGLEN.put(algorithm, new Integer(digest_len));
                return digest_len;
            } catch (java.security.NoSuchAlgorithmException exc) {
                throw new alto.sys.Error.Argument(algorithm, exc);
            }
        }
    }

    /**
     * @since Java 1.1
     */
    public static final int Digest(java.security.MessageDigest md, byte[] into, int into_ofs, int into_len) {
        try {
            return md.digest(into, into_ofs, into_len);
        } catch (Exception exc) {
            byte[] from = md.digest();
            int count = Math.min(from.length, into_len);
            System.arraycopy(from, 0, into, 0, count);
            return count;
        }
    }

    /**
     * @param blklen Desired output block size.  Block length minus
     * input length (inlen) is the number of prefixed null bytes in
     * the returned array.
     * @param in Input buffer
     * @param inofs Offset into the input buffer
     * @param inlen Number of bytes to use from the input buffer
     * @return Null for block length less than or equal to input
     * length, meaning one of either padding not applicable or not
     * necessary.
     */
    public static final byte[] Prepad(int blklen, byte[] in, int inofs, int inlen) {
        if (blklen <= inlen) return null; else {
            int pofs = (blklen - inlen);
            byte[] padded = new byte[blklen];
            System.arraycopy(in, inofs, padded, pofs, inlen);
            return padded;
        }
    }

    public static final byte[] Trim(byte[] in, int to) {
        if (null == in) return null; else if (in.length <= to) return in; else {
            byte[] copier = new byte[to];
            System.arraycopy(in, 0, copier, 0, to);
            return copier;
        }
    }

    public static final byte[] Trim(byte[] in, int ofs, int to) {
        if (null == in) return null; else if (0 != ofs) {
            int inlen = in.length;
            int nlen = Math.min((inlen - ofs), to);
            byte[] copier = new byte[nlen];
            System.arraycopy(in, ofs, copier, 0, nlen);
            return copier;
        } else if (in.length <= to) return in; else {
            byte[] copier = new byte[to];
            System.arraycopy(in, 0, copier, 0, to);
            return copier;
        }
    }

    public static final byte[] Trim(BigInteger bint) {
        byte[] bary = bint.toByteArray();
        int nil = 0, count = bary.length;
        for (int cc = 0; cc < count; cc++) {
            if (0 == bary[cc]) nil += 1; else break;
        }
        if (0 < nil) {
            int nlen = (count - nil);
            byte[] copier = new byte[nlen];
            System.arraycopy(bary, nil, copier, 0, nlen);
            return copier;
        } else return bary;
    }

    public static final void ComparisonPrint(byte[] ptext, byte[] ttext, java.io.PrintStream out) {
        out.println();
        int done = 0;
        for (int pc = 0, tc = 0, count = Math.max(ptext.length, ttext.length); ; pc++, tc++) {
            out.print("   ");
            for (int cc = 0; cc < 4; cc++) {
                if (pc < ptext.length) {
                    out.print(encode(ptext[pc++]) + ' ');
                } else {
                    out.print("   ");
                    done |= 1;
                }
            }
            out.print(" |  ");
            for (int cc = 0; cc < 4; cc++) {
                if (tc < ttext.length) {
                    out.print(encode(ttext[tc++]) + ' ');
                } else {
                    done |= 2;
                    out.print("   ");
                }
            }
            out.println();
            if (3 == done) break;
        }
    }
}
