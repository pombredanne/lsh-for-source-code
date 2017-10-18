package alto.hash;

/**
 * <p> Vernam / One Time Pad / RC4 </p>
 *
 * <p> The key material is used as a seed for the key stream
 * generator, K, after it is hashed.  The hash of the key is the
 * actual seed to the key stream generator. </p>
 * 
 * <p><b>Application Notes</b></p>
 * 
 * <p> Never use a key for more than one plaintext. </p>
 * 
 * <p><b>Implementation Notes</b></p>
 * 
 * <p> The original purpose of this class was to split {@link FOAM},
 * in order to make it an {@link OAEP} user like {@link RSAO}. </p>
 * 
 *
 * 
 * @author John Pritchard
 * @since 1.2
 */
public class OTP extends Tools implements Cipher {

    private final String KN;

    private final java.security.MessageDigest KH;

    private final int KH_len;

    private java.security.SecureRandom K;

    public OTP(byte[] key) throws java.security.NoSuchAlgorithmException {
        this("SHA1PRNG", key);
    }

    public OTP(String KN, byte[] key) throws java.security.NoSuchAlgorithmException {
        this(KN, java.security.MessageDigest.getInstance("SHA1"), key);
    }

    /**
     * @param K Key stream generator, a deterministic (secure) PRNG.
     *
     * @param KH Key hash function for seeding the the key stream
     * generator.
     *
     * @param key Shared secret for initializing the key stream
     * generator -- must be the same on both ends of the
     * communications link.
     */
    public OTP(String KN, java.security.MessageDigest KH, byte[] key) throws java.security.NoSuchAlgorithmException {
        super();
        this.KN = KN;
        this.KH = KH;
        this.KH_len = DigestLength(this.KH);
        this.reset(key);
    }

    public int blockLengthPlain() {
        return -1;
    }

    public int blockLengthCipher() {
        return -1;
    }

    public int encipherOutputLength(int blk) {
        return blk;
    }

    public int decipherOutputLength(int blk) {
        return blk;
    }

    /**
     * Initialize the cipher for use or reuse.
     *
     * @param K Key.  Never reuse a key for more than one plaintext.
     *
     * @exception IllegalArgumentException For weak keys.  In this
     * case, make a new key and "reset" again.
     */
    public void reset(byte[] K) throws java.security.NoSuchAlgorithmException {
        if (null != K) {
            this.KH.reset();
            this.KH.update(K);
            byte[] md = this.KH.digest();
            this.K = java.security.SecureRandom.getInstance(this.KN);
            this.K.setSeed(md);
        } else throw new java.lang.IllegalArgumentException();
    }

    public int encipher(byte[] input, int inofs, int inlen, byte[] output, int outofs) {
        return this.block(input, inofs, inlen, output, outofs);
    }

    public int decipher(byte[] input, int inofs, int inlen, byte[] output, int outofs) {
        return this.block(input, inofs, inlen, output, outofs);
    }

    private int block(byte[] input, int inofs, int inlen, byte[] output, int outofs) {
        byte k[] = new byte[inlen];
        this.K.nextBytes(k);
        for (int kc = 0, ic = inofs, oc = outofs, iz = (inofs + inlen); ic < iz; kc++, ic++, oc++) {
            int a = (k[kc] & 0xff);
            int b = (input[ic] & 0xff);
            output[oc] = (byte) ((a ^ b) & 0xff);
        }
        return inlen;
    }

    private static final byte[] main_test_key = { 'h', 'e', 'l', 'l', 'o' };

    public static void main(String[] argv) {
        OTP otp = null;
        try {
            String msg = argv[0];
            byte[] ptext = alto.io.u.Utf8.encode(msg);
            otp = new OTP(main_test_key);
            int ctextLen = otp.encipherOutputLength(ptext.length);
            if (0 < ctextLen) {
                byte[] ctext = new byte[ctextLen];
                int olen = otp.encipher(ptext, 0, ptext.length, ctext, 0);
                otp.reset(main_test_key);
                int ool = otp.decipherOutputLength(olen);
                if (0 < ool) {
                    byte[] ttext = new byte[ool];
                    olen = otp.decipher(ctext, 0, olen, ttext, 0);
                    ttext = Trim(ttext, olen);
                    try {
                        String validateMsg = new String(alto.io.u.Utf8.decode(ttext));
                        System.out.println(validateMsg);
                        System.exit(0);
                    } catch (alto.sys.Error.State decoderr) {
                        ComparisonPrint(ptext, ttext, System.err);
                        System.exit(1);
                    }
                } else throw new alto.sys.Error.State("otp.decipherOutputLength(" + olen + ") = " + ool);
            } else throw new alto.sys.Error.State("otp.encipherOutputLength(" + ptext.length + ") = " + ctextLen);
        } catch (Exception exc) {
            exc.printStackTrace();
            System.exit(1);
        }
    }
}
