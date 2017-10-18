package demo.pkcs.pkcs11;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import iaik.asn1.structures.AlgorithmID;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs7.DigestInfo;

/**
 * This is an implementation of a JCA Signature class that uses the PKCS#11
 * wrapper to create the signature. This implementation hashes outside the
 * token (i.e. in software) and support only signing but not verification.
 *
 * @author <a href="mailto:Karl.Scheibelhofer@iaik.at"> Karl Scheibelhofer </a>
 * @version 0.1
 * @invariants
 */
public class PKCS11SignatureEngine extends Signature {

    /**
   * The session that this object uses for signing with the token.
   */
    protected Session session_;

    /**
   * The mechanism that this object uses for signing with the token.
   */
    protected Mechanism signatureMechanism_;

    /**
   * The PKCS#11 key that this object uses for signing with the token.
   */
    protected Key signatureKey_;

    /**
   * The hash algorithm to use for hashing the data.
   */
    protected AlgorithmID hashAlgorithm_;

    /**
   * The digest engine used to hash the data.
   */
    protected MessageDigest digestEngine_;

    /**
   * Creates a new signature engine that uses the given parameters to create
   * the signature on the PKCS#11 token.
   *
   * @param algorithmName The name of the signature algorithm. This class does
   *                      not interpret this name; it uses it as is.
   * @param session The PKCS#11 session to use for signing. It must have the
   *                permissions to sign with the used private key; e.g. it may
   *                require a user session.
   * @param signatureMechanism The PKCS#11 mechanism to use for signing; e.g.
   *                           Mechanism.RSA_PKCS.
   * @param hashAlgorithm The hash algorithm to use for hashing the data;
   *                      e.g. AlgorithmID.sha1.
   * @exception NoSuchAlgorithmException If the hash algorithm is not available.
   * @preconditions
   * @postconditions
   */
    public PKCS11SignatureEngine(String algorithmName, Session session, Mechanism signatureMechanism, AlgorithmID hashAlgorithm) throws NoSuchAlgorithmException {
        super(algorithmName);
        session_ = session;
        signatureMechanism_ = signatureMechanism;
        hashAlgorithm_ = hashAlgorithm;
        digestEngine_ = hashAlgorithm_.getMessageDigestInstance();
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected boolean engineVerify(byte[] signatureValue) throws SignatureException {
        throw new UnsupportedOperationException();
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected java.lang.Object engineGetParameter(String name) throws InvalidParameterException {
        throw new UnsupportedOperationException();
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected void engineSetParameter(String param, java.lang.Object value) throws InvalidParameterException {
        throw new UnsupportedOperationException();
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected void engineInitSign(java.security.PrivateKey privateKey) throws InvalidKeyException {
        if (!(privateKey instanceof TokenPrivateKey)) {
            throw new InvalidKeyException("Private key must be of instance InvalidKeyException");
        }
        signatureKey_ = ((TokenPrivateKey) privateKey).getTokenPrivateKey();
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected byte[] engineSign() throws SignatureException {
        byte[] hashToBeSigned = digestEngine_.digest();
        DigestInfo digestInfoEngine = new DigestInfo(AlgorithmID.sha1, hashToBeSigned);
        byte[] toBeEncrypted = digestInfoEngine.toByteArray();
        byte[] signatureValue = null;
        try {
            session_.signInit(signatureMechanism_, signatureKey_);
            signatureValue = session_.sign(toBeEncrypted);
        } catch (TokenException ex) {
            throw new SignatureException(ex.toString());
        }
        return signatureValue;
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected void engineInitVerify(java.security.PublicKey publicKey) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected void engineUpdate(byte dataByte) throws SignatureException {
        digestEngine_.update(dataByte);
    }

    /**
   * SPI: see documentation of java.security.Signature.
   */
    protected void engineUpdate(byte[] data, int offset, int length) throws SignatureException {
        digestEngine_.update(data, offset, length);
    }
}
