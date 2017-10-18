package test.be.fedict.eid.applet.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
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
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.SignatureService;

@Stateless
@Local(SignatureService.class)
@LocalBinding(jndiBinding = "test/eid/applet/model/FilesSignatureServiceBean")
public class FilesSignatureServiceBean implements SignatureService {

    private static final Log LOG = LogFactory.getLog(FilesSignatureServiceBean.class);

    public void postSign(byte[] signatureValue, List<X509Certificate> signingCertificateChain) {
        LOG.debug("postSign");
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        String signatureValueStr = new String(Hex.encodeHex(signatureValue));
        HttpSession session = httpServletRequest.getSession();
        session.setAttribute("SignatureValue", signatureValueStr);
        session.setAttribute("SigningCertificateChain", signingCertificateChain);
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain) throws NoSuchAlgorithmException {
        LOG.debug("preSign");
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        HttpSession session = httpServletRequest.getSession();
        String signDigestAlgo = (String) session.getAttribute("signDigestAlgo");
        LOG.debug("signature digest algo: " + signDigestAlgo);
        List<String> fileDescriptions = new LinkedList<String>();
        MessageDigest messageDigest = MessageDigest.getInstance(signDigestAlgo, new BouncyCastleProvider());
        for (DigestInfo digestInfo : digestInfos) {
            LOG.debug("processing digest for: " + digestInfo.description);
            fileDescriptions.add(digestInfo.description + "\n");
            messageDigest.update(digestInfo.digestValue);
        }
        byte[] digestValue = messageDigest.digest();
        session.setAttribute("signedFiles", fileDescriptions);
        String description = "Local Test Files";
        return new DigestInfo(digestValue, signDigestAlgo, description);
    }

    public String getFilesDigestAlgorithm() {
        LOG.debug("getFileDigestAlgoritm()");
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        HttpSession session = httpServletRequest.getSession();
        String filesDigestAlgo = (String) session.getAttribute("filesDigestAlgo");
        LOG.debug("files digest algo: " + filesDigestAlgo);
        return filesDigestAlgo;
    }
}
