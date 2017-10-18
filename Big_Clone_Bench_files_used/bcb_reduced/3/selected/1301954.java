package org.hardtokenmgmt.core.token;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.MechanismInfo;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.State;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.Session.UserType;
import iaik.pkcs.pkcs11.Token.SessionReadWriteBehavior;
import iaik.pkcs.pkcs11.Token.SessionType;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.CharArrayAttribute;
import iaik.pkcs.pkcs11.objects.Data;
import iaik.pkcs.pkcs11.objects.Key;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.PrivateKey;
import iaik.pkcs.pkcs11.objects.PublicKey;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.RSAPublicKey;
import iaik.pkcs.pkcs11.objects.Storage;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.hardtokenmgmt.core.InterfaceFactory;
import org.hardtokenmgmt.core.log.LocalLog;
import org.hardtokenmgmt.core.util.CertUtils;
import org.hardtokenmgmt.core.util.PKCS11CertificationRequest;
import org.hardtokenmgmt.core.util.UserDataGenerator;

/**
 * A Base Token implementation that contains generic operations on
 * token that can be reused by most token implementations
 * 
 * @author Philip Vendil 2006-aug-30
 *
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public abstract class BaseToken implements IToken {

    /**
     * @throws TokenException 
     * @see IToken#getHardTokenSN()
     */
    public String getHardTokenSN() throws TokenException {
        Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        String serialNumber = basictoken.getSlot().getToken().getTokenInfo().getSerialNumber().trim();
        if (!InterfaceFactory.getGlobalSettings().getPropertyAsBoolean("token.useprefix", true)) {
            if (serialNumber.length() > 8) {
                serialNumber = serialNumber.substring(serialNumber.length() - 9, serialNumber.length() - 1);
            }
        }
        return serialNumber;
    }

    /**
	 * Constant indicating maximum of certificates that can be stored
	 */
    protected int MAXNUMOBJECTS = 16;

    private boolean useVirtualSlots = false;

    private long slotId = 0;

    private HashMap<String, List<X509Certificate>> certificateCache = new HashMap<String, List<X509Certificate>>();

    private HashMap<String, List<String>> certificateLabelCache = new HashMap<String, List<String>>();

    protected HashMap<String, Token> tokens = new HashMap<String, Token>();

    /**
	 * @throws TokenException 
	 * @see org.hardtokenmgmt.core.token.IToken#init(boolean, Token)
	 */
    public void init(boolean useVirtualSlots, Token token) throws TokenException {
        this.useVirtualSlots = useVirtualSlots;
        slotId = token.getSlot().getSlotID();
        if (useVirtualSlots) {
            String tokenSerial = token.getTokenInfo().getSerialNumber();
            if (isInitialized(token)) {
                String basicLabel = null;
                String signatureLabel = null;
                try {
                    basicLabel = getPINLabel(token, IToken.PINTYPE_BASIC);
                } catch (OperationNotSupportedException e) {
                    throw new TokenException("Error token not supported ", e);
                }
                try {
                    signatureLabel = getPINLabel(token, IToken.PINTYPE_SIGN);
                } catch (OperationNotSupportedException e) {
                }
                Collection availableSlots = InterfaceFactory.getTokenManager().getSlots(TokenManager.SLOTTYPE_ALL);
                Iterator iter = availableSlots.iterator();
                while (iter.hasNext()) {
                    Slot slot = (Slot) iter.next();
                    String tokenLabel = slot.getToken().getTokenInfo().getLabel().trim();
                    if (tokenSerial.equals(slot.getToken().getTokenInfo().getSerialNumber())) {
                        if (tokenLabel.endsWith(basicLabel)) {
                            tokens.put(IToken.PINTYPE_BASIC, slot.getToken());
                        }
                        if (tokenLabel.endsWith(signatureLabel)) {
                            tokens.put(IToken.PINTYPE_SIGN, slot.getToken());
                        }
                    }
                }
            } else {
                tokens.put(IToken.PINTYPE_BASIC, token);
            }
        } else {
            tokens.put(IToken.PINTYPE_BASIC, token);
        }
    }

    /**
	 * Method that should return true if the token is currently initialized.
	 * @return method that should return true if the token is currently initialized.
	 */
    protected abstract boolean isInitialized(Token token) throws TokenException;

    /**
	 * Method that should return the label of the tokens
	 * virtual slot that is associated with the given pin type.
	 * <p>
	 * Only the last significant characters is necessary that distinguishes the virutal slot.
	 * 
	 * @param pintype
	 * @return The label of the token in the slot
	 */
    protected abstract String getPINLabel(Token token, String pintype) throws OperationNotSupportedException, TokenException;

    /**
	 * Method that should find the right key label given the keytype.
	 * 
	 * @param keytype the keytype to find
	 */
    protected abstract String getPrivateKeyLabel(String keytype) throws OperationNotSupportedException;

    protected PrivateKey findPrivateKey(Session session, String keytype) throws OperationNotSupportedException, TokenException {
        String keylabel = getPrivateKeyLabel(keytype);
        session.findObjectsInit(new RSAPrivateKey());
        Object[] objects = session.findObjects(MAXNUMOBJECTS);
        try {
            for (int i = 0; i < objects.length; i++) {
                RSAPrivateKey tokenkey = (RSAPrivateKey) objects[i];
                if (keylabel.equals(new String(tokenkey.getLabel().getCharArrayValue()))) {
                    return tokenkey;
                }
            }
            throw new TokenException("Error Key " + keytype + " doesn't seem to exist on token");
        } finally {
            session.findObjectsFinal();
        }
    }

    protected void findObjects(Session session) throws OperationNotSupportedException, TokenException {
        session.findObjectsInit(new Object());
        Object[] objects = session.findObjects(30);
        try {
            for (int i = 0; i < objects.length; i++) {
                Object object = (Object) objects[i];
                object.readAttributes(session);
                System.out.println(" Object found : " + object.toString());
            }
        } finally {
            session.findObjectsFinal();
        }
    }

    protected PublicKey findPublicKey(Session session, String keytype, UserDataGenerator userDataGenerator) throws OperationNotSupportedException, TokenException {
        PublicKey retval = null;
        List<String> keylabels = getPublicKeyLabels(keytype, userDataGenerator);
        session.findObjectsInit(new RSAPublicKey());
        Object[] objects = session.findObjects(MAXNUMOBJECTS);
        try {
            for (int i = 0; i < objects.length; i++) {
                RSAPublicKey tokenkey = (RSAPublicKey) objects[i];
                tokenkey.readAttributes(session);
                BigInteger modulus = new BigInteger(1, tokenkey.getModulus().getByteArrayValue());
                BigInteger publicExponent = new BigInteger(1, tokenkey.getPublicExponent().getByteArrayValue());
                RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    keyFactory.generatePublic(rsaPublicKeySpec);
                } catch (Exception e) {
                    throw new TokenException(e);
                }
                if (keylabels.contains(new String(tokenkey.getLabel().getCharArrayValue()))) {
                    retval = tokenkey;
                }
            }
            if (retval == null) {
                throw new TokenException("Error Key " + keytype + " doesn't seem to exist on token");
            }
        } finally {
            session.findObjectsFinal();
        }
        return retval;
    }

    /**
	 * Method returning a list of possible key labels for a public key,
	 * by default it adds the private key label but also all cert labels
	 * associated with the key.
	 * 
	 */
    protected List<String> getPublicKeyLabels(String keytype, UserDataGenerator userDataGenerator) throws OperationNotSupportedException {
        ArrayList<String> retval = new ArrayList<String>();
        retval.add(getPrivateKeyLabel(keytype));
        List<String> keyTypes = userDataGenerator.getKeyTypes();
        List<String> certLabels = userDataGenerator.getCertLabels();
        for (int i = 0; i < keyTypes.size(); i++) {
            if (keyTypes.get(i).equals(keytype)) {
                retval.add(certLabels.get(i));
            }
        }
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#genKey(String, String, String, String, String, int, String)
	 */
    public void genKey(String pintype, String pin, String basicpin, String keytype, String algorithm, int keysize, String label) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        Session session = null;
        Session basicsession = null;
        Token basictoken = null;
        if (!pintype.equals(IToken.PINTYPE_BASIC)) {
            basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
        }
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        try {
            if (algorithm.equals(IToken.KEYALG_RSA)) {
                session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
                if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                    session.login(UserType.USER, pin.toCharArray());
                }
                if (!pintype.equals(IToken.PINTYPE_BASIC)) {
                    basicsession = basictoken.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
                    if (!basicsession.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                        basicsession.login(UserType.USER, basicpin.toCharArray());
                    }
                }
                RSAPublicKey rsaPublicKeyTemplate = new RSAPublicKey();
                RSAPrivateKey rsaPrivateKeyTemplate = new RSAPrivateKey();
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
                rsaPublicKeyTemplate.getEncrypt().setBooleanValue(new Boolean(signatureMechanismInfo.isEncrypt()));
                rsaPublicKeyTemplate.getDerive().setBooleanValue(new Boolean(signatureMechanismInfo.isDerive()));
                rsaPublicKeyTemplate.getWrap().setBooleanValue(new Boolean(signatureMechanismInfo.isWrap()));
                rsaPublicKeyTemplate.getLabel().setCharArrayValue(label.toCharArray());
                rsaPrivateKeyTemplate.getSign().setBooleanValue(new Boolean(signatureMechanismInfo.isSign()));
                rsaPrivateKeyTemplate.getSignRecover().setBooleanValue(new Boolean(signatureMechanismInfo.isSignRecover()));
                rsaPrivateKeyTemplate.getDecrypt().setBooleanValue(new Boolean(signatureMechanismInfo.isDecrypt()));
                rsaPrivateKeyTemplate.getDerive().setBooleanValue(new Boolean(signatureMechanismInfo.isDerive()));
                rsaPrivateKeyTemplate.getUnwrap().setBooleanValue(new Boolean(signatureMechanismInfo.isUnwrap()));
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
            if (basicsession != null) {
                basicsession.closeSession();
            }
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#genPKCS10(String, String, String, UserDataGenerator)
	 */
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
                session.login(UserType.USER, pin.toCharArray());
            }
            RSAPrivateKey privKey = (RSAPrivateKey) findPrivateKey(session, keytype);
            RSAPublicKey pubKey = (RSAPublicKey) findPublicKey(session, keytype, userDataGenerator);
            BigInteger modulus = new BigInteger(1, pubKey.getModulus().getByteArrayValue());
            BigInteger publicExponent = new BigInteger(1, pubKey.getPublicExponent().getByteArrayValue());
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                java.security.interfaces.RSAPublicKey javaRsaPublicKey = (java.security.interfaces.RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);
                PKCS11CertificationRequest certReq = new PKCS11CertificationRequest(session, Mechanism.RSA_PKCS, CertUtils.stringToBcX509Name("CN=notused"), javaRsaPublicKey, null, privKey);
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
	 * @see org.hardtokenmgmt.core.token.IToken#downloadCert(String, String, String, String, X509Certificate)
	 */
    public void downloadCert(String label, String pintype, String pin, String basicpin, X509Certificate cert) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        Token basetoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
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
            session = basetoken.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
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
	 * @see org.hardtokenmgmt.core.token.IToken#downloadKeyStore(String, String, String, String, KeyStore, String)
	 */
    public void downloadKeyStore(String keytype, String type, String pin, String certLabel, KeyStore keyStore, String keyStorePasswd) throws ObjectAlreadyExistsException, OperationNotSupportedException, TokenException {
        throw new OperationNotSupportedException("TODO downloadKeyStore");
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#getCertificates(String)
	 */
    public Collection getCertificates(String pintype) throws OperationNotSupportedException, TokenException {
        if (certificateCache.get(pintype) == null) {
            Session session = null;
            Token token = (Token) tokens.get(pintype);
            if (token == null) {
                throw new OperationNotSupportedException("PIN type not supported");
            }
            try {
                ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
                ArrayList<String> labels = new ArrayList<String>();
                session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RO_SESSION, null, null);
                session.findObjectsInit(new X509PublicKeyCertificate());
                Object[] objects = session.findObjects(MAXNUMOBJECTS);
                try {
                    for (int i = 0; i < objects.length; i++) {
                        X509PublicKeyCertificate tokencert = (X509PublicKeyCertificate) objects[i];
                        try {
                            Hashtable attributes = tokencert.getAttributeTable();
                            ByteArrayAttribute valueAttribute = (ByteArrayAttribute) attributes.get(Attribute.VALUE);
                            byte[] value = valueAttribute.getByteArrayValue();
                            if (value != null) {
                                byte[] valueCopy = new byte[value.length];
                                for (int j = 0; j < value.length; j++) {
                                    valueCopy[j] = value[j];
                                }
                                X509Certificate cert = CertUtils.getCertfromByteArray(valueCopy);
                                if (cert != null) {
                                    certs.add(cert);
                                }
                            }
                            CharArrayAttribute labelArray = (CharArrayAttribute) attributes.get(Attribute.LABEL);
                            if (labelArray == null || labelArray.getCharArrayValue() == null) {
                                labels.add("");
                            } else {
                                labels.add(new String(labelArray.getCharArrayValue()));
                            }
                        } catch (CertificateException e) {
                            LocalLog.getLogger().log(Level.WARNING, "Corrupt certificate on token");
                            LocalLog.debug(e);
                        }
                    }
                } finally {
                    session.findObjectsFinal();
                }
                certificateCache.put(pintype, certs);
                certificateLabelCache.put(pintype, labels);
            } catch (PKCS11Exception e) {
                throw new TokenException(e.getMessage(), e);
            } finally {
                if (session != null) {
                    session.closeSession();
                }
            }
        }
        return certificateCache.get(pintype);
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#getCertificate(String)
	 */
    public X509Certificate getCertificate(String certificateLabel) throws OperationNotSupportedException, TokenException {
        X509Certificate retval = null;
        String trimmedCertificateLabel = certificateLabel.trim();
        String[] supportPINTypes = getSupportedPINTypes();
        for (int i = 0; i < supportPINTypes.length; i++) {
            getCertificates(supportPINTypes[i]);
            List<String> labels = certificateLabelCache.get(supportPINTypes[i]);
            for (int j = 0; j < labels.size(); j++) {
                if (labels.get(j).equals(trimmedCertificateLabel)) {
                    retval = certificateCache.get(supportPINTypes[i]).get(j);
                    break;
                }
            }
            if (retval != null) {
                break;
            }
        }
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#removeCertificate(String, String, String, X509Certificate)
	 */
    public void removeCertificate(String pintype, String pin, String basicpin, X509Certificate cert) throws OperationNotSupportedException, TokenException {
        clearCertificateCache();
        Session session = null;
        Session session2 = null;
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
            Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
            session2 = basictoken.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session2.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session2.login(UserType.USER, basicpin.toCharArray());
            }
            String certFingerprint = CertUtils.getFingerprintAsString(cert);
            session.findObjectsInit(new X509PublicKeyCertificate());
            Object[] objects = session.findObjects(MAXNUMOBJECTS);
            try {
                for (int i = 0; i < objects.length; i++) {
                    X509PublicKeyCertificate tokencert = (X509PublicKeyCertificate) objects[i];
                    try {
                        Hashtable attributes = tokencert.getAttributeTable();
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
            if (session2 != null) {
                session2.closeSession();
            }
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#addObject(String pintype, String pin, IObject object)
	 */
    public void addObject(String pintype, String pin, IObject object) throws OperationNotSupportedException, TokenException {
        if (object instanceof DataObject) {
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
                session.findObjectsInit(object.getPKCS11Object());
                Object[] objects = session.findObjects(MAXNUMOBJECTS);
                try {
                    for (int i = 0; i < objects.length; i++) {
                        DataObject dataObject = (DataObject) object;
                        if (new String(((Storage) objects[i]).getLabel().getCharArrayValue()).equals(((DataObject) object).getLabel())) {
                            throw new TokenException("Error Data Object with label : " + dataObject.getLabel() + " already exists.");
                        }
                    }
                } finally {
                    session.findObjectsFinal();
                }
                session.createObject(object.getPKCS11Object());
            } catch (PKCS11Exception e) {
                throw new TokenException(e.getMessage(), e);
            } finally {
                if (session != null) {
                    session.closeSession();
                }
            }
        } else {
            throw new OperationNotSupportedException("Invalid Object only data and domain parameters can be added");
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#getObjects(String, String, String)
	 */
    public Collection getObjects(String pintype, String pin, String objectType) throws OperationNotSupportedException, TokenException {
        ArrayList retval = new ArrayList();
        if (objectType.equals(IToken.OBJECTTYPE_DATA)) {
            Session session = null;
            Token token = (Token) tokens.get(pintype);
            if (token == null) {
                throw new OperationNotSupportedException("PIN type not supported");
            }
            Object objectTemplate = new Data();
            try {
                session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RO_SESSION, null, null);
                if (!session.getSessionInfo().getState().toString().equals(State.RO_USER_FUNCTIONS.toString())) {
                    session.login(UserType.USER, pin.toCharArray());
                }
                session.findObjectsInit(objectTemplate);
                Object[] objects = session.findObjects(MAXNUMOBJECTS);
                try {
                    for (int i = 0; i < objects.length; i++) {
                        retval.add(new DataObject((Data) objects[i]));
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
        } else {
            throw new OperationNotSupportedException("Invalid Object only data and domain parameters can be added");
        }
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#removeObject(String, String, String, IObject)
	 */
    public void removeObject(String pintype, String pin, String basicpin, IObject object) throws OperationNotSupportedException, TokenException {
        clearCertificateCache();
        if (object instanceof DataObject) {
            Session session = null;
            Session session2 = null;
            Token token = (Token) tokens.get(pintype);
            if (token == null) {
                throw new OperationNotSupportedException("PIN type not supported");
            }
            try {
                session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
                if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                    session.login(UserType.USER, pin.toCharArray());
                }
                Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
                session2 = basictoken.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
                if (!session2.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                    session2.login(UserType.USER, basicpin.toCharArray());
                }
                session.findObjectsInit(object.getPKCS11Object());
                Object[] objects = session.findObjects(MAXNUMOBJECTS);
                try {
                    for (int i = 0; i < objects.length; i++) {
                        if (new String(((Storage) objects[i]).getLabel().getCharArrayValue()).equals(((DataObject) object).getLabel())) {
                            if (pintype.equals(IToken.PINTYPE_SIGN)) {
                                session2.destroyObject(objects[i]);
                            } else {
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
        } else {
            throw new OperationNotSupportedException("Invalid Object only data and domain parameters can be removed");
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#getPINInfo(String)
	 */
    public PINInfo getPINInfo(String pintype) throws OperationNotSupportedException, TokenException {
        PINInfo retval = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        retval = new PINInfo(token.getTokenInfo().isLoginRequired(), token.getTokenInfo().isUserPinCountLow(), token.getTokenInfo().isUserPinFinalTry(), token.getTokenInfo().isUserPinLocked(), token.getTokenInfo().isSoPinCountLow(), token.getTokenInfo().isSoPinFinalTry(), token.getTokenInfo().isSoPinLocked());
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#changePIN(String, String, String)
	 */
    public PINInfo changePIN(String pintype, String oldpin, String newpin) throws OperationNotSupportedException, TokenException {
        PINInfo retval = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            try {
                if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                    session.login(UserType.USER, oldpin.toCharArray());
                }
                session.setPIN(oldpin.toCharArray(), newpin.toCharArray());
            } catch (PKCS11Exception e) {
                LocalLog.debug(e);
            }
        } finally {
            if (session != null) {
                if (session.getSessionInfo().getState() == State.RW_USER_FUNCTIONS) {
                    session.closeSession();
                }
            }
        }
        reInitToken();
        retval = new PINInfo(token.getTokenInfo().isLoginRequired(), token.getTokenInfo().isUserPinCountLow(), token.getTokenInfo().isUserPinFinalTry(), token.getTokenInfo().isUserPinLocked(), token.getTokenInfo().isSoPinCountLow(), token.getTokenInfo().isSoPinFinalTry(), token.getTokenInfo().isSoPinLocked());
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#unlockPIN(String, String)
	 */
    public PINInfo unlockPIN(String pintype, String pin) throws OperationNotSupportedException, TokenException {
        PINInfo retval = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        Boolean pinLockedFromException = null;
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                try {
                    session.login(UserType.USER, pin.toCharArray());
                    retval = new PINInfo(token.getTokenInfo().isLoginRequired(), token.getTokenInfo().isUserPinCountLow(), token.getTokenInfo().isUserPinFinalTry(), token.getTokenInfo().isUserPinLocked(), token.getTokenInfo().isSoPinCountLow(), token.getTokenInfo().isSoPinFinalTry(), token.getTokenInfo().isSoPinLocked());
                } catch (PKCS11Exception e) {
                    if (e.getMessage() != null && e.getMessage().trim().equals("CKR_PIN_LOCKED")) {
                        pinLockedFromException = true;
                    } else {
                        LocalLog.debug(e);
                    }
                }
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (retval == null) {
                retval = new PINInfo(token.getTokenInfo().isLoginRequired(), token.getTokenInfo().isUserPinCountLow(), token.getTokenInfo().isUserPinFinalTry(), (pinLockedFromException != null ? pinLockedFromException : token.getTokenInfo().isUserPinLocked()), token.getTokenInfo().isSoPinCountLow(), token.getTokenInfo().isSoPinFinalTry(), token.getTokenInfo().isSoPinLocked());
            }
            if (session != null) {
                session.closeSession();
            }
        }
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#unblockPIN(String, String, String)
	 */
    public PINInfo unblockPIN(String pintype, String puk, String newpin) throws OperationNotSupportedException, TokenException {
        PINInfo retval = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        Session session = null;
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            try {
                if (!session.getSessionInfo().getState().toString().equals(State.RW_SO_FUNCTIONS.toString())) {
                    session.login(UserType.SO, puk.toCharArray());
                }
                session.initPIN(newpin.toCharArray());
            } catch (PKCS11Exception e) {
                LocalLog.debug(e);
            }
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        reInitToken();
        retval = new PINInfo(token.getTokenInfo().isLoginRequired(), token.getTokenInfo().isUserPinCountLow(), token.getTokenInfo().isUserPinFinalTry(), token.getTokenInfo().isUserPinLocked(), token.getTokenInfo().isSoPinCountLow(), token.getTokenInfo().isSoPinFinalTry(), token.getTokenInfo().isSoPinLocked());
        return retval;
    }

    /**
    *  @see org.hardtokenmgmt.core.token.IToken#removeKey(String, String, String, String)
    */
    public void removeKey(String pintype, String pin, String basicpin, String label) throws OperationNotSupportedException, TokenException {
        Session session = null;
        Session session2 = null;
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
            Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
            session2 = basictoken.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session2.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session2.login(UserType.USER, basicpin.toCharArray());
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
                            if (label.equals(new String(labelAttribute.getCharArrayValue()).trim())) {
                                found = true;
                                session.destroyObject(objects[i]);
                            }
                        }
                    }
                }
            } finally {
                session.findObjectsFinal();
            }
            if (!found) {
                throw new TokenException("Error Key couldn't be found in the token.");
            }
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
            if (session2 != null) {
                session2.closeSession();
            }
        }
    }

    /**
	    *  Removes all keys for a keytype.
	    */
    protected void removeAllKeys(String pintype, String pin, String basicpin) throws OperationNotSupportedException, TokenException {
        Session session = null;
        Session session2 = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        try {
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session.login(UserType.USER, pin.toCharArray());
            }
            Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
            session2 = basictoken.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
            if (!session2.getSessionInfo().getState().toString().equals(State.RW_USER_FUNCTIONS.toString())) {
                session2.login(UserType.USER, basicpin.toCharArray());
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
                            if (pintype.equals(IToken.PINTYPE_SIGN)) {
                                session2.destroyObject(objects[i]);
                            } else {
                                session.destroyObject(objects[i]);
                            }
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
            if (session2 != null) {
                session2.closeSession();
            }
        }
    }

    /**
	    *  @see org.hardtokenmgmt.core.token.IToken#getKeyLabels(String)
	    */
    public Collection getKeyLabels(String pintype) throws OperationNotSupportedException, TokenException {
        Session session = null;
        Token token = (Token) tokens.get(pintype);
        if (token == null) {
            throw new OperationNotSupportedException("PIN type not supported");
        }
        try {
            ArrayList<String> retval = new ArrayList<String>();
            session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RO_SESSION, null, null);
            session.findObjectsInit(new Key());
            Object[] objects = session.findObjects(MAXNUMOBJECTS);
            try {
                for (int i = 0; i < objects.length; i++) {
                    if (objects[i] instanceof Key) {
                        Key tokenkey = (Key) objects[i];
                        Hashtable attributes = tokenkey.getAttributeTable();
                        CharArrayAttribute labelAttribute = (CharArrayAttribute) attributes.get(Attribute.LABEL);
                        if (labelAttribute.getCharArrayValue() != null) {
                            String label = new String(labelAttribute.getCharArrayValue());
                            if (!retval.contains(label)) {
                                retval.add(label);
                            }
                        }
                    }
                }
            } finally {
                session.findObjectsFinal();
            }
            return retval;
        } catch (PKCS11Exception e) {
            throw new TokenException(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /**
	 * Method used to reinitialize the token. A step that needs to be performe
	 * @throws TokenException 
	 *
	 */
    protected void reInitToken() throws TokenException {
        PKCS11Factory.getPKCS11Module().finalize(null);
        PKCS11Factory.getPKCS11Module().initialize(null);
        tokens = new HashMap<String, Token>();
        Slot[] slots = PKCS11Factory.getPKCS11Module().getSlotList(Module.SlotRequirement.ALL_SLOTS);
        Token token = null;
        for (Slot slot : slots) {
            if (slot.getSlotID() == slotId) {
                token = slot.getToken();
            }
        }
        init(useVirtualSlots, token);
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#blockPIN(String)
	 */
    public PINInfo blockPIN(String pintype) throws OperationNotSupportedException, TokenException {
        PINInfo pinInfo = getPINInfo(pintype);
        if (!pinInfo.isPINLocked()) {
            Random rand = new Random();
            Token basictoken = (Token) tokens.get(IToken.PINTYPE_BASIC);
            String dummyPIN = "";
            for (int j = 0; j < basictoken.getTokenInfo().getMaxPinLen(); j++) {
                dummyPIN += Integer.toString(rand.nextInt(9));
                if (j >= basictoken.getTokenInfo().getMinPinLen() - 1) {
                    for (int k = 0; k < 4; k++) {
                        if (!pinInfo.isPINLocked()) {
                            Token token = (Token) tokens.get(pintype);
                            if (token == null) {
                                throw new OperationNotSupportedException("PIN type not supported");
                            }
                            Session session = null;
                            try {
                                session = token.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
                                String currentState = session.getSessionInfo().getState().toString();
                                if (currentState.equals(State.RW_USER_FUNCTIONS.toString()) || currentState.equals(State.RW_SO_FUNCTIONS.toString())) {
                                    session.logout();
                                }
                                try {
                                    session.login(UserType.USER, dummyPIN.toCharArray());
                                } catch (PKCS11Exception e) {
                                    LocalLog.debug(e);
                                }
                            } catch (PKCS11Exception e) {
                                throw new TokenException(e.getMessage(), e);
                            } finally {
                                if (session != null) {
                                    session.closeSession();
                                }
                            }
                        } else {
                            break;
                        }
                        pinInfo = getPINInfo(pintype);
                    }
                    if (pinInfo.isPINLocked()) {
                        break;
                    }
                }
            }
        }
        return getPINInfo(pintype);
    }

    /**
	 * Method used to clear the internal certificate cache.
	 */
    public void clearCertificateCache() {
        certificateCache = new HashMap<String, List<X509Certificate>>();
        certificateLabelCache = new HashMap<String, List<String>>();
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#checkPIN(String, String)
	 */
    public boolean checkPIN(String pintype, String pIN) throws OperationNotSupportedException, TokenException {
        boolean retval = true;
        Token t = (Token) tokens.get(pintype);
        Session session = t.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
        try {
            try {
                if (pIN == null) {
                    session.login(UserType.USER, null);
                } else {
                    session.login(UserType.USER, pIN.toCharArray());
                }
            } catch (iaik.pkcs.pkcs11.wrapper.PKCS11Exception e) {
                if (e.getMessage() != null && e.getMessage().trim().equals("CKR_USER_ALREADY_LOGGED_IN")) {
                    try {
                        session.logout();
                        session.login(UserType.USER, pIN.toCharArray());
                    } catch (iaik.pkcs.pkcs11.wrapper.PKCS11Exception e1) {
                        LocalLog.debug(e1);
                        retval = false;
                    }
                } else {
                    retval = false;
                }
            }
            ;
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
        return retval;
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#checkPIN(String, String)
	 */
    public boolean checkSSOPIN(String pintype, String pIN) throws OperationNotSupportedException, TokenException {
        Token t = (Token) tokens.get(pintype);
        Session session = t.openSession(SessionType.SERIAL_SESSION, SessionReadWriteBehavior.RW_SESSION, null, null);
        try {
            try {
                session.login(UserType.SO, pIN.toCharArray());
                session.logout();
            } catch (iaik.pkcs.pkcs11.wrapper.PKCS11Exception e) {
                return false;
            }
            ;
            return true;
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }

    /**
	 * @see org.hardtokenmgmt.core.token.IToken#checkPIN(String, String)
	 */
    public long getSlotId(String pintype) throws OperationNotSupportedException, TokenException {
        Token t = (Token) tokens.get(pintype);
        return t.getSlot().getSlotID();
    }

    /**
	 * By default is no default PUK supported an null is returned.
	 * @param pintype will return null for all types.
	 * @return null by default is no default PUK supported an null is returned.
	 * @throws TokenException for token related failures.
	 */
    @Override
    public String getDefaultPUK(String pintype) throws TokenException {
        return null;
    }

    /**
	 * By default is this operation not supported.
	 */
    @Override
    public PINInfo changePUK(String pintype, String oldpuk, String newpuk) throws OperationNotSupportedException, TokenException {
        throw new OperationNotSupportedException("Change PUK isn't supported by token of type: " + this.getClass().getSimpleName());
    }

    /**
	 * Default isn't unblock response supported
	 * @see org.hardtokenmgmt.core.token.IToken#generateUnblockResponse(java.lang.String, java.lang.String, java.lang.String)
	 */
    @Override
    public String generateUnblockResponse(String pintype, String puk, String challenge) throws InvalidChallengeException, OperationNotSupportedException, TokenException {
        throw new OperationNotSupportedException("Challenge Response Unblock isn't supported by token: " + this.getClass().getSimpleName());
    }

    /**
	 * Default isn't unblock response supported
	 * @see org.hardtokenmgmt.core.token.IToken#isChallengeResponseUnblockSupported()
	 */
    @Override
    public boolean isChallengeResponseUnblockSupported() {
        return false;
    }
}
