package alto.sec.pkcs;

import alto.sec.util.DerInputStream;
import alto.sec.util.DerOutputStream;
import alto.sec.util.DerValue;
import alto.sec.util.ObjectIdentifier;
import alto.sec.pkcs.ContentInfo;
import alto.sec.x509.AlgorithmId;
import alto.sec.x509.X500Name;
import alto.sec.x509.X509Certificate;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.io.*;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;
import java.security.UnrecoverableKeyException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;
import java.math.*;

/**
 * Rewritten from (sun security pkcs12) PKCS12KeyStore because a basic
 * PKCS12 operator is needed independent of the nice to have feature
 * of KeyStore integration.  A KeyStore will integrate from here.
 * 
 * <h3>{@link PKCS12}</h3>
 * 
 * Implements the PKCS#12 PFX protected using the Password privacy mode.
 * The contents are protected using Password integrity mode.
 *
 * <h3>Interoperability Notes</h3>
 * 
 * Currently {@link PKCS12} does not support PKCS#12 TrustedCertEntries.
 *
 * Currently {@link PKCS12}  supports the following PBE algorithms
 * <ul>
 *  <li> <code>pbeWithSHAAnd3KeyTripleDESCBC</code> to encrypt private keys</li>
 *  <li> <code>pbeWithSHAAnd40BitRC2CBC</code> to encrypt certificates</li>
 * </ul>
 *
 * Supported encryption in other PKCS#12 implementations
 * <pre>
 * Software and mode.     Certificate encryption  Private key encryption
 * ---------------------------------------------------------------------
 * MSIE4 (domestic            40 bit RC2.            40 bit RC2
 * and xport versions)
 * PKCS#12 export.
 *
 * MSIE4, 5 (domestic         40 bit RC2,            40 bit RC2,
 * and export versions)       3 key triple DES       3 key triple DES
 * PKCS#12 import.
 *
 * MSIE5                      40 bit RC2             3 key triple DES,
 * PKCS#12 export.                                   with SHA1 (168 bits)
 *
 * Netscape Communicator      40 bit RC2             3 key triple DES,
 * (domestic and export                              with SHA1 (168 bits)
 * versions) PKCS#12 export
 *
 * Netscape Communicator      40 bit ciphers only    All.
 * (export version)
 * PKCS#12 import.
 *
 * Netscape Communicator      All.                   All.
 * (domestic or fortified
 * version) PKCS#12 import.
 *
 * OpenSSL PKCS#12 code.      All.                   All.
 * ---------------------------------------------------------------------
 * </pre>
 *
 * 
 * 
 * @author Seema Malkani
 * @author Jeff Nisewanger
 * @author Jan Luehe
 * @author jdp
 */
public final class PKCS12 extends Object {

    private final String SHA1 = "SHA1";

    public static final int VERSION_3 = 3;

    private static final int keyBag[] = { 1, 2, 840, 113549, 1, 12, 10, 1, 2 };

    private static final int certBag[] = { 1, 2, 840, 113549, 1, 12, 10, 1, 3 };

    private static final int pkcs9Name[] = { 1, 2, 840, 113549, 1, 9, 20 };

    private static final int pkcs9KeyId[] = { 1, 2, 840, 113549, 1, 9, 21 };

    private static final int pkcs9certType[] = { 1, 2, 840, 113549, 1, 9, 22, 1 };

    private static final int pbeWithSHAAnd40BitRC2CBC[] = { 1, 2, 840, 113549, 1, 12, 1, 6 };

    private static final int pbeWithSHAAnd3KeyTripleDESCBC[] = { 1, 2, 840, 113549, 1, 12, 1, 3 };

    private static ObjectIdentifier PKCS8ShroudedKeyBag_OID;

    private static ObjectIdentifier CertBag_OID;

    private static ObjectIdentifier PKCS9FriendlyName_OID;

    private static ObjectIdentifier PKCS9LocalKeyId_OID;

    private static ObjectIdentifier PKCS9CertType_OID;

    private static ObjectIdentifier pbeWithSHAAnd40BitRC2CBC_OID;

    private static ObjectIdentifier pbeWithSHAAnd3KeyTripleDESCBC_OID;

