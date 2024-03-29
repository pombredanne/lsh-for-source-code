package be.fedict.eid.applet.service.impl.handler;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.fedict.eid.applet.service.dto.DTOMapper;
import be.fedict.eid.applet.service.impl.RequestContext;
import be.fedict.eid.applet.service.impl.ServiceLocator;
import be.fedict.eid.applet.service.impl.tlv.TlvParser;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.AuditService;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.IdentityIntegrityService;
import be.fedict.eid.applet.service.spi.IdentityRequest;
import be.fedict.eid.applet.service.spi.IdentityService;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.applet.shared.SignCertificatesDataMessage;
import be.fedict.eid.applet.shared.SignRequestMessage;

/**
 * Sign Certificate Data Message Handler.
 * 
 * @author Frank Cornelis
 * 
 */
@HandlesMessage(SignCertificatesDataMessage.class)
public class SignCertificatesDataMessageHandler implements MessageHandler<SignCertificatesDataMessage> {

    private static final Log LOG = LogFactory.getLog(SignCertificatesDataMessageHandler.class);

    @InitParam(HelloMessageHandler.SIGNATURE_SERVICE_INIT_PARAM_NAME)
    private ServiceLocator<SignatureService> signatureServiceLocator;

    @InitParam(HelloMessageHandler.REMOVE_CARD_INIT_PARAM_NAME)
    private boolean removeCard;

    @InitParam(HelloMessageHandler.LOGOFF_INIT_PARAM_NAME)
    private boolean logoff;

    @InitParam(HelloMessageHandler.REQUIRE_SECURE_READER_INIT_PARAM_NAME)
    private boolean requireSecureReader;

    @InitParam(HelloMessageHandler.IDENTITY_INTEGRITY_SERVICE_INIT_PARAM_NAME)
    private ServiceLocator<IdentityIntegrityService> identityIntegrityServiceLocator;

    @InitParam(AuthenticationDataMessageHandler.AUDIT_SERVICE_INIT_PARAM_NAME)
    private ServiceLocator<AuditService> auditServiceLocator;

    @InitParam(HelloMessageHandler.IDENTITY_SERVICE_INIT_PARAM_NAME)
    private ServiceLocator<IdentityService> identityServiceLocator;

