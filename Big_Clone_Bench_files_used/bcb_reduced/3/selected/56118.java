package test.be.fedict.eid.applet.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.ejb3.annotation.LocalBinding;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;

@Stateless
@Local(SignatureServiceEx.class)
@LocalBinding(jndiBinding = "test/eid/applet/model/IdentitySignatureServiceBean")
public class IdentitySignatureServiceBean implements SignatureServiceEx {

    private static final Log LOG = LogFactory.getLog(IdentitySignatureServiceBean.class);

    public void postSign(byte[] signatureValue, List<X509Certificate> signingCertificateChain) {
        LOG.debug("postSign");
        String signatureValueStr = new String(Hex.encodeHex(signatureValue));
        HttpSession session = getHttpSession();
        session.setAttribute("SignatureValue", signatureValueStr);
        session.setAttribute("SigningCertificateChain", signingCertificateChain);
    }

    private HttpSession getHttpSession() {
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        HttpSession httpSession = httpServletRequest.getSession();
        return httpSession;
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain, IdentityDTO identity, AddressDTO address, byte[] photo) throws NoSuchAlgorithmException {
        LOG.debug("preSign (ex)");
        String toBeSigned = identity.name + address.city;
        String digestAlgo = "SHA-1";
        HttpSession httpSession = getHttpSession();
        httpSession.setAttribute("IdentityName", identity.name);
        httpSession.setAttribute("IdentityCity", address.city);
        MessageDigest messageDigest = MessageDigest.getInstance(digestAlgo, new BouncyCastleProvider());
        byte[] digestValue = messageDigest.digest(toBeSigned.getBytes());
        String description = "Test Text Document";
        return new DigestInfo(digestValue, digestAlgo, description);
    }

    public String getFilesDigestAlgorithm() {
        return null;
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain) throws NoSuchAlgorithmException {
        throw new UnsupportedOperationException("this is a SignatureServiceEx implementation");
    }
}
