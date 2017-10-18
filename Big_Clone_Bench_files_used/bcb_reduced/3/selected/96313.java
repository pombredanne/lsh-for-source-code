package edu.vt.middleware.crypt.signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import edu.vt.middleware.crypt.CryptException;
import edu.vt.middleware.crypt.digest.DigestAlgorithm;
import edu.vt.middleware.crypt.digest.SHA1;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.DSASigner;

/**
 * Implements the DSA signature algorithm.
 *
 * @author  Middleware Services
 * @version  $Revision: 3 $
 */
public class DSASignature extends SignatureAlgorithm {

    /** Signature algorithm name. */
    private static final String ALGORITHM = "DSA";

    /** Computes DSA signatures on hashed messages. */
    private final DSASigner signer = new DSASigner();

    /**
   * Creates a new DSA signature class that uses a SHA-1 for message digest
   * computation. This is the conventional DSA signature configuration.
   */
    public DSASignature() {
        this(new SHA1());
    }

    /**
   * Creates a new DSA signature class that uses the given digest algorithm for
   * message digest computation.
   *
   * @param  d  Message digest algorithm.
   */
    public DSASignature(final DigestAlgorithm d) {
        super(ALGORITHM);
        digest = d;
    }

    /** {@inheritDoc} */
    public void setSignKey(final PrivateKey key) {
        if (!DSAPrivateKey.class.isInstance(key)) {
            throw new IllegalArgumentException("DSA private key required.");
        }
        super.setSignKey(key);
    }

    /** {@inheritDoc} */
    public void setVerifyKey(final PublicKey key) {
        if (!DSAPublicKey.class.isInstance(key)) {
            throw new IllegalArgumentException("DSA public key required.");
        }
        super.setVerifyKey(key);
    }

    /** {@inheritDoc} */
    public void initSign() {
        if (signKey == null) {
            throw new IllegalStateException("Sign key must be set prior to initialization.");
        }
        final DSAPrivateKey privKey = (DSAPrivateKey) signKey;
        final DSAParams params = privKey.getParams();
        final DSAPrivateKeyParameters bcParams = new DSAPrivateKeyParameters(privKey.getX(), new DSAParameters(params.getP(), params.getQ(), params.getG()));
        init(true, bcParams);
    }

    /** {@inheritDoc} */
    public void initVerify() {
        if (verifyKey == null) {
            throw new IllegalStateException("Verify key must be set prior to initialization.");
        }
        final DSAPublicKey pubKey = (DSAPublicKey) verifyKey;
        final DSAParams params = pubKey.getParams();
        final DSAPublicKeyParameters bcParams = new DSAPublicKeyParameters(pubKey.getY(), new DSAParameters(params.getP(), params.getQ(), params.getG()));
        init(false, bcParams);
    }

    /** {@inheritDoc} */
    public byte[] sign(final byte[] data) throws CryptException {
        final BigInteger[] out = signer.generateSignature(digest.digest(data));
        return encode(out[0], out[1]);
    }

    /** {@inheritDoc} */
    public byte[] sign(final InputStream in) throws CryptException, IOException {
        final BigInteger[] out = signer.generateSignature(digest.digest(in));
        return encode(out[0], out[1]);
    }

    /** {@inheritDoc} */
    public boolean verify(final byte[] data, final byte[] signature) throws CryptException {
        final BigInteger[] sig = decode(signature);
        return signer.verifySignature(digest.digest(data), sig[0], sig[1]);
    }

    /** {@inheritDoc} */
    public boolean verify(final InputStream in, final byte[] signature) throws CryptException, IOException {
        final BigInteger[] sig = decode(signature);
        return signer.verifySignature(digest.digest(in), sig[0], sig[1]);
    }

    /**
   * Initialize the signer.
   *
   * @param  forSigning  Whether to initialize signer for the sign operation.
   * @param  params  BC cipher parameters.
   */
    protected void init(final boolean forSigning, final CipherParameters params) {
        if (forSigning && randomProvider != null) {
            signer.init(forSigning, new ParametersWithRandom(params, randomProvider));
        } else {
            signer.init(forSigning, params);
        }
    }

    /**
   * Produces DER-encoded byte array output from the raw DSA signature output
   * parameters, r and s.
   *
   * @param  r  DSA signature output integer r.
   * @param  s  DSA signature output integer s.
   *
   * @return  DER-encoded concatenation of byte representations of r and s.
   *
   * @throws  CryptException  On cryptographic errors.
   */
    protected byte[] encode(final BigInteger r, final BigInteger s) throws CryptException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERInteger(r));
        v.add(new DERInteger(s));
        try {
            new DEROutputStream(out).writeObject(new DERSequence(v));
        } catch (IOException e) {
            throw new CryptException("Error encoding DSA signature.", e);
        }
        return out.toByteArray();
    }

    /**
   * Produces the r,s integer pair of a DSA signature from a DER-encoded byte
   * representation.
   *
   * @param  in  DER-encoded concatenation of byte representation of r and s.
   *
   * @return  DSA signature output parameters (r,s).
   *
   * @throws  CryptException  On cryptographic errors.
   */
    protected BigInteger[] decode(final byte[] in) throws CryptException {
        ASN1Sequence s;
        try {
            s = (ASN1Sequence) new ASN1InputStream(in).readObject();
        } catch (IOException e) {
            throw new CryptException("Error decoding DSA signature.", e);
        }
        return new BigInteger[] { ((DERInteger) s.getObjectAt(0)).getValue(), ((DERInteger) s.getObjectAt(1)).getValue() };
    }
}
