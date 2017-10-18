package xades4j.production;

import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import xades4j.properties.QualifyingProperty;
import xades4j.UnsupportedAlgorithmException;
import xades4j.properties.data.BaseCertRefsData;
import xades4j.properties.data.CertRef;
import xades4j.properties.data.PropertyDataObject;
import xades4j.providers.AlgorithmsProviderEx;
import xades4j.providers.MessageDigestEngineProvider;

/**
 *
 * @author Luís
 */
class DataGenBaseCertRefs {

    private final AlgorithmsProviderEx algorithmsProvider;

    private final MessageDigestEngineProvider messageDigestProvider;

    protected DataGenBaseCertRefs(AlgorithmsProviderEx algorithmsProvider, MessageDigestEngineProvider messageDigestProvider) {
        this.algorithmsProvider = algorithmsProvider;
        this.messageDigestProvider = messageDigestProvider;
    }

    protected PropertyDataObject generate(Collection<X509Certificate> certs, BaseCertRefsData certRefsData, QualifyingProperty prop) throws PropertyDataGenerationException {
        if (null == certs) {
            throw new PropertyDataGenerationException(prop, "certificates not provided");
        }
        try {
            String digestAlgUri = this.algorithmsProvider.getDigestAlgorithmForReferenceProperties();
            MessageDigest messageDigest = this.messageDigestProvider.getEngine(digestAlgUri);
            for (X509Certificate cert : certs) {
                byte[] digestValue = messageDigest.digest(cert.getEncoded());
                certRefsData.addCertRef(new CertRef(cert.getIssuerX500Principal().getName(), cert.getSerialNumber(), digestAlgUri, digestValue));
            }
            return certRefsData;
        } catch (UnsupportedAlgorithmException ex) {
            throw new PropertyDataGenerationException(prop, ex.getMessage(), ex);
        } catch (CertificateEncodingException ex) {
            throw new PropertyDataGenerationException(prop, "cannot get encoded certificate", ex);
        }
    }
}
