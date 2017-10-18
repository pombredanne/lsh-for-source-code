package org.isodl.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.CVCertificate;
import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.FileInfo;
import net.sourceforge.scuba.smartcards.FileSystemStructured;
import net.sourceforge.scuba.tlv.BERTLVInputStream;

/**
 * Card service for reading datagroups and using the BAP, AA, and EAP protocols
 * on the driving license. Defines secure messaging (BAP). Defines Extended
 * Access Protection. Defines active authentication.
 * 
 * Based on ISO18013-3.
 * 
 * Usage:
 * 
 * <pre>
 *          &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt; doBAP(...) ==&gt; doEAP(...) ==&gt; doAA() ==&gt; close()
 * </pre>
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class DrivingLicenseService extends DrivingLicenseApduService {

    private static final long serialVersionUID = 1251224366317059401L;

    /** Data group 1 contains the mandatory driver data. */
    public static final short EF_DG1 = 0x0001;

    public static final byte SF_DG1 = 0x01;

    /** Data group 2 contains optional license holder data. */
    public static final short EF_DG2 = 0x0002;

    public static final byte SF_DG2 = 0x02;

    /** Data group 3 contains issuing authority details. */
    public static final short EF_DG3 = 0x0003;

    public static final byte SF_DG3 = 0x03;

    /** Data group 4 contains optional portrait image. */
    public static final short EF_DG4 = 0x0004;

    public static final byte SF_DG4 = 0x0004;

    /** Data group 5 contains optional signature image. */
    public static final short EF_DG5 = 0x0005;

    public static final byte SF_DG5 = 0x05;

    /** Data group 6 contains optional facial biometric template. */
    public static final short EF_DG6 = 0x0006;

    public static final byte SF_DG6 = 0x06;

    /** Data group 7 contains optional finger biometric template. */
    public static final short EF_DG7 = 0x0007;

    public static final byte SF_DG7 = 0x07;

    /** Data group 8 contains optional iris biometric template. */
    public static final short EF_DG8 = 0x0008;

    public static final byte SF_DG8 = 0x08;

    /** Data group 9 contains optional other biometric templates. */
    public static final short EF_DG9 = 0x0009;

    public static final byte SF_DG9 = 0x09;

    /** Data group 10 is RFU. */
    public static final short EF_DG10 = 0x000A;

    public static final byte SF_DG10 = 0x0A;

    /** Data group 11 contains optional domestic application data. */
    public static final short EF_DG11 = 0x000B;

    public static final byte SF_DG11 = 0x0B;

    /** Data group 12 contains non-match alert. */
    public static final short EF_DG12 = 0x000C;

    public static final byte SF_DG12 = 0x0C;

    /** Data group 13 contains active authentication (AA) certificate. */
    public static final short EF_DG13 = 0x000D;

    public static final byte SF_DG13 = 0x0D;

    /** Data group 14 contains extended access protection (EAP) public key. */
    public static final short EF_DG14 = 0x000E;

    public static final byte SF_DG14 = 0x0E;

    /** The security document. */
    public static final short EF_SOD = 0x001D;

    public static final byte SF_SOD = 0x1D;

    /**
     * File indicating which data groups are present and security mechanism
     * indicators.
     */
    public static final short EF_COM = 0x001E;

    public static final byte SF_COM = 0x1E;

    /**
     * The file read block size, some cards cannot handle large values.
     * 
     * TODO: get the read block size from the card FCI data or similar.
     * 
     * @deprecated hack
     */
    public static int maxBlockSize = 255;

    private static final int SESSION_STOPPED_STATE = 0;

    private static final int SESSION_STARTED_STATE = 1;

    private static final int BAP_AUTHENTICATED_STATE = 2;

    private static final int AA_AUTHENTICATED_STATE = 3;

    private static final int CA_AUTHENTICATED_STATE = 5;

    private static final int TA_AUTHENTICATED_STATE = 6;

    private static final int EAP_AUTHENTICATED_STATE = 7;

    private int state;

    private Collection<AuthListener> authListeners;

    private SecureMessagingWrapper wrapper;

    private Signature aaSignature;

    private MessageDigest aaDigest;

    private Cipher aaCipher;

    private Random random;

    private DrivingLicenseFileSystem fs;

    /**
     * Creates a new driving license service for accessing the license.
     * 
     * @param service
     *            another service which will deal with sending the apdus to the
     *            card.
     * 
     * @throws GeneralSecurityException
     *             when the available JCE providers cannot provide the necessary
     *             cryptographic primitives.
     */
    public DrivingLicenseService(CardService service) throws CardServiceException {
        super(service);
        try {
            aaSignature = Signature.getInstance("SHA1WithRSA/ISO9796-2");
            aaDigest = MessageDigest.getInstance("SHA1");
            aaCipher = Cipher.getInstance("RSA/NONE/NoPadding");
            random = new SecureRandom();
            authListeners = new ArrayList<AuthListener>();
            fs = new DrivingLicenseFileSystem();
        } catch (GeneralSecurityException gse) {
            throw new CardServiceException(gse.toString());
        }
        state = SESSION_STOPPED_STATE;
    }

    /**
     * Opens a session. This is done by connecting to the card, selecting the
     * driving license application.
     */
    public void open() throws CardServiceException {
        if (isOpen()) {
            return;
        }
        super.open();
        state = SESSION_STARTED_STATE;
    }

    public boolean isOpen() {
        return (state != SESSION_STOPPED_STATE);
    }

    /**
     * Performs the <i>Basic Access Protection</i> protocol.
     * 
     * @param keySeedString
     *            the document keying material ("MRZ") K_DOC, ISO18013-3 SAI.
     * 
     * @throws CardServiceException
     *             if authentication failed
     */
    public synchronized void doBAP(byte[] keySeed) throws CardServiceException {
        try {
            if (keySeed == null) {
                return;
            }
            if (keySeed.length < 16) {
                throw new IllegalStateException("Key seed too short");
            }
            SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            byte[] rndICC = sendGetChallenge(wrapper);
            byte[] rndIFD = new byte[8];
            random.nextBytes(rndIFD);
            byte[] kIFD = new byte[16];
            random.nextBytes(kIFD);
            byte[] response = sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
            byte[] kICC = new byte[16];
            System.arraycopy(response, 16, kICC, 0, 16);
            keySeed = new byte[16];
            for (int i = 0; i < 16; i++) {
                keySeed[i] = (byte) ((kIFD[i] & 0xFF) ^ (kICC[i] & 0xFF));
            }
            SecretKey ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            SecretKey ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
            wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
            BAPEvent event = new BAPEvent(this, rndICC, rndIFD, kICC, kIFD, true);
            notifyBAPPerformed(event);
            state = BAP_AUTHENTICATED_STATE;
        } catch (GeneralSecurityException gse) {
            throw new CardServiceException(gse.toString());
        }
    }

    /**
     * Adds an authentication event listener.
     * 
     * @param l
     *            listener
     */
    public void addAuthenticationListener(AuthListener l) {
        authListeners.add(l);
    }

    /**
     * Removes an authentication event listener.
     * 
     * @param l
     *            listener
     */
    public void removeAuthenticationListener(AuthListener l) {
        authListeners.remove(l);
    }

    /**
     * Notifies listeners about BAP events.
     * 
     * @param event
     *            BAP event
     */
    protected void notifyBAPPerformed(BAPEvent event) {
        for (AuthListener l : authListeners) {
            l.performedBAP(event);
        }
    }

    /**
     * Performs the <i>Active Authentication</i> protocol.
     * 
     * @param publicKey
     *            the public key to use (usually read from the card, DG13)
     * 
     * @return a boolean indicating whether the card was authenticated
     * 
     * @throws CardServiceException
     *             if something goes wrong
     */
    public boolean doAA(PublicKey publicKey) throws CardServiceException {
        try {
            byte[] m2 = new byte[8];
            random.nextBytes(m2);
            byte[] response = sendAA(publicKey, m2);
            aaCipher.init(Cipher.DECRYPT_MODE, publicKey);
            aaSignature.initVerify(publicKey);
            int digestLength = aaDigest.getDigestLength();
            byte[] plaintext = aaCipher.doFinal(response);
            byte[] m1 = Util.recoverMessage(digestLength, plaintext);
            aaSignature.update(m1);
            aaSignature.update(m2);
            boolean success = aaSignature.verify(response);
            AAEvent event = new AAEvent(this, publicKey, m1, m2, success);
            notifyAAPerformed(event);
            if (success) {
                state = AA_AUTHENTICATED_STATE;
            }
            return success;
        } catch (IllegalArgumentException iae) {
            throw new CardServiceException(iae.toString());
        } catch (GeneralSecurityException gse) {
            throw new CardServiceException(gse.toString());
        }
    }

    /**
     * Performs the Chip Authentication (CA) part of the EAP protocol with the
     * driving license. For details see ISO18013-3. In short, authenticate the
     * chip with DH key aggrement protocol (new secure messaging keys are
     * created then).
     * 
     * @param keyId
     *            passport's public key id (stored in DG14), -1 if none.
     *            Currently unused.
     * @param key
     *            cards public key (stored in DG14 on the card).
     * @return the EAP key pair used by the host
     * @throws CardServiceException
     *             on error
     */
    public synchronized KeyPair doCA(int keyId, PublicKey key) throws CardServiceException {
        try {
            String algName = (key instanceof ECPublicKey) ? "ECDH" : "DH";
            KeyPairGenerator genKey = KeyPairGenerator.getInstance(algName);
            AlgorithmParameterSpec spec = null;
            if ("DH".equals(algName)) {
                DHPublicKey k = (DHPublicKey) key;
                spec = k.getParams();
            } else {
                ECPublicKey k = (ECPublicKey) key;
                spec = k.getParams();
            }
            genKey.initialize(spec);
            KeyPair keyPair = genKey.generateKeyPair();
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH", "BC");
            agreement.init(keyPair.getPrivate());
            agreement.doPhase(key, true);
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] secret = md.digest(agreement.generateSecret());
            byte[] keyData = null;
            if ("DH".equals(algName)) {
                DHPublicKey k = (DHPublicKey) keyPair.getPublic();
                keyData = k.getY().toByteArray();
            } else {
                org.bouncycastle.jce.interfaces.ECPublicKey k = (org.bouncycastle.jce.interfaces.ECPublicKey) keyPair.getPublic();
                keyData = k.getQ().getEncoded();
            }
            keyData = tagData((byte) 0x91, keyData);
            sendMSE(wrapper, 0x41, 0xA6, keyData);
            SecretKey ksEnc = Util.deriveKey(secret, Util.ENC_MODE);
            SecretKey ksMac = Util.deriveKey(secret, Util.MAC_MODE);
            wrapper = new SecureMessagingWrapper(ksEnc, ksMac, 0L);
            state = CA_AUTHENTICATED_STATE;
            return keyPair;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new CardServiceException("Problem occured during Chip Authentication: " + ex.getMessage());
        }
    }

    /**
     * Performs the Terminal Authentication (TA) part of the EAP protocol with
     * the driving license. For details see ISO18013-3. In short: (a) feed the
     * sequence of terminal certificates to the card for verification. (b) get a
     * challenge from the card, sign it with terminal private key, send back to
     * the card for verification.
     * 
     * @param terminalCertificates
     *            the list/chain of terminal certificates
     * @param terminalKey
     *            terminal private key
     * @param sicId
     *            the SIC ID number
     * @return the card's challenge
     * @throws CardServiceException
     *             on error
     */
    public synchronized byte[] doTA(List<CVCertificate> terminalCertificates, PrivateKey terminalKey, String sicId) throws CardServiceException {
        try {
            String sigAlg = null;
            for (CVCertificate cert : terminalCertificates) {
                byte[] body = cert.getCertificateBody().getDEREncoded();
                byte[] sig = cert.getSignatureWrapped();
                byte[] certData = new byte[body.length + sig.length];
                System.arraycopy(body, 0, certData, 0, body.length);
                System.arraycopy(sig, 0, certData, body.length, sig.length);
                sendPSO(wrapper, certData);
                sigAlg = AlgorithmUtil.getAlgorithmName(cert.getCertificateBody().getPublicKey().getObjectIdentifier());
            }
            byte[] challenge = sendGetChallenge(wrapper);
            Signature sig = Signature.getInstance(sigAlg);
            sig.initSign(terminalKey);
            ByteArrayOutputStream dtbs = new ByteArrayOutputStream();
            dtbs.write(sicId.getBytes());
            dtbs.write(challenge);
            sig.update(dtbs.toByteArray());
            sendMutualAuthenticate(wrapper, sig.sign());
            state = TA_AUTHENTICATED_STATE;
            return challenge;
        } catch (CardServiceException cse) {
            throw cse;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new CardServiceException("Problem occured during TA: " + ex.getMessage());
        }
    }

    /**
     * Performs the EAP protocol with the driving license. For details see
     * ISO18013-3. Do CA and then TA.
     * 
     * @param keyId
     *            passport's public key id (stored in DG14), -1 if none.
     *            Currently unused.
     * @param key
     *            cards EC public key (stored in DG14 on the card).
     * @param terminalCertificates
     *            the list/chain of terminal certificates
     * @param terminalKey
     *            terminal private key
     * @param sicId
     *            the SIC ID number
     * @throws CardServiceException
     *             on error
     */
    public synchronized void doEAP(int keyId, PublicKey key, List<CVCertificate> terminalCertificates, PrivateKey terminalKey, String sicId) throws CardServiceException {
        KeyPair keyPair = doCA(keyId, key);
        byte[] challenge = doTA(terminalCertificates, terminalKey, sicId);
        EAPEvent event = new EAPEvent(this, keyId, keyPair, terminalCertificates, terminalKey, sicId, challenge, true);
        notifyEAPPerformed(event);
        state = EAP_AUTHENTICATED_STATE;
    }

    /**
     * Simple method to attach single byte tag to the data.
     * 
     * @param tag
     *            the tag
     * @param data
     *            the data
     * @return the tagged data
     */
    static byte[] tagData(byte tag, byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(tag);
            out.write(data.length);
            out.write(data);
        } catch (IOException ioe) {
        }
        return out.toByteArray();
    }

    /**
     * Performs the <i>Active Authentication</i> protocol. This method just
     * gives the response from the card without checking. Use
     * {@link #doAA(PublicKey)} instead.
     * 
     * @param publicKey
     *            the public key to use (usually read from the card)
     * @param challenge
     *            the random challenge of exactly 8 bytes
     * 
     * @return response from the card
     */
    public byte[] sendAA(PublicKey publicKey, byte[] challenge) throws CardServiceException {
        if (publicKey == null) {
            throw new IllegalArgumentException("AA failed: bad key");
        }
        if (challenge == null || challenge.length != 8) {
            throw new IllegalArgumentException("AA failed: bad challenge");
        }
        byte[] response = sendInternalAuthenticate(wrapper, challenge);
        return response;
    }

    /**
     * Notifies listeners about AA event.
     * 
     * @param event
     *            AA event.
     */
    protected void notifyAAPerformed(AAEvent event) {
        for (AuthListener l : authListeners) {
            l.performedAA(event);
        }
    }

    /**
     * Notifies listeners about EAP event.
     * 
     * @param event
     *            EAP event.
     */
    protected void notifyEAPPerformed(EAPEvent event) {
        for (AuthListener l : authListeners) {
            l.performedEAP(event);
        }
    }

    public void close() {
        try {
            wrapper = null;
            super.close();
        } finally {
            state = SESSION_STOPPED_STATE;
        }
    }

    /**
     * Gets the wrapper. Returns <code>null</code> until BAP has been
     * performed.
     * 
     * @return the wrapper
     */
    public SecureMessagingWrapper getWrapper() {
        return wrapper;
    }

    public FileSystemStructured getFileSystem() {
        return fs;
    }

    /**
     * Gets the file indicated by a file identifier.
     * 
     * @param fid
     *            ISO18013 file identifier
     * 
     * @return the file
     * 
     * @throws IOException
     *             if the file cannot be read
     */
    public CardFileInputStream readFile() throws CardServiceException {
        return new CardFileInputStream(maxBlockSize, fs);
    }

    public CardFileInputStream readDataGroup(int tag) throws CardServiceException {
        short fid = DrivingLicenseFile.lookupFIDByTag(tag);
        fs.selectFile(fid);
        return readFile();
    }

    private class DrivingLicenseFileSystem implements FileSystemStructured {

        private DrivingLicenseFileInfo selectedFile;

        public synchronized byte[] readBinary(int offset, int length) throws CardServiceException {
            return sendReadBinary(wrapper, (short) offset, length);
        }

        public synchronized void selectFile(short fid) throws CardServiceException {
            sendSelectFile(wrapper, fid);
            selectedFile = new DrivingLicenseFileInfo(fid, getFileLength());
        }

        public synchronized int getFileLength() throws CardServiceException {
            try {
                byte[] prefix = readBinary(0, 8);
                ByteArrayInputStream baIn = new ByteArrayInputStream(prefix);
                BERTLVInputStream tlvIn = new BERTLVInputStream(baIn);
                tlvIn.readTag();
                int vLength = tlvIn.readLength();
                int tlLength = prefix.length - baIn.available();
                return tlLength + vLength;
            } catch (IOException ioe) {
                throw new CardServiceException(ioe.toString());
            }
        }

        public FileInfo[] getSelectedPath() {
            return new DrivingLicenseFileInfo[] { selectedFile };
        }
    }

    private class DrivingLicenseFileInfo extends FileInfo {

        private short fid;

        private int length;

        public DrivingLicenseFileInfo(short fid, int length) {
            this.fid = fid;
            this.length = length;
        }

        public short getFID() {
            return fid;
        }

        public int getFileLength() {
            return length;
        }
    }
}
