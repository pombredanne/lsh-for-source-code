package gnu.java.security.sig.rsa;

import gnu.java.security.Registry;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.sig.BaseSignature;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * The RSA-PKCS1-V1.5 signature scheme is a digital signature scheme with
 * appendix (SSA) combining the RSA algorithm with the EMSA-PKCS1-v1_5 encoding
 * method.
 * <p>
 * References:
 * <ol>
 * <li><a
 * href="http://www.cosic.esat.kuleuven.ac.be/nessie/workshop/submissions/rsa-pss.zip">
 * RSA-PSS Signature Scheme with Appendix, part B.</a><br>
 * Primitive specification and supporting documentation.<br>
 * Jakob Jonsson and Burt Kaliski.</li>
 * <li><a href="http://www.ietf.org/rfc/rfc3447.txt">Public-Key Cryptography
 * Standards (PKCS) #1:</a><br>
 * RSA Cryptography Specifications Version 2.1.<br>
 * Jakob Jonsson and Burt Kaliski.</li>
 * </ol>
 */
public class RSAPKCS1V1_5Signature extends BaseSignature {

    /** The underlying EMSA-PKCS1-v1.5 instance for this object. */
    private EMSA_PKCS1_V1_5 pkcs1;

    /**
   * Default 0-arguments constructor. Uses SHA-1 as the default hash.
   */
    public RSAPKCS1V1_5Signature() {
        this(Registry.SHA160_HASH);
    }

    /**
   * Constructs an instance of this object using the designated message digest
   * algorithm as its underlying hash function.
   * 
   * @param mdName the canonical name of the underlying hash function.
   */
    public RSAPKCS1V1_5Signature(final String mdName) {
        this(HashFactory.getInstance(mdName));
    }

    public RSAPKCS1V1_5Signature(IMessageDigest md) {
        super(Registry.RSA_PKCS1_V1_5_SIG, md);
        pkcs1 = EMSA_PKCS1_V1_5.getInstance(md.name());
    }

    /** Private constructor for cloning purposes. */
    private RSAPKCS1V1_5Signature(final RSAPKCS1V1_5Signature that) {
        this(that.md.name());
        this.publicKey = that.publicKey;
        this.privateKey = that.privateKey;
        this.md = (IMessageDigest) that.md.clone();
        this.pkcs1 = (EMSA_PKCS1_V1_5) that.pkcs1.clone();
    }

    public Object clone() {
        return new RSAPKCS1V1_5Signature(this);
    }

    protected void setupForVerification(final PublicKey k) throws IllegalArgumentException {
        if (!(k instanceof RSAPublicKey)) throw new IllegalArgumentException();
        publicKey = k;
    }

    protected void setupForSigning(final PrivateKey k) throws IllegalArgumentException {
        if (!(k instanceof RSAPrivateKey)) throw new IllegalArgumentException();
        privateKey = k;
    }

    protected Object generateSignature() throws IllegalStateException {
        final int modBits = ((RSAPrivateKey) privateKey).getModulus().bitLength();
        final int k = (modBits + 7) / 8;
        final byte[] EM = pkcs1.encode(md.digest(), k);
        final BigInteger m = new BigInteger(1, EM);
        final BigInteger s = RSA.sign(privateKey, m);
        return RSA.I2OSP(s, k);
    }

    protected boolean verifySignature(final Object sig) throws IllegalStateException {
        if (publicKey == null) throw new IllegalStateException();
        final byte[] S = (byte[]) sig;
        final int modBits = ((RSAPublicKey) publicKey).getModulus().bitLength();
        final int k = (modBits + 7) / 8;
        if (S.length != k) return false;
        final BigInteger s = new BigInteger(1, S);
        final BigInteger m;
        try {
            m = RSA.verify(publicKey, s);
        } catch (IllegalArgumentException x) {
            return false;
        }
        final byte[] EM;
        try {
            EM = RSA.I2OSP(m, k);
        } catch (IllegalArgumentException x) {
            return false;
        }
        final byte[] EMp = pkcs1.encode(md.digest(), k);
        return Arrays.equals(EM, EMp);
    }
}
