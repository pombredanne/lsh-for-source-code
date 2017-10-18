package test.be.fedict.eid.applet.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.ejb.Local;
import javax.ejb.Stateless;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.applet.service.spi.TrustCertificateSecurityException;

@Stateless
@Local(SignatureService.class)
@LocalBinding(jndiBinding = "test/eid/applet/model/UntrustedSignatureServiceBean")
public class UntrustedSignatureServiceBean implements SignatureService {

    private static final Log LOG = LogFactory.getLog(UntrustedSignatureServiceBean.class);

    public void postSign(byte[] signatureValue, List<X509Certificate> signingCertificateChain) throws TrustCertificateSecurityException {
        LOG.debug("postSign");
        throw new TrustCertificateSecurityException();
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain) throws NoSuchAlgorithmException {
        LOG.debug("preSign");
        String toBeSigned = "to be signed";
        String digestAlgo = "SHA-1";
        MessageDigest messageDigest = MessageDigest.getInstance(digestAlgo);
        byte[] digestValue = messageDigest.digest(toBeSigned.getBytes());
        String description = "Test Document";
        return new DigestInfo(digestValue, digestAlgo, description);
    }

    public String getFilesDigestAlgorithm() {
        return null;
    }
}
