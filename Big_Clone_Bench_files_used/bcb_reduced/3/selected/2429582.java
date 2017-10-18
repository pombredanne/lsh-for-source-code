package org.hardtokenmgmt.core.token;

import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Session.UserType;
import iaik.pkcs.pkcs11.State;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.Token.SessionReadWriteBehavior;
import iaik.pkcs.pkcs11.Token.SessionType;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.CharArrayAttribute;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.PrivateKey;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Random;
import java.util.logging.Level;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.util.encoders.Hex;
import org.hardtokenmgmt.core.InterfaceFactory;
import org.hardtokenmgmt.core.log.LocalLog;
import org.hardtokenmgmt.core.util.CertUtils;
import org.hardtokenmgmt.core.util.ProfilingTools;

/**
 * Implementation specific token class for Setec 32K SetCos 441 Instant eID cards
 * 
 * @author Philip Vendil 2006-aug-30
 *
 * @version $Id$
 */
public class NetIdGemaltoClassicClientToken extends BaseToken {

    private static final String[] SUPPORRTEDPINTYPES = { IToken.PINTYPE_BASIC, IToken.PINTYPE_SIGN };

    private static final String[] SUPPORTEDLABELS = { "Tjanstekort EID", "Tj�nstekort EID", "Tjänstekort EID" };

    private static final String[] TJANSTEKORT = { "Tjanstekort EID (legitimera)", "Tjanstekort EID (underskrift)" };

    private static final String[] TJANSTEKORT2 = { "Tj�nstekort EID (legitimera)", "Tj�nstekort EID (underskrift)" };

    private static final String[] TJANSTEKORT3 = { "Tjänstekort EID (legitimera)", "Tjänstekort EID (underskrift)" };

    private static final String[][] PINLABELMAPPER = { TJANSTEKORT, TJANSTEKORT2, TJANSTEKORT3 };

