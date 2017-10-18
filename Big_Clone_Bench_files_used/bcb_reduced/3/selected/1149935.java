package com.primianotucci.smartcard.openpgp;

import com.primianotucci.smartcard.iso7816.ISO7816FileSystem;
import com.primianotucci.smartcard.iso7816.ISO7816Comm;
import com.primianotucci.smartcard.iso7816.ISO7816Exception;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;

/**
 * Performs operations on a OpenPGP card (built on 1.1 Specs)
 * NOTE: this driver performs some kind of data caching in order to reduce 
 * card operations.
 * If you to retrieve updated data on the card (e.g. after a put* call),
 * make sure to call the corresponding reload* function (or initializeAndReload,
 * of course)
 * @author Primiano Tucci - http://www.primianotucci.com/
 */
public class OpenPGPCard {

    /**
     * Driver version number
     */
    public static final String DRIVER_VERSION = "1.0.0";

    /**
     * Expected ATR for an OpenPGP card
     */
    public static final ATR OPENPGP_CARD_ATR = new ATR(new byte[] { (byte) 0x3B, (byte) 0xFA, (byte) 0x13, (byte) 0x00, (byte) 0xFF, (byte) 0x81, (byte) 0x31, (byte) 0x80, (byte) 0x45, (byte) 0x00, (byte) 0x31, (byte) 0xC1, (byte) 0x73, (byte) 0xC0, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x00, (byte) 0xB1 });

    /**
     * The OID for SHA-1 hash operation
     */
    public static final byte[] SHA1_OID = StringUtil.stringToByteArr("3021300906052B0E03021A05000414");

    /**
     * The OID for RIPEMD-160 hash operation
     */
    public static final byte[] RIPEMD160_OID = StringUtil.stringToByteArr("3021300906052B2403020105000414");

    /**
     * Maximum length for an "Optional DO" field (DO[1-4])
     */
    public static final int MAX_DO_LEN = 254;

    /**
     * Maximum length for the "Login Data" field
     */
    public static final int MAX_LOGIN_LEN = 254;

    /**
     * Maximum length for the "Name" field
     */
    public static final int MAX_NAME_LEN = 39;

    /**
     * Maximum length for the "Languages" field
     */
    public static final int MAX_LANG_LEN = 8;

    /**
     * Maximum length for the "URL" field
     */
    public static final int MAX_URL_LEN = 254;

    /**
     * Byte code for sex=Male (according to ISO-5218)
     */
    public static final int SEX_MALE = 0x31;

    /**
     * Byte code for sex=Female (according to ISO-5218)
     */
    public static final int SEX_FEMALE = 0x32;

    /**
     * Byte code for sex=N/A (according to ISO-5218)
     */
    public static final int SEX_NOT_SPECIFIED = 0x39;

    /**
     * RSA Algorithm identifier
     */
    public static final int ALGO_RSA = 0x01;

    private ISO7816Comm iso;

    private Card card;

    private boolean dontMatchATR = false;

