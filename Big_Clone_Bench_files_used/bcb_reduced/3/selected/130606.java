package org.waveprotocol.wave.crypto;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Class representing a signer (basically, a cert chain).
 */
public class SignerInfo {

    private static final String PKI_PATH_ENCODING = "PkiPath";

    private static final String X509 = "X.509";

    private final List<X509Certificate> certChain;

    private final ProtocolSignerInfo protobuf;

    private final byte[] signerId;

    /**
   * Public constructor.
   * @param hashAlg The hash algorithm to use to calculate the signer id (which
   * is the base-64-encoding of the hash of the PkiPath-encoding of the cert
   * chain).
   * @param certs the cert chain used by this signer. Cert of the signer is
   *   first, and cert of the CA is last.
   * @param domain the domain that the certificates are issued to. This should
   *   match the CN in the target certificate.
   * @throws SignatureException if the certs couldn't be parsed into a cert
   *   chain, or if the hash couldn't be calculated.
   */
    public SignerInfo(HashAlgorithm hashAlg, List<X509Certificate> certs, String domain) throws SignatureException {
        Preconditions.checkArgument(certs.size() > 0, "need at least one" + "cert in the chain");
        try {
            this.protobuf = ProtocolSignerInfo.newBuilder().setHashAlgorithm(hashAlg).setDomain(domain).addAllCertificate(getCertificatesAsListOfByteArrays(certs)).build();
        } catch (CertificateEncodingException e) {
            throw new SignatureException("couldn't parse certificates", e);
        }
        this.certChain = ImmutableList.copyOf(certs);
        this.signerId = calculateSignerId(this.certChain);
    }

    /**
   * Public constructor from a protobuf.
   *
   * @param protobuf
   * @throws SignatureException
   */
    public SignerInfo(ProtocolSignerInfo protobuf) throws SignatureException {
        this.protobuf = protobuf;
        this.certChain = getCertificatesFromListOfByteArrays(protobuf.getCertificateList());
        this.signerId = calculateSignerId(this.certChain);
    }

    private List<ByteString> getCertificatesAsListOfByteArrays(List<X509Certificate> certs) throws CertificateEncodingException {
        List<ByteString> result = Lists.newArrayList();
        for (X509Certificate cert : certs) {
            result.add(ByteString.copyFrom(cert.getEncoded()));
        }
        return result;
    }

    private List<X509Certificate> getCertificatesFromListOfByteArrays(List<? extends ByteString> certs) throws SignatureException {
        try {
            List<X509Certificate> result = Lists.newArrayList();
            CertificateFactory certFactory = CertificateFactory.getInstance(X509);
            for (ByteString certAsBytes : certs) {
                result.add((X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certAsBytes.toByteArray())));
            }
            return result;
        } catch (CertificateException e) {
            throw new SignatureException(e);
        }
    }

    /**
   * Returns the cert chain used by this signer. Cert of the signer is
   * first, and cert of the CA is last.
   */
    public List<X509Certificate> getCertificates() {
        return certChain;
    }

    /**
   * Returns the hash algorithm used to calculate the signer id from the cert
   * chain.
   */
    public HashAlgorithm getHashAlgorithm() {
        return protobuf.getHashAlgorithm();
    }

    /**
   * The domain that this signer claims to belong to. It is the responsibility
   * of the client of this interface to verify that the domain matches the
   * principal to which the target certificate of the certificate chain was
   * issued.
   */
    public String getDomain() {
        return protobuf.getDomain();
    }

    /**
   * Returns the id of this signer (cert chain). The signer id is the
   * base-64-encoding of the hash of the PkiPath-encoding of the cert chain.
   */
    public byte[] getSignerId() {
        return signerId;
    }

    public ProtocolSignerInfo toProtoBuf() {
        return protobuf;
    }

    private byte[] calculateSignerId(List<? extends X509Certificate> certs) throws SignatureException {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance(X509);
            CertPath path = certFactory.generateCertPath(certs);
            byte[] encodedCertPath = path.getEncoded(PKI_PATH_ENCODING);
            MessageDigest digest = MessageDigest.getInstance(AlgorithmUtil.getJceName(getHashAlgorithm()));
            return digest.digest(encodedCertPath);
        } catch (CertificateException e) {
            throw new SignatureException("could not parse certificate chain", e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException("could not calculate hash of cert chain", e);
        }
    }
}
