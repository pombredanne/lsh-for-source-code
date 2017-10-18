package codec.pkcs7;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1OctetString;
import codec.asn1.ASN1Type;
import codec.asn1.DEREncoder;
import codec.pkcs9.Attributes;
import codec.pkcs9.InvalidAttributeException;
import codec.util.JCA;
import codec.x501.Attribute;

/**
 * ATTENTION : if the strict DER Encoding shall be used, the function
 * setStrict(true) must be called right after instantiating the Object.
 * 
 * Signs a given <code>Signable</code> object, e.g. a <code>
 * SignedData</code>
 * or a <code>SignedAndEnvelopedData</code>.
 * 
 * @author Volker Roth
 * @version "$Id: Signer.java,v 1.5 2004/08/12 12:27:56 pebinger Exp $"
 */
public class Signer extends Object {

    /**
     * The OID of PKCS#9 MessageDigest Attribute
     */
    private ASN1ObjectIdentifier MESSAGE_DIGEST = new ASN1ObjectIdentifier(new int[] { 1, 2, 840, 113549, 1, 9, 4 });

    /**
     * The OID of PKCS#9 ContentType Attribute
     */
    private ASN1ObjectIdentifier CONTENT_TYPE = new ASN1ObjectIdentifier(new int[] { 1, 2, 840, 113549, 1, 9, 3 });

    /**
     * The size of the buffer allocated for reading and signing data in case
     * this is a detached signature file.
     */
    public static final int BUFFER_SIZE = 1024;

    /**
     * The <code>Signable</code> that is signed.
     */
    protected Signable target_;

    /**
     * The signature engine that is used to compute signatures.
     */
    private Signature sig_;

    /**
     * The {@link SignerInfo SignerInfo} of the signer whose signature
     * generation is in progress.
     */
    protected SignerInfo info_;

    /**
     * The message digest engine that is used while signing is in progress. The
     * digest engine is used only in the presence of authenticated attributes.
     */
    protected MessageDigest digest_;

    /**
     * The content type to be signed.
     */
    protected ASN1ObjectIdentifier contentType_;

    /**
     * <code>true</code> if signing is done with authenticated attributes.
     */
    protected boolean twostep_ = false;

    /**
     * if true, the strict DER Encoding rules are used.
     */
    private boolean strict = false;

    /**
     * Creates an instance ready for signing.
     * 
     * @param sigdat
     *                The <code>Signable</code> to which <code>
     *   SignerInfo</code>
     *                instances are added.
     * @param info
     *                The <code>SignerInfo</code> with the attributes that are
     *                signed along with the data. This instance is later added
     *                to the <code>Signable
     *   </code>.
     * @param key
     *                The private key to use for signing.
     * @throws NoSuchAlgorithmException
     *                 if some required algorithm implementation cannot be
     *                 found.
     * @throws InvalidAlgorithmParameterException
     *                 if some parameters do not match the required algorithms.
     * @throws InvalidKeyException
     *                 if the public key does not match the signature algorithm.
     * @throws InvalidAttributeException
     *                 if the PKCS#9 ContentType attribute in the given
     *                 <code>SignerInfo
     *   </code> does not match the content type
     *                 of the corresponding <code>SignedData</code>.
     */
    public Signer(Signable sigdat, SignerInfo info, PrivateKey key) throws GeneralSecurityException {
        AlgorithmParameterSpec spec;
        ASN1ObjectIdentifier oid;
        Attributes attributes;
        Attribute attribute;
        String sigalg;
        String mdalg;
        if (sigdat == null || info == null || key == null) {
            throw new NullPointerException("Need a Signable, SignerInfo and PrivateKey!");
        }
        info_ = info;
        target_ = sigdat;
        sigalg = info_.getAlgorithm();
        attributes = info_.authenticatedAttributes();
        oid = target_.getContentType();
        if (attributes.size() > 0) {
            twostep_ = true;
            attribute = info_.authenticatedAttributes().getAttribute(CONTENT_TYPE);
            if (attribute == null) {
                attribute = new Attribute((ASN1ObjectIdentifier) CONTENT_TYPE.clone(), (ASN1ObjectIdentifier) oid.clone());
                attributes.add(attribute);
            } else if (attribute.valueCount() < 1) {
                throw new InvalidAttributeException("Content type attribute has no value!");
            } else if (!attribute.valueAt(0).equals(oid)) {
                throw new InvalidAttributeException("Content type attribute has wrong value!");
            }
            attribute = info_.authenticatedAttributes().getAttribute(MESSAGE_DIGEST);
            if (attribute != null) {
                throw new IllegalArgumentException("Message digest attribute already exists!");
            }
            mdalg = JCA.getName(JCA.getDigestOID(sigalg));
            if (mdalg == null) {
                throw new NoSuchAlgorithmException("Cannot determine digest algorithm for " + sigalg);
            }
            digest_ = MessageDigest.getInstance(mdalg);
        }
        sig_ = Signature.getInstance(sigalg);
        spec = info_.getParameterSpec();
        if (spec != null) {
            sig_.setParameter(spec);
        }
        sig_.initSign(key);
    }