    public Object handleMessage(SignCertificatesDataMessage message, Map<String, String> httpHeaders, HttpServletRequest request, HttpSession session) throws ServletException {
        SignatureService signatureService = this.signatureServiceLocator.locateService();
        List<X509Certificate> signingCertificateChain = message.certificateChain;
        X509Certificate signingCertificate = signingCertificateChain.get(0);
        if (null == signingCertificate) {
            throw new ServletException("missing non-repudiation certificate");
        }
        LOG.debug("signing certificate: " + signingCertificateChain.get(0).getSubjectX500Principal());
        RequestContext requestContext = new RequestContext(session);
        boolean includeIdentity = requestContext.includeIdentity();
        boolean includeAddress = requestContext.includeAddress();
        boolean includePhoto = requestContext.includePhoto();
        Identity identity = null;
        Address address = null;
        if (includeIdentity || includeAddress || includePhoto) {
            if (includeIdentity) {
                if (null == message.identityData) {
                    throw new ServletException("identity data missing");
                }
                identity = TlvParser.parse(message.identityData, Identity.class);
            }
            if (includeAddress) {
                if (null == message.addressData) {
                    throw new ServletException("address data missing");
                }
                address = TlvParser.parse(message.addressData, Address.class);
            }
            if (includePhoto) {
                if (null == message.photoData) {
                    throw new ServletException("photo data missing");
                }
                if (null != identity) {
                    byte[] expectedPhotoDigest = identity.photoDigest;
                    byte[] actualPhotoDigest = digestPhoto(message.photoData);
                    if (false == Arrays.equals(expectedPhotoDigest, actualPhotoDigest)) {
                        throw new ServletException("photo digest incorrect");
                    }
                }
            }
            IdentityIntegrityService identityIntegrityService = this.identityIntegrityServiceLocator.locateService();
            if (null != identityIntegrityService) {
                if (null == message.rrnCertificate) {
                    throw new ServletException("national registry certificate not included while requested");
                }
                PublicKey rrnPublicKey = message.rrnCertificate.getPublicKey();
                if (null != message.identityData) {
                    if (null == message.identitySignatureData) {
                        throw new ServletException("missing identity data signature");
                    }
                    verifySignature(message.identitySignatureData, rrnPublicKey, request, message.identityData);
                    if (null != message.addressData) {
                        if (null == message.addressSignatureData) {
                            throw new ServletException("missing address data signature");
                        }
                        byte[] addressFile = trimRight(message.addressData);
                        verifySignature(message.addressSignatureData, rrnPublicKey, request, addressFile, message.identitySignatureData);
                    }
                }
                LOG.debug("checking national registration certificate: " + message.rrnCertificate.getSubjectX500Principal());
                List<X509Certificate> rrnCertificateChain = new LinkedList<X509Certificate>();
                rrnCertificateChain.add(message.rrnCertificate);
                rrnCertificateChain.add(message.rootCertificate);
                identityIntegrityService.checkNationalRegistrationCertificate(rrnCertificateChain);
            }
        }
        DigestInfo digestInfo;
        LOG.debug("signature service class: " + signatureService.getClass().getName());
        if (SignatureServiceEx.class.isAssignableFrom(signatureService.getClass())) {
            LOG.debug("SignatureServiceEx SPI implementation detected");
            SignatureServiceEx signatureServiceEx = (SignatureServiceEx) signatureService;
            DTOMapper dtoMapper = new DTOMapper();
            IdentityDTO identityDTO = dtoMapper.map(identity, IdentityDTO.class);
            AddressDTO addressDTO = dtoMapper.map(address, AddressDTO.class);
            try {
                digestInfo = signatureServiceEx.preSign(null, signingCertificateChain, identityDTO, addressDTO, message.photoData);
            } catch (NoSuchAlgorithmException e) {
                throw new ServletException("no such algo: " + e.getMessage(), e);
            }
        } else {
            LOG.debug("regular SignatureService SPI implementation");
            try {
                digestInfo = signatureService.preSign(null, signingCertificateChain);
            } catch (NoSuchAlgorithmException e) {
                throw new ServletException("no such algo: " + e.getMessage(), e);
            }
        }
        SignatureDataMessageHandler.setDigestValue(digestInfo.digestValue, digestInfo.digestAlgo, session);
        IdentityService identityService = this.identityServiceLocator.locateService();
        boolean removeCard;
        if (null != identityService) {
            IdentityRequest identityRequest = identityService.getIdentityRequest();
            removeCard = identityRequest.removeCard();
        } else {
            removeCard = this.removeCard;
        }
        SignRequestMessage signRequestMessage = new SignRequestMessage(digestInfo.digestValue, digestInfo.digestAlgo, digestInfo.description, this.logoff, removeCard, this.requireSecureReader);
        return signRequestMessage;
    }

    public void init(ServletConfig config) throws ServletException {
    }

    private byte[] digestPhoto(byte[] photoFile) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA1 error: " + e.getMessage(), e);
        }
        byte[] photoDigest = messageDigest.digest(photoFile);
        return photoDigest;
    }

    private void verifySignature(byte[] signatureData, PublicKey publicKey, HttpServletRequest request, byte[]... data) throws ServletException {
        Signature signature;
        try {
            signature = Signature.getInstance("SHA1withRSA");
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("algo error: " + e.getMessage(), e);
        }
        try {
            signature.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            throw new ServletException("key error: " + e.getMessage(), e);
        }
        try {
            for (byte[] dataItem : data) {
                signature.update(dataItem);
            }
            boolean result = signature.verify(signatureData);
            if (false == result) {
                AuditService auditService = this.auditServiceLocator.locateService();
                if (null != auditService) {
                    String remoteAddress = request.getRemoteAddr();
                    auditService.identityIntegrityError(remoteAddress);
                }
                throw new ServletException("signature incorrect");
            }
        } catch (SignatureException e) {
            throw new ServletException("signature error: " + e.getMessage(), e);
        }
    }

    private byte[] trimRight(byte[] addressFile) {
        int idx;
        for (idx = 0; idx < addressFile.length; idx++) {
            if (0 == addressFile[idx]) {
                break;
            }
        }
        byte[] result = new byte[idx];
        System.arraycopy(addressFile, 0, result, 0, idx);
        return result;
    }
}
