package sun.security.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import com.sun.jarsigner.*;
import java.util.Arrays;
import sun.security.pkcs.*;
import sun.security.timestamp.*;
import sun.security.util.*;
import sun.security.x509.*;

/**
 * This class implements a content signing service.
 * It generates a timestamped signature for a given content according to
 * <a href="http://www.ietf.org/rfc/rfc3161.txt">RFC 3161</a>.
 * The signature along with a trusted timestamp and the signer's certificate
 * are all packaged into a standard PKCS #7 Signed Data message.
 *
 * @author Vincent Ryan
 */
public final class TimestampedSigner extends ContentSigner {

    private static final SecureRandom RANDOM;

    static {
        SecureRandom tmp = null;
        try {
            tmp = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
        }
        RANDOM = tmp;
    }

    private static final String SUBJECT_INFO_ACCESS_OID = "1.3.6.1.5.5.7.1.11";

    private static final String KP_TIMESTAMPING_OID = "1.3.6.1.5.5.7.3.8";

    private static final ObjectIdentifier AD_TIMESTAMPING_Id;

    static {
        ObjectIdentifier tmp = null;
        try {
            tmp = new ObjectIdentifier("1.3.6.1.5.5.7.48.3");
        } catch (IOException e) {
        }
        AD_TIMESTAMPING_Id = tmp;
    }

    private String tsaUrl = null;

    private X509Certificate tsaCertificate = null;

    private MessageDigest messageDigest = null;

    private boolean tsRequestCertificate = true;

    /**
     * Instantiates a content signer that supports timestamped signatures.
     */
    public TimestampedSigner() {
    }

    /**
     * Generates a PKCS #7 signed data message that includes a signature
     * timestamp.
     * This method is used when a signature has already been generated.
     * The signature, a signature timestamp, the signer's certificate chain,
     * and optionally the content that was signed, are packaged into a PKCS #7
     * signed data message.
     *
     * @param parameters The non-null input parameters.
     * @param omitContent true if the content should be omitted from the
     *        signed data message. Otherwise the content is included.
     * @param applyTimestamp true if the signature should be timestamped.
     *        Otherwise timestamping is not performed.
     * @return A PKCS #7 signed data message including a signature timestamp.
     * @throws NoSuchAlgorithmException The exception is thrown if the signature
     *         algorithm is unrecognised.
     * @throws CertificateException The exception is thrown if an error occurs
     *         while processing the signer's certificate or the TSA's
     *         certificate.
     * @throws IOException The exception is thrown if an error occurs while
     *         generating the signature timestamp or while generating the signed
     *         data message.
     * @throws NullPointerException The exception is thrown if parameters is
     *         null.
     */
    public byte[] generateSignedData(ContentSignerParameters parameters, boolean omitContent, boolean applyTimestamp) throws NoSuchAlgorithmException, CertificateException, IOException {
        if (parameters == null) {
            throw new NullPointerException();
        }
        String signatureAlgorithm = parameters.getSignatureAlgorithm();
        String keyAlgorithm = AlgorithmId.getEncAlgFromSigAlg(signatureAlgorithm);
        String digestAlgorithm = AlgorithmId.getDigAlgFromSigAlg(signatureAlgorithm);
        AlgorithmId digestAlgorithmId = AlgorithmId.get(digestAlgorithm);
        X509Certificate[] signerCertificateChain = parameters.getSignerCertificateChain();
        Principal issuerName = signerCertificateChain[0].getIssuerDN();
        if (!(issuerName instanceof X500Name)) {
            X509CertInfo tbsCert = new X509CertInfo(signerCertificateChain[0].getTBSCertificate());
            issuerName = (Principal) tbsCert.get(CertificateIssuerName.NAME + "." + CertificateIssuerName.DN_NAME);
        }
        BigInteger serialNumber = signerCertificateChain[0].getSerialNumber();
        byte[] content = parameters.getContent();
        ContentInfo contentInfo;
        if (omitContent) {
            contentInfo = new ContentInfo(ContentInfo.DATA_OID, null);
        } else {
            contentInfo = new ContentInfo(content);
        }
        byte[] signature = parameters.getSignature();
        SignerInfo signerInfo = null;
        if (applyTimestamp) {
            tsaCertificate = parameters.getTimestampingAuthorityCertificate();
            URI tsaUri = parameters.getTimestampingAuthority();
            if (tsaUri != null) {
                tsaUrl = tsaUri.toString();
            } else {
                String certUrl = getTimestampingUrl(tsaCertificate);
                if (certUrl == null) {
                    throw new CertificateException("Subject Information Access extension not found");
                }
                tsaUrl = certUrl;
            }
            byte[] tsToken = generateTimestampToken(signature);
            PKCS9Attributes unsignedAttrs = new PKCS9Attributes(new PKCS9Attribute[] { new PKCS9Attribute(PKCS9Attribute.SIGNATURE_TIMESTAMP_TOKEN_STR, tsToken) });
            signerInfo = new SignerInfo((X500Name) issuerName, serialNumber, digestAlgorithmId, null, AlgorithmId.get(keyAlgorithm), signature, unsignedAttrs);
        } else {
            signerInfo = new SignerInfo((X500Name) issuerName, serialNumber, digestAlgorithmId, AlgorithmId.get(keyAlgorithm), signature);
        }
        SignerInfo[] signerInfos = { signerInfo };
        AlgorithmId[] algorithms = { digestAlgorithmId };
        PKCS7 p7 = new PKCS7(algorithms, contentInfo, signerCertificateChain, signerInfos);
        ByteArrayOutputStream p7out = new ByteArrayOutputStream();
        p7.encodeSignedData(p7out);
        return p7out.toByteArray();
    }

