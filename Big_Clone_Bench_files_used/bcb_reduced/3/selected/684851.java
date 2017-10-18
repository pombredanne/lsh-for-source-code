package org.hardtokenmgmt.core.token.etoken;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.MechanismInfo;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.State;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.Session.UserType;
import iaik.pkcs.pkcs11.Token.SessionReadWriteBehavior;
import iaik.pkcs.pkcs11.Token.SessionType;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.BooleanAttribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.CharArrayAttribute;
import iaik.pkcs.pkcs11.objects.HardwareFeature;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs11.objects.LongAttribute;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.RSAPublicKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.wrapper.CK_ATTRIBUTE;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.hardtokenmgmt.core.InterfaceFactory;
import org.hardtokenmgmt.core.token.BaseToken;
import org.hardtokenmgmt.core.token.IToken;
import org.hardtokenmgmt.core.token.InvalidChallengeException;
import org.hardtokenmgmt.core.token.ObjectAlreadyExistsException;
import org.hardtokenmgmt.core.token.OperationNotSupportedException;
import org.hardtokenmgmt.core.token.PINInfo;
import org.hardtokenmgmt.core.token.TokenManager;
import org.hardtokenmgmt.core.util.CertUtils;
import org.hardtokenmgmt.core.util.PKCS11CertificationRequest;
import org.hardtokenmgmt.core.util.ProfilingTools;
import org.hardtokenmgmt.core.util.UserDataGenerator;

/**
 * Implementation specific token class for Setec 32K SetCos 441 Instant eID cards
 * 
 * @author Philip Vendil 2006-aug-30
 *
 * @version $Id$
 */
public class AladdinEToken extends BaseToken {

    private static final String KEYTYPE_AUTH_LABEL = "key aut + enc";

    private static final String KEYTYPE_SIGN_LABEL = "key sign";

    private static final String KEYTYPE_SECONDARY_AUTH_LABEL = "key secondary auth";

    private static final String[] SUPPORRTEDPINTYPES = { IToken.PINTYPE_BASIC, IToken.PINTYPE_SIGN };

