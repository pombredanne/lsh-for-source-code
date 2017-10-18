package xades4j.verification;

import com.google.inject.Inject;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;
import xades4j.properties.CompleteRevocationRefsProperty;
import xades4j.properties.QualifyingProperty;
import xades4j.UnsupportedAlgorithmException;
import xades4j.properties.data.CRLRef;
import xades4j.properties.data.CompleteRevocationRefsData;
import xades4j.providers.MessageDigestEngineProvider;
import xades4j.utils.CrlExtensionsUtils;

/**
 * XAdES G.2.2.13
 * @author Luís
 */
class CompleteRevocRefsVerifier implements QualifyingPropertyVerifier<CompleteRevocationRefsData> {

    private final MessageDigestEngineProvider digestEngineProvider;

    @Inject
    public CompleteRevocRefsVerifier(MessageDigestEngineProvider digestEngineProvider) {
        this.digestEngineProvider = digestEngineProvider;
    }

    @Override
    public QualifyingProperty verify(CompleteRevocationRefsData propData, QualifyingPropertyVerificationContext ctx) throws InvalidPropertyException {
        Collection<X509CRL> crls = ctx.getCertChainData().getCrls();
        Collection<CRLRef> crlRefs = new ArrayList<CRLRef>(propData.getCrlRefs());
        if (crls.isEmpty()) throw new CompleteRevocRefsCRLsNotAvailableException();
        for (X509CRL crl : crls) {
            CRLRef match = null;
            for (CRLRef crlRef : crlRefs) {
                if (!crl.getIssuerX500Principal().equals(new X500Principal(crlRef.issuerDN)) || !crl.getThisUpdate().equals(crlRef.issueTime.getTime())) continue;
                try {
                    if (crlRef.serialNumber != null) {
                        BigInteger crlNum = CrlExtensionsUtils.getCrlNumber(crl);
                        if (crlNum != null && !crlRef.serialNumber.equals(crlNum)) continue;
                    }
                    MessageDigest md = this.digestEngineProvider.getEngine(crlRef.digestAlgUri);
                    if (Arrays.equals(md.digest(crl.getEncoded()), crlRef.digestValue)) {
                        match = crlRef;
                        break;
                    }
                } catch (IOException ex) {
                    throw new CompleteRevocRefsReferenceException(crl, ex.getMessage());
                } catch (CRLException ex) {
                    throw new CompleteRevocRefsReferenceException(crl, ex.getMessage());
                } catch (UnsupportedAlgorithmException ex) {
                    throw new CompleteRevocRefsReferenceException(crl, ex.getMessage());
                }
            }
            if (null == match) throw new CompleteRevocRefsReferenceException(crl, "no matching reference");
            crlRefs.remove(match);
        }
        return new CompleteRevocationRefsProperty(crls);
    }
}