    /**
     * Examine the certificate for a Subject Information Access extension
     * (<a href="http://www.ietf.org/rfc/rfc3280.txt">RFC 3280</a>).
     * The extension's <tt>accessMethod</tt> field should contain the object
     * identifier defined for timestamping: 1.3.6.1.5.5.7.48.3 and its
     * <tt>accessLocation</tt> field should contain an HTTP URL.
     *
     * @param tsaCertificate An X.509 certificate for the TSA.
     * @return An HTTP URL or null if none was found.
     */
    public static String getTimestampingUrl(X509Certificate tsaCertificate) {
        if (tsaCertificate == null) {
            return null;
        }
        try {
            byte[] extensionValue = tsaCertificate.getExtensionValue(SUBJECT_INFO_ACCESS_OID);
            if (extensionValue == null) {
                return null;
            }
            DerInputStream der = new DerInputStream(extensionValue);
            der = new DerInputStream(der.getOctetString());
            DerValue[] derValue = der.getSequence(5);
            AccessDescription description;
            GeneralName location;
            URIName uri;
            for (int i = 0; i < derValue.length; i++) {
                description = new AccessDescription(derValue[i]);
                if (description.getAccessMethod().equals(AD_TIMESTAMPING_Id)) {
                    location = description.getAccessLocation();
                    if (location.getType() == GeneralNameInterface.NAME_URI) {
                        uri = (URIName) location.getName();
                        if (uri.getScheme().equalsIgnoreCase("http")) {
                            return uri.getName();
                        }
                    }
                }
            }
        } catch (IOException ioe) {
        }
        return null;
    }

    private byte[] generateTimestampToken(byte[] toBeTimestamped) throws CertificateException, IOException {
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
            }
        }
        byte[] digest = messageDigest.digest(toBeTimestamped);
        TSRequest tsQuery = new TSRequest(digest, "SHA-1");
        BigInteger nonce = null;
        if (RANDOM != null) {
            nonce = new BigInteger(64, RANDOM);
            tsQuery.setNonce(nonce);
        }
        tsQuery.requestCertificate(tsRequestCertificate);
        Timestamper tsa = new HttpTimestamper(tsaUrl);
        TSResponse tsReply = tsa.generateTimestamp(tsQuery);
        int status = tsReply.getStatusCode();
        if (status != 0 && status != 1) {
            int failureCode = tsReply.getFailureCode();
            if (failureCode == -1) {
                throw new IOException("Error generating timestamp: " + tsReply.getStatusCodeAsText());
            } else {
                throw new IOException("Error generating timestamp: " + tsReply.getStatusCodeAsText() + " " + tsReply.getFailureCodeAsText());
            }
        }
        PKCS7 tsToken = tsReply.getToken();
        TimestampToken tst = new TimestampToken(tsToken.getContentInfo().getData());
        if (!tst.getHashAlgorithm().equals(new AlgorithmId(new ObjectIdentifier("1.3.14.3.2.26")))) {
            throw new IOException("Digest algorithm not SHA-1 in timestamp token");
        }
        if (!Arrays.equals(tst.getHashedMessage(), digest)) {
            throw new IOException("Digest octets changed in timestamp token");
        }
        ;
        BigInteger replyNonce = tst.getNonce();
        if (replyNonce == null && nonce != null) {
            throw new IOException("Nonce missing in timestamp token");
        }
        if (replyNonce != null && !replyNonce.equals(nonce)) {
            throw new IOException("Nonce changed in timestamp token");
        }
        List<String> keyPurposes = null;
        X509Certificate[] certs = tsToken.getCertificates();
        if (certs != null && certs.length > 0) {
            for (X509Certificate cert : certs) {
                boolean isSigner = false;
                for (X509Certificate cert2 : certs) {
                    if (cert != cert2) {
                        if (cert.getSubjectDN().equals(cert2.getIssuerDN())) {
                            isSigner = true;
                            break;
                        }
                    }
                }
                if (!isSigner) {
                    keyPurposes = cert.getExtendedKeyUsage();
                    if (keyPurposes == null || !keyPurposes.contains(KP_TIMESTAMPING_OID)) {
                        throw new CertificateException("Certificate is not valid for timestamping");
                    }
                    break;
                }
            }
        }
        return tsReply.getEncodedToken();
    }
}
