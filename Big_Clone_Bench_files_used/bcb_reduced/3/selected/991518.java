package org.xmldap.rp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.InfoCardProcessingException;
import org.xmldap.saml.Conditions;
import org.xmldap.util.XSDDateTime;
import org.xmldap.ws.WSConstants;
import org.xmldap.xmldsig.ValidatingBaseEnvelopedSignature;

public class Token {

    private boolean haveValidatedAll = false;

    private boolean valid = false;

    private boolean haveValidatedConditions = false;

    private boolean conditionsValid = false;

    private Conditions conditions = null;

    private boolean haveAudience = false;

    private String audience = null;

    private boolean haveValidatedCertificate = false;

    private boolean certificateValid = true;

    private X509Certificate certificate = null;

    private boolean haveValidatedSignature = false;

    private boolean signatureValid = false;

    private String tokenStr = null;

    private String decryptedToken = null;

    private Document doc = null;

    private HashMap claims = null;

    boolean isEncrypted = false;

    public Token(String tokenStr, PrivateKey privateKey) throws InfoCardProcessingException {
        this.tokenStr = tokenStr;
        try {
            DecryptUtil decryptUtil = new DecryptUtil(tokenStr);
            isEncrypted = decryptUtil.isEncrypted();
            if (isEncrypted) {
                decryptedToken = decryptUtil.decryptToken(privateKey);
                if (decryptedToken == null) {
                    throw new InfoCardProcessingException("Result of token decryption was null");
                }
                Token tmpToken = new Token(decryptedToken, privateKey);
                if (tmpToken.isEncrypted()) {
                    String decryptedTmpToken = tmpToken.getDecryptedToken();
                    if (decryptedTmpToken == null) {
                        throw new InfoCardProcessingException("Result of DOUBLE token decryption was null");
                    }
                    decryptedToken = decryptedTmpToken;
                }
                System.out.println("Token.java decryptedToken=" + decryptedToken);
            }
        } catch (CryptoException e) {
            throw new InfoCardProcessingException("Error decrypting encrypted token", e);
        }
    }

    public Document getDoc() throws InfoCardProcessingException {
        if (doc == null) {
            try {
                if (isEncrypted) {
                    doc = org.xmldap.xml.XmlUtils.parse(decryptedToken);
                } else {
                    doc = org.xmldap.xml.XmlUtils.parse(tokenStr);
                }
            } catch (ParsingException e) {
                throw new InfoCardProcessingException("Unable to parse decrypted token into a XOM document", e);
            } catch (IOException e) {
                throw new InfoCardProcessingException("Unable to parse decrypted token into a XOM document", e);
            }
        }
        return doc;
    }

    public String getEncryptedToken() {
        return tokenStr;
    }

    public String getDecryptedToken() {
        return decryptedToken;
    }

    public String getAudience() throws InfoCardProcessingException {
        if (!haveValidatedConditions) {
            validateConditions();
        }
        return audience;
    }

    public boolean isConditionsValid() throws InfoCardProcessingException {
        if (!haveValidatedConditions) validateConditions();
        return conditionsValid;
    }

    public Calendar getStartValidityPeriod() throws InfoCardProcessingException {
        if (!haveValidatedConditions) validateConditions();
        return conditions.getNotBefore();
    }

    public Calendar getEndValidityPeriod() throws InfoCardProcessingException {
        if (!haveValidatedConditions) validateConditions();
        return conditions.getNotOnOrAfter();
    }

    public X509Certificate getCertificate() throws InfoCardProcessingException {
        if (certificate == null) parseCertificate();
        return certificate;
    }

    public Map getClaims() throws InfoCardProcessingException {
        if (claims == null) parseClaims();
        return claims;
    }

    public boolean isCertificateValid() throws InfoCardProcessingException {
        if (!haveValidatedCertificate) {
            try {
                certificate = getCertificateOrNull();
                if (certificate != null) {
                    certificate.checkValidity();
                } else {
                    throw new InfoCardProcessingException("This token does not have a certificate");
                }
            } catch (CertificateExpiredException e) {
                System.out.println("Certificate expired: " + e.getMessage());
                certificateValid = false;
            } catch (CertificateNotYetValidException e) {
                System.out.println("Certificate not yet valid");
                certificateValid = false;
            }
        }
        return certificateValid;
    }

    public boolean isSignatureValid() throws InfoCardProcessingException {
        if (!haveValidatedSignature) {
            try {
                signatureValid = (ValidatingBaseEnvelopedSignature.validate(getDoc()) != null);
            } catch (CryptoException e) {
                throw new InfoCardProcessingException("Error validating signature", e);
            }
        }
        return signatureValid;
    }

    public boolean isValid() throws InfoCardProcessingException {
        if (!haveValidatedAll) {
            if (isConditionsValid() && isCertificateValid() && isSignatureValid()) valid = true;
            haveValidatedAll = true;
        }
        return valid;
    }

