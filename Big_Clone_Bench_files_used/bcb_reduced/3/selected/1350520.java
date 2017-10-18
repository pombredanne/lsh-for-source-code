package alto.hash;

/**
 * <p> A basic OAEP implementation for AONT applications.  Requires
 * Generator and Hash functions. </p>
 *
 * <p> OAEP was created by Mihir Bellare and Phillip Rogaway, in their
 * paper "Optimal Asymmetric Encryption", November 1995.  This
 * software implements the basic scheme described in detail on page
 * eight of that paper, and analyzed by Rivest's student Victor Boyko
 * in his thesis. </p>
 * 
 * <p><b>Implementation Notes</b>
 * 
 * <p> This software provides open choices for hash and generator
 * functions.  </p>
 * 
 * <p> This software is incompatible with the EME-OAEP known to
 * SSL. </p>
 * 
 * <p><b>Application Notes</b> </p> 
 * 
 * <p> The intented use of this software is for AONT applications: it
 * is not compatible with EME-OAEP for RSA "padding"; and it is not
 * the plaintext aware OAEP (guarantees that a ciphertext can only be
 * produced from a particular plaintext) with padding.  </p>
 * 
 * <p> An AONT is valuable where the provable weakness of an
 * encryption algorithm is in partial plaintexts -- as in Vernam
 * streams like RC4.  The AONT encoding of the plaintext ensures that
 * a cipher's partial plaintext (the AONT ciphertext) remains a
 * completely secure message (AONT plaintext).  Refer to the AONT
 * papers by Boyko ("Security Properties of OAEP") and Rivest
 * ("Package Transform") for more information. </p>
 * 
 * <p> Boyko's work (Aug 1999) has shown that OAEP is provably secure
 * in the random oracle model for two random oracles: a generator (G),
 * and a hash function (H).  Meaning that all possible attacks on OAEP
 * are necessarily an attack on one of these one- way functions, G or
 * H (G uses H in EME-OAEP). </p> 
 * 
 * <p> 
 * 
 * @see Foam
 *
 * @author John Pritchard
 * @since 1.2
 */
public class OAEP extends Tools implements Cipher {

    /**
     * The OAEP generator could be a hash function or a PRNG or both.
     * 
     * @see OAEP
     *
     * @author John Pritchard
     */
    public interface OAEG {

        /**
         * The OAEP generator function produces a data mask (One Time Pad)
         * given a random seed.
         * 
         * @param oae Caller
         * 
         * @param seed Random seed buffer
         * 
         * @param seedofs Offset index into random seed buffer from which
         * to read the required seed.  
         * 
         * @param seedlen Length of the required seed, in bytes, from the
         * random seed buffer offset.
         * 
         * @param outlen Required number of bytes of output.
         */
        public byte[] G(OAEP oae, byte[] seed, int seedofs, int seedlen, int outlen);
    }

    public static final OAEG OaegDefault = new SHACompressor();

    /**
     * A default generator is the SHA compressor from EOAE.  Better
     * generators are available.
     * 
     * @author John Pritchard
     */
    public static final class SHACompressor implements OAEG {

        public SHACompressor() {
            super();
        }

        /**
         * The OAEP generator function produces a data mask (One Time Pad)
         * given a random seed.
         * 
         * @param oae Caller
         * 
         * @param seed Random seed buffer
         * 
         * @param seedofs Offset index into random seed buffer from which
         * to read the required seed.  
         * 
         * @param seedlen Length of the required seed, in bytes, from the
         * random seed buffer offset.
         * 
         * @param outlen Required number of bytes of output.
         */
        public byte[] G(OAEP oae, byte[] seed, int seedofs, int seedlen, int outlen) {
            int loopbound;
            {
                double lo = outlen, lh = oae.H_length;
                loopbound = (int) (Math.ceil(lo / lh));
            }
            byte[] hbuf = new byte[oae.H_length], mask = new byte[outlen];
            int maskofs = 0;
            for (int i = 0; i < loopbound; i++) {
                oae.H.update(seed, seedofs, seedlen);
                oae.H.update(Integer(i));
                Digest(oae.H, hbuf, 0, oae.H_length);
                if ((maskofs + oae.H_length) < outlen) java.lang.System.arraycopy(hbuf, 0, mask, maskofs, oae.H_length); else {
                    int copymany = outlen - maskofs;
                    if (0 < copymany) java.lang.System.arraycopy(hbuf, 0, mask, maskofs, copymany);
                    return mask;
                }
                maskofs += oae.H_length;
            }
            return mask;
        }
    }

    protected java.security.MessageDigest H;

    protected int H_length;

    protected OAEG G;

    protected java.util.Random rng;