    private static final byte[] AID = new byte[] { (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x01, (byte) 0x24, (byte) 0x01 };

    private static final int[] TLV_LOGIN = { 0x00, 0x5E };

    private static final int[] TLV_HOLDER = { 0x00, 0x65 };

    private static final int[] TLV_HOLDER_NAME = { 0x00, 0x5B };

    private static final int[] TLV_HOLDER_LANG = { 0x5F, 0x2D };

    private static final int[] TLV_HOLDER_SEX = { 0x5F, 0x35 };

    private static final int[] TLV_APPDATA = { 0x00, 0x6E };

    private static final int[] TLV_URL = { 0x5F, 0x50 };

    private static final int[] TLV_CHV = { 0x00, 0xC4 };

    private static final int[] TLV_ST = { 0x00, 0x7A };

    private static final int TLV_OPTDO = 0x01;

    private static final int TLV_CHV1 = 0x81;

    private static final int TLV_CHV2 = 0x82;

    private static final int TLV_CHV3 = 0x83;

    private static final byte[] KS_SIG = { (byte) 0xB6, (byte) 0x00 };

    private static final byte[] KS_DEC = { (byte) 0xB8, (byte) 0x00 };

    private static final byte[] KS_AUT = { (byte) 0xA4, (byte) 0x00 };

    private static final int[] TLV_FPR_SIG = { 0x00, 0xC7 };

    private static final int[] TLV_FPR_DEC = { 0x00, 0xC8 };

    private static final int[] TLV_FPR_AUT = { 0x00, 0xC9 };

    private static final int[] TLV_FPR_CA1 = { 0x00, 0xCA };

    private static final int[] TLV_FPR_CA2 = { 0x00, 0xCB };

    private static final int[] TLV_FPR_CA3 = { 0x00, 0xCC };

    private static final int[] TLV_DATE_SIG = { 0x00, 0xCE };

    private static final int[] TLV_DATE_DEC = { 0x00, 0xCF };

    private static final int[] TLV_DATE_AUT = { 0x00, 0xD0 };

    private static final int[] TLV_PRIV_SIG = { 0x00, 0xE0 };

    private static final int[] TLV_PRIV_DEC = { 0x00, 0xE1 };

    private static final int[] TLV_PRIV_AUT = { 0x00, 0xE2 };

    private static final int KS_DO = 0x7F49;

    private static final int KS_RSA_MOD = 0x81;

    private static final int KS_RSA_EXP = 0x82;

    private static final int EF_HOLDER_NAME = 0x5B;

    private static final int EF_HOLDER_LANG = 0x5F2D;

    private static final int EF_HOLDER_SEX = 0x5F35;

    private byte[] cardid;

    private byte[] loginData;

    private String holderNameString;

    private String holderName;

    private String holderSurname;

    private int holderSex;

    private String holderLang;

    private String certURL;

    private byte[] optDO1;

    private byte[] optDO2;

    private byte[] optDO3;

    private byte[] optDO4;

    private int chvStatus;

    private int maxLenCHV1;

    private int maxLenCHV2;

    private int maxLenCHV3;

    private int counterCHV1;

    private int counterCHV2;

    private int counterCHV3;

    private int signatureCounter;

    private boolean supportsGetChallenge = false;

    private boolean supportsKeyImport = false;

    private boolean supportsCHVStatusChange = false;

    private boolean supportsPrivateDOs = false;

    private int keySizeSig;

    private int keyExpSizeSig;

    private int keySizeDec;

    private int keyExpSizeDec;

    private int keySizeAut;

    private int keyExpSizeAut;

    private byte[] fingerprintSig;

    private byte[] fingerprintDec;

    private byte[] fingerprintAut;

    private Date keyDateSig;

    private byte[] keyDateSigBytes;

    private Date keyDateDec;

    private byte[] keyDateDecBytes;

    private Date keyDateAut;

    private byte[] keyDateAutBytes;

    private BigInteger pubkeyModulusSig;

    private BigInteger pubkeyExponentSig;

    private BigInteger pubkeyModulusDec;

    private BigInteger pubkeyExponentDec;

    private BigInteger pubkeyModulusAut;

    private BigInteger pubkeyExponentAut;

    private boolean CHV3SafetyLock = true;

    /**
     * Instantiates a new OpenPGPCard object (but does NOT initialize and read card data)
     * @param iCard the javax.smartcardio.Card Card that handles card communication
     * @see javax.smartcardio.Card
     */
    public OpenPGPCard(Card iCard) {
        card = iCard;
    }

    /**
     * Instantiates a new OpenPGPCard object
     * @param iCard the javax.smartcardio.Card Card that handles card communication
     * @see javax.smartcardio.Card
     * @param iInit Boolean: initialize and read card data
     * @param iDontMatchATR: disable ATR matching
     */
    public OpenPGPCard(Card iCard, boolean iInit, boolean iDontMatchATR) throws OpenPGPCardException {
        card = iCard;
        dontMatchATR = iDontMatchATR;
        if (iInit) initializeAndReload();
    }

    /**
     * Instantiates a new OpenPGPCard object (Matches the OpenPGP card ATR per default)
     * @param iCard the javax.smartcardio.Card Card that handles card communication
     * @see javax.smartcardio.Card
     * @param iInit Boolean: initialize and read card data
     */
    public OpenPGPCard(Card iCard, boolean iInit) throws OpenPGPCardException {
        this(iCard, iInit, false);
    }

    /**
     * Initializes the OpenPGP card and reloads all data (that does not require CHV pins)
     * E.G. this does not load DO3 and DO4 (that requires CHV2 and CHV3 to have been entered)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void initializeAndReload() throws OpenPGPCardException {
        if (!dontMatchATR) {
            if (!card.getATR().equals(OPENPGP_CARD_ATR)) throw new OpenPGPCardException("Could not recognize OpenPGP card ATR");
        }
        iso = new ISO7816Comm(card.getBasicChannel());
        try {
            byte[] aid = iso.selectFileDirect(AID);
            if (aid.length < 20) throw new OpenPGPCardException("Invalid AID (" + aid.length + " bytes, expecting 20 bytes)");
            cardid = Arrays.copyOfRange(aid, 10, 18);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not select AID (" + ex.getMessage() + ")");
        }
        reloadLoginData();
        reloadHolderData();
        reloadURL();
        reloadDataObjects(false, false);
        reloadKeyData();
        reloadSignatureCounter();
        reloadCHVStatus();
        reloadPubKeys();
    }

    /**
     * Stores data for Optional DO field [1-4] on the card
     * @param iDOIdx Field index (1,2,3 or 4)
     * @param iData Data to be stored
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     *         An error has occoured (e.g. CHV3 pin was not entered)
     */
    public void putDO(int iDOIdx, byte[] iData) throws OpenPGPCardException {
        if (iDOIdx < 1 || iDOIdx > 4) throw new IllegalArgumentException("Invalid index (expecting [1-4])");
        putGenericData(TLV_OPTDO, iDOIdx, iData, MAX_DO_LEN);
    }

    /**
     * Stores the provided URL field on the card
     * @param iUrl URL where public key for the card can be retrieved
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putURL(String iUrl) throws OpenPGPCardException {
        byte[] data = iUrl.getBytes();
        putGenericData(TLV_URL[0], TLV_URL[1], data, MAX_URL_LEN);
    }

    /**
     * Stores data for the "Login data" field on the card
     * @param iData the data buffer
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putLoginData(byte[] iData) throws OpenPGPCardException {
        putGenericData(TLV_LOGIN[0], TLV_LOGIN[1], iData, MAX_LOGIN_LEN);
    }

    /**
     * Stores the holder name and surname on the card
     * @param iName Holder name
     * @param iSurname Holder surname
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putHolderName(String iName, String iSurname) throws OpenPGPCardException {
        byte[] data = (iSurname + "<<" + iName).getBytes();
        putGenericData(TLV_HOLDER_NAME[0], TLV_HOLDER_NAME[1], data, MAX_NAME_LEN);
    }

    /**
     * Stores holder sex on the card
     * @param iSex Byte code for sex (According to ISO-5218)
     *             (one of SEX_MALE / SEX_FEMALE / SEX_NOT_SPECIFIED)
     * @see   #SEX_MALE
     * @see   #SEX_FEMALE
     * @see   #SEX_NOT_SPECIFIED
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putHolderSex(int iSex) throws OpenPGPCardException {
        byte[] data = { (byte) iSex };
        putGenericData(TLV_HOLDER_SEX[0], TLV_HOLDER_SEX[1], data, 1);
    }

    /**
     * Stores holder languages on the card
     * @param iLangs Language string (According to ISO-639). Should be made up 
     *               of 2,4 or 6 characters (es: it, itde, enitde)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putHolderLang(String iLangs) throws OpenPGPCardException {
        byte[] data = iLangs.getBytes();
        putGenericData(TLV_HOLDER_LANG[0], TLV_HOLDER_LANG[1], data, MAX_LANG_LEN);
    }

    /**
     * Stores the Signature key fingerprint (20 bytes) on the card
     * @param iFpr Fingerprint data (20 bytes)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putFingerprintSig(byte[] iFpr) throws OpenPGPCardException {
        if (iFpr.length != 20) throw new IllegalArgumentException("Fingerprint must be exactly 20 bytes long");
        putGenericData(TLV_FPR_SIG[0], TLV_FPR_SIG[1], iFpr, 20);
    }

    /**
     * Stores the Decryption key fingerprint (20 bytes) on the card
     * @param iFpr Fingerprint data (20 bytes)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putFingerprintDec(byte[] iFpr) throws OpenPGPCardException {
        if (iFpr.length != 20) throw new IllegalArgumentException("Fingerprint must be exactly 20 bytes long");
        putGenericData(TLV_FPR_DEC[0], TLV_FPR_DEC[1], iFpr, 20);
    }

    /**
     * Stores the Authentication key fingerprint (20 bytes) on the card
     * @param iFpr Fingerprint data (20 bytes)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putFingerprintAut(byte[] iFpr) throws OpenPGPCardException {
        if (iFpr.length != 20) throw new IllegalArgumentException("Fingerprint must be exactly 20 bytes long");
        putGenericData(TLV_FPR_AUT[0], TLV_FPR_AUT[1], iFpr, 20);
    }

    /**
     * Stores the CA1 fingerprint (20 bytes) on the card (for Ultimately Trusted Keys)
     * @param iFpr Fingerprint data (20 bytes)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putFingerprintCA1(byte[] iFpr) throws OpenPGPCardException {
        if (iFpr.length != 20) throw new IllegalArgumentException("Fingerprint must be exactly 20 bytes long");
        putGenericData(TLV_FPR_CA1[0], TLV_FPR_CA1[1], iFpr, 20);
    }

    /**
     * Stores the CA2 fingerprint (20 bytes) on the card (for Ultimately Trusted Keys)
     * @param iFpr Fingerprint data (20 bytes)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putFingerprintCA2(byte[] iFpr) throws OpenPGPCardException {
        if (iFpr.length != 20) throw new IllegalArgumentException("Fingerprint must be exactly 20 bytes long");
        putGenericData(TLV_FPR_CA2[0], TLV_FPR_CA2[1], iFpr, 20);
    }

    /**
     * Stores the CA3 fingerprint (20 bytes) on the card (for Ultimately Trusted Keys)
     * @param iFpr Fingerprint data (20 bytes)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putFingerprintCA3(byte[] iFpr) throws OpenPGPCardException {
        if (iFpr.length != 20) throw new IllegalArgumentException("Fingerprint must be exactly 20 bytes long");
        putGenericData(TLV_FPR_CA3[0], TLV_FPR_CA3[1], iFpr, 20);
    }

    /**
     * Stores the generation date of the signature key on the card
     * @param iDate Date of key generation
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putKeyDateSig(Date iDate) throws OpenPGPCardException {
        putGenericData(TLV_DATE_SIG[0], TLV_DATE_SIG[1], dateToTimestamp(iDate), 4);
    }

    /**
     * Stores the generation date of the encryption key on the card
     * @param iDate Date of key generation
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putKeyDateDec(Date iDate) throws OpenPGPCardException {
        putGenericData(TLV_DATE_DEC[0], TLV_DATE_DEC[1], dateToTimestamp(iDate), 4);
    }

    /**
     * Stores the generation date of the authentication key on the card
     * @param iDate Date of key generation
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putKeyDateAut(Date iDate) throws OpenPGPCardException {
        putGenericData(TLV_DATE_AUT[0], TLV_DATE_AUT[1], dateToTimestamp(iDate), 4);
    }

    /**
     * Stores the private key for Signature on the card
     * @param iPublicExponent The public exponent
     * @param iP The P part of the private exponent (according to Chinese Remainder Theorem)
     * @param iQ The Q part of the private exponent (according to Chinese Remainder Theorem)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putPrivateKeySig(BigInteger iPublicExponent, BigInteger iP, BigInteger iQ) throws OpenPGPCardException {
        putPrivateKey(TLV_PRIV_SIG, iPublicExponent, iP, iQ);
    }

    /**
     * Stores the private key for Decryption on the card
     * @param iPublicExponent The public exponent
     * @param iP The P part of the private exponent (according to Chinese Remainder Theorem)
     * @param iQ The Q part of the private exponent (according to Chinese Remainder Theorem)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putPrivateKeyDec(BigInteger iPublicExponent, BigInteger iP, BigInteger iQ) throws OpenPGPCardException {
        putPrivateKey(TLV_PRIV_DEC, iPublicExponent, iP, iQ);
    }

    /**
     * Stores the private key for Authentication on the card
     * @param iPublicExponent The public exponent
     * @param iP The P part of the private exponent (according to Chinese Remainder Theorem)
     * @param iQ The Q part of the private exponent (according to Chinese Remainder Theorem)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void putPrivateKeyAut(BigInteger iPublicExponent, BigInteger iP, BigInteger iQ) throws OpenPGPCardException {
        putPrivateKey(TLV_PRIV_AUT, iPublicExponent, iP, iQ);
    }

    private void putPrivateKey(int[] iTlvKey, BigInteger iPublicExponent, BigInteger iP, BigInteger iQ) throws OpenPGPCardException {
        int len = this.getKeySizeDec() / 8 / 2;
        int explen = getKeyExpSizeDec() / 8;
        if (iPublicExponent.bitCount() > explen * 8) throw new IllegalArgumentException("iPublicExponent (" + iPublicExponent.bitCount() + " > " + explen + ")");
        if (iP.bitCount() > len * 8) throw new IllegalArgumentException("iP");
        if (iQ.bitCount() > len * 8) throw new IllegalArgumentException("iQ");
        ISO7816FileSystem.DF df = new ISO7816FileSystem.DF(0);
        df.addSubEF(new ISO7816FileSystem.EF(0xC0, bigIntegerToUnsignedByteArray(iPublicExponent, explen)));
        df.addSubEF(new ISO7816FileSystem.EF(0xC1, bigIntegerToUnsignedByteArray(iP, len)));
        df.addSubEF(new ISO7816FileSystem.EF(0xC2, bigIntegerToUnsignedByteArray(iQ, len)));
        byte[] data = df.toByteArray(false);
        putGenericData(iTlvKey[0], iTlvKey[1], data, data.length);
    }

    /**
     * Stores a generic data field on the card (should not be used, it's 
     * just for expandibility)
     * @param iTLV1 P1 
     * @param iTLV2 P2
     * @param iData APDU Data
     * @param iMaxLen Maximum length
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    private void putGenericData(int iTLV1, int iTLV2, byte[] iData, int iMaxLen) throws OpenPGPCardException {
        try {
            if (iData.length > iMaxLen) throw new IllegalArgumentException("Data too big (max " + iMaxLen + " bytes)");
            iso.putData(iTLV1, iTLV2, iData);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not complete action (" + ex.getMessage() + ")");
        }
    }

    /**
     * Lets the card generate a new private key for Signature 
     * (Note: new key will be stored on card and will replace the old one)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void generateKeySig() throws OpenPGPCardException {
        try {
            iso.generateKeyPair(KS_SIG);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not generate key pair (" + ex.getMessage() + ")");
        }
    }

    /**
     * Lets the card generate a new private key for Decryption
     * (Note: new key will be stored on card and will replace the old one)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void generateKeyDec() throws OpenPGPCardException {
        try {
            iso.generateKeyPair(KS_DEC);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not generate key pair (" + ex.getMessage() + ")");
        }
    }

    /**
     * Lets the card generate a new private key for Authentication
     * (Note: new key will be stored on card and will replace the old one)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void generateKeyAut() throws OpenPGPCardException {
        try {
            iso.generateKeyPair(KS_AUT);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not generate key pair (" + ex.getMessage() + ")");
        }
    }

    /**
     * Reloads public keys data from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadPubKeys() throws OpenPGPCardException {
        try {
            ISO7816FileSystem.DF dfSig = iso.parseFileSystemStructure(iso.getKeyPair(KS_SIG)).getMasterFile().getSubDF(KS_DO);
            pubkeyModulusSig = new BigInteger(1, dfSig.getSubEF(KS_RSA_MOD).getData());
            pubkeyExponentSig = new BigInteger(1, dfSig.getSubEF(KS_RSA_EXP).getData());
        } catch (Exception ex) {
        }
        try {
            ISO7816FileSystem.DF dfDec = iso.parseFileSystemStructure(iso.getKeyPair(KS_DEC)).getMasterFile().getSubDF(KS_DO);
            pubkeyModulusDec = new BigInteger(1, dfDec.getSubEF(KS_RSA_MOD).getData());
            pubkeyExponentDec = new BigInteger(1, dfDec.getSubEF(KS_RSA_EXP).getData());
        } catch (Exception ex) {
        }
        try {
            ISO7816FileSystem.DF dfAut = iso.parseFileSystemStructure(iso.getKeyPair(KS_AUT)).getMasterFile().getSubDF(KS_DO);
            pubkeyModulusAut = new BigInteger(1, dfAut.getSubEF(KS_RSA_MOD).getData());
            pubkeyExponentAut = new BigInteger(1, dfAut.getSubEF(KS_RSA_EXP).getData());
        } catch (Exception ex) {
        }
    }

    /**
     * Reloads URL from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadURL() throws OpenPGPCardException {
        try {
            certURL = new String(iso.getData(TLV_URL[0], TLV_URL[1]), StringUtil.OPENPGP_CARD_CHARSET);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not get cert URL (" + ex.getMessage() + ")");
        }
    }

    /**
     * Reloads optional data objecs from the card
     * @param iLoadAlsoPrivateDO3 Load also DO3 (CHV2 must have been entered)
     * @param iLoadAlsoPrivateDO4 Load also DO4 (CHV3 must have been entered)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadDataObjects(boolean iLoadAlsoPrivateDO3, boolean iLoadAlsoPrivateDO4) throws OpenPGPCardException {
        try {
            optDO1 = iso.getData(TLV_OPTDO, 0x01);
            optDO2 = iso.getData(TLV_OPTDO, 0x02);
            if (iLoadAlsoPrivateDO3) optDO3 = iso.getData(TLV_OPTDO, 0x03);
            if (iLoadAlsoPrivateDO4) optDO4 = iso.getData(TLV_OPTDO, 0x04);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not get optional DO(s) (" + ex.getMessage() + ")");
        }
    }

    /**
     * Reloads holder data (name, sex, languages) from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadHolderData() throws OpenPGPCardException {
        try {
            byte[] holderData = iso.getData(TLV_HOLDER[0], TLV_HOLDER[1]);
            ISO7816FileSystem.DF df = iso.parseFileSystemStructure(holderData).getMasterFile();
            holderNameString = new String(df.getSubEF(EF_HOLDER_NAME).getData());
            String[] arr = holderNameString.split("<<", 2);
            if (arr.length == 2) {
                holderName = arr[1];
                holderSurname = arr[0];
            } else {
                holderName = arr[0];
                holderSurname = "";
            }
            holderSex = df.getSubEF(EF_HOLDER_SEX).getData()[0] & 0xFF;
            holderLang = new String(df.getSubEF(EF_HOLDER_LANG).getData());
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not get holder data (" + ex.getMessage() + ")");
        }
    }

    /**
     * Reloads key data (fingerprints, generation dates, capabilities) 
     * from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadKeyData() throws OpenPGPCardException {
        try {
            byte[] appData = iso.getData(TLV_APPDATA[0], TLV_APPDATA[1]);
            ISO7816FileSystem fs = iso.parseFileSystemStructure(appData);
            byte[] fingerprints = fs.getMasterFile().getSubDF(0x73).getSubEF(0xC5).getData();
            fingerprintSig = Arrays.copyOfRange(fingerprints, 0, 20);
            fingerprintDec = Arrays.copyOfRange(fingerprints, 20, 40);
            fingerprintAut = Arrays.copyOfRange(fingerprints, 40, 60);
            byte capabilities = fs.getMasterFile().getSubDF(0x73).getSubEF(0xC0).getData()[0];
            supportsGetChallenge = (capabilities & (1 << 7)) != 0;
            supportsKeyImport = (capabilities & (1 << 6)) != 0;
            supportsCHVStatusChange = (capabilities & (1 << 5)) != 0;
            supportsPrivateDOs = (capabilities & (1 << 4)) != 0;
            byte[] kDataSig = fs.getMasterFile().getSubDF(0x73).getSubEF(0xC1).getData();
            keySizeSig = ((kDataSig[1] & 0xFF) << 8) | (kDataSig[2] & 0xFF);
            keyExpSizeSig = ((kDataSig[3] & 0xFF) << 8) | (kDataSig[4] & 0xFF);
            byte[] kDataDec = fs.getMasterFile().getSubDF(0x73).getSubEF(0xC2).getData();
            keySizeDec = ((kDataDec[1] & 0xFF) << 8) | (kDataDec[2] & 0xFF);
            keyExpSizeDec = ((kDataDec[3] & 0xFF) << 8) | (kDataDec[4] & 0xFF);
            byte[] kDataAut = fs.getMasterFile().getSubDF(0x73).getSubEF(0xC3).getData();
            keySizeAut = ((kDataAut[1] & 0xFF) << 8) | (kDataAut[2] & 0xFF);
            keyExpSizeAut = ((kDataAut[3] & 0xFF) << 8) | (kDataAut[4] & 0xFF);
            if (kDataSig[0] != ALGO_RSA || kDataDec[0] != ALGO_RSA || kDataAut[0] != ALGO_RSA) throw new OpenPGPCardException("Card algorithm not supported (not RSA)");
            byte[] dates = fs.getMasterFile().getSubEF(0xCD).getData();
            keyDateSig = new Date((long) ((dates[0] & 0xFF) << 24 | (dates[1] & 0xFF) << 16 | (dates[2] & 0xFF) << 8 | (dates[3] & 0xFF)) * 1000);
            keyDateSigBytes = Arrays.copyOfRange(dates, 0, 4);
            keyDateDec = new Date((long) ((dates[4] & 0xFF) << 24 | (dates[5] & 0xFF) << 16 | (dates[6] & 0xFF) << 8 | (dates[7] & 0xFF)) * 1000);
            keyDateDecBytes = Arrays.copyOfRange(dates, 4, 8);
            keyDateAut = new Date((long) ((dates[8] & 0xFF) << 24 | (dates[9] & 0xFF) << 16 | (dates[10] & 0xFF) << 8 | (dates[11] & 0xFF)) * 1000);
            keyDateAutBytes = Arrays.copyOfRange(dates, 8, 12);
        } catch (Exception ex) {
            throw new OpenPGPCardException("Could not get app data (" + ex.getMessage() + ")");
        }
    }

    /**
     * Calculates the fingeprint of the public key for Signature
     * (Note: could be different from the fingerprint stored on card)
     * @return 20 bytes fingerprint
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] calculateFingerprintOfKeySig() throws OpenPGPCardException {
        return calculateFingerprintOfKey(pubkeyModulusSig, pubkeyExponentSig, keySizeSig, keyExpSizeSig, keyDateSigBytes);
    }

    /**
     * Calculates the fingeprint of the public key for Decryption
     * (Note: could be different from the fingerprint stored on card)
     * @return 20 bytes fingerprint
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] calculateFingerprintOfKeyDec() throws OpenPGPCardException {
        return calculateFingerprintOfKey(pubkeyModulusDec, pubkeyExponentDec, keySizeDec, keyExpSizeDec, keyDateDecBytes);
    }

    /**
     * Calculates the fingeprint of the public key for Authentication
     * (Note: could be different from the fingerprint stored on card)
     * @return 20 bytes fingerprint
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] calculateFingerprintOfKeyAut() throws OpenPGPCardException {
        return calculateFingerprintOfKey(pubkeyModulusAut, pubkeyExponentAut, keySizeAut, keyExpSizeAut, keyDateAutBytes);
    }

    private byte[] calculateFingerprintOfKey(BigInteger iMod, BigInteger iExp, int iModLen, int iExpLen, byte[] iDate) throws OpenPGPCardException {
        try {
            int modLen = (int) Math.ceil((double) iMod.bitLength() / 8);
            int expLen = (int) Math.ceil((double) iExp.bitLength() / 8);
            int n = 6 + 2 + modLen + 2 + expLen;
            int nbits;
            byte[] data;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(0x99);
            buf.write(n >> 8);
            buf.write(n & 0xFF);
            buf.write(4);
            buf.write(iDate, 0, 4);
            buf.write(ALGO_RSA);
            nbits = iMod.bitLength();
            buf.write(nbits >> 8);
            buf.write(nbits & 0xFF);
            data = iMod.toByteArray();
            buf.write(data, data.length - modLen, modLen);
            nbits = iExp.bitLength();
            buf.write(nbits >> 8);
            buf.write(nbits & 0xFF);
            data = iExp.toByteArray();
            buf.write(data, data.length - expLen, expLen);
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            return sha1.digest(buf.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new OpenPGPCardException("Could not get an instance for SHA1 MessageDigest");
        }
    }

    /**
     * Reloads the "login data" field from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadLoginData() throws OpenPGPCardException {
        try {
            loginData = iso.getData(TLV_LOGIN[0], TLV_LOGIN[1]);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not get account (" + ex.getMessage() + ")");
        }
    }

    /**
     * Reloads signature counters from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadSignatureCounter() throws OpenPGPCardException {
        try {
            byte[] stData = iso.getData(TLV_ST[0], TLV_ST[1]);
            byte[] sl = iso.parseFileSystemStructure(stData).getMasterFile().getSubEF(0x93).getData();
            signatureCounter = (sl[2] & 0xFF) | ((sl[1] & 0xFF) << 8) | ((sl[0] & 0xFF) << 16);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not get Security Template (" + ex.getMessage() + ")");
        }
    }

    /**
     * Reloads CHV status (and counters) from the card
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void reloadCHVStatus() throws OpenPGPCardException {
        try {
            byte[] chvStat = iso.getData(TLV_CHV[0], TLV_CHV[1]);
            if (chvStat.length < 7) throw new OpenPGPCardException("Invalid CHV status word (got " + chvStat.length + " bytes, expecting 7 bytes)");
            chvStatus = chvStat[0] & 0xFF;
            maxLenCHV1 = chvStat[1] & 0xFF;
            maxLenCHV2 = chvStat[2] & 0xFF;
            maxLenCHV3 = chvStat[3] & 0xFF;
            counterCHV1 = chvStat[4] & 0xFF;
            counterCHV2 = chvStat[5] & 0xFF;
            counterCHV3 = chvStat[6] & 0xFF;
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not get CHV(s) status (" + ex.getMessage() + ")");
        }
    }

    /**
     * Change the CHV1 pin
     * @param iNewPin The new CHV1
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void changeCHV1Pin(byte[] iNewPin) throws OpenPGPCardException {
        try {
            iso.changePin(TLV_CHV1, iNewPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not change pin. Make sure you've entered the old pin before changing it (" + ex.getMessage() + ")");
        }
    }

    /**
     * Change the CHV2 pin
     * @param iNewPin The new CHV2
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void changeCHV2Pin(byte[] iNewPin) throws OpenPGPCardException {
        try {
            iso.changePin(TLV_CHV2, iNewPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not change pin. Make sure you've entered the old pin before changing it (" + ex.getMessage() + ")");
        }
    }

    /**
     * Change the CHV3 pin
     * @param iNewPin The new CHV3
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void changeCHV3Pin(byte[] iNewPin) throws OpenPGPCardException {
        if (CHV3SafetyLock) throw new OpenPGPCardException("CHV3 Disabled by default (Could destroy your card). Disable CHV3SafetyLock property in order to access CHV3");
        try {
            iso.changePin(TLV_CHV3, iNewPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not change pin. Make sure you've entered the old pin before changing it (" + ex.getMessage() + ")");
        }
    }

    /**
     * Resets the CHV1 pin (requires CHV3 to have been entered)
     * @param iNewPin the new CHV1
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void resetCHV1Pin(byte[] iNewPin) throws OpenPGPCardException {
        try {
            iso.resetPin(TLV_CHV1, iNewPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not reset pin. Make sure you've entered CHV3 (" + ex.getMessage() + ")");
        }
    }

    /**
     * Resets the CHV2 pin (requires CHV3 to have been entered)
     * @param iNewPin the new CHV2
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void resetCHV2Pin(byte[] iNewPin) throws OpenPGPCardException {
        try {
            iso.resetPin(TLV_CHV2, iNewPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not reset pin. Make sure you've entered CHV3 (" + ex.getMessage() + ")");
        }
    }

    /**
     * Presents CHV1 pin to the card
     * @param iPin Pin
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void enterCHV1Pin(byte[] iPin) throws OpenPGPCardException {
        try {
            iso.verifyPin(TLV_CHV1, iPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not verify pin (" + ex.getMessage() + ")");
        }
    }

    /**
     * Presents CHV2 pin to the card
     * @param iPin Pin
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void enterCHV2Pin(byte[] iPin) throws OpenPGPCardException {
        try {
            iso.verifyPin(TLV_CHV2, iPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not verify pin (" + ex.getMessage() + ")");
        }
    }

    /**
     * Presents CHV3 pin to the card. NOTE: Since this operation is really 
     * dangerous (3 wrong CHV3 attempts would destroy the card) the 
     * CHV3SafetyLock should have been unlocked prior to use this function
     * @param iPin Pin
     * @see #setCHV3SafetyLock(boolean)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public void enterCHV3Pin(byte[] iPin) throws OpenPGPCardException {
        if (CHV3SafetyLock) throw new OpenPGPCardException("CHV3 Disabled by default (Could destroy your card). Disable CHV3SafetyLock property in order to access CHV3");
        try {
            iso.verifyPin(TLV_CHV3, iPin);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not verify pin (" + ex.getMessage() + ")");
        }
    }

    /**
     * Gets random bytes from the card (using GET CHALLENGE feature)
     * @param iLen Number of random bytes to get
     * @return Byte array containing random bytes
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] getRandomBytes(int iLen) throws OpenPGPCardException {
        try {
            return iso.getChallenge(iLen);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not perform GET_CHALLENGE (" + ex.getMessage() + ")");
        }
    }

    /**
     * Performs a digital signature of the given hash data
     * @param iOID The OID corresponding to the hash function used
     *        (SHA1_OID or RIPEMD160_OID)
     * @param iData The hashed data (20 bytes for SHA-1, 16 bytes for RIPEMD-160)
     * @return Computed signature bytes (PKCS1 padded)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] sign(byte[] iOID, byte[] iData) throws OpenPGPCardException {
        if (iData.length != 20) throw new OpenPGPCardException("Expecting 20 byte SHA-1 data (got " + iData.length + " bytes)");
        if (iOID.length != 15) throw new OpenPGPCardException("Expecting 15 byte Digest-Info OID (got " + iOID.length + " bytes)");
        try {
            byte[] buf = Arrays.copyOf(iOID, 35);
            for (int i = 15; i < 35; i++) buf[i] = iData[i - 15];
            return iso.computeDigitalSignature(buf);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not perform operation (" + ex.getMessage() + ")");
        }
    }

    /**
     * Authenticates the input data (using the Authentication key)
     * @param iData Input byte array (maximum keySizeBits/8 * 0.4 -6) bytes
     *              (PKCS1 padding will be applied, so do NOT pad data)
     * @return The authenticated data (includes PKCS1 padding)
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] authenticate(byte[] iData) throws OpenPGPCardException {
        int sizeLimit = ((int) (keySizeAut / 8 * 0.4)) - 6;
        if (iData.length > sizeLimit) throw new OpenPGPCardException("Expecting at most " + sizeLimit + " bytes (got " + iData.length + " bytes)");
        try {
            return iso.internalAuthenticate(PKCS1Padding.getPadding(iData, (int) (keySizeAut / 8 * 0.4)));
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not perform operation (" + ex.getMessage() + ")");
        }
    }

    /**
     * Decrypts data using the Decryption key
     * @param iPKCS1PaddedData Input data (must have been PKCS1 padded before encryption)
     * @return Decrypted data
     * @throws com.primianotucci.smartcard.openpgp.OpenPGPCardException
     */
    public byte[] decipher(byte[] iPKCS1PaddedData) throws OpenPGPCardException {
        try {
            byte[] data = new byte[iPKCS1PaddedData.length + 1];
            data[0] = 0x00;
            for (int i = 0; i < iPKCS1PaddedData.length; i++) data[i + 1] = iPKCS1PaddedData[i];
            return iso.decipher(data);
        } catch (ISO7816Exception ex) {
            throw new OpenPGPCardException("Could not perform operation (" + ex.getMessage() + ")");
        }
    }

    /**
     * Retrieves CardID as a byte array
     * @return CardID (as byte[])
     */
    public byte[] getCardIDBytes() {
        return cardid;
    }

    /**
     * Retrieves CardID as a String
     * @return CardID (as String)
     */
    public String getCardID() {
        if (cardid == null || cardid.length != 8) return "N/A";
        return StringUtil.ATRToString(cardid, "");
    }

    /**
     * Retrieve Login data
     * @return Login data
     */
    public byte[] getLoginData() {
        return loginData;
    }

    /**
     * Retrieves the full "card holder" field
     * @return Surname<<Name formatted String
     */
    public String getHolderNameString() {
        return holderNameString;
    }

    /**
     * Retrieves card holder Sex (according to ISO-5218)
     * @return Byte value for holder Sex
     */
    public int getHolderSex() {
        return holderSex;
    }

    /**
     * Retrieves card holder languages (according to ISO-639)
     * @return 0,2,4 or 6 characters long String. (2 chars for each languagee)
     */
    public String getHolderLang() {
        return holderLang;
    }

    /**
     * Retrieves the public key URL 
     * (where the public key should be downloaded from)
     * @return Url string
     */
    public String getURL() {
        return certURL;
    }

    /**
     * Retrieves the optional data DO1 field
     * @return optional DO1 data
     */
    public byte[] getOptDO1() {
        return optDO1;
    }

    /**
     * Retrieves the optional data DO2 field
     * @return optional DO2 data
     */
    public byte[] getOptDO2() {
        return optDO2;
    }

    /**
     * Retrieves the optional data DO3 field. NOTE: this can be read only after
     * presentation of CHV2 and subsequent call to reloadDataObjects(true,*);
     * @return optional DO3 data
     * @see #reloadDataObjects(boolean, boolean)
     */
    public byte[] getOptDO3() {
        return optDO3;
    }

    /**
     * Retrieves the optional data DO4 field. NOTE: this can be read only after
     * presentation of CHV3 and subsequent call to reloadDataObjects(true,*);
     * @return optional DO4 data
     * @see #reloadDataObjects(boolean, boolean)
     */
    public byte[] getOptDO4() {
        return optDO4;
    }

    /**
     * Retrieves the Maximum length for CHV1
     * @return Maximum length for CHV1
     */
    public int getMaxLenCHV1() {
        return maxLenCHV1;
    }

    /**
     * Retrieves the Maximum length for CHV2
     * @return Maximum length for CHV2
     */
    public int getMaxLenCHV2() {
        return maxLenCHV2;
    }

    /**
     * Retrieves the Maximum length for CHV3
     * @return Maximum length for CHV3
     */
    public int getMaxLenCHV3() {
        return maxLenCHV3;
    }

    /**
     * Retrieves the CHV1 retry counter (3 downto 0)
     * @return CHV1 retry counter
     */
    public int getCounterCHV1() {
        return counterCHV1;
    }

    /**
     * Retrieves the CHV2 retry counter (3 downto 0)
     * @return CHV2 retry counter
     */
    public int getCounterCHV2() {
        return counterCHV2;
    }

    /**
     * Retrieves the CHV3 retry counter (3 downto 0). 0 means the card is locked-up
     * @return CHV3 retry counter
     */
    public int getCounterCHV3() {
        return counterCHV3;
    }

    /**
     * Retrieve the CHV status flag
     * @return CHV status flag
     */
    public int getChvStatus() {
        return chvStatus;
    }

    /**
     * Retrieves the size (in bits) of the Signature key
     * @return Key size in bits
     */
    public int getKeySizeSig() {
        return keySizeSig;
    }

    /**
     * Retrieves the size (in bits) of the Decryption key
     * @return Key size in bits
     */
    public int getKeySizeDec() {
        return keySizeDec;
    }

    /**
     * Retrieves the size (in bits) of the Authentication key
     * @return Key size in bits
     */
    public int getKeySizeAut() {
        return keySizeAut;
    }

    /**
     * Retrieves the stored fingerprint of the Signature key.
     * NOTE could not match the real fingerprint of the actual key
     * @return 20 bytes fingerprint
     */
    public byte[] getFingerprintSig() {
        return fingerprintSig;
    }

    /**
     * Retrieves the stored fingerprint of the Decryption key.
     * NOTE could not match the real fingerprint of the actual key
     * @return 20 bytes fingerprint
     */
    public byte[] getFingerprintDec() {
        return fingerprintDec;
    }

    /**
     * Retrieves the stored fingerprint of the Authentication key.
     * NOTE could not match the real fingerprint of the actual key
     * @return 20 bytes fingerprint
     */
    public byte[] getFingerprintAut() {
        return fingerprintAut;
    }

    /**
     * Retrieve the creation date of the Signature key
     * @return Creation date
     */
    public Date getKeyDateSig() {
        return keyDateSig;
    }

    /**
     * Retrieve the creation date of the Decryption key
     * @return Creation date
     */
    public Date getKeyDateDec() {
        return keyDateDec;
    }

    /**
     * Retrieve the creation date of the Authentication key
     * @return Creation date
     */
    public Date getKeyDateAut() {
        return keyDateAut;
    }

    /**
     * Retrieve the status of the CHV3 safety lock (Enabled per default)
     * @return Safety lock status
     * @see #setCHV3SafetyLock(boolean)
     */
    public boolean isCHV3SafetyLock() {
        return CHV3SafetyLock;
    }

    /**
     * Enables or disables the CHV3 safety lock (Enabled per default).
     * Must be disabled if you intend to present CHV3 to the card
     * @param CHV3SafetyLock Lock status
     */
    public void setCHV3SafetyLock(boolean CHV3SafetyLock) {
        this.CHV3SafetyLock = CHV3SafetyLock;
    }

    /**
     * Retrieves the signature counter (that is reset to 0 by the card when
     * generating a new Signature key or importing a new one)
     * @return Signature counter
     */
    public int getSignatureCounter() {
        return signatureCounter;
    }

    /**
     * Retrieves the exponent size (in bits) for the Signature key
     * @return exponent size in bits
     */
    public int getKeyExpSizeSig() {
        return keyExpSizeSig;
    }

    /**
     * Retrieves the exponent size (in bits) for the Decryption key
     * @return exponent size in bits
     */
    public int getKeyExpSizeDec() {
        return keyExpSizeDec;
    }

    /**
     * Retrieves the exponent size (in bits) for the Authentication key
     * @return exponent size in bits
     */
    public int getKeyExpSizeAut() {
        return keyExpSizeAut;
    }

    /**
     * Retrieves the modulus of the Signature public key
     * @return public key modulus or null (no key on card)
     */
    public BigInteger getPubkeyModulusSig() {
        return pubkeyModulusSig;
    }

    /**
     * Retrieves the exponent of the Signature public key
     * @return public key exponent or null (no key on card)
     */
    public BigInteger getPubkeyExponentSig() {
        return pubkeyExponentSig;
    }

    /**
     * Retrieves the modulus of the Decryption(read Encryption) public key
     * @return public key modulus or null (no key on card)
     */
    public BigInteger getPubkeyModulusDec() {
        return pubkeyModulusDec;
    }

    /**
     * Retrieves the exponent of the Decryption(read Encryption) public key
     * @return public key exponent or null (no key on card)
     */
    public BigInteger getPubkeyExponentDec() {
        return pubkeyExponentDec;
    }

    /**
     * Retrieves the modulus of the Authentication public key
     * @return public key modulus or null (no key on card)
     */
    public BigInteger getPubkeyModulusAut() {
        return pubkeyModulusAut;
    }

    /**
     * Retrieves the exponent of the Authentication public key
     * @return public key exponent or null (no key on card)
     */
    public BigInteger getPubkeyExponentAut() {
        return pubkeyExponentAut;
    }

    /**
     * Check whether card support GET CHALLENGE (get random bytes)
     * @return True/False flag
     */
    public boolean supportsGetChallenge() {
        return supportsGetChallenge;
    }

    /**
     * Check whether card support private key import (from the external)
     * @return True/False flag
     */
    public boolean supportsKeyImport() {
        return supportsKeyImport;
    }

    /**
     * Check whether card support CHV status change flag
     * @return True/False flag
     */
    public boolean supportsCHVStatusChange() {
        return supportsCHVStatusChange;
    }

    /**
     * Check whether card support private Data Objects
     * @return True/False flag
     */
    public boolean supportsPrivateDOs() {
        return supportsPrivateDOs;
    }

    /**
     * Retrieves card holder fore-name
     * @return Card holder fore-name
     */
    public String getHolderName() {
        return holderName;
    }

    /**
     * Retrieves card holder family-name
     * @return Card holder family-name
     */
    public String getHolderSurname() {
        return holderSurname;
    }

    private static byte[] dateToTimestamp(Date iDate) {
        int tstamp = (int) (iDate.getTime() / 1000);
        return new byte[] { (byte) ((tstamp >> 24) & 0xFF), (byte) ((tstamp >> 16) & 0xFF), (byte) ((tstamp >> 8) & 0xFF), (byte) (tstamp & 0xFF) };
    }

    private static byte[] bigIntegerToUnsignedByteArray(BigInteger iNum, int iLen) {
        if (iNum.signum() == -1) throw new IllegalArgumentException("iNum");
        byte[] barr = iNum.toByteArray();
        byte[] data;
        if (iNum.bitLength() % 8 == 0) data = Arrays.copyOfRange(barr, 1, barr.length); else data = barr;
        byte[] out = new byte[iLen];
        int off = iLen - data.length;
        for (int i = 0; i < data.length; i++) out[i + off] = data[i];
        return out;
    }
}
