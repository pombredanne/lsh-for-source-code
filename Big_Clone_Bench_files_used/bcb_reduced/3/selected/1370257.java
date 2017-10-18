package org.hardtokenmgmt.core.token;

import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.State;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.Session.UserType;
import iaik.pkcs.pkcs11.Token.SessionReadWriteBehavior;
import iaik.pkcs.pkcs11.Token.SessionType;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.hardtokenmgmt.core.InterfaceFactory;
import org.hardtokenmgmt.core.util.CertUtils;
import org.hardtokenmgmt.core.util.ProfilingTools;

/**
 * Implementation specific token class for Setec 32K SetCos 441 Instant eID cards
 * 
 * @author Philip Vendil 2006-aug-30
 *
 * @version $Id$
 */
public class HIDCresendo700Token extends BaseToken {

    private static final String[] SUPPORRTEDPINTYPES = { IToken.PINTYPE_BASIC };

    private static final String[] SUPPORTEDLABELS = { "Crescendo C700" };

    private static final String[] CRESENDO700 = { "Crescendo C700" };

    private static final String[][] PINLABELMAPPER = { CRESENDO700 };

    public boolean isTokenSupported(Token token) throws TokenException {
        if (token.getTokenInfo().getModel() != null && token.getTokenInfo().getHardwareVersion() != null) {
            if ((token.getTokenInfo().getLabel().trim().equals("Crescendo C700"))) {
                return true;
            }
        }
        return false;
    }

    public String[] getSupportedPINTypes() {
        return SUPPORRTEDPINTYPES;
    }

    @Override
    public void initToken(String tokenlabel, String tokenSerial, String[] pintypes, String[] pins, String[] puks, String[] currentpuks) throws OperationNotSupportedException, TokenException {
        String basicPIN = pins[0];
        String pUK = puks[0];
        Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        basictoken.closeAllSessions();
        tokenlabel = "";
        ProfilingTools.initProfiling("tokenInit");
        ProfilingTools.profile("tokenInit", "Calling tokenInit for Cresendo 700");
        basictoken.initToken(pUK.toCharArray(), CRESENDO700[0]);
        ProfilingTools.profile("tokenInit", "tokenInit for Cresendo 700 Done");
        ProfilingTools.profile("tokenInit", "Calling initPIN for Cresendo 700");
        initPIN(basictoken, pUK, basicPIN);
        ProfilingTools.profile("tokenInit", "initPIN for Cresendo 700 Done");
        InterfaceFactory.getTokenManager().flushTokenCache();
        InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
        super.init(true, basictoken);
        try {
            ProfilingTools.profile("tokenInit", "Generating 2048 bit RSA sigature key ");
            genKey(IToken.PINTYPE_BASIC, basicPIN, basicPIN, IToken.KEYTYPE_SIGN, IToken.KEYALG_RSA, 1024, "key sign");
            ProfilingTools.profile("tokenInit", "2048 bit RSA signature key  Generated");
            ProfilingTools.profile("tokenInit", "Generating 2048 bit RSA authentication key");
            genKey(IToken.PINTYPE_BASIC, basicPIN, basicPIN, IToken.KEYTYPE_AUTH, IToken.KEYALG_RSA, 1024, "key aut + enc");
            ProfilingTools.profile("tokenInit", "2048 bit RSA authentication key Generated");
        } catch (ObjectAlreadyExistsException e) {
            throw new TokenException(e);
        }
        ProfilingTools.profile("tokenInit", "Token initialized.");
    }

    /**
	 * Method to clear the content of a SetCos 441 card
	 * 
	 */
    public void clearToken(String[] pintypes, String[] puks) throws OperationNotSupportedException, TokenException {
        String pUK = puks[0];
        if (pUK == null) {
            throw new TokenException("Error trying to fetch PUK data in cleartoken operation");
        }
        String initBasicPIN = InterfaceFactory.getGlobalSettings().getTokenInitialBasicPIN();
        if (initBasicPIN == null) {
            throw new OperationNotSupportedException("Error, no init basic PIN setting have been defined in global.properties");
        }
        Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        basictoken.closeAllSessions();
        ProfilingTools.initProfiling("clearToken");
        ProfilingTools.profile("clearToken", "Calling tokenInit for eToken");
        basictoken.initToken(pUK.toCharArray(), CRESENDO700[0]);
        ProfilingTools.profile("clearToken", "tokenInit for eToken Done");
        InterfaceFactory.getTokenManager().flushTokenCache();
        InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
        super.init(true, basictoken);
        ProfilingTools.profile("clearToken", "Calling initPIN for Cresendo 700");
        initPIN(basictoken, pUK, initBasicPIN);
        ProfilingTools.profile("clearToken", "initPIN for Cresendo 700 Done");
        ProfilingTools.profile("clearToken", "Token Clean");
    }

