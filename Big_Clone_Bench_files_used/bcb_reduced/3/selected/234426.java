package org.apache.ws.security.message;

import java.security.MessageDigest;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import javax.crypto.SecretKey;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.axis.encoding.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSDocInfoStore;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.w3c.dom.Document;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.util.KerberosSession;
import org.apache.ws.security.util.KerberosSessionCache;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.algorithms.SignatureAlgorithm;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.params.InclusiveNamespaces;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.XMLUtils;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.w3c.dom.Element;

/**
 * WsSecKerberosToken is responsible for creating WS-Security kerberos token
 * elements and corresponding signatures for outgoing messages.
 * 
 * @author Anthony Bull (antsbull@gmail.com)
 */
public class WSSecKerberosToken extends WSSecSignature {

    private static Log log = LogFactory.getLog(WSSecKerberosToken.class.getName());

    protected String tokenUri = null;

    protected Subject subject = null;

    private CredentialsCallbackHandler credHandler;

    private String servicePrincipalName;

    protected WSSecHeader wsSecHeader = null;

    public WSSecKerberosToken() {
        super();
    }

    /**
     * Set the service principal name for the service that the outgoing request
     * is being sent to. This is used to request a service-ticket from the KDC.
     * @param servicePrincipalName the service principal name of the target
     * service
     */
    public void setServicePrincipalName(String servicePrincipalName) {
        this.servicePrincipalName = servicePrincipalName;
    }

    /**
     * Builds a signed soap envelope.
     * 
     * @param doc The unsigned SOAP envelope as <code>Document</code>
     * @param secHeader the security header element to build the kerberos token
     * into
     * @return A signed SOAP envelope as <code>Document</code>
     * @throws WSSecurityException on any errors or exceptions that occur while
     * building the token, including errors reported by the KDC
     */
    public Document build(Document doc, WSSecHeader secHeader) throws WSSecurityException {
        doDebug = log.isDebugEnabled();
        if (doDebug) {
            log.debug("Beginning kerberos token processing...");
        }
        credHandler = new CredentialsCallbackHandler(user, password);
        this.document = doc;
        this.wsSecHeader = secHeader;
        prepare(secHeader);
        if (bstToken != null) {
            prependBSTElementToHeader(secHeader);
        }
        return document;
    }

    private KerberosTicket getTicketGrantingTicket() throws LoginException {
        LoginContext loginContext = new LoginContext("ClientConfiguration", credHandler);
        loginContext.login();
        subject = loginContext.getSubject();
        Principal principal = (Principal) subject.getPrincipals().iterator().next();
        KerberosTicket ticket = (KerberosTicket) subject.getPrivateCredentials(KerberosTicket.class).iterator().next();
        return ticket;
    }

    private byte[] getServiceTicketData(final String servicePrincipalName) throws GSSException {
        GSSManager manager = GSSManager.getInstance();
        final Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");
        GSSName serverName = manager.createName(servicePrincipalName, GSSName.NT_HOSTBASED_SERVICE);
        final GSSContext context = manager.createContext(serverName, krb5Oid, null, GSSContext.DEFAULT_LIFETIME);
        byte[] serviceTicket = (byte[]) Subject.doAs(subject, new PrivilegedAction() {

            public Object run() {
                try {
                    byte[] token = new byte[0];
                    context.requestMutualAuth(false);
                    return context.initSecContext(token, 0, token.length);
                } catch (GSSException e) {
                    log.error("Error retrieving GSS service ticket for " + servicePrincipalName, e);
                }
                return null;
            }
        });
        return serviceTicket;
    }

    private SecretKey getSessionKey(KerberosTicket tgt) throws WSSecurityException {
        Iterator creds = subject.getPrivateCredentials().iterator();
        while (creds.hasNext()) {
            Object cred = creds.next();
            if (cred instanceof KerberosTicket && !cred.equals(tgt)) {
                KerberosTicket ticket = (KerberosTicket) cred;
                return ticket.getSessionKey();
            }
        }
        throw new WSSecurityException("Could not find service ticket with server principal name " + servicePrincipalName);
    }

    private SecretKey sessionKey = null;

