package org.xmldap.sts.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.ParsingException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.exceptions.TokenIssuanceException;
import org.xmldap.infocard.ManagedToken;
import org.xmldap.saml.Subject;
import org.xmldap.sts.db.CardStorage;
import org.xmldap.sts.db.DbSupportedClaim;
import org.xmldap.sts.db.ManagedCard;
import org.xmldap.sts.db.SupportedClaims;
import org.xmldap.util.Bag;
import org.xmldap.util.RandomGUID;
import org.xmldap.util.XSDDateTime;
import org.xmldap.ws.WSConstants;
import org.xmldap.xmldsig.AsymmetricKeyInfo;

public class Utils {

    static Logger log = Logger.getLogger("org.xmldap.sts.servlet.Utils");

    public static Bag parseRequest(Element requestXML, String requestURL) throws ParsingException {
        Bag requestElements = new Bag();
        XPathContext context = buildRSTcontext();
        Nodes cids = requestXML.query("//wsid:CardId", context);
        Element cid = (Element) cids.get(0);
        String cardIdUri = cid.getValue();
        String cardId = null;
        if (cardIdUri.startsWith(requestURL + "/card/")) {
            cardId = cardIdUri.substring(requestURL.length() + 6);
        } else {
            throw new ParsingException("Expected:" + requestURL.toString() + ", but found:" + cardIdUri);
        }
        log.finest("cardId: " + cardId);
        requestElements.put("cardId", cardId);
        Nodes cvs = requestXML.query("//wsid:CardVersion", context);
        Element cv = (Element) cvs.get(0);
        String cardVersion = cv.getValue();
        log.finest("CardVersion: " + cardVersion);
        requestElements.put("cardVersion", cardVersion);
        Nodes claims = requestXML.query("//wsid:ClaimType", context);
        for (int i = 0; i < claims.size(); i++) {
            Element claimElm = (Element) claims.get(i);
            Attribute uri = claimElm.getAttribute("Uri");
            String claim = uri.getValue();
            log.finest("claim:" + claim);
            requestElements.put("claim", claim);
        }
        Nodes kts = requestXML.query("//wst:KeyType", context);
        if (kts != null) {
            Element kt = (Element) kts.get(0);
            String keyType = kt.getValue();
            log.finest("keyType: " + keyType);
            requestElements.put("keyType", keyType);
        }
        Nodes tts = requestXML.query("//wst:TokenType", context);
        Element tt = (Element) tts.get(0);
        String tokenType = tt.getValue();
        log.finest("tokenType: " + tokenType);
        requestElements.put("tokenType", tokenType);
        {
            Nodes nodes = requestXML.query("//PPID", context);
            if ((nodes != null) && (nodes.size() > 0)) {
                Element one = (Element) nodes.get(0);
                String ppid = one.getValue();
                log.finest("PPID:" + ppid);
                requestElements.put("PPID", ppid);
            }
        }
        {
            Nodes nodes = requestXML.query("//wsp:AppliesTo", context);
            if ((nodes != null) && (nodes.size() > 0)) {
            }
        }
        return requestElements;
    }

