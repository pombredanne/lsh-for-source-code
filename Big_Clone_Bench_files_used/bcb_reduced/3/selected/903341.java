package es.caib.signatura.provider.impl.common;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import es.caib.signatura.api.SignatureVerifyException;
import es.caib.signatura.api.SignatureProviderException;
import es.caib.signatura.api.SignatureTimestampException;
import es.caib.signatura.provider.impl.common.TimeStampManager;

/**
 * Implementation of the version of CMSSignature that mends the timestamp generation. CMSSignature generates
 * the timestamp of the document. CMSSignaturev2 generates the timestamp of the signature. 
 * 
 */
public class CMSSignatureImplv2 extends CMSSignatureImpl {

    public CMSSignatureImplv2() {
        super();
    }

    protected boolean verifyTimestamp(TimeStampToken tst, SignerInformation si, byte[] documentDigest) throws SignatureProviderException, IOException, SignatureVerifyException {
        boolean timeStampVerified = false;
        try {
            byte signatureDigest[] = SHA1Util.digest(si.getSignature());
            if (tst != null) {
                CertStore certs = tst.getCertificatesAndCRLs("Collection", "BC");
                if (certs != null) {
                    Collection certificates = certs.getCertificates(tst.getSID());
                    if (certificates != null && certificates.size() > 0) {
                        X509Certificate timeStampCertificate = getTimeStampCertificates(certificates)[0];
                        try {
                            tst.validate(timeStampCertificate, "BC");
                            timeStampVerified = true;
                            TimeStampTokenInfo tsTokenInfo = tst.getTimeStampInfo();
                            byte[] hashTimeStamp = tsTokenInfo.getMessageImprintDigest();
                            timeStampVerified = timeStampVerified && hashTimeStamp.length == signatureDigest.length;
                            for (int i = 0; i < signatureDigest.length && timeStampVerified; i++) {
                                timeStampVerified = timeStampVerified && (hashTimeStamp[i] == signatureDigest[i]);
                            }
                        } catch (Exception e) {
                            throw new SignatureVerifyException(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
        return timeStampVerified;
    }

    protected CMSSignedData addTimeStamp(CMSSignedData signedData) throws SignatureTimestampException, IOException, TSPException, CMSException, NoSuchAlgorithmException, NoSuchProviderException {
        TimeStampManager tsm = new TimeStampManager();
        return tsm.addTimestamp(getCert(), signedData);
    }
}