    static {
        try {
            PKCS8ShroudedKeyBag_OID = new ObjectIdentifier(keyBag);
            CertBag_OID = new ObjectIdentifier(certBag);
            PKCS9FriendlyName_OID = new ObjectIdentifier(pkcs9Name);
            PKCS9LocalKeyId_OID = new ObjectIdentifier(pkcs9KeyId);
            PKCS9CertType_OID = new ObjectIdentifier(pkcs9certType);
            pbeWithSHAAnd40BitRC2CBC_OID = new ObjectIdentifier(pbeWithSHAAnd40BitRC2CBC);
            pbeWithSHAAnd3KeyTripleDESCBC_OID = new ObjectIdentifier(pbeWithSHAAnd3KeyTripleDESCBC);
        } catch (IOException ioe) {
        }
    }

    private static final int ITERATION_COUNT = 1024;

    private static final int SALT_LEN = 20;

    private int privateKeyCount;

    private SecureRandom random;

    private Date date;

    private byte[] protectedPrivKey;

    private X509Certificate chain[];

    private byte[] keyId;

    private String alias;

    public PKCS12(String alias, PrivateKey key, String password, X509Certificate cert) throws IOException, CertificateException {
        this(alias, key, password.toCharArray(), new X509Certificate[] { cert });
    }

    public PKCS12(String alias, PrivateKey key, String password, X509Certificate[] chain) throws IOException, CertificateException {
        this(alias, key, password.toCharArray(), chain);
    }

    /**
     * Assigns the given key to the given alias, protecting it with the given
     * password.
     *
     * <p>If the given key is of type <code>java.security.PrivateKey</code>,
     * it must be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key to be associated with the alias
     * @param password the password to protect the key
     * @param chain the certificate chain for the corresponding public
     * key (only required if the given key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if the given key cannot be protected, or
     * this operation fails for some other reason
     */
    public PKCS12(String alias, PrivateKey key, char[] password, X509Certificate[] chain) throws IOException, CertificateException {
        this.date = new Date();
        String keyFormat = key.getFormat();
        if ((keyFormat.equals("PKCS#8")) || (keyFormat.equals("PKCS8"))) {
            try {
                this.protectedPrivKey = this.encryptPrivateKey(key.getEncoded(), password);
            } catch (NoSuchAlgorithmException exc) {
                throw new CertificateException(exc);
            } catch (UnrecoverableKeyException exc) {
                throw new CertificateException(exc);
            } catch (InvalidParameterSpecException exc) {
                throw new CertificateException(exc);
            } catch (InvalidKeySpecException exc) {
                throw new CertificateException(exc);
            } catch (InvalidKeyException exc) {
                throw new CertificateException(exc);
            } catch (InvalidAlgorithmParameterException exc) {
                throw new CertificateException(exc);
            }
        } else {
            throw new CertificateException("Private key (" + keyFormat + ") is not encoded in PKCS#8");
        }
        if (chain != null) {
            if ((chain.length > 1) && (!validateChain(chain))) throw new CertificateException("Certificate chain is not valid"); else this.chain = chain.clone();
        }
        this.alias = alias.toLowerCase();
    }

    /**
     * Assigns the given key (that has already been protected) to the given
     * alias.
     *
     * <p>If the protected key is of type
     * <code>java.security.PrivateKey</code>, it must be accompanied by a
     * certificate chain certifying the corresponding public key. If the
     * underlying keystore implementation is of type <code>jks</code>,
     * <code>key</code> must be encoded as an
     * <code>EncryptedPrivateKeyInfo</code> as defined in the PKCS #8 standard.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key (in protected format) to be associated with the alias
     * @param chain the certificate chain for the corresponding public
     * key (only useful if the protected key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if this operation fails.
     */
    public PKCS12(String alias, byte[] key, X509Certificate[] chain) throws InvalidKeyException {
        super();
        try {
            new EncryptedPrivateKeyInfo(key);
        } catch (IOException ioe) {
            throw new InvalidKeyException("Private key is not stored as PKCS#8 EncryptedPrivateKeyInfo", ioe);
        }
        this.date = new Date();
        this.alias = alias;
        this.protectedPrivKey = key.clone();
        if (chain != null) {
            this.chain = chain.clone();
        }
    }

    /**
     * @see #decode(java.io.InputStream,char[])
     */
    public PKCS12(InputStream stream, char[] password) throws IOException, CertificateException {
        super();
        this.decode(stream, password);
    }