    private static final Integer KEY_LENGTH = new Integer(InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.keylength", 2048));

    private static final Boolean DEFAULT_PIN_POLICY_MODIFYABLE = InterfaceFactory.getGlobalSettings().getPropertyAsBoolean("token.etoken.pinpolicy.modifyable", true);

    private static final Boolean DEFAULT_PIN_MIX_CHARS = InterfaceFactory.getGlobalSettings().getPropertyAsBoolean("token.etoken.pinpolicy.mixchars", true);

    private static final Long DEFAULT_PIN_MIN_AGE = new Long(InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.pinpolicy.pinminage", 0));

    private static final Long DEFAULT_PIN_MAX_AGE = new Long(InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.pinpolicy.pinmaxage", 0));

    private static final Long DEFAULT_PIN_WARN_PERIOD = new Long(InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.pinpolicy.warnperiod", 0));

    private static final Long DEFAULT_PIN_HISTORY_SIZE = new Long(InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.pinpolicy.historysize", 10));

    private static final int DEFAULT_USER_PIN_RETRY_COUNTER = InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.userpin.retrycounter", 5);

    private static final int DEFAULT_ADMIN_PIN_RETRY_COUNTER = InterfaceFactory.getGlobalSettings().getPropertyAsInt("token.etoken.adminpin.retrycounter", 10);

    private static final Long DEFAULT_PIN_MIN_LEN = new Long(InterfaceFactory.getGlobalSettings().getPropertyAsInt("pinlockedcontroller.pin.minlength", 6));

    private static final Boolean GENERATE_SECONDARY_AUTH_KEY = InterfaceFactory.getGlobalSettings().getPropertyAsBoolean("token.generate.secondaryauthkey", false);

    public boolean isTokenSupported(Token token) throws TokenException {
        if (token.getTokenInfo().getModel() != null && token.getTokenInfo().getHardwareVersion() != null) {
            if ((token.getTokenInfo().getModel().trim().equals("eToken"))) {
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
        String basicPIN;
        String signaturePIN;
        String pUK = puks[0];
        if (pintypes[0].equals(IToken.PINTYPE_BASIC)) {
            basicPIN = pins[0];
            signaturePIN = pins[1];
            pUK = puks[0];
        } else {
            basicPIN = pins[1];
            signaturePIN = pins[0];
            pUK = puks[1];
        }
        char[] basicKeyContainerName = generateRandomContainerName();
        char[] secondaryAuthContainerName = generateRandomContainerName();
        char[] signKeyContainerName = generateRandomContainerName();
        Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        basictoken.closeAllSessions();
        tokenlabel = "";
        ProfilingTools.initProfiling("tokenInit");
        ProfilingTools.profile("tokenInit", "Calling eTCInitTokenInit for eToken");
        Slot slot = basictoken.getSlot();
        int sessionId = AladdinETokenConnector.eTCInitTokenInit(slot, "eToken", pUK, DEFAULT_ADMIN_PIN_RETRY_COUNTER);
        ProfilingTools.profile("tokenInit", "eTCInitTokenInit for eToken Done");
        ProfilingTools.profile("tokenInit", "Creating token object for eToken");
        ETokenSession initETokenSession = new ETokenSession(basictoken, sessionId);
        HardwareFeature tokenObject = genTokenObject();
        initETokenSession.createObject(tokenObject);
        ProfilingTools.profile("tokenInit", "Creating token object for eToken Done");
        ProfilingTools.profile("tokenInit", "Creating pinPolicy for eToken");
        HardwareFeature pinPolicy = genPINPolicyObject();
        initETokenSession.createObject(pinPolicy);
        ProfilingTools.profile("tokenInit", "Creating pinPolicy for eToken Done");
        ProfilingTools.profile("tokenInit", "Creating 2Auth for eToken");
        HardwareFeature secondAuthObject = gen2AuthObject();
        initETokenSession.createObject(secondAuthObject);
        ProfilingTools.profile("tokenInit", "Creating 2Auth for eToken Done");
        ProfilingTools.profile("tokenInit", "Calling eTCInitPIN for eToken");
        AladdinETokenConnector.eTCInitPIN(sessionId, basicPIN, DEFAULT_USER_PIN_RETRY_COUNTER, false);
        ProfilingTools.profile("tokenInit", "eTCInitPIN for eToken Done");
        ProfilingTools.profile("tokenInit", "Calling eTCInitTokenFinal for eToken");
        AladdinETokenConnector.eTCInitTokenFinal(sessionId);
        ProfilingTools.profile("tokenInit", "eTCInitTokenFinal for eToken Done");
        InterfaceFactory.getTokenManager().flushTokenCache();
        InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
        super.init(true, basictoken);
        try {
            ProfilingTools.profile("tokenInit", "Generating " + KEY_LENGTH + " bit RSA authentication key");
            genKey(IToken.PINTYPE_BASIC, basicPIN, basicPIN, IToken.KEYTYPE_AUTH, IToken.KEYALG_RSA, KEY_LENGTH, KEYTYPE_AUTH_LABEL, basicKeyContainerName);
            ProfilingTools.profile("tokenInit", KEY_LENGTH + " bit RSA authentication key Generated");
            if (GENERATE_SECONDARY_AUTH_KEY) {
                ProfilingTools.profile("tokenInit", "Generating " + KEY_LENGTH + " bit RSA secondary authentication key");
                genKey(IToken.PINTYPE_BASIC, basicPIN, basicPIN, IToken.KEYTYPE_SECONDARY_AUTH, IToken.KEYALG_RSA, KEY_LENGTH, KEYTYPE_SECONDARY_AUTH_LABEL, secondaryAuthContainerName);
                ProfilingTools.profile("tokenInit", KEY_LENGTH + " bit RSA secondary authentication key Generated");
            }
            ProfilingTools.profile("tokenInit", "Generating " + KEY_LENGTH + " bit RSA sigature key ");
            genKey(IToken.PINTYPE_SIGN, signaturePIN, basicPIN, IToken.KEYTYPE_SIGN, IToken.KEYALG_RSA, KEY_LENGTH, KEYTYPE_SIGN_LABEL, signKeyContainerName);
            ProfilingTools.profile("tokenInit", KEY_LENGTH + " bit RSA signature key  Generated");
        } catch (ObjectAlreadyExistsException e) {
            throw new TokenException(e);
        }
        ProfilingTools.profile("tokenInit", "Token initialized.");
    }

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
        basictoken.initToken("1234567890".toCharArray(), "eToken");
        ProfilingTools.profile("clearToken", "tokenInit for eToken Done");
        InterfaceFactory.getTokenManager().flushTokenCache();
        InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
        super.init(true, basictoken);
        ProfilingTools.profile("clearToken", "Calling changeSOPIN for eToken");
        changeSOPIN(basictoken, "1234567890", pUK);
        ProfilingTools.profile("clearToken", "changeSOPIN for eToken Done");
        ProfilingTools.profile("clearToken", "Calling initPIN for eToken");
        initPIN(basictoken, pUK, initBasicPIN);
        ProfilingTools.profile("clearToken", "initPIN for eToken Done");
        ProfilingTools.profile("clearToken", "Token Clean");
    }

    protected String getPrivateKeyLabel(String keytype) throws OperationNotSupportedException {
        if (keytype.equals(IToken.KEYTYPE_AUTH)) {
            return KEYTYPE_AUTH_LABEL;
        } else if (keytype.equals(IToken.KEYTYPE_SIGN)) {
            return KEYTYPE_SIGN_LABEL;
        } else if (keytype.equals(IToken.KEYTYPE_SECONDARY_AUTH)) {
            return KEYTYPE_SECONDARY_AUTH_LABEL;
        } else {
            throw new OperationNotSupportedException("Keytype " + keytype + " not supported.");
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#genKey(String, String, String, String, String, int, String)
	 */
    @Override
    public void genKey(String pintype, String pin, String basicpin, String keytype, String algorithm, int keysize, String label) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        genKey(pintype, pin, basicpin, keytype, algorithm, keysize, label, null);
    }

    private void genKey(String pintype, String pin, String basicpin, String keytype, String algorithm, int keysize, String label, char[] containerName) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        boolean isSignKey = keytype.equals(KEYTYPE_SIGN);
        try {
            if (algorithm.equals(IToken.KEYALG_RSA)) {
                session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
                if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                    session.login(UserType.USER, basicpin.toCharArray());
                }
                RSAPublicKey rsaPublicKeyTemplate = new RSAPublicKey();
                RSAPrivateKey rsaPrivateKeyTemplate = new ETokenRSAPrivateKey(isSignKey, pin, containerName);
                rsaPublicKeyTemplate.getModulusBits().setLongValue(new Long(keysize));
                byte[] publicExponentBytes = { 0x01, 0x00, 0x01 };
                rsaPublicKeyTemplate.getPublicExponent().setByteArrayValue(publicExponentBytes);
                rsaPublicKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
                byte[] id = new byte[20];
                new Random().nextBytes(id);
                rsaPublicKeyTemplate.getId().setByteArrayValue(id);
                rsaPrivateKeyTemplate.getSensitive().setBooleanValue(Boolean.TRUE);
                rsaPrivateKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
                rsaPrivateKeyTemplate.getPrivate().setBooleanValue(Boolean.TRUE);
                rsaPrivateKeyTemplate.getId().setByteArrayValue(id);
                rsaPrivateKeyTemplate.getLabel().setCharArrayValue(label.toCharArray());
                MechanismInfo signatureMechanismInfo = token.getMechanismInfo(Mechanism.RSA_PKCS);
                rsaPublicKeyTemplate.getVerify().setBooleanValue(new Boolean(signatureMechanismInfo.isVerify()));
                rsaPublicKeyTemplate.getVerifyRecover().setBooleanValue(new Boolean(signatureMechanismInfo.isVerifyRecover()));
                rsaPublicKeyTemplate.getEncrypt().setBooleanValue(new Boolean(!isSignKey));
                rsaPublicKeyTemplate.getDerive().setBooleanValue(new Boolean(signatureMechanismInfo.isDerive()));
                rsaPublicKeyTemplate.getWrap().setBooleanValue(new Boolean(!isSignKey));
                rsaPublicKeyTemplate.getLabel().setCharArrayValue(label.toCharArray());
                rsaPrivateKeyTemplate.getSign().setBooleanValue(new Boolean(signatureMechanismInfo.isSign()));
                rsaPrivateKeyTemplate.getSignRecover().setBooleanValue(new Boolean(signatureMechanismInfo.isSignRecover()));
                rsaPrivateKeyTemplate.getDecrypt().setBooleanValue(new Boolean(!isSignKey));
                rsaPrivateKeyTemplate.getDerive().setBooleanValue(new Boolean(signatureMechanismInfo.isDerive()));
                rsaPrivateKeyTemplate.getUnwrap().setBooleanValue(new Boolean(!isSignKey));
                rsaPrivateKeyTemplate.getAlwaysAuthenticate().setBooleanValue(new Boolean(isSignKey));
                rsaPublicKeyTemplate.getKeyType().setPresent(false);
                rsaPublicKeyTemplate.getObjectClass().setPresent(false);
                rsaPrivateKeyTemplate.getKeyType().setPresent(false);
                rsaPrivateKeyTemplate.getObjectClass().setPresent(false);
                session.generateKeyPair(Mechanism.RSA_PKCS_KEY_PAIR_GEN, rsaPublicKeyTemplate, rsaPrivateKeyTemplate);
            } else {
                throw new OperationNotSupportedException("Currently is only RSA key ssupported");
            }
        } catch (PKCS11Exception e) {
            e.printStackTrace();
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /**
	 * 
	 * 
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
                session.login(UserType.USER, basicpin.toCharArray());
            }
            Object searchTemplate = null;
            if (publicKey.getAlgorithm().equalsIgnoreCase("RSA")) {
                java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
                ETokenRSAPrivateKey rsaPrivateKeySearchTemplate = new ETokenRSAPrivateKey();
                byte[] modulus = iaik.pkcs.pkcs11.Util.unsignedBigIntergerToByteArray(rsaPublicKey.getModulus());
                rsaPrivateKeySearchTemplate.getModulus().setByteArrayValue(modulus);
                searchTemplate = rsaPrivateKeySearchTemplate;
            }
            byte[] objectID = null;
            String cName = null;
            if (searchTemplate != null) {
                session.findObjectsInit(searchTemplate);
                Object[] foundKeyObjects = session.findObjects(1);
                if (foundKeyObjects.length > 0) {
                    Key foundKey = (Key) foundKeyObjects[0];
                    objectID = foundKey.getId().getByteArrayValue();
                    ETokenRSAPrivateKey eTokenKeyData = new ETokenRSAPrivateKey(session, foundKey.getObjectHandle());
                    cName = new String(eTokenKeyData.getContainerNameAttributeAsBytes().getByteArrayValue(), "UTF-8");
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
            if (cName != null && CertUtils.getUPNAltName(cert) != null) {
                setDefaultCAPIContainer(token, session, cName);
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenException(e.getMessage(), e);
        } catch (IOException e) {
            throw new TokenException(e.getMessage(), e);
        } catch (CertificateEncodingException e) {
            throw new TokenException(e.getMessage(), e);
        } catch (CertificateParsingException e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        clearCertificateCache();
    }

    protected String getPINLabel(Token token, String pintype) throws OperationNotSupportedException, TokenException {
        return "";
    }

    /**
	 * Method generating one and the same PUK for all PIN types.
	 * 
	 * @see org.hardtokenmgmt.core.token.IToken#generatePUK(String)
	 */
    @Override
    public String generatePUK(String pintype) throws OperationNotSupportedException {
        String retval = null;
        if (generatedPUK == null) {
            String ret = "";
            Random rand = new Random();
            while (ret.length() < 8) {
                ret += "" + rand.nextInt();
            }
            generatedPUK = ret.substring(ret.length() - 8);
        }
        retval = generatedPUK;
        return retval;
    }

    private String generatedPUK = null;

    /**
	 * @throws TokenException 
	 * @see org.hardtokenmgmt.core.token.IToken#getInitializationRequirements()
	 */
    public InitializationRequirements getInitializationRequirements() throws TokenException {
        return InitializationRequirements.NOSOPINREQUIRED;
    }

    private void changeSOPIN(Token token, String currentSOPIN, String newSOPIN) throws OperationNotSupportedException, TokenException {
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            session.login(Session.UserType.SO, currentSOPIN.toCharArray());
            session.setPIN(currentSOPIN.toCharArray(), newSOPIN.toCharArray());
            session.logout();
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
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

    private HardwareFeature genTokenObject() {
        return new ETokenTokenObject();
    }

    private HardwareFeature gen2AuthObject() {
        return new EToken2AuthObject();
    }

    private HardwareFeature genPINPolicyObject() {
        return new ETokenPINPolicyObject();
    }

    private class ETokenTokenObject extends HardwareFeature {

        @SuppressWarnings("unchecked")
        private ETokenTokenObject() {
            super();
            hardwareFeatureType_.setLongValue(AladdinETokenConnector.ETCKH_TOKEN_OBJECT);
            BooleanAttribute oneFactor = new BooleanAttribute(AladdinETokenConnector.ETCKA_ONE_FACTOR);
            oneFactor.setBooleanValue(new Boolean(false));
            attributeTable_.put(AladdinETokenConnector.ETCKA_ONE_FACTOR, oneFactor);
            BooleanAttribute rSA2048 = new BooleanAttribute(AladdinETokenConnector.ETCKA_RSA_2048);
            rSA2048.setBooleanValue(new Boolean(true));
            attributeTable_.put(AladdinETokenConnector.ETCKA_RSA_2048, rSA2048);
        }
    }

    /**
	 * Object used to fetch hardware information from the token such as
	 * the current user and SO PIN retry counter.
	 * 
	 * @author Philip Vendil 17 nov 2010
	 *
	 * @version $Id$
	 */
    private class ETokenETCKHTokenObjectInfo extends HardwareFeature {

        ByteArrayAttribute userRetryCounter;

        ByteArrayAttribute soRetryCounter;

        private ETokenETCKHTokenObjectInfo() {
            super();
            hardwareFeatureType_.setLongValue(AladdinETokenConnector.ETCKH_TOKEN_OBJECT);
        }

        public void readAttributes(Session session) throws TokenException {
            super.readAttributes(session);
            Object.getAttributeValue(session, objectHandle_, userRetryCounter);
            Object.getAttributeValue(session, objectHandle_, soRetryCounter);
        }

        @SuppressWarnings("unchecked")
        protected void putAttributesInTable(ETokenETCKHTokenObjectInfo object) {
            if (object == null) {
                throw new NullPointerException("Argument \"object\" must not be null.");
            }
            object.attributeTable_.put(AladdinETokenConnector.ETCKA_RETRY_USER, object.userRetryCounter);
            object.attributeTable_.put(AladdinETokenConnector.ETCKA_RETRY_SO, object.soRetryCounter);
        }

        protected void allocateAttributes() {
            super.allocateAttributes();
            userRetryCounter = new ByteArrayAttribute(AladdinETokenConnector.ETCKA_RETRY_USER);
            soRetryCounter = new ByteArrayAttribute(AladdinETokenConnector.ETCKA_RETRY_SO);
        }

        public ETokenETCKHTokenObjectInfo getAttributes(Session session, long objectHandle) throws PKCS11Exception {
            ETokenETCKHTokenObjectInfo retval = new ETokenETCKHTokenObjectInfo();
            Object.getAttributeValue(session, objectHandle, retval.getUserRetryCounter());
            Object.getAttributeValue(session, objectHandle, retval.getSORetryCounter());
            return retval;
        }

        ByteArrayAttribute getUserRetryCounter() {
            return userRetryCounter;
        }

        ByteArrayAttribute getSORetryCounter() {
            return soRetryCounter;
        }
    }

    private class ETokenPINPolicyObject extends HardwareFeature {

        @SuppressWarnings("unchecked")
        private ETokenPINPolicyObject() {
            super();
            hardwareFeatureType_.setLongValue(AladdinETokenConnector.ETCKH_PIN_POLICY);
            LongAttribute pinType = new LongAttribute(AladdinETokenConnector.ETCKA_PIN_POLICY_TYPE);
            pinType.setLongValue(AladdinETokenConnector.ETCKPT_GENERAL_PIN_POLICY);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_POLICY_TYPE, pinType);
            BooleanAttribute modifyable = new BooleanAttribute(Attribute.MODIFIABLE);
            modifyable.setBooleanValue(DEFAULT_PIN_POLICY_MODIFYABLE);
            attributeTable_.put(Attribute.MODIFIABLE, modifyable);
            LongAttribute pinMinLength = new LongAttribute(AladdinETokenConnector.ETCKA_PIN_MIN_LEN);
            pinMinLength.setLongValue(DEFAULT_PIN_MIN_LEN);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_MIN_LEN, pinMinLength);
            LongAttribute historySize = new LongAttribute(AladdinETokenConnector.ETCKA_PIN_HISTORY_SIZE);
            historySize.setLongValue(DEFAULT_PIN_HISTORY_SIZE);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_HISTORY_SIZE, historySize);
            LongAttribute warnPeriod = new LongAttribute(AladdinETokenConnector.ETCKA_PIN_WARN_PERIOD);
            warnPeriod.setLongValue(DEFAULT_PIN_WARN_PERIOD);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_WARN_PERIOD, warnPeriod);
            LongAttribute minPinAge = new LongAttribute(AladdinETokenConnector.ETCKA_PIN_MIN_AGE);
            minPinAge.setLongValue(DEFAULT_PIN_MIN_AGE);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_MIN_AGE, minPinAge);
            LongAttribute maxPinAge = new LongAttribute(AladdinETokenConnector.ETCKA_PIN_MAX_AGE);
            maxPinAge.setLongValue(DEFAULT_PIN_MAX_AGE);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_MAX_AGE, maxPinAge);
            BooleanAttribute mixChars = new BooleanAttribute(AladdinETokenConnector.ETCKA_PIN_MIX_CHARS);
            mixChars.setBooleanValue(DEFAULT_PIN_MIX_CHARS);
            attributeTable_.put(AladdinETokenConnector.ETCKA_PIN_MIX_CHARS, mixChars);
        }
    }

    /**
	 * Etoken Auth Object containing data about secondary authentication.
	 * 
	 * @author Philip Vendil 25 okt 2010
	 *
	 * @version $Id$
	 */
    private class EToken2AuthObject extends HardwareFeature {

        @SuppressWarnings("unchecked")
        private EToken2AuthObject() {
            super();
            hardwareFeatureType_.setLongValue(AladdinETokenConnector.ETCKH_2NDAUTH);
            BooleanAttribute modifyable = new BooleanAttribute(Attribute.MODIFIABLE);
            modifyable.setBooleanValue(Boolean.TRUE);
            attributeTable_.put(Attribute.MODIFIABLE, modifyable);
            LongAttribute secondAuthPolicy = new LongAttribute(AladdinETokenConnector.ETCKA_2NDAUTH_CREATE);
            secondAuthPolicy.setLongValue(AladdinETokenConnector.ETCK_2NDAUTH_PROMPT_CONDITIONAL);
            attributeTable_.put(AladdinETokenConnector.ETCKA_2NDAUTH_CREATE, secondAuthPolicy);
        }
    }

    private class ETokenRSAPrivateKey extends RSAPrivateKey {

        private CharArrayAttribute containerNameAttribute;

        private ByteArrayAttribute containerNameAttributeAsBytes;

        private CharArrayAttribute secondAuthPINAttribute;

        private BooleanAttribute cAPIKeySignatureAttribute;

        @SuppressWarnings("unchecked")
        private ETokenRSAPrivateKey(boolean isSignKey, String signaturePIN, char[] containerName) {
            super();
            char[] cName = containerName;
            if (cName == null) {
                cName = generateRandomContainerName();
            }
            containerNameAttribute = new CharArrayAttribute(AladdinETokenConnector.ETCKA_CAPI_KEY_CONTAINER);
            containerNameAttribute.setCharArrayValue(cName);
            attributeTable_.put(AladdinETokenConnector.ETCKA_CAPI_KEY_CONTAINER, containerNameAttribute);
            if (isSignKey) {
                secondAuthPINAttribute = new CharArrayAttribute(AladdinETokenConnector.ETCKA_2NDAUTH_PIN);
                secondAuthPINAttribute.setCharArrayValue(signaturePIN.toCharArray());
                cAPIKeySignatureAttribute = new BooleanAttribute(AladdinETokenConnector.ETCKA_CAPI_KEYSIGNATURE);
                cAPIKeySignatureAttribute.setBooleanValue(new Boolean(true));
                attributeTable_.put(AladdinETokenConnector.ETCKA_CAPI_KEYSIGNATURE, cAPIKeySignatureAttribute);
                attributeTable_.put(AladdinETokenConnector.ETCKA_2NDAUTH_PIN, secondAuthPINAttribute);
            }
        }

        public ETokenRSAPrivateKey(Session session, long objectHandle) throws TokenException {
            allocateAttributes();
            Object.getAttributeValue(session, objectHandle, containerNameAttributeAsBytes);
            Object.getAttributeValue(session, objectHandle, cAPIKeySignatureAttribute);
        }

        public ETokenRSAPrivateKey() throws TokenException {
            super();
        }

        public void readAttributes(Session session) throws TokenException {
            super.readAttributes(session);
            Object.getAttributeValue(session, objectHandle_, containerNameAttributeAsBytes);
            Object.getAttributeValue(session, objectHandle_, cAPIKeySignatureAttribute);
        }

        @SuppressWarnings("unchecked")
        protected void putAttributesInTable(ETokenRSAPrivateKey object) {
            if (object == null) {
                throw new NullPointerException("Argument \"object\" must not be null.");
            }
            object.attributeTable_.put(AladdinETokenConnector.ETCKA_CAPI_KEY_CONTAINER, object.containerNameAttribute);
            object.attributeTable_.put(AladdinETokenConnector.ETCKA_CAPI_KEYSIGNATURE, object.cAPIKeySignatureAttribute);
        }

        protected void allocateAttributes() {
            super.allocateAttributes();
            containerNameAttributeAsBytes = new ByteArrayAttribute(AladdinETokenConnector.ETCKA_CAPI_KEY_CONTAINER);
            containerNameAttribute = new CharArrayAttribute(AladdinETokenConnector.ETCKA_CAPI_KEY_CONTAINER);
            cAPIKeySignatureAttribute = new BooleanAttribute(AladdinETokenConnector.ETCKA_CAPI_KEYSIGNATURE);
        }

        ByteArrayAttribute getContainerNameAttributeAsBytes() {
            return containerNameAttributeAsBytes;
        }

        BooleanAttribute getCAPIKeySignatureAttribute() {
            return cAPIKeySignatureAttribute;
        }
    }

    private static final char[] HexTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static char[] generateRandomContainerName() {
        char[] cName = new char[16];
        Random rand = new Random();
        for (int i = 0; i < 16; i++) {
            cName[i] = HexTable[rand.nextInt(16)];
        }
        return cName;
    }

    private class ETokenSession extends Session {

        public ETokenSession(Token token, long sessionId) {
            super(token, sessionId);
        }

        public Object createObject(Object templateObject) throws TokenException {
            CK_ATTRIBUTE[] ckAttributes = Object.getSetAttributes(templateObject);
            pkcs11Module_.C_CreateObject(sessionHandle_, ckAttributes);
            return null;
        }

        public void setAttributeValue(Long objectHandle, CK_ATTRIBUTE[] ck_attr) throws TokenException {
            pkcs11Module_.C_SetAttributeValue(sessionHandle_, objectHandle, ck_attr);
        }
    }

    /**
	 * Method computing the challenge response code of the eToken using  
	 */
    @Override
    public String generateUnblockResponse(String pinType, String sOPIN, String challenge) throws InvalidChallengeException, OperationNotSupportedException, TokenException {
        return AladdinETokenConnector.computeUnblockResponse(challenge, sOPIN);
    }

    /**
	 * Custom implementation of the genPKCS10 that handles special handling of the signature PIN.
	 * 
	 * @see org.hardtokenmgmt.core.token.BaseToken#genPKCS10(java.lang.String, java.lang.String, java.lang.String, org.hardtokenmgmt.core.util.UserDataGenerator)
	 */
    @Override
    public PKCS10CertificationRequest genPKCS10(String keytype, String pintype, String pin, String basicPin, UserDataGenerator userDataGenerator) throws OperationNotSupportedException, TokenException {
        PKCS10CertificationRequest retval = null;
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session.login(UserType.USER, basicPin.toCharArray());
            }
            RSAPrivateKey privKey = (RSAPrivateKey) findPrivateKey(session, keytype);
            RSAPublicKey pubKey = (RSAPublicKey) findPublicKey(session, keytype, userDataGenerator);
            BigInteger modulus = new BigInteger(1, pubKey.getModulus().getByteArrayValue());
            BigInteger publicExponent = new BigInteger(1, pubKey.getPublicExponent().getByteArrayValue());
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                java.security.interfaces.RSAPublicKey javaRsaPublicKey = (java.security.interfaces.RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);
                PKCS11CertificationRequest certReq = new AladdinPKCS11CertificationRequest(session, Mechanism.RSA_PKCS, CertUtils.stringToBcX509Name("CN=notused"), javaRsaPublicKey, null, privKey, pintype, pin);
                retval = certReq.getPKCS10CertificationRequest();
            } catch (Exception e) {
                throw new TokenException(e);
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        return retval;
    }

    /**
	 * Custom method that never block the sign pin.
	 * @see org.hardtokenmgmt.core.token.BaseToken#blockPIN(java.lang.String)
	 */
    @Override
    public PINInfo blockPIN(String pintype) throws OperationNotSupportedException, TokenException {
        if (pintype != IToken.PINTYPE_SIGN) {
            return super.blockPIN(pintype);
        }
        return super.getPINInfo(pintype);
    }

    /**
	 * Custom implementation that reads a hardware object to see the current retry counter.
	 * 
	 * @see org.hardtokenmgmt.core.token.IToken#getPINInfo(String)
	 */
    @Override
    public PINInfo getPINInfo(String pintype) throws OperationNotSupportedException, TokenException {
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RO_SESSION, null, null);
            long currentUserRetryCount = 0;
            long currentSORetryCount = 0;
            ETokenETCKHTokenObjectInfo objTemplate = new ETokenETCKHTokenObjectInfo();
            session.findObjectsInit(objTemplate);
            Object[] tokenObject = session.findObjects(1);
            if (tokenObject.length > 0) {
                ETokenETCKHTokenObjectInfo eTCKHToken = (ETokenETCKHTokenObjectInfo) objTemplate.getAttributes(session, tokenObject[0].getObjectHandle());
                ByteArrayAttribute userRetryAttr = eTCKHToken.getUserRetryCounter();
                if (userRetryAttr != null) {
                    currentUserRetryCount = userRetryAttr.getByteArrayValue()[0];
                }
                ByteArrayAttribute soRetryAttr = eTCKHToken.getSORetryCounter();
                if (soRetryAttr != null) {
                    currentSORetryCount = soRetryAttr.getByteArrayValue()[0];
                }
            }
            PINInfo retval = new PINInfo(token.getTokenInfo().isLoginRequired(), currentUserRetryCount < 3, currentUserRetryCount == 1, currentUserRetryCount == 0, currentSORetryCount < 3, currentSORetryCount == 1, currentSORetryCount == 0);
            return retval;
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /**
	 * Generates a remote unblock challenge from the token for the given pin type.
	 * 
	 * @param pinType pin type to generate unblock challenge for, currently is only, basic pin supported.
	 * @return The generated unblock challenge in HEX encoding.
	 * @throws TokenException if communication problems occur with the token.
	 */
    public String genRemoteUnblockChallenge(String pinType) throws TokenException {
        String retval = null;
        Token token = tokens.get(pinType);
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RO_SESSION, null, null);
            retval = AladdinETokenConnector.eTCGenUnblockChallenge((int) session.getSessionHandle());
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        return retval;
    }

