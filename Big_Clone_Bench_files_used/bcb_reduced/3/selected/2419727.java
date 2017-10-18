package org.xmldap.firefox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmldap.asn1.Logotype;
import org.xmldap.asn1.LogotypeDetails;
import org.xmldap.asn1.LogotypeInfo;
import org.xmldap.exceptions.KeyStoreException;
import org.xmldap.exceptions.ParsingException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.exceptions.SigningException;
import org.xmldap.exceptions.TokenIssuanceException;
import org.xmldap.infocard.InfoCard;
import org.xmldap.infocard.SelfIssuedToken;
import org.xmldap.infocard.roaming.InformationCardMetaData;
import org.xmldap.infocard.roaming.InformationCardPrivateData;
import org.xmldap.infocard.roaming.ManagedInformationCardPrivateData;
import org.xmldap.infocard.roaming.RoamingInformationCard;
import org.xmldap.infocard.roaming.RoamingStore;
import org.xmldap.infocard.roaming.SelfIssuedInformationCardPrivateData;
import org.xmldap.saml.Subject;
import org.xmldap.util.Base64;
import org.xmldap.util.CertsAndKeys;
import org.xmldap.util.KeystoreUtil;
import org.xmldap.util.XSDDateTime;
import org.xmldap.ws.WSConstants;
import org.xmldap.xml.XmlUtils;
import org.xmldap.xmldsig.Jsr105Signatur;
import org.xmldap.xmlenc.EncryptedData;

public class TokenIssuer {

    static final String defaultSubjectConfirmationMethod = Subject.BEARER;

    static final String storePassword = "storepassword";

    static final String keyPassword = "keypassword";

    static final String nickname = "firefox";

    String keystorePath = null;

    X509Certificate signingCert = null;

    PrivateKey signingKey = null;