    public static String issue(ManagedCard card, Bag requestElements, Locale clientLocale, X509Certificate cert, RSAPrivateKey key, String issuer, SupportedClaims supportedClaimsImpl, String restrictedTo, String relyingPartyCertB64, String messageDigestAlgorithm) throws IOException, CryptoException, TokenIssuanceException {
        Element envelope = new Element(WSConstants.SOAP_PREFIX + ":Envelope", WSConstants.SOAP12_NAMESPACE);
        envelope.addNamespaceDeclaration(WSConstants.WSA_PREFIX, WSConstants.WSA_NAMESPACE_05_08);
        envelope.addNamespaceDeclaration(WSConstants.WSU_PREFIX, WSConstants.WSU_NAMESPACE);
        envelope.addNamespaceDeclaration(WSConstants.WSSE_PREFIX, WSConstants.WSSE_NAMESPACE_OASIS_10);
        envelope.addNamespaceDeclaration(WSConstants.TRUST_PREFIX, WSConstants.TRUST_NAMESPACE_05_02);
        envelope.addNamespaceDeclaration("ic", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        Element header = new Element(WSConstants.SOAP_PREFIX + ":Header", WSConstants.SOAP12_NAMESPACE);
        Element body = new Element(WSConstants.SOAP_PREFIX + ":Body", WSConstants.SOAP12_NAMESPACE);
        envelope.appendChild(header);
        envelope.appendChild(body);
        Element rstr = new Element(WSConstants.TRUST_PREFIX + ":RequestSecurityTokenResponse", WSConstants.TRUST_NAMESPACE_05_02);
        Attribute context = new Attribute("Context", "ProcessRequestSecurityToken");
        rstr.addAttribute(context);
        String tokentype = (String) requestElements.get("tokenType");
        if (tokentype == null) {
            tokentype = WSConstants.SAML11_NAMESPACE;
        }
        Element tokenTypeElt = new Element(WSConstants.TRUST_PREFIX + ":TokenType", WSConstants.TRUST_NAMESPACE_05_02);
        tokenTypeElt.appendChild(tokentype);
        rstr.appendChild(tokenTypeElt);
        log.log(java.util.logging.Level.INFO, "tokentype " + tokentype);
        Element requestType = new Element(WSConstants.TRUST_PREFIX + ":RequestType", WSConstants.TRUST_NAMESPACE_05_02);
        requestType.appendChild("http://schemas.xmlsoap.org/ws/2005/02/trust/Issue");
        rstr.appendChild(requestType);
        Element rst = new Element(WSConstants.TRUST_PREFIX + ":RequestedSecurityToken", WSConstants.TRUST_NAMESPACE_05_02);
        AsymmetricKeyInfo keyInfo = new AsymmetricKeyInfo(cert);
        String signatureAlgorithm = messageDigestAlgorithm + "with" + key.getAlgorithm();
        log.log(java.util.logging.Level.INFO, "signatureAlgorithm=" + signatureAlgorithm);
        ManagedToken token = new ManagedToken(keyInfo, key, tokentype, signatureAlgorithm);
        if (restrictedTo != null) {
            token.setRestrictedTo(restrictedTo, relyingPartyCertB64);
        }
        Set<String> cardClaims = card.getClaims();
        for (String claim : cardClaims) {
            int qm = claim.indexOf('?');
            if (qm > 0) {
                System.out.println("found dynamic claim " + claim + "\n");
                List requestedClaims = requestElements.getValues("claim");
                Iterator iter = requestedClaims.iterator();
                while (iter.hasNext()) {
                    String requestedXClaim = (String) iter.next();
                    String requestedClaim = requestedXClaim.replace("%3F", "?");
                    if (requestedClaim.startsWith(claim)) {
                        System.out.println("requestedClaim " + requestedClaim + " starts with " + claim);
                        token.setClaim(claim, requestedClaim.substring(qm));
                    } else {
                        System.out.println("requestedClaim " + requestedClaim + " does not starts with " + claim);
                    }
                }
            } else {
                List requestedClaims = requestElements.getValues("claim");
                Iterator iter = requestedClaims.iterator();
                while (iter.hasNext()) {
                    String requestedClaim = (String) iter.next();
                    if (claim.equals(requestedClaim)) {
                        String value = card.getClaim(claim);
                        if ((value != null) && !("".equals(value))) {
                            token.setClaim(claim, value);
                            System.out.println("found static claim " + claim + "\n");
                        }
                    }
                }
            }
        }
        String keyType = null;
        {
            List keyTypes = requestElements.getValues("keyType");
            if ((keyTypes != null) && (keyTypes.size() > 0)) {
                keyType = (String) keyTypes.get(0);
            }
        }
        if (keyType != null) {
            log.log(java.util.logging.Level.INFO, "keyType " + keyType);
            if ("http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey".equals(keyType)) {
                token.setConfirmationMethod(Subject.BEARER);
            } else if ("http://schemas.xmlsoap.org/ws/2005/02/trust/SymmetricKey".equals(keyType)) {
                log.log(java.util.logging.Level.SEVERE, "keyType " + keyType + "is not supported");
                throw new TokenIssuanceException("keyType " + keyType + "is not supported");
            } else if ("http://schemas.xmlsoap.org/ws/2005/02/trust/PublicKey".equals(keyType)) {
                log.log(java.util.logging.Level.SEVERE, "keyType " + keyType + "is not supported");
                throw new TokenIssuanceException("keyType " + keyType + "is not supported");
            } else {
            }
        }
        String ppid = null;
        {
            List ppids = requestElements.getValues("PPID");
            if ((ppids != null) && (ppids.size() > 0)) {
                ppid = (String) ppids.get(0);
            }
        }
        if (ppid != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String cardPPID = card.getPrivatePersonalIdentifier();
            baos.write(cardPPID.getBytes());
            baos.write(ppid.getBytes());
            String digest = CryptoUtils.digest(baos.toByteArray(), "SHA");
            token.setPrivatePersonalIdentifier(digest);
        } else {
            String cardPPID = card.getPrivatePersonalIdentifier();
            if (relyingPartyCertB64 != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(cardPPID.getBytes());
                baos.write(relyingPartyCertB64.getBytes());
                String digest = CryptoUtils.digest(baos.toByteArray(), "SHA");
                token.setPrivatePersonalIdentifier(digest);
            } else if (restrictedTo != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(restrictedTo.getBytes());
                baos.write(cardPPID.getBytes());
                String digest = CryptoUtils.digest(baos.toByteArray(), "SHA");
                token.setPrivatePersonalIdentifier(digest);
            } else {
                token.setPrivatePersonalIdentifier(cardPPID);
            }
        }
        token.setValidityPeriod(-3, 10);
        token.setIssuer(issuer);
        RandomGUID uuid = new RandomGUID();
        try {
            rst.appendChild(token.serialize(uuid));
        } catch (SerializationException e) {
            e.printStackTrace();
        }
        rstr.appendChild(rst);
        Element requestedAttachedReference = new Element(WSConstants.TRUST_PREFIX + ":RequestedAttachedReference", WSConstants.TRUST_NAMESPACE_05_02);
        Element securityTokenReference = new Element(WSConstants.WSSE_PREFIX + ":SecurityTokenReference", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Element keyIdentifier = new Element(WSConstants.WSSE_PREFIX + ":KeyIdentifier", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Attribute valueType = new Attribute("ValueType", "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID");
        keyIdentifier.addAttribute(valueType);
        keyIdentifier.appendChild("uuid-" + uuid.toString());
        securityTokenReference.appendChild(keyIdentifier);
        requestedAttachedReference.appendChild(securityTokenReference);
        rstr.appendChild(requestedAttachedReference);
        Element requestedUnAttachedReference = new Element(WSConstants.TRUST_PREFIX + ":RequestedUnattachedReference", WSConstants.TRUST_NAMESPACE_05_02);
        Element securityTokenReference1 = new Element(WSConstants.WSSE_PREFIX + ":SecurityTokenReference", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Element keyIdentifier1 = new Element(WSConstants.WSSE_PREFIX + ":KeyIdentifier", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Attribute valueType1 = new Attribute("ValueType", "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID");
        keyIdentifier1.addAttribute(valueType1);
        keyIdentifier1.appendChild("uuid-" + uuid.toString());
        securityTokenReference1.appendChild(keyIdentifier1);
        requestedUnAttachedReference.appendChild(securityTokenReference1);
        rstr.appendChild(requestedUnAttachedReference);
        Element requestedDisplayToken = new Element(WSConstants.INFOCARD_PREFIX + ":RequestedDisplayToken", WSConstants.INFOCARD_NAMESPACE);
        Element displayToken = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayToken", WSConstants.INFOCARD_NAMESPACE);
        {
            String languageIso639;
            if (clientLocale != null) {
                String uri = cardClaims.iterator().next();
                DbSupportedClaim dbClaim = supportedClaimsImpl.getClaimByUri(uri);
                Locale locale = dbClaim.getLocale(clientLocale);
                languageIso639 = locale.getLanguage();
            } else {
                languageIso639 = "en";
            }
            Attribute lang = new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", languageIso639);
            displayToken.addAttribute(lang);
        }
        requestedDisplayToken.appendChild(displayToken);
        for (String uri : cardClaims) {
            String value = card.getClaim(uri);
            DbSupportedClaim dbClaim = supportedClaimsImpl.getClaimByUri(uri);
            String displayTag = dbClaim.getDisplayTag(clientLocale);
            addDisplayClaim(displayToken, uri, displayTag, value);
        }
        addDisplayClaim(displayToken, org.xmldap.infocard.Constants.IC_NAMESPACE, "PPID", card.getPrivatePersonalIdentifier());
        rstr.appendChild(requestedDisplayToken);
        body.appendChild(rstr);
        return envelope.toXML();
    }

    public static void addDisplayClaim(Element displayToken, String claimUri, String claimName, String claimValue) {
        Element displayClaim = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayClaim", WSConstants.INFOCARD_NAMESPACE);
        Attribute uri = new Attribute("Uri", claimUri);
        displayClaim.addAttribute(uri);
        Element displayTag = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayTag", WSConstants.INFOCARD_NAMESPACE);
        displayTag.appendChild(claimName);
        Element displayValue = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayValue", WSConstants.INFOCARD_NAMESPACE);
        displayValue.appendChild(claimValue);
        displayClaim.appendChild(displayTag);
        displayClaim.appendChild(displayValue);
        displayToken.appendChild(displayClaim);
    }

    /**
	 * @return an XPathContext suitable to parse a RST 
	 */
    static XPathContext buildRSTcontext() {
        XPathContext context = new XPathContext();
        context.addNamespace("s", WSConstants.SOAP12_NAMESPACE);
        context.addNamespace("a", WSConstants.WSA_NAMESPACE_05_08);
        context.addNamespace("wst", "http://schemas.xmlsoap.org/ws/2005/02/trust");
        context.addNamespace("wsid", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        context.addNamespace(WSConstants.WSSE_PREFIX, WSConstants.WSSE_NAMESPACE_OASIS_10);
        context.addNamespace(WSConstants.WSU_PREFIX, WSConstants.WSU_NAMESPACE);
        context.addNamespace(WSConstants.POLICY_PREFIX, WSConstants.POLICY_NAMESPACE_04_09);
        return context;
    }

    private static Bag parseToken(Element tokenXML, XPathContext context) throws ParsingException {
        Bag tokenElements = new Bag();
        Nodes uns = tokenXML.query("//" + WSConstants.WSSE_PREFIX + ":Username", context);
        Element un = (Element) uns.get(0);
        String userName = un.getValue();
        log.finest("username: " + userName);
        tokenElements.put("username", userName);
        Nodes pws = tokenXML.query("//" + WSConstants.WSSE_PREFIX + ":Password", context);
        Element pw = (Element) pws.get(0);
        String password = pw.getValue();
        tokenElements.put("password", password);
        return tokenElements;
    }

    protected static boolean authenticate(CardStorage storage, String username, String password) {
        boolean isUser = storage.authenticate(username, password);
        System.out.println("STS Authenticated: " + username + ":" + isUser);
        return isUser;
    }

    private static boolean authenticate(CardStorage storage, Bag tokenElements) {
        String username = (String) tokenElements.get("username");
        String password = (String) tokenElements.get("password");
        return authenticate(storage, username, password);
    }

    /**
	 * @param req
	 * @return
     * @throws org.xmldap.exceptions.ParsingException 
	 */
    static boolean authenticate(CardStorage storage, Document req, XPathContext context) throws org.xmldap.exceptions.ParsingException {
        Bag tokenElements = null;
        Nodes tokenElm = req.query("//" + WSConstants.WSSE_PREFIX + ":UsernameToken", context);
        if (tokenElm.size() > 0) {
            Element token = (Element) tokenElm.get(0);
            log.finest("UsernameToken:" + token.toXML());
            try {
                tokenElements = parseToken(token, context);
            } catch (ParsingException e) {
                e.printStackTrace();
                throw new org.xmldap.exceptions.ParsingException("expected //o:UsernameToken");
            }
        }
        return authenticate(storage, tokenElements);
    }

    public static Bag parseAppliesTo(Document req, XPathContext context) throws ParsingException {
        Bag appliesToBag = new Bag();
        Nodes rsts = req.query("//" + WSConstants.POLICY_PREFIX + ":AppliesTo", context);
        if (rsts.size() > 0) {
            Element appliesTo = (Element) rsts.get(0);
            Element endpointReference = appliesTo.getFirstChildElement("EndpointReference", WSConstants.WSA_NAMESPACE_05_08);
            if (endpointReference != null) {
                Element elt = endpointReference.getFirstChildElement("Address", WSConstants.WSA_NAMESPACE_05_08);
                if (elt != null) {
                    String relyingPartyURL = elt.getValue();
                    appliesToBag.put("relyingPartyURL", relyingPartyURL);
                } else {
                    log.fine("found AppliesTo, but no Address");
                    throw new ParsingException("found AppliesTo/EndpointReference, but no Address");
                }
                elt = endpointReference.getFirstChildElement("Identity", WSConstants.WSA_ID_06_02);
                if (elt != null) {
                    elt = elt.getFirstChildElement("KeyInfo", WSConstants.DSIG_NAMESPACE);
                    if (elt != null) {
                        elt = elt.getFirstChildElement("X509Data", WSConstants.DSIG_NAMESPACE);
                        if (elt != null) {
                            elt = elt.getFirstChildElement("X509Certificate", WSConstants.DSIG_NAMESPACE);
                            if (elt != null) {
                                String relyingPartyCertB64 = elt.getValue();
                                appliesToBag.put("relyingPartyCertB64", relyingPartyCertB64);
                            } else {
                                log.fine("found AppliesTo/Identity/KeyInfo/X509Data, but no X509Certificate");
                                throw new ParsingException("found AppliesTo/Identity/KeyInfo/X509Data, but no X509Certificate");
                            }
                        } else {
                            log.fine("found AppliesTo/Identity/KeyInfo, but no X509Data");
                            throw new ParsingException("found AppliesTo/Identity/KeyInfo, but no X509Data");
                        }
                    } else {
                        log.fine("found AppliesTo/Identity, but no KeyInfo");
                        throw new ParsingException("found AppliesTo/Identity, but no KeyInfo");
                    }
                } else {
                    log.fine("found AppliesTo, but no Identity");
                    throw new ParsingException("found AppliesTo, but no Identity");
                }
            } else {
                log.fine("found AppliesTo, but no EndpointReference");
                throw new ParsingException("found AppliesTo, but no EndpointReference");
            }
        }
        return appliesToBag;
    }

    public static void soapFault(HttpServletRequest request, HttpServletResponse response, String faultCode, String subcode, String reasonText) throws IOException {
        Element envelope = new Element(WSConstants.SOAP_PREFIX + ":Envelope", WSConstants.SOAP12_NAMESPACE);
        envelope.addNamespaceDeclaration(WSConstants.WSA_PREFIX, WSConstants.WSA_NAMESPACE_05_08);
        envelope.addNamespaceDeclaration(WSConstants.WSU_PREFIX, WSConstants.WSU_NAMESPACE);
        envelope.addNamespaceDeclaration(WSConstants.WSSE_PREFIX, WSConstants.WSSE_NAMESPACE_OASIS_10);
        Element header = new Element(WSConstants.SOAP_PREFIX + ":Header", WSConstants.SOAP12_NAMESPACE);
        {
            {
                Element actionE = new Element(WSConstants.WSA_PREFIX + ":Action", WSConstants.WSA_NAMESPACE_05_08);
                Attribute mustUnderstandA = new Attribute(WSConstants.SOAP_PREFIX + ":mustUnderstand", WSConstants.SOAP12_NAMESPACE);
                mustUnderstandA.setValue("1");
                actionE.addAttribute(mustUnderstandA);
                actionE.appendChild("http://www.w3.org/2005/08/addressing/soap/fault");
                header.appendChild(actionE);
            }
            {
                Element securityE = new Element(WSConstants.WSSE_PREFIX + ":Security", WSConstants.WSSE_NAMESPACE_OASIS_10);
                Element timestampE = new Element(WSConstants.WSU_PREFIX + ":Timestamp", WSConstants.WSU_NAMESPACE);
                Attribute uId = new Attribute(WSConstants.WSU_PREFIX + ":Id", WSConstants.WSU_NAMESPACE);
                uId.setValue("_0");
                timestampE.addAttribute(uId);
                Element createdE = new Element(WSConstants.WSU_PREFIX + ":Created", WSConstants.WSU_NAMESPACE);
                createdE.appendChild(new XSDDateTime(-1).getDateTime());
                timestampE.appendChild(createdE);
                Element expiresE = new Element(WSConstants.WSU_PREFIX + ":Expires", WSConstants.WSU_NAMESPACE);
                expiresE.appendChild(new XSDDateTime(1).getDateTime());
                timestampE.appendChild(expiresE);
                securityE.appendChild(timestampE);
                header.appendChild(securityE);
            }
        }
        Element body = new Element(WSConstants.SOAP_PREFIX + ":Body", WSConstants.SOAP12_NAMESPACE);
        envelope.appendChild(header);
        envelope.appendChild(body);
        Element faultE = new Element(WSConstants.SOAP_PREFIX + ":Fault", WSConstants.SOAP12_NAMESPACE);
        {
            {
                Element codeE = new Element(WSConstants.SOAP_PREFIX + ":Code", WSConstants.SOAP12_NAMESPACE);
                codeE.appendChild(faultCode);
                Element subcodeE = new Element(WSConstants.SOAP_PREFIX + ":Subcode", WSConstants.SOAP12_NAMESPACE);
                subcodeE.appendChild(subcode);
                codeE.appendChild(subcodeE);
                faultE.appendChild(codeE);
            }
            {
                Element reasonE = new Element(WSConstants.SOAP_PREFIX + ":Reason", WSConstants.SOAP12_NAMESPACE);
                Element reasonTextE = new Element(WSConstants.SOAP_PREFIX + ":Text", WSConstants.SOAP12_NAMESPACE);
                reasonTextE.appendChild(reasonText);
                reasonE.appendChild(reasonTextE);
                faultE.appendChild(reasonE);
            }
        }
        body.appendChild(faultE);
        request.setCharacterEncoding("utf-8");
        PrintWriter out = response.getWriter();
        response.setContentType("application/soap+xml");
        out.print(envelope.toXML());
        out.flush();
        out.close();
    }
}