    public PKCS12(InputStream stream, String password, boolean close) throws IOException, CertificateException {
        this(stream, password.toCharArray(), close);
    }

    public PKCS12(InputStream stream, char[] password, boolean close) throws IOException, CertificateException {
        super();
        try {
            this.decode(stream, password);
        } finally {
            if (close) stream.close();
        }
    }

    /**
     * 
     */
    public void encode(OutputStream stream, String password) throws IOException, CertificateException {
        if (null != password) this.encode(stream, password.toCharArray()); else throw new IllegalArgumentException("password");
    }

    /**
     * @param stream the output stream to which this keystore is written.
     * @param password the password to generate the keystore integrity check
     *
     * @exception IOException if there was an I/O problem with data
     * @exception NoSuchAlgorithmException if the appropriate data integrity
     * algorithm could not be found
     * @exception CertificateException if any of the certificates included in
     * the keystore data could not be stored
     */
    public void encode(OutputStream stream, char[] password) throws IOException, CertificateException {
        if (password == null) {
            throw new IllegalArgumentException("password");
        } else {
            try {
                DerOutputStream pfx = new DerOutputStream();
                {
                    DerOutputStream version = new DerOutputStream();
                    version.putInteger(VERSION_3);
                    byte[] pfxVersion = version.toByteArray();
                    pfx.write(pfxVersion);
                }
                DerOutputStream authSafe = new DerOutputStream();
                DerOutputStream authSafeContentInfo = new DerOutputStream();
                {
                    byte[] safeContentData = this.createSafeContent();
                    ContentInfo dataContentInfo = new ContentInfo(safeContentData);
                    dataContentInfo.encode(authSafeContentInfo);
                }
                {
                    byte[] encrData = this.createEncryptedData(password);
                    ContentInfo encrContentInfo = new ContentInfo(ContentInfo.ENCRYPTED_DATA_OID, new DerValue(encrData));
                    encrContentInfo.encode(authSafeContentInfo);
                }
                {
                    DerOutputStream cInfo = new DerOutputStream();
                    cInfo.write(DerValue.tag_SequenceOf, authSafeContentInfo);
                    byte[] authenticatedSafe = cInfo.toByteArray();
                    ContentInfo contentInfo = new ContentInfo(authenticatedSafe);
                    contentInfo.encode(authSafe);
                    byte[] authSafeData = authSafe.toByteArray();
                    pfx.write(authSafeData);
                    byte[] macData = this.calculateMac(password, authenticatedSafe);
                    pfx.write(macData);
                }
                {
                    DerOutputStream pfxout = new DerOutputStream();
                    pfxout.write(DerValue.tag_Sequence, pfx);
                    byte[] pfxData = pfxout.toByteArray();
                    stream.write(pfxData, 0, pfxData.length);
                }
                stream.flush();
            } catch (NoSuchAlgorithmException exc) {
                throw new CertificateException(exc);
            } catch (InvalidParameterSpecException exc) {
                throw new CertificateException(exc);
            } catch (InvalidKeySpecException exc) {
                throw new CertificateException(exc);
            } catch (InvalidKeyException exc) {
                throw new CertificateException(exc);
            } catch (InvalidAlgorithmParameterException exc) {
                throw new CertificateException(exc);
            }
        }
    }