    private String initExtensionPath(String path) throws TokenIssuanceException {
        try {
            path = URLDecoder.decode(path.substring(7, path.length()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
        return path + "components/lib/firefox.jks";
    }

    private String initProfilePath(String path) throws TokenIssuanceException {
        return path + "firefox.jks";
    }

    private void storeCardCertKeystore(String cardCertNickname, X509Certificate cardCert, boolean overwrite) throws TokenIssuanceException {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(new File(keystorePath)), storePassword.toCharArray());
            if (ks.containsAlias(cardCertNickname)) {
                if (!overwrite) {
                    return;
                }
                ks.deleteEntry(cardCertNickname);
            }
            Certificate[] chain = { cardCert };
            ks.setKeyEntry(cardCertNickname, signingKey, keyPassword.toCharArray(), chain);
            FileOutputStream fos = new java.io.FileOutputStream(new File(keystorePath));
            ks.store(fos, storePassword.toCharArray());
            fos.close();
        } catch (java.security.KeyStoreException e) {
            throw new TokenIssuanceException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenIssuanceException(e);
        } catch (CertificateException e) {
            throw new TokenIssuanceException(e);
        } catch (IOException e) {
            throw new TokenIssuanceException(e);
        }
    }

    /**
   * Store the signingCert and the signingKey into firefox.jks This is called
   * only once Create firefox.jks
   * 
   * @param keystorePath
   * @throws TokenIssuanceException
   */
    private void storeCertKey(String keystorePath) throws TokenIssuanceException {
        try {
            KeystoreUtil keystore = new KeystoreUtil(keystorePath, storePassword);
            keystore.storeCert(nickname, signingCert, signingKey, keyPassword);
        } catch (KeyStoreException e) {
            throw new TokenIssuanceException(e);
        }
    }

    private void initCertKey(String keystorePath) throws TokenIssuanceException {
        KeystoreUtil keystore = null;
        try {
            keystore = new KeystoreUtil(keystorePath, storePassword);
            signingCert = keystore.getCertificate(nickname);
            if (signingCert == null) {
                throw new TokenIssuanceException("cert (" + nickname + ") not found");
            }
            signingKey = keystore.getPrivateKey(nickname, keyPassword);
            if (signingKey == null) {
                throw new TokenIssuanceException("private key (" + nickname + ") not found");
            }
        } catch (KeyStoreException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public TokenIssuer(String path) throws TokenIssuanceException {
        keystorePath = initExtensionPath(path);
        try {
            initCertKey(keystorePath);
        } catch (TokenIssuanceException e) {
            KeyPair kp = null;
            try {
                kp = CertsAndKeys.generateKeyPair();
                Provider provider = new BouncyCastleProvider();
                if (null == Security.getProvider(provider.getName())) {
                    Security.addProvider(provider);
                }
                signingCert = CertsAndKeys.generateCaCertificate(provider, nickname, kp);
                signingKey = kp.getPrivate();
                storeCertKey(keystorePath);
            } catch (NoSuchAlgorithmException e1) {
                throw new TokenIssuanceException(e1);
            } catch (NoSuchProviderException e1) {
                throw new TokenIssuanceException(e1);
            } catch (InvalidKeyException e1) {
                throw new TokenIssuanceException(e1);
            } catch (SecurityException e1) {
                throw new TokenIssuanceException(e1);
            } catch (SignatureException e1) {
                throw new TokenIssuanceException(e1);
            } catch (CertificateParsingException e1) {
                throw new TokenIssuanceException(e1);
            } catch (IOException e1) {
                throw new TokenIssuanceException(e1);
            } catch (CertificateEncodingException e1) {
                throw new TokenIssuanceException(e1);
            } catch (IllegalStateException e1) {
                throw new TokenIssuanceException(e1);
            } catch (CertificateException e1) {
                throw new TokenIssuanceException(e1);
            }
        }
    }

    public String init(String path) {
        return "TokenIssuer initialized";
    }

    public static boolean isExtendedEvaluationCert(X509Certificate relyingpartyCert) {
        return false;
    }

    public static byte[] rpIdentifier(X509Certificate relyingpartyCert, X509Certificate[] chain) throws TokenIssuanceException {
        if (isExtendedEvaluationCert(relyingpartyCert)) {
            return rpIdentifierEV(relyingpartyCert);
        } else {
            return rpIdentifierNonEV(relyingpartyCert, chain);
        }
    }

    static String orgIdString(X509Certificate relyingpartyCert) throws TokenIssuanceException {
        X500Principal principal = relyingpartyCert.getSubjectX500Principal();
        String dn = principal.getName();
        if (dn == null) {
            PublicKey publicKey = relyingpartyCert.getPublicKey();
            return new String(publicKey.getEncoded());
        }
        X509Name x509Name = new X509Name(dn);
        Vector<DERObjectIdentifier> oids = x509Name.getOIDs();
        Vector values = x509Name.getValues();
        int index = 0;
        String O = "O=\"\"|";
        String L = "L=\"\"|";
        String S = "S=\"\"|";
        String C = "C=\"\"|";
        for (DERObjectIdentifier oid : oids) {
            String id = oid.getId();
            if ("2.5.4.10".equals(id)) {
                String value = (String) values.get(index);
                if (value != null) {
                    O = "O=\"" + value + "\"|";
                }
            } else if ("2.5.4.7".equals(id)) {
                String value = (String) values.get(index);
                if (value != null) {
                    L = "L=\"" + value + "\"|";
                }
            } else if ("2.5.4.8".equals(id)) {
                String value = (String) values.get(index);
                if (value != null) {
                    S = "S=\"" + value + "\"|";
                }
            } else if ("2.5.4.6".equals(id)) {
                String value = (String) values.get(index);
                if (value != null) {
                    C = "C=\"" + value + "\"|";
                }
            } else {
                System.out.println("unused oid (" + oid + "). Value=" + (String) values.get(index));
            }
            index += 1;
        }
        String orgId = "|" + O + L + S + C;
        if ("|O=\"\"|L=\"\"|S=\"\"|C=\"\"|".equals(orgId)) {
            PublicKey publicKey = relyingpartyCert.getPublicKey();
            String publicKeyB64 = Base64.encodeBytesNoBreaks(publicKey.getEncoded());
            return publicKeyB64;
        }
        try {
            return Base64.encodeBytesNoBreaks(orgId.getBytes("UTF-16LE"));
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public static byte[] rpIdentifierNonEV(X509Certificate relyingpartyCert, X509Certificate[] chain) throws TokenIssuanceException {
        String orgIdString = orgIdString(relyingpartyCert);
        String qualifiedOrgIdString = qualifiedOrgIdString(chain, orgIdString);
        try {
            byte[] qualifiedOrgIdBytes = qualifiedOrgIdString.getBytes("UTF-8");
            byte[] rpIdentifier = sha256(qualifiedOrgIdBytes);
            return rpIdentifier;
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
    }

    /**
   * @param chain
   * @param orgIdString
   */
    public static String qualifiedOrgIdString(X509Certificate[] chain, String orgIdString) throws TokenIssuanceException {
        if (chain == null) {
            throw new TokenIssuanceException("qualifiedOrgIdString: chain == null");
        }
        StringBuffer qualifiedOrgIdString = new StringBuffer();
        for (int i = 0; i < chain.length; i++) {
            X509Certificate parent = chain[i];
            X500Principal parentPrincipal = parent.getSubjectX500Principal();
            String subjectDN = parentPrincipal.getName(X500Principal.RFC2253);
            qualifiedOrgIdString.append("|ChainElement=\"");
            qualifiedOrgIdString.append(subjectDN);
            qualifiedOrgIdString.append("\"");
        }
        qualifiedOrgIdString.append(orgIdString);
        return qualifiedOrgIdString.toString();
    }

    public static byte[] rpIdentifierEV(X509Certificate relyingpartyCert) throws TokenIssuanceException {
        String rpIdentifier = null;
        String orgIdString = orgIdString(relyingpartyCert);
        byte[] digest;
        try {
            digest = sha256(orgIdString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
        return digest;
    }

    private static byte[] sha256(byte[] bytes) throws TokenIssuanceException {
        MessageDigest mdAlgorithm;
        try {
            mdAlgorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new TokenIssuanceException(e);
        }
        mdAlgorithm.update(bytes);
        byte[] digest = mdAlgorithm.digest();
        return digest;
    }

    static String generateRPPPID(String cardId, X509Certificate relyingPartyCert, X509Certificate[] chain, String rpName) throws TokenIssuanceException {
        System.out.println("generateRPPPID: cardId:" + cardId);
        System.out.println("generateRPPPID: relyingPartyCert:" + relyingPartyCert);
        System.out.println("generateRPPPID: chain:" + chain);
        System.out.println("generateRPPPID: rpName:" + rpName);
        byte[] rpIdentifierBytes = null;
        try {
            if (relyingPartyCert != null) {
                rpIdentifierBytes = sha256(rpIdentifier(relyingPartyCert, chain));
            } else {
                if (rpName != null) {
                    rpIdentifierBytes = sha256(rpName.getBytes("UTF-16LE"));
                } else {
                    throw new TokenIssuanceException("generateRPPPID: both relyingPartyCert and rpName are null");
                }
            }
            System.out.println("generateRPPPID: hier");
            byte[] canonicalCardIdBytes = sha256(cardId.getBytes("UTF-8"));
            byte[] bytes = new byte[rpIdentifierBytes.length + canonicalCardIdBytes.length];
            System.arraycopy(rpIdentifierBytes, 0, bytes, 0, rpIdentifierBytes.length);
            System.arraycopy(canonicalCardIdBytes, 0, bytes, rpIdentifierBytes.length, canonicalCardIdBytes.length);
            System.out.println("generateRPPPID: da");
            byte[] ppidBytes = sha256(bytes);
            return Base64.encodeBytesNoBreaks(ppidBytes);
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public String generateRPPPID(String serializedPolicy) throws TokenIssuanceException {
        JSONObject policy = null;
        String der = null;
        try {
            policy = new JSONObject(serializedPolicy);
            if (policy.has("cert")) {
                der = (String) policy.get("cert");
            }
        } catch (JSONException e) {
            throw new TokenIssuanceException(e);
        }
        X509Certificate[] chain = null;
        X509Certificate relyingPartyCert = null;
        if (der != null) {
            try {
                relyingPartyCert = org.xmldap.util.CertsAndKeys.der2cert(der);
            } catch (CertificateException e) {
                throw new TokenIssuanceException(e);
            }
            int chainLength = 0;
            try {
                Object obj = policy.get("chainLength");
                if (obj instanceof String) {
                    String chainLengthStr = (String) obj;
                    chainLength = Integer.parseInt(chainLengthStr);
                } else {
                    if (obj instanceof Integer) {
                        Integer aInt = (Integer) obj;
                        chainLength = aInt.intValue();
                    } else {
                        throw new TokenIssuanceException("unsupported type of chainLength: " + obj.getClass().getName());
                    }
                }
            } catch (JSONException e) {
                throw new TokenIssuanceException(e);
            }
            if (chainLength > 0) {
                chain = new X509Certificate[chainLength];
                for (int i = 0; i < chainLength; i++) {
                    try {
                        String chainDer = (String) policy.get("certChain" + i);
                        X509Certificate chainCert = org.xmldap.util.CertsAndKeys.der2cert(chainDer);
                        chain[i] = chainCert;
                    } catch (JSONException e) {
                        throw new TokenIssuanceException(e);
                    } catch (CertificateException e) {
                        throw new TokenIssuanceException(e);
                    }
                }
            } else {
            }
        }
        String cardId = "";
        String rpName = "";
        String ppi = TokenIssuer.generateRPPPID(cardId, relyingPartyCert, chain, rpName);
        return ppi;
    }

    /**
   * Uses the first bytes of the infoCardPpi to encrypt a String which is
   * fixed for the relying party using AES. Remember: The input parameter
   * infoCardPpi is generated in the Firefox Identity Selector as a SHA-1 hash
   * of the string "cardname + random-numer + cardversion" once the card is
   * issued. This hash is 160 bit long. For the AES encryption we need a 128
   * bit key. This function just truncates the hash and uses the result as the
   * key for the AES encryption. As long as neither the infoCardPpi and the
   * rpSignature don't change this yields the same PPI for this RP everytime.
   * The result is then SHA-1 hashed again (to get a short ppi) and Base64
   * encoded to make it printable. The schema http://xmldap.org/Infocard.xsd
   * defines it as <xs:element name="PrivatePersonalIdentifier"
   * type="tns:Base64BinaryMaxSize1K"/>.
   * 
   * @param infoCardPpi
   * @param rpSignature
   * @return the new PPI which is unique for this relying party
   * @throws TokenIssuanceException
   */
    public String generatePpiForThisRP(String infoCardPpi, String rpData) throws TokenIssuanceException {
        byte[] keyBytes = new byte[16];
        byte[] b;
        try {
            b = infoCardPpi.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
        int len = b.length;
        if (len > keyBytes.length) len = keyBytes.length;
        System.arraycopy(b, 0, keyBytes, 0, len);
        SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(rpData.getBytes("UTF-8"));
            MessageDigest mdAlgorithm = MessageDigest.getInstance("SHA-1");
            mdAlgorithm.update(encrypted);
            byte[] digest = mdAlgorithm.digest();
            return Base64.encodeBytesNoBreaks(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenIssuanceException(e);
        } catch (NoSuchPaddingException e) {
            throw new TokenIssuanceException(e);
        } catch (InvalidKeyException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalBlockSizeException e) {
            throw new TokenIssuanceException(e);
        } catch (BadPaddingException e) {
            throw new TokenIssuanceException(e);
        } catch (UnsupportedEncodingException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public String newCardStore() throws SerializationException {
        RoamingStore rs = new RoamingStore();
        String result = rs.toXML();
        System.out.println("TokenIssuer.java newCardStore =" + result);
        return result;
    }

    public String importManagedCard(Element importedCard, Document cardStore, String issuerName) throws TokenIssuanceException {
        String storeFormat = null;
        RoamingStore roamingStore = null;
        if (cardStore != null) {
            Element root = cardStore.getRootElement();
            storeFormat = root.getLocalName();
            if ("RoamingStore".equals(root.getLocalName())) {
                try {
                    roamingStore = new RoamingStore(cardStore);
                } catch (IOException e) {
                    throw new TokenIssuanceException(e);
                } catch (ParsingException e) {
                    throw new TokenIssuanceException(e);
                } catch (nu.xom.ParsingException e) {
                    throw new TokenIssuanceException(e);
                }
            } else {
                throw new IllegalArgumentException("unsupported cardstore format: " + storeFormat);
            }
        }
        try {
            InfoCard card = new InfoCard(importedCard);
            if (!card.checkValidity(null)) {
                throw new TokenIssuanceException("the imported information card is not valid");
            }
            Random random = new Random();
            byte[] bytes = new byte[256];
            random.nextBytes(bytes);
            String hashSalt = Base64.encodeBytesNoBreaks(bytes);
            String timeLastUpdated = new XSDDateTime().getDateTime();
            String issuerId = card.getIssuer();
            String backgroundColor = "16777215";
            InformationCardMetaData informationCardMetaData = new InformationCardMetaData(card, false, null, hashSalt, timeLastUpdated, issuerId, issuerName, backgroundColor);
            byte[] masterKeyBytes = new SecureRandom().generateSeed(32);
            String masterKey = Base64.encodeBytes(masterKeyBytes);
            InformationCardPrivateData informationCardPrivateData = new ManagedInformationCardPrivateData(masterKey);
            RoamingInformationCard ric = new RoamingInformationCard(informationCardMetaData, informationCardPrivateData);
            if (roamingStore == null) {
                TreeSet<RoamingInformationCard> v = new TreeSet<RoamingInformationCard>();
                v.add(ric);
                roamingStore = new RoamingStore(v);
            } else {
                roamingStore.addRoamingInformationCard(ric);
            }
            return roamingStore.toXML();
        } catch (ParsingException e) {
            throw new TokenIssuanceException(e);
        } catch (SerializationException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public String importManagedCard(String importedCardJSONStr, String cardFileJSONStr) throws TokenIssuanceException {
        JSONObject result = new JSONObject();
        JSONObject importedCard;
        try {
            importedCard = new JSONObject(importedCardJSONStr);
        } catch (JSONException e) {
            throw new TokenIssuanceException(e);
        }
        if (importedCard.has("crdFileContent")) {
            String crdFileContent;
            try {
                crdFileContent = (String) importedCard.get("crdFileContent");
            } catch (JSONException e) {
                throw new TokenIssuanceException(e);
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(crdFileContent.getBytes());
            try {
                boolean valid = Jsr105Signatur.validateSignature(inputStream);
                try {
                    result.put("result", String.valueOf(valid));
                } catch (JSONException e) {
                    throw new TokenIssuanceException(e);
                }
                return result.toString();
            } catch (SigningException e) {
                throw new TokenIssuanceException(e);
            }
        } else {
            try {
                result.put("result", "no content");
            } catch (JSONException e) {
                throw new TokenIssuanceException(e);
            }
            return result.toString();
        }
    }

    public String getToken(String serializedPolicy) throws TokenIssuanceException {
        System.out.println("TokenIssuer: getToken policy=" + serializedPolicy);
        JSONObject policy = null;
        String der = null;
        String issuedToken = "";
        String type = null;
        String audience = null;
        try {
            policy = new JSONObject(serializedPolicy);
            type = (String) policy.get("type");
            if (policy.has("cert")) {
                der = (String) policy.get("cert");
            }
            if (policy.has("url")) {
                audience = (String) policy.get("url");
            }
        } catch (JSONException e) {
            throw new TokenIssuanceException(e);
        }
        X509Certificate[] chain = null;
        X509Certificate relyingPartyCert = null;
        if (der != null) {
            try {
                relyingPartyCert = org.xmldap.util.CertsAndKeys.der2cert(der);
            } catch (CertificateException e) {
                throw new TokenIssuanceException(e);
            }
            int chainLength = 0;
            try {
                String chainLengthStr = (String) policy.get("chainLength");
                chainLength = Integer.parseInt(chainLengthStr);
            } catch (JSONException e) {
                throw new TokenIssuanceException(e);
            }
            chain = new X509Certificate[chainLength];
            for (int i = 0; i < chainLength; i++) {
                try {
                    String chainDer = (String) policy.get("certChain" + i);
                    X509Certificate chainCert = org.xmldap.util.CertsAndKeys.der2cert(chainDer);
                    chain[i] = chainCert;
                } catch (JSONException e) {
                    throw new TokenIssuanceException(e);
                } catch (CertificateException e) {
                    throw new TokenIssuanceException(e);
                }
            }
        }
        System.out.println("TokenIssuer type: " + type);
        if (type.equals("selfAsserted")) {
            issuedToken = getSelfIssuedToken(policy, audience, chain, relyingPartyCert);
        } else {
            String assertion = null;
            try {
                assertion = (String) policy.get("assertion");
            } catch (JSONException e) {
                throw new TokenIssuanceException(e);
            }
            EncryptedData encryptor = new EncryptedData(relyingPartyCert);
            try {
                encryptor.setData(assertion);
                issuedToken = encryptor.toXML();
                System.out.println("getToken: encrypted issuedToken=" + issuedToken);
            } catch (SerializationException e) {
                throw new TokenIssuanceException(e);
            }
        }
        try {
            policy.put("tokenToReturn", issuedToken);
        } catch (JSONException e) {
            throw new TokenIssuanceException(e);
        }
        return policy.toString();
    }

    private String getSelfIssuedToken(JSONObject policy, String audience, X509Certificate[] chain, X509Certificate relyingPartyCert) throws TokenIssuanceException {
        String issuedToken;
        String requiredClaims = null;
        String optionalClaims = null;
        try {
            requiredClaims = policy.getString("requiredClaims");
        } catch (JSONException e) {
        }
        try {
            optionalClaims = policy.getString("optionalClaims");
        } catch (JSONException e) {
        }
        if (requiredClaims != null) {
            requiredClaims.replaceAll("\\s+", " ");
        }
        if (optionalClaims != null) {
            optionalClaims.replaceAll("\\s+", " ");
        }
        Logger gl = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        gl.info("TokenIssuer requiredClaims: " + requiredClaims);
        gl.info("TokenIssuer optionalClaims: " + optionalClaims);
        System.out.println("TokenIssuer requiredClaims: " + requiredClaims);
        System.out.println("TokenIssuer optionalClaims: " + optionalClaims);
        String card = null;
        try {
            card = (String) policy.get("card");
        } catch (JSONException e) {
            System.out.println("TokenIssuer no card in policy");
            throw new TokenIssuanceException(e);
        }
        String confirmationMethod = defaultSubjectConfirmationMethod;
        if (policy.has("confirmationMethod")) {
            try {
                confirmationMethod = policy.getString("confirmationMethod");
            } catch (JSONException e) {
            }
        }
        Document infocard;
        try {
            infocard = XmlUtils.parse(card);
        } catch (IOException e) {
            throw new TokenIssuanceException(e);
        } catch (nu.xom.ParsingException e) {
            throw new TokenIssuanceException(e);
        }
        System.out.println("TokenIssuer generateRPPPID");
        String ppi = generateRPPPID(relyingPartyCert, chain, audience, infocard);
        try {
            policy.put("rpPPID", ppi);
        } catch (JSONException e) {
            throw new TokenIssuanceException(e);
        }
        issuedToken = getSelfAssertedToken(infocard, relyingPartyCert, chain, requiredClaims, optionalClaims, signingCert, signingKey, audience, confirmationMethod, ppi);
        System.out.println("getToken: issuedToken=" + issuedToken);
        return issuedToken;
    }

    /**
   * @param policy
   * @param issuedToken
   * @param relyingPartyCert
   * @return
   * @throws TokenIssuanceException
   */
    public static String getSelfAssertedToken(Document infocard, X509Certificate relyingPartyCert, X509Certificate[] chain, String requiredClaims, String optionalClaims, X509Certificate signingCert, PrivateKey signingKey, String audience, String confirmationMethod, String ppi) throws TokenIssuanceException {
        System.out.println("INFO: TokenIssuer Infocard:" + infocard.toXML());
        SelfIssuedInformationCardPrivateData siicpd = null;
        {
            XPathContext context = new XPathContext();
            context.addNamespace("ic", WSConstants.INFOCARD_NAMESPACE);
            Nodes nodes = infocard.query("/infocard/ic:InformationCardPrivateData", context);
            if (nodes.size() > 0) {
                Element siicpdElt = (Element) nodes.get(0);
                try {
                    System.out.println("INFO: TokenIssuer hier: " + siicpdElt.toXML());
                    siicpd = new SelfIssuedInformationCardPrivateData(siicpdElt);
                    System.out.println("INFO: TokenIssuer da");
                } catch (ParsingException e) {
                    System.out.println("INFO: TokenIssuer ParsingException:" + e);
                    throw new TokenIssuanceException(e);
                }
            } else {
                System.out.println("Exception infocard:" + infocard.toXML());
                throw new TokenIssuanceException("Error: This infocard does not have '/infocard/ic:InformationCardPrivateData'!");
            }
        }
        if (siicpd != null) {
            System.out.println("INFO: TokenIssuer SelfIssuedInformationCardPrivateData:" + siicpd.toXML());
        } else {
            System.out.println("INFO: TokenIssuer SelfIssuedInformationCardPrivateData is null");
            throw new TokenIssuanceException("TokenIssuer SelfIssuedInformationCardPrivateData is null");
        }
        System.out.println("INFO: TokenIssuer requiredClaims: " + requiredClaims);
        System.out.println("INFO: TokenIssuer optionalClaims: " + optionalClaims);
        String issuedToken = getSelfAssertedToken(relyingPartyCert, requiredClaims, optionalClaims, siicpd, ppi, signingCert, signingKey, audience, confirmationMethod);
        return issuedToken;
    }

    static String generateRPPPID(X509Certificate relyingPartyCert, X509Certificate[] chain, String audience, Document infocard) throws TokenIssuanceException {
        String cardId = "";
        Nodes cardIdNodes = infocard.query("/infocard/id");
        Element cardIdElm = (Element) cardIdNodes.get(0);
        if (cardIdElm != null) {
            cardId = cardIdElm.getValue();
        } else {
            throw new TokenIssuanceException("Error: This infocard has no id!");
        }
        System.out.println("generateRPPPID: cardId=" + cardId);
        System.out.println("generateRPPPID: relyingPartyCert=" + relyingPartyCert);
        System.out.println("generateRPPPID: chain=" + chain);
        System.out.println("generateRPPPID: audience=" + audience);
        return generateRPPPID(relyingPartyCert, chain, audience, cardId);
    }

    private static String generateRPPPID(X509Certificate relyingPartyCert, X509Certificate[] chain, String audience, String cardId) throws TokenIssuanceException {
        String ppi;
        ppi = generateRPPPID(cardId, relyingPartyCert, chain, audience);
        return ppi;
    }

    /**
   * @param relyingPartyCert
   * @param requiredClaims
   * @param optionalClaims
   * @param issuedToken
   * @param data
   * @param ppi
   * @param signingCert
   * @param signingKey
   * @param audience
   * @param confirmationMethod
   * @return
   * @throws TokenIssuanceException
   */
    public static String getSelfAssertedToken(X509Certificate relyingPartyCert, String requiredClaims, String optionalClaims, SelfIssuedInformationCardPrivateData siicpd, String ppi, X509Certificate signingCert, PrivateKey signingKey, String audience, String confirmationMethod) throws TokenIssuanceException {
        System.out.println("getSelfAssertedToken requiredClaims:" + requiredClaims);
        System.out.println("getSelfAssertedToken optionalClaims:" + optionalClaims);
        if (siicpd == null) {
            throw new TokenIssuanceException("siicpd == null");
        }
        if (signingKey == null) {
            throw new TokenIssuanceException("signingKey == null");
        }
        if (signingCert == null) {
            throw new TokenIssuanceException("signingCert == null");
        }
        RSAPublicKey cardPublicKey;
        PublicKey pk = signingCert.getPublicKey();
        if (pk instanceof RSAPublicKey) {
            cardPublicKey = (RSAPublicKey) signingCert.getPublicKey();
        } else {
            throw new TokenIssuanceException("signingCert has no RSA key but a " + pk.getAlgorithm());
        }
        PrivateKey cardPrivateKey = signingKey;
        String signingAlgorithm = "SHA1withRSA";
        SelfIssuedToken token = new SelfIssuedToken(cardPublicKey, cardPrivateKey, signingAlgorithm);
        if (confirmationMethod != null) {
            if (Subject.BEARER.equals(confirmationMethod)) {
                token.setConfirmationMethodBEARER();
            } else if (Subject.HOLDER_OF_KEY.equals(confirmationMethod)) {
                RSAPublicKey proofKey = (RSAPublicKey) signingCert.getPublicKey();
                token.setConfirmationMethodHOLDER_OF_KEY(proofKey);
            } else {
                throw new TokenIssuanceException("unsupported confirmationsmethod: " + confirmationMethod);
            }
        }
        if (audience != null) {
            token.setAudience(audience);
        }
        token.setPrivatePersonalIdentifier(Base64.encodeBytesNoBreaks(ppi.getBytes()));
        token.setValidityPeriod(-5, 10);
        if (requiredClaims == null) {
            if (optionalClaims != null) {
                token = org.xmldap.infocard.SelfIssuedToken.setTokenClaims(siicpd.getSelfIssuedClaims(), token, optionalClaims);
            }
        } else {
            token = org.xmldap.infocard.SelfIssuedToken.setTokenClaims(siicpd.getSelfIssuedClaims(), token, requiredClaims);
            if (optionalClaims != null) {
                token = org.xmldap.infocard.SelfIssuedToken.setTokenClaims(siicpd.getSelfIssuedClaims(), token, optionalClaims);
            }
        }
        String issuedToken = null;
        try {
            if (relyingPartyCert != null) {
                Element securityToken = null;
                EncryptedData encryptor = new EncryptedData(relyingPartyCert);
                securityToken = token.serialize();
                System.out.println("saml assertion:" + securityToken.toXML());
                encryptor.setData(securityToken.toXML());
                issuedToken = encryptor.toXML();
            } else {
                issuedToken = token.serialize().toXML();
                System.out.println("SAML assertion:" + issuedToken);
            }
        } catch (SerializationException e) {
            throw new TokenIssuanceException(e);
        }
        return issuedToken;
    }

    public String getIssuerLogoURL(String serializedPolicy) throws TokenIssuanceException {
        try {
            JSONObject policy = new JSONObject(serializedPolicy);
            if (!policy.has("cert")) {
                return null;
            }
            String der = (String) policy.get("cert");
            X509Certificate cert = org.xmldap.util.CertsAndKeys.der2cert(der);
            byte[] fromExtensionValue = cert.getExtensionValue(Logotype.id_pe_logotype.getId());
            if ((fromExtensionValue == null) || (fromExtensionValue.length == 0)) {
                return null;
            }
            ASN1Encodable extVal = X509ExtensionUtil.fromExtensionValue(fromExtensionValue);
            if (extVal == null) return null;
            Logotype logotype = Logotype.getInstance((ASN1Sequence) extVal);
            LogotypeInfo issuerLogo = logotype.getIssuerLogo();
            if (issuerLogo == null) return "getIssuerLogo returned null";
            LogotypeDetails logotypeDetails[] = issuerLogo.getLogotypeData().getImages();
            if (logotypeDetails.length == 0) return "getLogotypeData returned zero length array";
            String urls[] = logotypeDetails[0].getLogotypeURI();
            if (urls.length == 0) return "logotypeDetails[0].getLogotypeURI() has length 0";
            return urls[0];
        } catch (CertificateException e) {
            throw new TokenIssuanceException(e);
        } catch (IOException e) {
            throw new TokenIssuanceException(e);
        } catch (JSONException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public boolean isPhoneAvailable() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Method isPhoneAvailableMethod = cardmanagerClasz.getDeclaredMethod("isPhoneAvailable");
            Boolean phoneAvailable = (Boolean) isPhoneAvailableMethod.invoke(cardmanager, (Object[]) null);
            return phoneAvailable.booleanValue();
        } catch (Exception e) {
            System.out.println("Exception in TokenIssuer.isPhoneAvailable(): " + e);
            throw new TokenIssuanceException(e);
        }
    }

    public void endCardSelection() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Method endCardSelectionMethod = cardmanagerClasz.getDeclaredMethod("endCardSelection");
            endCardSelectionMethod.invoke(cardmanager, (Object[]) null);
        } catch (SecurityException e) {
            throw new TokenIssuanceException(e);
        } catch (NoSuchMethodException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalArgumentException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalAccessException e) {
            throw new TokenIssuanceException(e);
        } catch (InvocationTargetException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public void startCardSelection(String serializedPolicy) throws TokenIssuanceException {
        System.out.println("TokenIssuer.startCardSelection");
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            System.out.println("startCardSelection ClassNotFoundException: " + e1);
            throw new TokenIssuanceException(e1);
        }
        Object cardmanager;
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
        } catch (Exception e) {
            System.out.println("startCardSelection ClassNotFoundException: " + e);
            throw new TokenIssuanceException(e);
        }
        try {
            Hashtable<String, String> ht = new Hashtable<String, String>();
            try {
                JSONObject policy = new JSONObject(serializedPolicy);
                Iterator<String> keyIter = (Iterator<String>) policy.keys();
                while (keyIter.hasNext()) {
                    String key = (String) keyIter.next();
                    String value = null;
                    try {
                        value = policy.getString(key);
                    } catch (JSONException e) {
                    }
                    if ((value != null) && !("null".equals(value))) {
                        ht.put(key, value);
                        System.out.println("startCardSelection: key=" + key + " value=" + value);
                    }
                }
            } catch (JSONException e) {
                System.out.println("startCardSelection JSONException: " + e);
                throw new TokenIssuanceException(e);
            }
            Class<?>[] paramTypes = { Hashtable.class };
            Method beginCardSelectionMethod = cardmanagerClasz.getDeclaredMethod("beginCardSelection", paramTypes);
            Object args[] = { ht };
            beginCardSelectionMethod.invoke(cardmanager, (Object[]) args);
        } catch (SecurityException e) {
            System.out.println("startCardSelection SecurityException: " + e);
            throw new TokenIssuanceException(e);
        } catch (NoSuchMethodException e) {
            System.out.println("startCardSelection NoSuchMethodException: " + e);
            throw new TokenIssuanceException(e);
        } catch (IllegalArgumentException e) {
            System.out.println("startCardSelection IllegalArgumentException: " + e);
            throw new TokenIssuanceException(e);
        } catch (IllegalAccessException e) {
            System.out.println("startCardSelection IllegalAccessException: " + e);
            throw new TokenIssuanceException(e);
        } catch (InvocationTargetException e) {
            System.out.println("startCardSelection InvocationTargetException: " + e);
            throw new TokenIssuanceException(e);
        }
    }

    public void beginCardSelection() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Class<?>[] paramTypes = { String.class };
            Method beginCardSelectionMethod = cardmanagerClasz.getDeclaredMethod("beginCardSelection", paramTypes);
            String[] args = { "Payment" };
            beginCardSelectionMethod.invoke(cardmanager, (Object[]) args);
        } catch (SecurityException e) {
            throw new TokenIssuanceException(e);
        } catch (NoSuchMethodException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalArgumentException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalAccessException e) {
            throw new TokenIssuanceException(e);
        } catch (InvocationTargetException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public String getSelectedCard() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Method getSelectedCardMethod = cardmanagerClasz.getDeclaredMethod("getSelectedCard");
            Object cardObject = getSelectedCardMethod.invoke(cardmanager, (Object[]) null);
            if (cardObject == null) {
                return null;
            } else {
                Class<?> cardClasz;
                try {
                    cardClasz = Class.forName("de.dtag.tlabs.mwallet.card.Card");
                } catch (ClassNotFoundException e1) {
                    throw new TokenIssuanceException(e1);
                }
                Method toStringMethod = cardClasz.getDeclaredMethod("toString");
                return (String) toStringMethod.invoke(cardObject, (Object[]) null);
            }
        } catch (SecurityException e) {
            throw new TokenIssuanceException(e);
        } catch (NoSuchMethodException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalArgumentException e) {
            throw new TokenIssuanceException(e);
        } catch (IllegalAccessException e) {
            throw new TokenIssuanceException(e);
        } catch (InvocationTargetException e) {
            throw new TokenIssuanceException(e);
        }
    }

    public void phoneFini() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Method finiMethod = cardmanagerClasz.getDeclaredMethod("fini");
            finiMethod.invoke(cardmanager, (Object[]) null);
        } catch (SecurityException e) {
            throw new TokenIssuanceException("phoneFini", e);
        } catch (NoSuchMethodException e) {
            throw new TokenIssuanceException("phoneFini", e);
        } catch (IllegalArgumentException e) {
            throw new TokenIssuanceException("phoneFini", e);
        } catch (IllegalAccessException e) {
            throw new TokenIssuanceException("phoneFini", e);
        } catch (InvocationTargetException e) {
            throw new TokenIssuanceException("phoneFini", e);
        }
    }

    public Exception getWalletException() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Method getWalletExceptionMethod = cardmanagerClasz.getDeclaredMethod("getWalletException");
            return (Exception) getWalletExceptionMethod.invoke(cardmanager, (Object[]) null);
        } catch (SecurityException e) {
            throw new TokenIssuanceException("getWalletException", e);
        } catch (NoSuchMethodException e) {
            throw new TokenIssuanceException("getWalletException", e);
        } catch (IllegalArgumentException e) {
            throw new TokenIssuanceException("getWalletException", e);
        } catch (IllegalAccessException e) {
            throw new TokenIssuanceException("getWalletException", e);
        } catch (InvocationTargetException e) {
            throw new TokenIssuanceException("getWalletException", e);
        }
    }

    public void resetWalletException() throws TokenIssuanceException {
        Class<?> cardmanagerClasz;
        try {
            cardmanagerClasz = Class.forName("de.dtag.tlabs.mwallet.card.CardManager");
        } catch (ClassNotFoundException e1) {
            throw new TokenIssuanceException(e1);
        }
        try {
            Method getInstanceMethod = cardmanagerClasz.getDeclaredMethod("getInstance");
            Object cardmanager = getInstanceMethod.invoke(null, (Object[]) null);
            Method getWalletExceptionMethod = cardmanagerClasz.getDeclaredMethod("resetWalletException");
            getWalletExceptionMethod.invoke(cardmanager, (Object[]) null);
        } catch (SecurityException e) {
            throw new TokenIssuanceException("resetWalletException", e);
        } catch (NoSuchMethodException e) {
            throw new TokenIssuanceException("resetWalletException", e);
        } catch (IllegalArgumentException e) {
            throw new TokenIssuanceException("resetWalletException", e);
        } catch (IllegalAccessException e) {
            throw new TokenIssuanceException("resetWalletException", e);
        } catch (InvocationTargetException e) {
            throw new TokenIssuanceException("resetWalletException", e);
        }
    }
}