    private void parseConditions() throws InfoCardProcessingException {
        XPathContext context = buildSamlXPathContext();
        Nodes nodes = getDoc().query("//saml:Conditions", context);
        Element element = (Element) nodes.get(0);
        String notBeforeVal = element.getAttribute("NotBefore").getValue();
        String notOnOrAfterVal = element.getAttribute("NotOnOrAfter").getValue();
        conditions = new Conditions(XSDDateTime.parse(notBeforeVal), XSDDateTime.parse(notOnOrAfterVal));
        Elements elements = element.getChildElements("AudienceRestrictionCondition", WSConstants.SAML11_NAMESPACE);
        if (elements.size() == 1) {
            Element audienceRestrictionCondition = elements.get(0);
            elements = audienceRestrictionCondition.getChildElements("Audience", WSConstants.SAML11_NAMESPACE);
            if (elements.size() == 1) {
                audience = elements.get(0).getValue();
            } else {
                throw new InfoCardProcessingException("Expected the element AudienceRestrictionCondition to have one child Audience, but found" + elements.size());
            }
        } else {
            audience = null;
        }
    }

    public String getConfirmationMethod() throws InfoCardProcessingException {
        XPathContext context = buildSamlXPathContext();
        Nodes nodes = null;
        try {
            nodes = getDoc().query("saml:Assertion/saml:AttributeStatement/saml:Subject/saml:SubjectConfirmation/saml:ConfirmationMethod", context);
            ;
        } catch (InfoCardProcessingException e) {
            return e.getMessage();
        }
        if (nodes.size() == 1) {
            Element element = (Element) nodes.get(0);
            return element.getValue();
        } else {
            return "Expected one saml:ConfirmationMethod, but found " + nodes.size();
        }
    }

    private void validateConditions() throws InfoCardProcessingException {
        Calendar now = XSDDateTime.parse(new XSDDateTime().getDateTime());
        parseConditions();
        conditionsValid = conditions.validate(now);
        haveValidatedConditions = true;
    }

    public String getClientDigest() throws InfoCardProcessingException, CryptoException {
        BigInteger modulus = ValidatingBaseEnvelopedSignature.validate(getDoc());
        String sha1 = CryptoUtils.digest(modulus.toByteArray(), "SHA");
        return sha1;
    }

    public String getModulusOrNull() throws InfoCardProcessingException {
        return ValidatingBaseEnvelopedSignature.getModulusOrNull(getDoc());
    }

    public X509Certificate getCertificateOrNull() throws InfoCardProcessingException {
        XPathContext thisContext = new XPathContext();
        thisContext.addNamespace("dsig", WSConstants.DSIG_NAMESPACE);
        Nodes nodes = getDoc().query("//dsig:X509Data/dsig:X509Certificate", thisContext);
        if ((nodes != null) && (nodes.size() > 0)) {
            String b64EncodedX509Certificate = nodes.get(0).getValue();
            try {
                return CryptoUtils.X509fromB64(b64EncodedX509Certificate);
            } catch (CryptoException e) {
                throw new InfoCardProcessingException(e);
            }
        }
        return null;
    }

    private void parseCertificate() throws InfoCardProcessingException {
        certificate = getCertificateOrNull();
        if (certificate == null) {
        } else {
            throw new InfoCardProcessingException("No X509Certificate provided in Token");
        }
    }

    private XPathContext buildSamlXPathContext() throws InfoCardProcessingException {
        XPathContext context = new XPathContext();
        String namespace = getDoc().getRootElement().getNamespaceURI();
        if (!WSConstants.SAML11_NAMESPACE.equals(namespace) && !WSConstants.SAML20_NAMESPACE.equals(namespace)) {
            throw new InfoCardProcessingException("Unsupported token namespace: " + namespace);
        }
        context.addNamespace("saml", namespace);
        return context;
    }

    private void parseClaims() throws InfoCardProcessingException {
        claims = new HashMap();
        XPathContext context = buildSamlXPathContext();
        String attributeName;
        String namespace = getDoc().getRootElement().getNamespaceURI();
        if (WSConstants.SAML11_NAMESPACE.equals(namespace)) {
            attributeName = "AttributeName";
        } else if (WSConstants.SAML20_NAMESPACE.equals(namespace)) {
            attributeName = "Name";
        } else {
            throw new InfoCardProcessingException("Unsupported token namespace: " + namespace);
        }
        Nodes claimNodeList = getDoc().query("/saml:Assertion/saml:AttributeStatement/saml:Attribute", context);
        for (int i = 0; i < claimNodeList.size(); i++) {
            Element claim = (Element) claimNodeList.get(i);
            Attribute nameAttr = claim.getAttribute(attributeName);
            String name = nameAttr.getValue();
            Element valueElm = claim.getFirstChildElement("AttributeValue", namespace);
            String value = valueElm.getValue();
            claims.put(name, value);
        }
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }
}