    /**
     * <p>If a password is given, it is used to check the integrity of the
     * keystore data. Otherwise, the integrity of the keystore is not checked.
     *
     * @param stream the input stream from which the keystore is loaded
     * @param password the (optional) password used to check the integrity of
     * the keystore.
     *
     * @exception IOException if there is an I/O or format problem with the
     * keystore data
     * @exception NoSuchAlgorithmException if the algorithm used to check
     * the integrity of the keystore cannot be found
     * @exception CertificateException if any of the certificates in the
     * keystore could not be loaded
     */
    public void decode(InputStream stream, char[] password) throws IOException, CertificateException {
        DataInputStream dis;
        CertificateFactory cf = null;
        byte[] encoded = null;
        DerValue val = new DerValue(stream);
        DerInputStream s = val.toDerInputStream();
        if (VERSION_3 != s.getInteger()) {
            throw new CertificateException("Not in PKCS#12 Version 3 format");
        } else {
            ContentInfo authSafe = new ContentInfo(s);
            ObjectIdentifier contentType = authSafe.getContentType();
            if (contentType.equals(ContentInfo.DATA_OID)) {
                byte[] authSafeData = authSafe.getData();
                DerInputStream as = new DerInputStream(authSafeData);
                DerValue[] safeContentsArray = as.getSequence(2);
                int contentInfoCount = safeContentsArray.length;
                this.privateKeyCount = 0;
                try {
                    for (int cc = 0; cc < contentInfoCount; cc++) {
                        DerInputStream sci = new DerInputStream(safeContentsArray[cc].toByteArray());
                        ContentInfo safeContents = new ContentInfo(sci);
                        contentType = safeContents.getContentType();
                        byte[] safeContentsData = null;
                        if (contentType.equals(ContentInfo.DATA_OID)) {
                            safeContentsData = safeContents.getData();
                        } else if (contentType.equals(ContentInfo.ENCRYPTED_DATA_OID)) {
                            if (null == password) continue; else {
                                DerInputStream edi = safeContents.getContent().toDerInputStream();
                                int edVersion = edi.getInteger();
                                DerValue[] seq = edi.getSequence(2);
                                ObjectIdentifier edContentType = seq[0].getOID();
                                byte[] eAlgId = seq[1].toByteArray();
                                if (!seq[2].isContextSpecific((byte) 0)) {
                                    throw new CertificateException("encrypted content not present!");
                                }
                                byte newTag = DerValue.tag_OctetString;
                                if (seq[2].isConstructed()) newTag |= 0x20;
                                seq[2].resetTag(newTag);
                                safeContentsData = seq[2].getOctetString();
                                DerInputStream in = seq[1].toDerInputStream();
                                ObjectIdentifier algOid = in.getOID();
                                AlgorithmParameters algParams = this.parseAlgParameters(in);
                                try {
                                    SecretKey skey = this.getPBEKey(password);
                                    Cipher cipher = Cipher.getInstance(algOid.toString());
                                    cipher.init(Cipher.DECRYPT_MODE, skey, algParams);
                                    safeContentsData = cipher.doFinal(safeContentsData);
                                } catch (javax.crypto.IllegalBlockSizeException exc) {
                                    throw new CertificateException(exc);
                                } catch (javax.crypto.NoSuchPaddingException exc) {
                                    throw new CertificateException(exc);
                                } catch (javax.crypto.BadPaddingException exc) {
                                    throw new CertificateException(exc);
                                }
                            }
                        } else {
                            throw new CertificateException("Public Key protected PKCS12 not supported");
                        }
                        DerInputStream sc = new DerInputStream(safeContentsData);
                        this.loadSafeContents(sc, password);
                    }
                    this.validateMac(authSafeData, password, s);
                } catch (NoSuchAlgorithmException exc) {
                    throw new CertificateException(exc);
                } catch (InvalidParameterSpecException exc) {
                    throw new CertificateException(exc);
                } catch (InvalidKeySpecException exc) {
                    throw new CertificateException(exc);
                } catch (InvalidKeyException exc) {
                    throw new CertificateException(exc);
                } catch (InvalidAlgorithmParameterException exc) {
                    throw new CertificateException(exc);
                }
            } else {
                throw new CertificateException("Public Key protected PKCS12 not supported");
            }
        }
    }

    public boolean hasAlias() {
        return (null != this.alias);
    }

    public String getAlias() {
        return this.alias;
    }

    public int countCertificates() {
        X509Certificate[] chain = this.chain;
        if (null != chain) return chain.length; else return 0;
    }

    public X509Certificate getCertificate(int idx) {
        X509Certificate[] chain = this.chain;
        if (null != chain && -1 < idx && idx < chain.length) return chain[idx]; else throw new java.lang.ArrayIndexOutOfBoundsException(String.valueOf(idx));
    }

    public PrivateKey getKey(String password) throws NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException {
        if (null != password) return this.getKey(password.toCharArray()); else throw new IllegalArgumentException();
    }