    public OAEP() throws java.security.NoSuchAlgorithmException {
        this(java.security.SecureRandom.getInstance("SHA1PRNG"));
    }

    public OAEP(java.util.Random R) throws java.security.NoSuchAlgorithmException {
        this("SHA1", R);
    }

    public OAEP(String H, java.util.Random R) throws java.security.NoSuchAlgorithmException {
        this(java.security.MessageDigest.getInstance(H), R);
    }

    public OAEP(java.security.MessageDigest H, java.util.Random R) {
        this(H, OaegDefault, R);
    }

    /**
     * Create an OAEP coder.
     * 
     * @param H Hash function, ie: SHA1.
     * 
     * @param G OAEP Generator function.
     * 
     * @param rng Secure generator, subclass of Random.
     */
    public OAEP(java.security.MessageDigest H, OAEG G, java.util.Random rng) {
        super();
        this.H = H;
        this.H_length = H.getDigestLength();
        this.G = G;
        this.rng = rng;
    }

    public void reset(byte[] key) {
        throw new java.lang.RuntimeException("The `reset(byte[])' method is unimplemented in OAEP.");
    }

    public int hLength() {
        return H_length;
    }

    public int blockLengthPlain() {
        return -1;
    }

    public int blockLengthCipher() {
        return -1;
    }

    public int encipherOutputLength(int blk) {
        return this.H_length + blk;
    }

    public int decipherOutputLength(int blk) {
        return blk - this.H_length;
    }

    /**
     *
     */
    public int encipher(byte[] input, int inofs, int inlen, byte[] output, int outofs) {
        int outlen = inlen + H_length, outbuflen = output.length;
        if (outlen > (outbuflen - outofs)) throw new java.lang.IllegalArgumentException("Output buffer too short, requires (inlen+(2*hashlen [" + H_length + "])+1) bytes (after outofs [" + outofs + "])."); else {
            int oc, ic, rc;
            byte[] r = new byte[H_length];
            rng.nextBytes(r);
            byte[] Gr = G.G(this, r, 0, H_length, inlen);
            for (oc = outofs, ic = inofs, rc = 0; ic < inlen; ic++, rc++, oc++) output[oc] = (byte) ((input[ic] & 0xff) ^ (Gr[rc] & 0xff));
            H.update(output, outofs, inlen);
            byte[] t = H.digest();
            for (oc = inlen, ic = 0, rc = 0; ic < H_length; oc++, ic++, rc++) output[oc] = (byte) ((r[rc] & 0xff) ^ (t[ic] & 0xff));
            return outlen;
        }
    }

    /**
     * Input must be a whole EME-OAEP ciphertext for AONT decoding.
     */
    public int decipher(byte[] input, int inofs, int inlen, byte[] output, int outofs) {
        int slen = inlen - H_length;
        int it = inofs + slen;
        byte[] Hs;
        H.update(input, inofs, it);
        Hs = H.digest();
        byte[] r = new byte[H_length];
        for (int ic = it, ir = 0; ir < H_length; ic++, ir++) r[ir] = (byte) ((Hs[ir] & 0xff) ^ (input[ic] & 0xff));
        byte[] Gr = G.G(this, r, 0, H_length, slen);
        for (int oc = outofs, ic = inofs, ir = 0; ir < slen; oc++, ic++, ir++) output[oc] = (byte) ((input[ic] & 0xff) ^ (Gr[ir] & 0xff));
        return slen;
    }

    public static void main(String[] argv) {
        OAEP oaep = null;
        try {
            String msg = argv[0];
            byte[] ptext = alto.io.u.Utf8.encode(msg);
            oaep = new OAEP();
            int ctextLen = oaep.encipherOutputLength(ptext.length);
            if (0 < ctextLen) {
                byte[] ctext = new byte[ctextLen];
                int olen = oaep.encipher(ptext, 0, ptext.length, ctext, 0);
                int ool = oaep.decipherOutputLength(olen);
                if (0 < ool) {
                    byte[] ttext = new byte[ool];
                    olen = oaep.decipher(ctext, 0, olen, ttext, 0);
                    ttext = Trim(ttext, olen);
                    try {
                        String validateMsg = new String(alto.io.u.Utf8.decode(ttext));
                        System.out.println(validateMsg);
                        System.exit(0);
                    } catch (alto.sys.Error.State decoderr) {
                        ComparisonPrint(ptext, ttext, System.err);
                        System.exit(1);
                    }
                } else throw new alto.sys.Error.State("oaep.decipherOutputLength(" + olen + ") = " + ool);
            } else throw new alto.sys.Error.State("oaep.encipherOutputLength(" + ptext.length + ") = " + ctextLen);
        } catch (Exception exc) {
            exc.printStackTrace();
            System.exit(1);
        }
    }
}
