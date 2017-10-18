package gnu.java.security.sig.rsa;

import gnu.java.security.Configuration;
import gnu.java.security.Registry;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.sig.BaseSignature;
import gnu.java.security.util.Util;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

/**
 * The RSA-PSS signature scheme is a public-key encryption scheme combining the
 * RSA algorithm with the Probabilistic Signature Scheme (PSS) encoding method.
 * <p>
 * The inventors of RSA are Ronald L. Rivest, Adi Shamir, and Leonard Adleman,
 * while the inventors of the PSS encoding method are Mihir Bellare and Phillip
 * Rogaway. During efforts to adopt RSA-PSS into the P1363a standards effort,
 * certain adaptations to the original version of RSA-PSS were made by Mihir
 * Bellare and Phillip Rogaway and also by Burt Kaliski (the editor of IEEE
 * P1363a) to facilitate implementation and integration into existing protocols.
 * <p>
 * References:
 * <ol>
 * <li><a
 * href="http://www.cosic.esat.kuleuven.ac.be/nessie/workshop/submissions/rsa-pss.zip">
 * RSA-PSS Signature Scheme with Appendix, part B.</a><br>
 * Primitive specification and supporting documentation.<br>
 * Jakob Jonsson and Burt Kaliski.</li>
 * </ol>
 */
public class RSAPSSSignature extends BaseSignature {

    private static final Logger log = Logger.getLogger(RSAPSSSignature.class.getName());

    /** The underlying EMSA-PSS instance for this object. */
    private EMSA_PSS pss;

    /** The desired length in octets of the EMSA-PSS salt. */
    private int sLen;

    /**
   * Default 0-arguments constructor. Uses SHA-1 as the default hash and a
   * 0-octet <i>salt</i>.
   */
    public RSAPSSSignature() {
        this(Registry.SHA160_HASH, 0);
    }

    /**
   * Constructs an instance of this object using the designated message digest
   * algorithm as its underlying hash function, and having 0-octet <i>salt</i>.
   * 
   * @param mdName the canonical name of the underlying hash function.
   */
    public RSAPSSSignature(String mdName) {
        this(mdName, 0);
    }

    /**
   * Constructs an instance of this object using the designated message digest
   * algorithm as its underlying hash function.
   * 
   * @param mdName the canonical name of the underlying hash function.
   * @param sLen the desired length in octets of the salt to use for encoding /
   *          decoding signatures.
   */
    public RSAPSSSignature(String mdName, int sLen) {
        this(HashFactory.getInstance(mdName), sLen);
    }

    public RSAPSSSignature(IMessageDigest md, int sLen) {
        super(Registry.RSA_PSS_SIG, md);
        pss = EMSA_PSS.getInstance(md.name());
        this.sLen = sLen;
    }

    /** Private constructor for cloning purposes. */
    private RSAPSSSignature(RSAPSSSignature that) {
        this(that.md.name(), that.sLen);
        this.publicKey = that.publicKey;
        this.privateKey = that.privateKey;
        this.md = (IMessageDigest) that.md.clone();
        this.pss = (EMSA_PSS) that.pss.clone();
    }

    public Object clone() {
        return new RSAPSSSignature(this);
    }

    protected void setupForVerification(PublicKey k) throws IllegalArgumentException {
        if (!(k instanceof RSAPublicKey)) throw new IllegalArgumentException();
        publicKey = (RSAPublicKey) k;
    }

    protected void setupForSigning(PrivateKey k) throws IllegalArgumentException {
        if (!(k instanceof RSAPrivateKey)) throw new IllegalArgumentException();
        privateKey = (RSAPrivateKey) k;
    }

    protected Object generateSignature() throws IllegalStateException {
        int modBits = ((RSAPrivateKey) privateKey).getModulus().bitLength();
        byte[] salt = new byte[sLen];
        this.nextRandomBytes(salt);
        byte[] EM = pss.encode(md.digest(), modBits - 1, salt);
        if (Configuration.DEBUG) log.fine("EM (sign): " + Util.toString(EM));
        BigInteger m = new BigInteger(1, EM);
        BigInteger s = RSA.sign(privateKey, m);
        int k = (modBits + 7) / 8;
        return RSA.I2OSP(s, k);
    }

    protected boolean verifySignature(Object sig) throws IllegalStateException {
        if (publicKey == null) throw new IllegalStateException();
        byte[] S = (byte[]) sig;
        int modBits = ((RSAPublicKey) publicKey).getModulus().bitLength();
        int k = (modBits + 7) / 8;
        if (S.length != k) return false;
        BigInteger s = new BigInteger(1, S);
        BigInteger m = null;
        try {
            m = RSA.verify(publicKey, s);
        } catch (IllegalArgumentException x) {
            return false;
        }
        int emBits = modBits - 1;
        int emLen = (emBits + 7) / 8;
        byte[] EM = m.toByteArray();
        if (Configuration.DEBUG) log.fine("EM (verify): " + Util.toString(EM));
        if (EM.length > emLen) return false; else if (EM.length < emLen) {
            byte[] newEM = new byte[emLen];
            System.arraycopy(EM, 0, newEM, emLen - EM.length, EM.length);
            EM = newEM;
        }
        byte[] mHash = md.digest();
        boolean result = false;
        try {
            result = pss.decode(mHash, EM, emBits, sLen);
        } catch (IllegalArgumentException x) {
            result = false;
        }
        return result;
    }
}