    /**
	 * Method used to finalize the remote pin unblock by
	 * 
	 * @param pinType pin type to generate unblock challenge for, currently is only, basic pin supported.
	 * @param response the response code generated by the server.
	 * @param newPIN the new PIN 
	 * @param pinRetryCounter the new PIN Retry Counter
	 * @param toBeChanged if the pin should be changed next time the user log-in.
	 * @throws TokenException if communication problems occur with the token.
	 */
    public void genRemoteUnblockComplete(String pinType, String response, String newPIN, int pinRetryCounter, boolean toBeChanged) throws TokenException {
        Token token = tokens.get(pinType);
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RO_SESSION, null, null);
            AladdinETokenConnector.eTCUnblockComplete((int) session.getSessionHandle(), response, newPIN, pinRetryCounter, toBeChanged);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /** 
	 * Challenge response is supported.
	 * 
	 * @see org.hardtokenmgmt.core.token.BaseToken#isChallengeResponseUnblockSupported()
	 */
    @Override
    public boolean isChallengeResponseUnblockSupported() {
        return true;
    }

    private void setDefaultCAPIContainer(Token token, Session session, String defaultContainerName) throws TokenException, UnsupportedEncodingException {
        ETokenCAPIObject cAPIObject = new ETokenCAPIObject();
        session.findObjectsInit(cAPIObject);
        try {
            Object[] objects = session.findObjects(1);
            CharArrayAttribute defaultContainer = new CharArrayAttribute(AladdinETokenConnector.ETCKA_CAPI_DEFAULT_KC);
            defaultContainer.setCharArrayValue(defaultContainerName.toCharArray());
            ETokenSession eTokenSession = new ETokenSession(token, session.getSessionHandle());
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[1];
            attributes[0] = new CK_ATTRIBUTE();
            attributes[0].type = AladdinETokenConnector.ETCKA_CAPI_DEFAULT_KC;
            attributes[0].pValue = defaultContainerName.toCharArray();
            eTokenSession.setAttributeValue(objects[0].getObjectHandle(), attributes);
        } finally {
            session.findObjectsFinal();
        }
    }

    class ETokenCAPIObject extends HardwareFeature {

        ETokenCAPIObject() {
            super();
            hardwareFeatureType_.setLongValue(AladdinETokenConnector.ETCKH_CAPI);
        }
    }
}