    protected String getPrivateKeyLabel(String keytype) throws OperationNotSupportedException {
        if (keytype.equals(IToken.KEYTYPE_AUTH)) {
            return "key aut + enc";
        } else if (keytype.equals(IToken.KEYTYPE_SIGN)) {
            return "key sign";
        } else {
            throw new OperationNotSupportedException("Keytype " + keytype + " not supported.");
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#downloadCert(String, String, String, String, X509Certificate)
	 */
    @Override
    public void downloadCert(String label, String pintype, String pin, String basicpin, X509Certificate cert) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        try {
            java.security.PublicKey publicKey = cert.getPublicKey();
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session.login(UserType.USER, pin.toCharArray());
            }
            Object searchTemplate = null;
            if (publicKey.getAlgorithm().equalsIgnoreCase("RSA")) {
                java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
                RSAPrivateKey rsaPrivateKeySearchTemplate = new RSAPrivateKey();
                byte[] modulus = iaik.pkcs.pkcs11.Util.unsignedBigIntergerToByteArray(rsaPublicKey.getModulus());
                rsaPrivateKeySearchTemplate.getModulus().setByteArrayValue(modulus);
                searchTemplate = rsaPrivateKeySearchTemplate;
            }
            byte[] objectID = null;
            if (searchTemplate != null) {
                session.findObjectsInit(searchTemplate);
                Object[] foundKeyObjects = session.findObjects(1);
                if (foundKeyObjects.length > 0) {
                    Key foundKey = (Key) foundKeyObjects[0];
                    objectID = foundKey.getId().getByteArrayValue();
                }
                session.findObjectsFinal();
            }
            session.findObjectsInit(new X509PublicKeyCertificate());
            Object[] objects = session.findObjects(MAXNUMOBJECTS);
            for (int i = 0; i < objects.length; i++) {
                X509PublicKeyCertificate tokencert = (X509PublicKeyCertificate) objects[i];
                if (label.equals(new String(tokencert.getLabel().getCharArrayValue()))) {
                    throw new ObjectAlreadyExistsException("Certificate with label : " + label + " already exists on the token");
                }
            }
            session.findObjectsFinal();
            byte[] newObjectID = null;
            if (objectID == null) {
                if (publicKey instanceof java.security.interfaces.RSAPublicKey) {
                    newObjectID = ((java.security.interfaces.RSAPublicKey) publicKey).getModulus().toByteArray();
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    newObjectID = digest.digest(newObjectID);
                } else {
                    newObjectID = CertUtils.getFingerprintAsString(cert).getBytes();
                }
            } else {
                newObjectID = objectID;
            }
            ASN1InputStream aSN1Stream = new ASN1InputStream(cert.getEncoded());
            DERObject aSN1Object = aSN1Stream.readObject();
            X509CertificateStructure certificateStructure = X509CertificateStructure.getInstance(aSN1Object);
            try {
                X509PublicKeyCertificate certObject = new X509PublicKeyCertificate();
                certObject.getToken().setBooleanValue(Boolean.TRUE);
                certObject.getPrivate().setBooleanValue(Boolean.FALSE);
                certObject.getLabel().setCharArrayValue(label.toCharArray());
                certObject.getId().setByteArrayValue(newObjectID);
                certObject.getSubject().setByteArrayValue(certificateStructure.getSubject().getDEREncoded());
                certObject.getIssuer().setByteArrayValue(certificateStructure.getIssuer().getDEREncoded());
                certObject.getSerialNumber().setByteArrayValue(certificateStructure.getSerialNumber().getDEREncoded());
                certObject.getValue().setByteArrayValue(cert.getEncoded());
                if (cert.getBasicConstraints() == -1) {
                    certObject.getCertificateCategory().setLongValue(new Long(0));
                } else {
                    certObject.getCertificateCategory().setLongValue(new Long(2));
                }
                session.createObject(certObject);
            } catch (CertificateEncodingException e) {
                throw new TokenException("Error in certificate encoding");
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenException(e.getMessage(), e);
        } catch (IOException e) {
            throw new TokenException(e.getMessage(), e);
        } catch (CertificateEncodingException e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        clearCertificateCache();
    }

    protected String getPINLabel(Token token, String pintype) throws OperationNotSupportedException, TokenException {
        String retval = null;
        String tokenLabel = token.getTokenInfo().getLabel().trim();
        for (int i = 0; i < SUPPORTEDLABELS.length; i++) {
            if (tokenLabel.startsWith(SUPPORTEDLABELS[i])) {
                retval = PINLABELMAPPER[i][0];
            }
        }
        if (retval == null) {
            throw new OperationNotSupportedException("Invalid PIN Type.");
        }
        return retval;
    }

    /**
	 * Method generating one and the same PUK for all PIN types.
	 * 
	 * @see org.hardtokenmgmt.core.token.IToken#generatePUK(String)
	 */
    @Override
    public String generatePUK(String pintype) throws OperationNotSupportedException {
        return "12345678";
    }

    /**
	 * @throws TokenException 
	 * @see org.hardtokenmgmt.core.token.IToken#getInitializationRequirements()
	 */
    public InitializationRequirements getInitializationRequirements() throws TokenException {
        return InitializationRequirements.DEFAULTSOPINFORNEWCARDS;
    }

    private void initPIN(Token token, String sOPIN, String newPIN) throws OperationNotSupportedException, TokenException {
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            session.login(Session.UserType.SO, sOPIN.toCharArray());
            session.initPIN(newPIN.toCharArray());
            session.logout();
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    @Override
    protected boolean isInitialized(Token token) throws TokenException {
        return true;
    }
}