    public boolean isTokenSupported(Token token) throws TokenException {
        if (token.getTokenInfo().getModel() != null && token.getTokenInfo().getHardwareVersion() != null) {
            if ((token.getTokenInfo().getModel().trim().equals("GemXpresso"))) {
                String tokenLabel = token.getTokenInfo().getLabel().trim();
                for (String label : SUPPORTEDLABELS) {
                    if (tokenLabel.startsWith(label)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String[] getSupportedPINTypes() {
        return SUPPORRTEDPINTYPES;
    }

    @Override
    public void initToken(String tokenlabel, String tokenSerial, String[] pintypes, String[] pins, String[] puks, String[] currentpuks) throws OperationNotSupportedException, TokenException {
        String basicPIN = null;
        String signaturePIN = null;
        String pUK = null;
        String currentBasicPUK = null;
        String currentSignaturePUK = null;
        if (currentpuks == null) {
            String initialPUK = InterfaceFactory.getGlobalSettings().getProperty("token.gemaltoclassicclient.defaultpuk");
            if (initialPUK == null) {
                throw new TokenException("Error initializing Gemalto Classic Client Card, no default PUK specified, check setting 'token.gemaltoclassicclient.defaultpuk' in global configuration.");
            }
            currentpuks = new String[] { initialPUK, initialPUK };
        }
        for (int i = 0; i < pintypes.length; i++) {
            if (pintypes[i].equals(IToken.PINTYPE_BASIC)) {
                basicPIN = pins[i];
                pUK = puks[i];
                currentBasicPUK = currentpuks[i];
            }
            if (pintypes[i].equals(IToken.PINTYPE_SIGN)) {
                signaturePIN = pins[i];
                currentSignaturePUK = currentpuks[i];
            }
        }
        if (Integer.parseInt(currentBasicPUK) < 0) {
            currentBasicPUK = "";
        }
        if (Integer.parseInt(currentSignaturePUK) < 0) {
            currentSignaturePUK = "";
        }
        Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        Token signtoken = (Token) tokens.get(IToken.PINTYPE_SIGN);
        tokenlabel = "";
        ProfilingTools.initProfiling("tokenInit");
        NetIdConnector.editIIDIniEntry("Number", tokenSerial, "RPS CIA1.SOPIN");
        NetIdConnector.editIIDIniEntry("Number", tokenSerial, "RPS CIA2.SOPIN");
        ProfilingTools.profile("tokenInit", "Calling tokenInit for RPS CIA1.SOPIN");
        NetIdConnector.initToken(basictoken.getSlot(), "RPS CIA1.SOPIN", tokenlabel, pUK, currentBasicPUK);
        ProfilingTools.profile("tokenInit", "tokenInit for RPS CIA1.SOPIN Done");
        ProfilingTools.profile("tokenInit", "Calling tokenInit for RPS CIA2.SOPIN");
        NetIdConnector.initToken(signtoken.getSlot(), "RPS CIA2.SOPIN", tokenlabel, pUK, currentSignaturePUK);
        ProfilingTools.profile("tokenInit", "tokenInit for RPS CIA2.SOPIN Done");
        NetIdConnector.editIIDIniEntry("Number", "", "RPS CIA1.SOPIN");
        NetIdConnector.editIIDIniEntry("Number", "", "RPS CIA2.SOPIN");
        ProfilingTools.profile("tokenInit", "Calling initPIN for RPS CIA1");
        initPIN(basictoken, currentBasicPUK, basicPIN);
        ProfilingTools.profile("tokenInit", "initPIN for RPS CIA1.SOPIN Done");
        ProfilingTools.profile("tokenInit", "Calling initPIN for RPS CIA2");
        initPIN(signtoken, currentSignaturePUK, signaturePIN);
        ProfilingTools.profile("tokenInit", "initPIN for RPS CIA2.SOPIN Done");
        ProfilingTools.profile("tokenInit", "Calling changeSOPIN for RPS CIA1");
        changeSOPIN(basictoken, currentBasicPUK, pUK);
        ProfilingTools.profile("tokenInit", "changeSOPIN for RPS CIA1.SOPIN Done");
        ProfilingTools.profile("tokenInit", "Calling changeSOPIN for RPS CIA2");
        changeSOPIN(signtoken, currentSignaturePUK, pUK);
        ProfilingTools.profile("tokenInit", "changeSOPIN for RPS CIA2.SOPIN Done");
        ProfilingTools.profile("tokenInit", "Calling initToken for RPS CIA1.USERPIN");
        NetIdConnector.initTokenWithUserPIN(basictoken, "RPS CIA1.USERPIN", basicPIN);
        ProfilingTools.profile("tokenInit", "tokenInit for RPS CIA1.USERPIN Done");
        ProfilingTools.profile("tokenInit", "Calling initToken for RPS CIA2.USERPIN");
        NetIdConnector.initTokenWithUserPIN(signtoken, "RPS CIA2.USERPIN", signaturePIN);
        ProfilingTools.profile("tokenInit", "tokenInit for RPS CIA2.USERPIN Done");
        InterfaceFactory.getTokenManager().flushTokenCache();
        InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
        super.init(true, basictoken);
        try {
            ProfilingTools.profile("tokenInit", "Generating 2048 bit RSA key RPS CIA1");
            genKey(IToken.PINTYPE_BASIC, basicPIN, basicPIN, IToken.KEYTYPE_AUTH, IToken.KEYALG_RSA, 2048, "key aut + enc");
            ProfilingTools.profile("tokenInit", "2048 bit RSA key RPS CIA1 Generated");
            ProfilingTools.profile("tokenInit", "Generating 2048 bit RSA key RPS CIA2");
            genKey(IToken.PINTYPE_SIGN, signaturePIN, basicPIN, IToken.KEYTYPE_SIGN, IToken.KEYALG_RSA, 2048, "key sign");
            ProfilingTools.profile("tokenInit", "2048 bit RSA key RPS CIA2 Generated");
        } catch (ObjectAlreadyExistsException e) {
            throw new TokenException(e);
        }
        ProfilingTools.profile("tokenInit", "Token initialized.");
    }

    /**
	 * Method that overloads the standard version and uses 
	 * native calls instead.
	 */
    @Override
    public PINInfo unblockPIN(String pintype, String puk, String newpin) throws OperationNotSupportedException, TokenException {
        PINInfo retval = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            NetIdConnector.unBlockPIN(session, token.getSlot(), newpin, puk);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        retval = getPINInfo(pintype);
        return retval;
    }

    /**
	 * Method to clear the content of a SetCos 441 card
	 * 
	 */
    public void clearToken(String[] pintypes, String[] puks) throws OperationNotSupportedException, TokenException {
        String basicPUK = null;
        String sigPUK = null;
        if (pintypes[0].equals(PINTYPE_BASIC)) {
            basicPUK = puks[0];
            sigPUK = puks[1];
        } else {
            sigPUK = puks[0];
            basicPUK = puks[1];
        }
        if (basicPUK == null) {
            throw new TokenException("Error trying to fetch PUK data in cleartoken operation");
        }
        String initBasicPIN = InterfaceFactory.getGlobalSettings().getTokenInitialBasicPIN();
        if (initBasicPIN == null) {
            throw new OperationNotSupportedException("Error, no init basic PIN setting have been defined in global.properties");
        }
        String initSigPIN = InterfaceFactory.getGlobalSettings().getTokenInitialSignaturePIN();
        if (initSigPIN == null) {
            throw new OperationNotSupportedException("Error, no init sigature PIN setting have been defined in global.properties");
        }
        Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        Token signtoken = (Token) tokens.get(IToken.PINTYPE_SIGN);
        ProfilingTools.initProfiling("clearToken");
        ProfilingTools.profile("clearToken", "Calling initPIN for RPS CIA1");
        initPIN(basictoken, basicPUK, initBasicPIN);
        ProfilingTools.profile("clearToken", "initPIN for RPS CIA1 Done");
        ProfilingTools.profile("clearToken", "Calling initPIN for RPS CIA2");
        initPIN(signtoken, sigPUK, initSigPIN);
        ProfilingTools.profile("clearToken", "initPIN for RPS CIA2 Done");
        ProfilingTools.profile("clearToken", "Calling initToken for RPS CIA1.USERPIN");
        NetIdConnector.initTokenWithUserPIN(basictoken, "RPS CIA1.USERPIN ERASEALL", initBasicPIN);
        ProfilingTools.profile("clearToken", "initToken for RPS CIA1.USERPIN Done");
        ProfilingTools.profile("clearToken", "Calling initToken for RPS CIA2.USERPIN");
        NetIdConnector.initTokenWithUserPIN(signtoken, "RPS CIA2.USERPIN ERASEALL", initSigPIN);
        ProfilingTools.profile("clearToken", "initToken for RPS CIA2.USERPIN Done");
        InterfaceFactory.getTokenManager().flushTokenCache();
        InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
        super.init(true, basictoken);
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

    protected String getPINLabel(Token token, String pintype) throws OperationNotSupportedException, TokenException {
        String retval = null;
        String tokenLabel = token.getTokenInfo().getLabel().trim();
        for (int i = 0; i < SUPPORTEDLABELS.length; i++) {
            if (tokenLabel.startsWith(SUPPORTEDLABELS[i])) {
                if (pintype.equals(IToken.PINTYPE_BASIC)) {
                    retval = PINLABELMAPPER[i][0];
                }
                if (pintype.equals(IToken.PINTYPE_SIGN)) {
                    retval = PINLABELMAPPER[i][1];
                }
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
        String retval = null;
        if (InterfaceFactory.getGlobalSettings().getPropertyAsBoolean("token.gemaltoclassicclient.testmode", false)) {
            try {
                retval = getDefaultPUK(pintype);
            } catch (TokenException e) {
                LocalLog.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            if (generatedPUK == null) {
                String ret = "";
                Random rand = new Random();
                while (ret.length() < 8) {
                    ret += "" + rand.nextInt(10);
                }
                generatedPUK = ret.substring(ret.length() - 8);
            }
            retval = generatedPUK;
        }
        return retval;
    }

    private String generatedPUK = null;

    /**
	 * @throws TokenException 
	 * @see org.hardtokenmgmt.core.token.IToken#getInitializationRequirements()
	 */
    public InitializationRequirements getInitializationRequirements() throws TokenException {
        return InitializationRequirements.DEFAULTSOPINFORNEWCARDS;
    }

    private void initPIN(Token token, String puk, String newpin) throws OperationNotSupportedException, TokenException {
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            NetIdConnector.unBlockPIN(session, token.getSlot(), newpin, puk);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    @Override
    public PINInfo changePUK(String pintype, String oldpuk, String newpuk) throws OperationNotSupportedException, TokenException {
        Token token = (Token) tokens.get(pintype);
        changeSOPIN(token, oldpuk, newpuk);
        if (!verifySSOPINWithAPDU(pintype, token.getSlot().getSlotInfo().getSlotDescription(), newpuk)) {
            throw new TokenException("Error new PUK not set correctly, aborting initialisation.");
        }
        return getPINInfo(pintype);
    }

    private void changeSOPIN(Token token, String currentSOPIN, String newSOPIN) throws OperationNotSupportedException, TokenException {
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            try {
                session.logout();
            } catch (PKCS11Exception e) {
            }
            session.login(Session.UserType.SO, currentSOPIN.toCharArray());
            session.setPIN(currentSOPIN.toCharArray(), newSOPIN.toCharArray());
            session.logout();
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /**
	    *  Removes all keys for a keytype.
	    */
    @SuppressWarnings("unchecked")
    protected void removeAllKeys(String pintype, String pin, String basicpin) throws OperationNotSupportedException, TokenException {
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session.login(UserType.USER, pin.toCharArray());
            }
            session.findObjectsInit(new PrivateKey());
            Object[] objects = session.findObjects(MAXNUMOBJECTS);
            try {
                for (int i = 0; i < objects.length; i++) {
                    if (objects[i] instanceof PrivateKey) {
                        Key tokenkey = (Key) objects[i];
                        Hashtable attributes = tokenkey.getAttributeTable();
                        CharArrayAttribute labelAttribute = (CharArrayAttribute) attributes.get(Attribute.LABEL);
                        if (labelAttribute.getCharArrayValue() != null) {
                            session.destroyObject(objects[i]);
                        }
                    }
                }
            } finally {
                session.findObjectsFinal();
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
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

    public String getDefaultPUK(String pintype) throws TokenException {
        String defaultPUK = InterfaceFactory.getGlobalSettings().getProperty("token.gemaltoclassicclient.defaultpuk");
        if (defaultPUK == null) {
            throw new TokenException("Error initializing Gemalto Classic Client Card, no default PUK specified, check setting 'token.gemaltoclassicclient.defaultpuk' in global configuration.");
        }
        return defaultPUK;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#downloadCert(String, String, String, String, X509Certificate)
	 */
    public void downloadCert(String label, String pintype, String pin, String basicpin, X509Certificate cert) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
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
            session.closeSession();
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session.login(UserType.USER, basicpin.toCharArray());
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
            DERInteger dERInteger = new DERInteger(cert.getSerialNumber());
            byte[] serialNumber = dERInteger.getDEREncoded();
            try {
                X509PublicKeyCertificate certObject = new X509PublicKeyCertificate();
                certObject.getToken().setBooleanValue(Boolean.TRUE);
                certObject.getPrivate().setBooleanValue(Boolean.FALSE);
                certObject.getLabel().setCharArrayValue(label.toCharArray());
                certObject.getId().setByteArrayValue(newObjectID);
                certObject.getSubject().setByteArrayValue(cert.getSubjectDN().getName().getBytes());
                certObject.getIssuer().setByteArrayValue(cert.getIssuerDN().getName().getBytes());
                certObject.getSerialNumber().setByteArrayValue(serialNumber);
                certObject.getValue().setByteArrayValue(cert.getEncoded());
                session.createObject(certObject);
            } catch (CertificateEncodingException e) {
                throw new TokenException("Error in certificate encoding");
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        clearCertificateCache();
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#removeCertificate(String, String, String, X509Certificate)
	 */
    public void removeCertificate(String pintype, String pin, String basicpin, X509Certificate cert) throws OperationNotSupportedException, TokenException {
        clearCertificateCache();
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        try {
            boolean found = false;
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session.login(UserType.USER, pin.toCharArray());
            }
            String certFingerprint = CertUtils.getFingerprintAsString(cert);
            session.findObjectsInit(new X509PublicKeyCertificate());
            Object[] objects = session.findObjects(MAXNUMOBJECTS);
            try {
                for (int i = 0; i < objects.length; i++) {
                    X509PublicKeyCertificate tokencert = (X509PublicKeyCertificate) objects[i];
                    try {
                        Hashtable<?, ?> attributes = tokencert.getAttributeTable();
                        ByteArrayAttribute valueAttribute = (ByteArrayAttribute) attributes.get(Attribute.VALUE);
                        byte[] value = valueAttribute.getByteArrayValue();
                        if (value != null) {
                            String thisCertFingerprint = CertUtils.getFingerprintAsString(CertUtils.getCertfromByteArray(value));
                            if (certFingerprint.equals(thisCertFingerprint)) {
                                found = true;
                                session.destroyObject(objects[i]);
                            }
                        }
                    } catch (CertificateException e) {
                        LocalLog.getLogger().log(Level.WARNING, "Corrupt certificate on token");
                        LocalLog.debug(e);
                    }
                }
            } finally {
                session.findObjectsFinal();
            }
            if (!found) {
                throw new TokenException("Error Certificate couldn't be found in the token.");
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#getPINInfo(String)
	 */
    @Override
    public PINInfo getPINInfo(String pintype) throws OperationNotSupportedException, TokenException {
        PINInfo retval = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        int[] pinCounters = getPINAttemptsLeft(token.getSlot().getSlotInfo().getSlotDescription());
        int pinCounter = 0;
        if (pintype.equals(IToken.PINTYPE_BASIC)) {
            pinCounter = pinCounters[PINATTEMPTS_BASIC];
        } else {
            pinCounter = pinCounters[PINATTEMPTS_SIGNATURE];
        }
        retval = new PINInfo(token.getTokenInfo().isLoginRequired(), pinCounter == 2, pinCounter == 1, pinCounter == 0, false, false, false);
        return retval;
    }

    private static final int PINATTEMPTS_BASIC = 0;

    private static final int PINATTEMPTS_SIGNATURE = 1;

    /**
     * Method that communicates through the
     * @param readerName the name of the reader the slot is connected to
     * @return an array of two integers containing the number of attempts left, first
     * integer is for basic PIN and second for basic PUK.
     * @throws TokenException if communication problems occur with the card.
     */
    private static int[] getPINAttemptsLeft(String readerName) throws TokenException {
        int[] retval = new int[2];
        try {
            TerminalFactory tf = TerminalFactory.getDefault();
            CardTerminals cts = tf.terminals();
            for (CardTerminal ct : cts.list()) {
                if (readerName.startsWith(ct.getName())) {
                    Card card = ct.connect("T=0");
                    CardChannel cc = null;
                    cc = card.getBasicChannel();
                    CommandAPDU selectClassicAppletCommand = genCommand("00 A4 04 00 0C A0 00 00 00 18 0C 00 00 01 63 42 00");
                    ResponseAPDU response = cc.transmit(selectClassicAppletCommand);
                    CommandAPDU selectMFCommand = genCommand("00 A4 00 00 02 3F 00");
                    response = cc.transmit(selectMFCommand);
                    CommandAPDU selectDF5000Command = genCommand("00 A4 00 00 02 50 00");
                    response = cc.transmit(selectDF5000Command);
                    CommandAPDU verifyPin1Command = genCommand("00 20 00 81 00");
                    response = cc.transmit(verifyPin1Command);
                    verifyResponse(response, null, "63", "*");
                    retval[PINATTEMPTS_BASIC] = response.getSW2() ^ 0xC0;
                    CommandAPDU selectDF5100Command = genCommand("00 A4 00 00 02 51 00");
                    response = cc.transmit(selectDF5100Command);
                    CommandAPDU verifyPin2Command = genCommand("00 20 00 83 00");
                    response = cc.transmit(verifyPin2Command);
                    verifyResponse(response, null, "63", "*");
                    retval[PINATTEMPTS_SIGNATURE] = response.getSW2() ^ 0xC0;
                }
            }
        } catch (CardException e) {
            throw new TokenException("Error sending APDU commands to card: " + e.getMessage(), e);
        }
        return retval;
    }

    /**
     * Method that communicates through the
     * @param readerName the name of the reader the slot is connected to
     * @return an array of two integers containing the number of attempts left, first
     * integer is for basic PIN and second for basic PUK.
     * @throws TokenException if communication problems occur with the card.
     */
    public static boolean verifySSOPINWithAPDU(String pinType, String readerName, String PUK) throws TokenException {
        boolean retval = false;
        if (PUK == null) {
            throw new TokenException("Error verifying SSO PIN, PUK code cannot be null");
        }
        if (PUK.length() > 8) {
            throw new TokenException("Error verifying SSO PIN, PUK code must be between 4 and 8 characters");
        }
        char[] pukChars = PUK.toCharArray();
        String pukEncodedString = "";
        for (int i = 0; i < pukChars.length; i++) {
            pukEncodedString += " " + Integer.toString((int) pukChars[i], 16);
        }
        for (int i = pukChars.length; i < 16; i++) {
            pukEncodedString += " 00";
        }
        try {
            TerminalFactory tf = TerminalFactory.getDefault();
            CardTerminals cts = tf.terminals();
            for (CardTerminal ct : cts.list()) {
                if (readerName.startsWith(ct.getName())) {
                    Card card = ct.connect("T=0");
                    CardChannel cc = null;
                    cc = card.getBasicChannel();
                    CommandAPDU selectClassicAppletCommand = genCommand("00 A4 04 00 0C A0 00 00 00 18 0C 00 00 01 63 42 00");
                    ResponseAPDU response = cc.transmit(selectClassicAppletCommand);
                    CommandAPDU selectMFCommand = genCommand("00 A4 00 00 02 3F 00");
                    response = cc.transmit(selectMFCommand);
                    if (pinType.equals(IToken.PINTYPE_BASIC)) {
                        CommandAPDU selectDF5000Command = genCommand("00 A4 00 00 02 50 00");
                        response = cc.transmit(selectDF5000Command);
                        CommandAPDU verifyPin1Command = genCommand("00 20 00 82 10" + pukEncodedString);
                        response = cc.transmit(verifyPin1Command);
                        try {
                            verifyResponse(response, null, "90", "00");
                            retval = true;
                        } catch (TokenException e) {
                        }
                    }
                    if (pinType.equals(IToken.PINTYPE_SIGN)) {
                        CommandAPDU selectDF5100Command = genCommand("00 A4 00 00 02 51 00");
                        response = cc.transmit(selectDF5100Command);
                        CommandAPDU verifyPin2Command = genCommand("00 20 00 84 10" + pukEncodedString);
                        response = cc.transmit(verifyPin2Command);
                        try {
                            verifyResponse(response, null, "90", "00");
                            retval = true;
                        } catch (TokenException e) {
                        }
                    }
                }
            }
        } catch (CardException e) {
            throw new TokenException("Error sending APDU commands to card: " + e.getMessage(), e);
        }
        return retval;
    }

    /**
	 * Method that generates a Command APDU from a space separated hex encoded byte string.
	 * <br>
	 * Ex: "00 20 12 13 14 15 A1"
	 * 
	 * The string should contain the class, ins, p1 and p2
	 * @param commandString string to parse, never null.
	 * @return a Command APDU used to send to the card.
	 */
    private static CommandAPDU genCommand(String commandString) {
        String[] byteStrings = commandString.split(" ");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String byteString : byteStrings) {
            baos.write(Integer.parseInt(byteString, 16));
        }
        return new CommandAPDU(baos.toByteArray());
    }

    /**
	 * Checks that the appropriate response is returned from the card.
	 * 
	 * @param response the  response from the card after sending a command.
	 * @param expectedResponse space separated hex formated string containing the expected response, use '*' in a byte
	 * position to match any byte, ex "0C * 13 14", null means any response data match.
	 * @param expectedSW1 the expected SW1 value in hex, use "*" for any value, never null.
	 * @param expectedSW2 the expected SW2 value in hex, use "*" for any value, never null.
	 * @throws TokenException if inexpected response was returned.
	 */
    private static void verifyResponse(ResponseAPDU response, String expectedResponse, String expectedSW1, String expectedSW2) throws TokenException {
        if (!expectedSW1.equals("*")) {
            int eSW1 = Integer.parseInt(expectedSW1, 16);
            if (eSW1 != response.getSW1()) {
                throw new TokenException("Unexpected response from token");
            }
        }
        if (!expectedSW2.equals("*")) {
            int eSW2 = Integer.parseInt(expectedSW2, 16);
            if (eSW2 != response.getSW2()) {
                throw new TokenException("Unexpected response from token");
            }
        }
        if (expectedResponse == null) {
            return;
        }
        if (expectedResponse.trim().equals("") && response.getData().length == 0) {
            return;
        }
        byte[] rData = response.getData();
        String[] byteStrings = expectedResponse.split(" ");
        System.out.println(new String(Hex.encode(rData)));
        if (rData.length != byteStrings.length) {
            throw new TokenException("Unexpected response from token");
        }
        for (int i = 0; i < rData.length; i++) {
            if (!byteStrings[i].trim().equals("*")) {
                int expected = Integer.parseInt(byteStrings[i], 16);
                if (expected != rData[i]) {
                    throw new TokenException("Unexpected response from token");
                }
            }
        }
    }
}