    public void setStrict(boolean strictness) {
        this.strict = strictness;
    }

    /**
     * Update operation for signing or verification. The given input stream is
     * not closed after completition of this method.
     * 
     * @param in
     *                The input data to be signed or verified.
     * @throws IOException
     *                 if an I/O error occurs while reading from the given
     *                 stream.
     * @throws SignatureException
     *                 if this instance is not properly initialized.
     * @throws IOException
     *                 if an I/O exception occurs while reading from the input
     *                 stream.
     */
    public void update(InputStream in) throws SignatureException, IOException {
        byte[] buf;
        int n;
        buf = new byte[BUFFER_SIZE];
        try {
            while ((n = in.read(buf)) > 0) {
                update(buf, 0, n);
            }
        } catch (IOException e) {
            reset();
            throw e;
        }
    }

    /**
     * Update operation. Updates the signature computation with the content of
     * the <code>SignedData</code> specified at creation time. If the
     * <code>SignedData</code> has no content then no updating takes place.
     * <p>
     * 
     * <b>Note:</b> updating must be done on the contents octets of the content
     * only, no identifier and length octets are hashed or signed (Verison 1.5).
     * Because the contents are already decoded by the <code>
     * ContentInfo</code>
     * we have to re-encode them according to DER. Unfortunately we cannot tell
     * how many identifier and length octets we have to skip without decoding
     * them first. There is a trick, though. We can briefly modify the tagging
     * of the contents to IMPLICIT tagging while encoding them. That way, the
     * identifier and length octets won't be encoded.
     * <p>
     * 
     * <b>Note:</b> Remember, the tagging will be changed for re-encoding
     * purposes. Custom content type instances must support this (it's supported
     * by default in all <code>codec.asn1.&ast;</code> types).
     * <p>
     * 
     * If the content type is <code>Data</code> then there is no problem
     * because we can simply grab the contents octets from it.
     */
    public void update() throws GeneralSecurityException {
        ASN1Type t;
        boolean tagging;
        t = target_.getContent();
        if (t == null) {
            return;
        }
        if (t instanceof Data) {
            update(((Data) t).getByteArray());
            return;
        }
        ByteArrayOutputStream bos;
        DEREncoder enc;
        tagging = t.isExplicit();
        bos = new ByteArrayOutputStream();
        enc = new DEREncoder(bos);
        if (this.strict) enc.setStrict(true);
        try {
            t.setExplicit(false);
            enc.writeType(t);
            update(bos.toByteArray());
        } catch (Exception e) {
            throw new SignatureException("Exception while re-encoding!");
        } finally {
            t.setExplicit(tagging);
            try {
                enc.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Update operation.
     * 
     * @param b
     *                The input bytes.
     */
    public void update(byte[] b) throws SignatureException {
        update(b, 0, b.length);
    }

    /**
     * Update operation.
     * 
     * @param b
     *                The input bytes.
     * @param offset
     *                The offset into <code>b</code> at which the data to be
     *                signed starts.
     * @param len
     *                The number of bytes starting with <code>offset
     *   </code> to
     *                be signed.
     */
    public void update(byte[] b, int offset, int len) throws SignatureException {
        try {
            if (twostep_) {
                digest_.update(b, offset, len);
            } else {
                sig_.update(b, offset, len);
            }
        } catch (SignatureException e) {
            reset();
            throw e;
        }
    }

    /**
     * Resets this instance to a state before initialization for signing or
     * verifying.
     */
    private void reset() {
        sig_ = null;
        info_ = null;
        digest_ = null;
        target_ = null;
    }

    /**
     * Completes the signing. The <code>SignerInfo</code> is added to the
     * target <code>SignedData</code> automatically.
     * <p>
     * 
     * <b>Note:</b> The signer's certificate is not added to the target
     * <code>SignedData</code>. This has to be done separately. Application
     * shall have full control over the embedding of certificates, because
     * certificates are likely to be distributed by other means as well (e.g.
     * LDAP). So there might not be a need to distibute them with
     * <code>SignedData</code> objects.
     */
    public void sign() throws GeneralSecurityException {
        Attribute attribute;
        byte[] b;
        if (twostep_) {
            b = digest_.digest();
            attribute = new Attribute((ASN1ObjectIdentifier) MESSAGE_DIGEST.clone(), new ASN1OctetString(b));
            info_.addAuthenticatedAttribute(attribute);
            info_.update(sig_);
        }
        if (target_ instanceof SignedAndEnvelopedData) {
            SignedAndEnvelopedData saed;
            byte[] edig;
            saed = (SignedAndEnvelopedData) target_;
            edig = saed.encryptBulkData(sig_.sign());
            info_.setEncryptedDigest(edig);
        } else {
            info_.setEncryptedDigest(sig_.sign());
        }
        target_.addSignerInfo(info_);
    }
}