    private void prepare(WSSecHeader secHeader) throws WSSecurityException {
        KerberosSession krbSession = KerberosSessionCache.getInstance().getCurrentSession();
        if (krbSession == null) {
            krbSession = KerberosSessionCache.getInstance().getSession(user, servicePrincipalName);
        }
        KerberosSessionCache.getInstance().removeCurrentSession();
        secRef = new SecurityTokenReference(document);
        strUri = "STRId-" + secRef.hashCode();
        secRef.setID(strUri);
        byte[] tokenData = null;
        if (krbSession == null) {
            try {
                KerberosTicket tgt = getTicketGrantingTicket();
                tokenData = getServiceTicketData(servicePrincipalName);
                sessionKey = getSessionKey(tgt);
                MessageDigest digest = MessageDigest.getInstance("SHA");
                digest.update(tokenData);
                byte[] thumbPrintBytes = digest.digest();
                krbSession = new KerberosSession(null, Base64.encode(thumbPrintBytes), sessionKey);
                krbSession.setClientPrincipalName(user);
                krbSession.setServerPrincipalName(servicePrincipalName);
                KerberosSessionCache.getInstance().addSession(krbSession);
            } catch (LoginException e) {
                throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, "kerberosLoginFailed", new Object[] { e.getMessage() });
            } catch (GSSException e) {
                throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, "kerberosSTReqFailed", new Object[] { servicePrincipalName, e.getMessage() });
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, "kerberosSTReqFailed", new Object[] { servicePrincipalName, e.getMessage() });
            }
            if (tokenData == null) {
                throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION, "kerberosSTReqFailed", new Object[] { servicePrincipalName, "Check service principal exists in KDC" });
            }
            tokenUri = "KerbTokenId-" + tokenData.hashCode();
        } else {
            keyIdentifierType = WSConstants.THUMBPRINT_IDENTIFIER;
        }
        wsDocInfo = new WSDocInfo(document.hashCode());
        switch(keyIdentifierType) {
            case WSConstants.BST_DIRECT_REFERENCE:
                Reference ref = new Reference(document);
                ref.setURI("#" + tokenUri);
                bstToken = new KerberosSecurity(document);
                ((KerberosSecurity) bstToken).setKerberosToken(tokenData);
                ref.setValueType(bstToken.getValueType());
                secRef.setReference(ref);
                bstToken.setID(tokenUri);
                wsDocInfo.setBst(bstToken.getElement());
                break;
            case WSConstants.THUMBPRINT_IDENTIFIER:
                secRef.setKerberosIdentifierThumb(krbSession);
                sessionKey = krbSession.getSessionKey();
                break;
            default:
                throw new WSSecurityException(WSSecurityException.FAILURE, "unsupportedKeyId");
        }
    }

    public void signMessage() throws WSSecurityException {
        if (sigAlgo == null) {
            sigAlgo = XMLSignature.ALGO_ID_MAC_HMAC_SHA1;
        }
        if (canonAlgo.equals(WSConstants.C14N_EXCL_OMIT_COMMENTS)) {
            Element canonElem = XMLUtils.createElementInSignatureSpace(document, Constants._TAG_CANONICALIZATIONMETHOD);
            canonElem.setAttributeNS(null, Constants._ATT_ALGORITHM, canonAlgo);
            if (wssConfig.isWsiBSPCompliant()) {
                Set prefixes = getInclusivePrefixes(wsSecHeader.getSecurityHeader(), false);
                InclusiveNamespaces inclusiveNamespaces = new InclusiveNamespaces(document, prefixes);
                canonElem.appendChild(inclusiveNamespaces.getElement());
            }
            try {
                SignatureAlgorithm signatureAlgorithm = new SignatureAlgorithm(document, sigAlgo);
                sig = new XMLSignature(document, null, signatureAlgorithm.getElement(), canonElem);
            } catch (XMLSecurityException e) {
                log.error("", e);
                throw new WSSecurityException(WSSecurityException.FAILED_SIGNATURE, "noXMLSig");
            }
        } else {
            try {
                sig = new XMLSignature(document, null, sigAlgo, canonAlgo);
            } catch (XMLSecurityException e) {
                log.error("", e);
                throw new WSSecurityException(WSSecurityException.FAILED_SIGNATURE, "noXMLSig");
            }
        }
        sig.addResourceResolver(EnvelopeIdResolver.getInstance());
        String sigUri = "Signature-" + sig.hashCode();
        sig.setId(sigUri);
        keyInfo = sig.getKeyInfo();
        keyInfoUri = "KeyId-" + keyInfo.hashCode();
        keyInfo.setId(keyInfoUri);
        keyInfo.addUnknownElement(secRef.getElement());
        SOAPConstants soapConstants = WSSecurityUtil.getSOAPConstants(document.getDocumentElement());
        if (parts == null) {
            parts = new Vector();
            WSEncryptionPart encP = new WSEncryptionPart(soapConstants.getBodyQName().getLocalPart(), soapConstants.getEnvelopeURI(), "Content");
            parts.add(encP);
        }
        addReferencesToSign(parts, wsSecHeader);
        prependToHeader(wsSecHeader);
        computeSignature();
    }

    /**
     * Compute the Signature over the references.
     * 
     * After references are set this method computes the Signature for them.
     * This method can be called any time after the references were set. See
     * <code>addReferencesToSign()</code>.
     * 
     * @throws WSSecurityException
     */
    public void computeSignature() throws WSSecurityException {
        WSDocInfoStore.store(wsDocInfo);
        try {
            sig.sign(sessionKey);
            org.apache.ws.security.util.XMLUtils.ElementToStream(sig.getElement(), System.out);
            signatureValue = sig.getSignatureValue();
        } catch (XMLSignatureException e1) {
            throw new WSSecurityException(WSSecurityException.FAILED_SIGNATURE, null, null, e1);
        } catch (Exception e1) {
            throw new WSSecurityException(WSSecurityException.FAILED_SIGNATURE, null, null, e1);
        } finally {
            WSDocInfoStore.delete(wsDocInfo);
        }
    }

    /**
    * Prepend the BinarySecurityToken to the elements already in the Security
    * header.
    * 
    * The method can be called any time after <code>prepare()</code>.
    * This allows to insert the BST element at any position in the Security
    * header.
    * 
    * @param secHeader
    *            The security header that holds the BST element.
    */
    public void prependBSTElementToHeader(WSSecHeader secHeader) {
        if (bstToken != null) {
            WSSecurityUtil.prependChildElement(document, secHeader.getSecurityHeader(), bstToken.getElement(), false);
        }
    }
}
