package codec.x509.extensions;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import codec.asn1.ASN1Exception;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1OctetString;
import codec.asn1.ASN1Sequence;
import codec.asn1.Decoder;
import codec.x509.AlgorithmIdentifier;
import codec.x509.X509Extension;

/**
 * id-sigi-at-certHash OBJECT IDENTIFIER ::= { 1 3 36 8 3 13 }
 * 
 * <pre>
 * certHash EXTENSION ::= {
 *   SYNTAX CertHashSyntax
 *   IDENTIFIED BY id-sigi-at-certHash
 * }
 * CertHashSyntax ::= SEQUENCE {
 *   hashAlgorithm AlgorithmIdentifier,
 *   certificateHash OCTET STRING
 * }
 * </pre>
 * 
 * @author Volker Roth
 * @version "$Id: CertHashExtension.java,v 1.1 2004/08/16 13:31:37 pebinger Exp $"
 */
public class CertHashExtension extends X509Extension {

    public static final String DEFAULT_HASH_ALG = "SHA1";

    public static final String EXTENSION_OID = "1.3.36.8.3.13";

    private ASN1Sequence syntax_;

    private AlgorithmIdentifier hashAlgorithm_;

    private ASN1OctetString certificateHash_;

    /**
     * Creates an instance ready for decoding.
     */
    public CertHashExtension() throws ASN1Exception, CertificateEncodingException {
        setOID(new ASN1ObjectIdentifier(EXTENSION_OID));
        setCritical(false);
        syntax_ = new ASN1Sequence(2);
        hashAlgorithm_ = new AlgorithmIdentifier();
        certificateHash_ = new ASN1OctetString();
        syntax_.add(hashAlgorithm_);
        syntax_.add(certificateHash_);
        setValue(syntax_);
    }

    /**
     * Creates an instance with the hash of the given certificate. The hash is
     * computed with the default hash function {@link #DEFAULT_HASH_ALG
     * DEFAULT_HASH_ALG}.
     */
    public CertHashExtension(X509Certificate cert) throws ASN1Exception, GeneralSecurityException {
        this(cert, DEFAULT_HASH_ALG);
    }

    /**
     * Creates an instance with a hash of the given certificate where the hash
     * is computed with the given hash algorithm.
     * 
     * @throws NoSuchAlgorithmException
     *                 if the given algorithm is not available.
     * @throws CertificateEncodingException
     *                 if the given certificate cannot be encoded correctly.
     * @throws GeneralSecurityException
     *                 if there is another security related error condition.
     * @throws ASN1Exception
     *                 hardly ever, this exception must be declared basically
     *                 because it is declared in the constructor of the super
     *                 class.
     */
    public CertHashExtension(X509Certificate cert, String alg) throws ASN1Exception, GeneralSecurityException {
        this(cert.getEncoded(), alg);
    }

    /**
     * Creates an instance with a hash of the given encoded certificate where
     * the hash is computed with the given hash algorithm.
     * 
     * @throws NoSuchAlgorithmException
     *                 if the given algorithm is not available.
     * @throws GeneralSecurityException
     *                 if there is another security related error condition.
     * @throws ASN1Exception
     *                 hardly ever, this exception must be declared basically
     *                 because it is declared in the constructor of the super
     *                 class.
     */
    public CertHashExtension(byte[] cert, String alg) throws ASN1Exception, GeneralSecurityException {
        if (cert == null) {
            throw new NullPointerException("cert");
        }
        AlgorithmIdentifier aid;
        MessageDigest dig;
        byte[] buf;
        aid = new AlgorithmIdentifier(alg);
        dig = MessageDigest.getInstance(alg);
        buf = dig.digest(cert);
        syntax_ = new ASN1Sequence(2);
        hashAlgorithm_ = aid;
        certificateHash_ = new ASN1OctetString(buf);
        syntax_.add(hashAlgorithm_);
        syntax_.add(certificateHash_);
        setOID(new ASN1ObjectIdentifier(EXTENSION_OID));
        setCritical(false);
        setValue(syntax_);
    }

    public void decode(Decoder dec) throws ASN1Exception, IOException {
        super.decode(dec);
        super.decodeExtensionValue(syntax_);
    }

    public AlgorithmIdentifier getHashAlgorithmID() {
        return hashAlgorithm_;
    }

    public String getHashAlgorithmName() {
        return hashAlgorithm_.getAlgorithmName();
    }

    /**
     * @return <code>true</code> if the hash of the given certificate equals
     *         the hash stored in this structure.
     */
    public boolean verify(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        return verify(cert.getEncoded());
    }

    /**
     * @return <code>true</code> if the hash of the given certificate equals
     *         the hash stored in this structure.
     */
    public boolean verify(byte[] cert) throws NoSuchAlgorithmException {
        MessageDigest dig;
        String alg;
        byte[] buf;
        alg = hashAlgorithm_.getAlgorithmName();
        if (alg == null) {
            throw new NoSuchAlgorithmException(hashAlgorithm_.getAlgorithmOID().toString());
        }
        dig = MessageDigest.getInstance(alg);
        buf = dig.digest(cert);
        return Arrays.equals(buf, certificateHash_.getByteArray());
    }
}