    /**
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     * @param password the password for recovering the key
     *
     * @return the requested key, or null if the given alias does not exist
     * or does not identify a <i>key entry</i>.
     *
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     * key cannot be found
     * @exception UnrecoverableKeyException if the key cannot be recovered
     * (e.g., the given password is wrong).
     */
    public PrivateKey getKey(char[] password) throws NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException {
        byte[] encrBytes = this.protectedPrivKey;
        byte[] encryptedKey;
        AlgorithmParameters algParams;
        ObjectIdentifier algOid;
        try {
            EncryptedPrivateKeyInfo encrInfo = new EncryptedPrivateKeyInfo(encrBytes);
            encryptedKey = encrInfo.getEncryptedData();
            DerValue val = new DerValue(encrInfo.getAlgorithm().encode());
            DerInputStream in = val.toDerInputStream();
            algOid = in.getOID();
            algParams = parseAlgParameters(in);
        } catch (IOException ioe) {
            throw new InvalidKeyException("Private key not stored as PKCS#8 EncryptedPrivateKeyInfo", ioe);
        }
        try {
            SecretKey skey = this.getPBEKey(password);
            Cipher cipher = Cipher.getInstance(algOid.toString());
            cipher.init(Cipher.DECRYPT_MODE, skey, algParams);
            byte[] privateKeyInfo = cipher.doFinal(encryptedKey);
            PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(privateKeyInfo);
            DerValue val = new DerValue(privateKeyInfo);
            DerInputStream in = val.toDerInputStream();
            int i = in.getInteger();
            DerValue[] value = in.getSequence(2);
            AlgorithmId algId = new AlgorithmId(value[0].getOID());
            String algName = algId.getName();
            KeyFactory kfac = KeyFactory.getInstance(algName);
            return kfac.generatePrivate(kspec);
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    /**
     * Generate random salt
     */
    private byte[] getSalt() {
        byte[] salt = new byte[SALT_LEN];
        if (random == null) {
            random = new SecureRandom();
        }
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Generate PBE Algorithm Parameters
     */
    private AlgorithmParameters getAlgorithmParameters(String algorithm) throws NoSuchAlgorithmException, InvalidParameterSpecException {
        PBEParameterSpec paramSpec = new PBEParameterSpec(this.getSalt(), ITERATION_COUNT);
        AlgorithmParameters algParams = AlgorithmParameters.getInstance(algorithm);
        algParams.init(paramSpec);
        return algParams;
    }

    /**
     */
    private AlgorithmParameters parseAlgParameters(DerInputStream in) throws IOException, NoSuchAlgorithmException, InvalidParameterSpecException {
        AlgorithmParameters algParams = null;
        DerValue params;
        if (in.available() == 0) {
            params = null;
        } else {
            params = in.getDerValue();
            if (params.tag == DerValue.tag_Null) {
                params = null;
            }
        }
        if (params != null) {
            algParams = AlgorithmParameters.getInstance("PBE");
            algParams.init(params.toByteArray());
        }
        return algParams;
    }

    /**
     * Generate PBE key
     */
    private SecretKey getPBEKey(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKeyFactory skFac = SecretKeyFactory.getInstance("PBE");
        return skFac.generateSecret(keySpec);
    }

    /**
     * Encrypt private key using Password-based encryption (PBE)
     * as defined in PKCS#5.
     *
     * NOTE: Currently pbeWithSHAAnd3-KeyTripleDES-CBC algorithmID is
     *       used to derive the key and IV.
     *
     * @return encrypted private key encoded as EncryptedPrivateKeyInfo
     */
    private byte[] encryptPrivateKey(byte[] data, char[] password) throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidParameterSpecException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameters algParams = getAlgorithmParameters("PBEWithSHA1AndDESede");
        try {
            SecretKey skey = getPBEKey(password);
            Cipher cipher = Cipher.getInstance("PBEWithSHA1AndDESede");
            cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);
            byte[] encryptedKey = cipher.doFinal(data);
            AlgorithmId algid = new AlgorithmId(pbeWithSHAAnd3KeyTripleDESCBC_OID, algParams);
            EncryptedPrivateKeyInfo encrInfo = new EncryptedPrivateKeyInfo(algid, encryptedKey);
            return encrInfo.getEncoded();
        } catch (javax.crypto.IllegalBlockSizeException exc) {
            throw new NoSuchAlgorithmException(exc);
        } catch (javax.crypto.NoSuchPaddingException exc) {
            throw new NoSuchAlgorithmException(exc);
        } catch (javax.crypto.BadPaddingException exc) {
            throw new NoSuchAlgorithmException(exc);
        }
    }

    private byte[] generateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(data);
        return md.digest();
    }

    /** 
     * Calculate MAC using HMAC algorithm for password integrity.
     *
     * Hash-based MAC algorithm combines secret key with message digest to
     * create a message authentication code (MAC)
     */
    private byte[] calculateMac(char[] passwd, byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] salt = this.getSalt();
        Mac m = Mac.getInstance("HmacPBESHA1");
        PBEParameterSpec params = new PBEParameterSpec(salt, ITERATION_COUNT);
        SecretKey key = this.getPBEKey(passwd);
        m.init(key, params);
        m.update(data);
        byte[] macResult = m.doFinal();
        PKCS12MacData macData = new PKCS12MacData(SHA1, macResult, salt, ITERATION_COUNT);
        DerOutputStream bytes = new DerOutputStream();
        bytes.write(macData.getEncoded());
        return bytes.toByteArray();
    }

    private void validateMac(byte[] authSafeData, char[] password, DerInputStream s) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        if (password != null && s.available() > 0) {
            PKCS12MacData macData = new PKCS12MacData(s);
            String algName = macData.getDigestAlgName().toUpperCase();
            if (algName.equals("SHA") || algName.equals("SHA-1")) {
                algName = SHA1;
            }
            Mac m = Mac.getInstance("HmacPBE" + algName);
            PBEParameterSpec params = new PBEParameterSpec(macData.getSalt(), macData.getIterations());
            SecretKey key = this.getPBEKey(password);
            m.init(key, params);
            m.update(authSafeData);
            byte[] macResult = m.doFinal();
            if (!Arrays.equals(macData.getDigest(), macResult)) {
                throw new SecurityException("Failed PKCS12 integrity checking");
            }
        }
    }

    /**
     * Validate Certificate Chain
     */
    private boolean validateChain(X509Certificate[] certChain) {
        int count1 = certChain.length, count0 = (count1 - 1);
        for (int c0 = 0, c1; c0 < count0; c0++) {
            X500Name issuerDN = certChain[c0].getIssuerX500Principal();
            c1 = (c0 + 1);
            if (c1 < count1) {
                X500Name subjectDN = certChain[c1].getSubjectX500Principal();
                if (!(issuerDN.equals(subjectDN))) return false;
            } else return false;
        }
        return true;
    }

    /**
     * Create PKCS#12 Attributes, friendlyName and localKeyId.
     *
     * Although attributes are optional, they could be required.
     * For e.g. localKeyId attribute is required to match the
     * private key with the associated end-entity certificate.
     *
     * PKCS8ShroudedKeyBags include unique localKeyID and friendlyName.
     * CertBags may or may not include attributes depending on the type
     * of Certificate. In end-entity certificates, localKeyID should be
     * unique, and the corresponding private key should have the same
     * localKeyID. For trusted CA certs in the cert-chain, localKeyID
     * attribute is not required, hence most vendors don't include it.
     * NSS/Netscape require it to be unique or null, where as IE/OpenSSL
     * ignore it.
     *
     * Here is a list of pkcs12 attribute values in CertBags.
     * <pre>
     * PKCS12 Attribute       NSS/Netscape    IE     OpenSSL    J2SE
     * --------------------------------------------------------------
     * LocalKeyId
     * (In EE cert only,
     *  NULL in CA certs)      true          true     true      true
     *
     * friendlyName            unique        same/    same/     unique
     *                                       unique   unique/
     *                                                null
     * </pre>
     *
     * Note: OpenSSL adds friendlyName for end-entity cert only, and
     * removes the localKeyID and friendlyName for CA certs.
     * If the CertBag did not have a friendlyName, most vendors will
     * add it, and assign it to the DN of the cert.
     */
    private byte[] getBagAttributes(String alias, byte[] keyId) throws IOException {
        if ((alias == null) && (keyId == null)) {
            return null;
        } else {
            byte[] localKeyID = null;
            byte[] friendlyName = null;
            DerOutputStream bagAttrs = new DerOutputStream();
            if (alias != null) {
                DerOutputStream bagAttr1 = new DerOutputStream();
                bagAttr1.putOID(PKCS9FriendlyName_OID);
                DerOutputStream bagAttrContent1 = new DerOutputStream();
                DerOutputStream bagAttrValue1 = new DerOutputStream();
                bagAttrContent1.putBMPString(alias);
                bagAttr1.write(DerValue.tag_Set, bagAttrContent1);
                bagAttrValue1.write(DerValue.tag_Sequence, bagAttr1);
                friendlyName = bagAttrValue1.toByteArray();
            }
            if (keyId != null) {
                DerOutputStream bagAttr2 = new DerOutputStream();
                bagAttr2.putOID(PKCS9LocalKeyId_OID);
                DerOutputStream bagAttrContent2 = new DerOutputStream();
                DerOutputStream bagAttrValue2 = new DerOutputStream();
                bagAttrContent2.putOctetString(keyId);
                bagAttr2.write(DerValue.tag_Set, bagAttrContent2);
                bagAttrValue2.write(DerValue.tag_Sequence, bagAttr2);
                localKeyID = bagAttrValue2.toByteArray();
            }
            DerOutputStream attrs = new DerOutputStream();
            if (friendlyName != null) {
                attrs.write(friendlyName);
            }
            if (localKeyID != null) {
                attrs.write(localKeyID);
            }
            bagAttrs.write(DerValue.tag_Set, attrs);
            return bagAttrs.toByteArray();
        }
    }

    /**
     * Create EncryptedData content type, that contains EncryptedContentInfo.
     * Includes certificates in individual SafeBags of type CertBag.
     * Each CertBag may include pkcs12 attributes
     * (see comments in getBagAttributes)
     */
    private byte[] createEncryptedData(char[] password) throws IOException, CertificateException, NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        DerOutputStream out = new DerOutputStream();
        String alias = this.alias;
        int chainLen;
        if (this.chain == null) {
            chainLen = 0;
        } else {
            chainLen = this.chain.length;
        }
        for (int i = 0; i < chainLen; i++) {
            X509Certificate cert = this.chain[i];
            DerOutputStream safeBag = new DerOutputStream();
            safeBag.putOID(CertBag_OID);
            DerOutputStream certBag = new DerOutputStream();
            certBag.putOID(PKCS9CertType_OID);
            {
                DerOutputStream certValue = new DerOutputStream();
                certValue.putOctetString(cert.getEncoded());
                certBag.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), certValue);
            }
            {
                DerOutputStream certout = new DerOutputStream();
                certout.write(DerValue.tag_Sequence, certBag);
                byte[] certBagValue = certout.toByteArray();
                DerOutputStream bagValue = new DerOutputStream();
                bagValue.write(certBagValue);
                safeBag.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), bagValue);
            }
            {
                byte[] bagAttrs = null;
                String friendlyName = cert.getSubjectX500Principal().getName();
                if (i == 0) {
                    bagAttrs = this.getBagAttributes(friendlyName, this.keyId);
                } else {
                    bagAttrs = this.getBagAttributes(friendlyName, null);
                }
                if (bagAttrs != null) {
                    safeBag.write(bagAttrs);
                }
            }
            out.write(DerValue.tag_Sequence, safeBag);
        }
        {
            DerOutputStream safeBagValue = new DerOutputStream();
            safeBagValue.write(DerValue.tag_SequenceOf, out);
            byte[] safeBagData = safeBagValue.toByteArray();
            byte[] encrContentInfo = this.encryptContent(safeBagData, password);
            DerOutputStream encrData = new DerOutputStream();
            DerOutputStream encrDataContent = new DerOutputStream();
            encrData.putInteger(0);
            encrData.write(encrContentInfo);
            encrDataContent.write(DerValue.tag_Sequence, encrData);
            return encrDataContent.toByteArray();
        }
    }

    private byte[] createSafeContent() throws IOException, CertificateException {
        DerOutputStream out = new DerOutputStream();
        DerOutputStream safeBag = new DerOutputStream();
        safeBag.putOID(PKCS8ShroudedKeyBag_OID);
        byte[] encrBytes = this.protectedPrivKey;
        EncryptedPrivateKeyInfo encrInfo = new EncryptedPrivateKeyInfo(encrBytes);
        DerOutputStream bagValue = new DerOutputStream();
        bagValue.write(encrInfo.getEncoded());
        safeBag.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), bagValue);
        byte[] bagAttrs = this.getBagAttributes(this.alias, this.keyId);
        safeBag.write(bagAttrs);
        out.write(DerValue.tag_Sequence, safeBag);
        DerOutputStream safeBagValue = new DerOutputStream();
        safeBagValue.write(DerValue.tag_Sequence, out);
        return safeBagValue.toByteArray();
    }

    private byte[] encryptContent(byte[] data, char[] password) throws IOException, NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] encryptedData = null;
        AlgorithmParameters algParams = getAlgorithmParameters("PBEWithSHA1AndRC2_40");
        DerOutputStream bytes = new DerOutputStream();
        AlgorithmId algId = new AlgorithmId(pbeWithSHAAnd40BitRC2CBC_OID, algParams);
        algId.encode(bytes);
        byte[] encodedAlgId = bytes.toByteArray();
        try {
            SecretKey skey = getPBEKey(password);
            Cipher cipher = Cipher.getInstance("PBEWithSHA1AndRC2_40");
            cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);
            encryptedData = cipher.doFinal(data);
        } catch (javax.crypto.IllegalBlockSizeException exc) {
            throw new NoSuchAlgorithmException(exc);
        } catch (javax.crypto.NoSuchPaddingException exc) {
            throw new NoSuchAlgorithmException(exc);
        } catch (javax.crypto.BadPaddingException exc) {
            throw new NoSuchAlgorithmException(exc);
        }
        DerOutputStream bytes2 = new DerOutputStream();
        bytes2.putOID(ContentInfo.DATA_OID);
        bytes2.write(encodedAlgId);
        DerOutputStream tmpout2 = new DerOutputStream();
        tmpout2.putOctetString(encryptedData);
        bytes2.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT, false, (byte) 0), tmpout2);
        DerOutputStream out = new DerOutputStream();
        out.write(DerValue.tag_Sequence, bytes2);
        return out.toByteArray();
    }

    private void loadSafeContents(DerInputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        DerValue[] safeBags = stream.getSequence(2);
        int count = safeBags.length;
        for (int i = 0; i < count; i++) {
            ObjectIdentifier bagId;
            DerInputStream sbi;
            DerValue bagValue;
            Object bagItem = null;
            sbi = safeBags[i].toDerInputStream();
            bagId = sbi.getOID();
            bagValue = sbi.getDerValue();
            if (!bagValue.isContextSpecific((byte) 0)) {
                throw new CertificateException("unsupported PKCS12 bag value type " + bagValue.tag);
            }
            bagValue = bagValue.data.getDerValue();
            if (bagId.equals(PKCS8ShroudedKeyBag_OID)) {
                this.protectedPrivKey = bagValue.toByteArray();
                privateKeyCount++;
            } else if (bagId.equals(CertBag_OID)) {
                DerInputStream cs = new DerInputStream(bagValue.toByteArray());
                DerValue[] certValues = cs.getSequence(2);
                ObjectIdentifier certId = certValues[0].getOID();
                if (!certValues[1].isContextSpecific((byte) 0)) {
                    throw new CertificateException("unsupported PKCS12 cert value type " + certValues[1].tag);
                }
                DerValue certValue = certValues[1].data.getDerValue();
                X509Certificate cert = new X509Certificate(new ByteArrayInputStream(certValue.getOctetString()));
                if (null == this.chain) this.chain = new X509Certificate[] { cert }; else {
                    X509Certificate[] chain = this.chain;
                    int chainlen = chain.length;
                    X509Certificate[] copier = new X509Certificate[chainlen + 1];
                    System.arraycopy(chain, 0, copier, 0, chainlen);
                    copier[chainlen] = cert;
                    this.chain = copier;
                }
            }
            try {
                DerValue[] attrSet = sbi.getSet(2);
                if (attrSet != null) {
                    for (int j = 0; j < attrSet.length; j++) {
                        DerInputStream as = new DerInputStream(attrSet[j].toByteArray());
                        DerValue[] attrSeq = as.getSequence(2);
                        ObjectIdentifier attrId = attrSeq[0].getOID();
                        DerInputStream vs = new DerInputStream(attrSeq[1].toByteArray());
                        DerValue[] valSet;
                        try {
                            valSet = vs.getSet(1);
                        } catch (IOException e) {
                            throw new CertificateException("Attribute " + attrId + " should have a value ", e);
                        }
                        if (attrId.equals(PKCS9FriendlyName_OID)) {
                            this.alias = valSet[0].getBMPString();
                        } else if (attrId.equals(PKCS9LocalKeyId_OID)) {
                            this.keyId = valSet[0].getOctetString();
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }
}
