package issrg.SAWS;

import issrg.SAWS.callback.CertificateDataCallback;
import issrg.SAWS.callback.SAWSChoiceCallback;
import issrg.SAWS.callback.SAWSGUICallbackHandler;
import issrg.SAWS.callback.SAWSPasswordCallback;
import issrg.SAWS.callback.SAWSTextInputCallback;
import issrg.SAWS.callback.SAWSTextOutputCallback;
import issrg.SAWS.util.CertificateData;
import issrg.SAWS.util.SAWSLogWriter;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.X509V1CertificateGenerator;

/**
 *  Class to manage the key store.
 *
 * @author W.Xu, E. Silva
  */
public class TCBKeystoreManagement {

    private String encryptionKeystoreLocation = null;

    private String signingKeystoreLocation = null;

    private int numberOfPasswordShares = 2;

    private int numberOfEncPasswordShares = 2;

    private String rootCA = null;

    private String vtPKC = null;

    private File encryptionKeyfile = null;

    private File signingKeyfile = null;

    private String sawsPW = null;

    private String sawsEncPW = null;

    private PublicKey sawsEncryptionPublicKey = null;

    private PrivateKey sawsEncryptionPrivateKey = null;

    private PublicKey sawsSigningPublicKey = null;

    private PrivateKey sawsSigningPrivateKey = null;

    private PublicKey vtEncryptionPublicKey = null;

    private PublicKey rootCAPublicKey = null;

    private int debugLevel = 0;

    private SecretKey sawsTCBSecretKey = null;

    private PBEParameterSpec paramSpec = null;

    private byte[] baSigningPublicKeyCert = null;

    private String signingAlgName = null;

    private CallbackHandler callbackHandler = new SAWSGUICallbackHandler();

    private Callback[] cbs = null;

    private java.security.cert.Certificate sawsEncCertificate;

    private java.security.cert.Certificate sawsSigCertificate;

    private static SAWSLogWriter sawsDebugLog = new SAWSLogWriter(TCBKeystoreManagement.class.getName());

    /** Creates a new instance of TCBKeystoreManagement. */
    public TCBKeystoreManagement(String signkeystoreLocation, int numOfPassShares, String encKeystoreLocation, int numOfEncPassShares, String rootCAPara, String vtPKCPara, int debugLevel, String signingAlgName) {
        signingKeystoreLocation = signkeystoreLocation;
        numberOfPasswordShares = numOfPassShares;
        encryptionKeystoreLocation = encKeystoreLocation;
        numberOfEncPasswordShares = numOfEncPassShares;
        rootCA = rootCAPara;
        vtPKC = vtPKCPara;
        this.debugLevel = debugLevel;
        this.signingAlgName = signingAlgName;
    }

    public TCBKeystoreManagement(String signkeystoreLocation, int numOfPassShares, String encKeystoreLocation, int numOfEncPassShares, String rootCAPara, String vtPKCPara, int debugLevel, String signingAlgName, CallbackHandler ch) {
        this(signkeystoreLocation, numOfPassShares, encKeystoreLocation, numOfEncPassShares, rootCAPara, vtPKCPara, debugLevel, signingAlgName);
        this.callbackHandler = ch;
    }

    /**
   * Checks if the signing keystore exists.
   * SAWS stops if this keystore is missing.
   */
    public void checkSigningKeystoreFile() {
        signingKeyfile = new File(signingKeystoreLocation);
        if (!signingKeyfile.exists()) {
            this.showMessage("The signing keystore is missing: " + signingKeystoreLocation + "\n\nThis is the first time to run SAWS, or the signing keystore has been removed illegally." + "\nSAWS will stop. Please use SAWS to generate a new signing keystore, then restart SAWS.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
    }

    /**
   * Checks if the encryption keystore exists.
   * SAWS stops if this keystore is missing.
   */
    public void checkEncKeystoreFile() {
        encryptionKeyfile = new File(encryptionKeystoreLocation);
        if (!encryptionKeyfile.exists()) {
            this.showMessage("The encryption keystore is missing: " + encryptionKeystoreLocation + "\n\nThis is the first time to run SAWS, or the encryption keystore is removed illegally. " + "\nSAWS will stop. " + "\nPlease recover the encryption keystore from your backup if you have backed it up before, " + "\nor use SAWS to create a new encryption keystore, then restart SAWS.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
    }

    /**
   * This method reads in the signing keypair and the encryption keypair, generates the symmetric key used for reading
   * and writing lastSN and lastHash files. Called by SAWSServer(). 
   * 
   */
    public void readKeystores() {
        sawsPW = getSAWSPassword3Attempts(signingKeystoreLocation, numberOfPasswordShares, "signing", false);
        if (sawsPW == null) {
            System.exit(-1);
        }
        sawsEncPW = getSAWSPassword3Attempts(encryptionKeystoreLocation, numberOfEncPasswordShares, "encryption", false);
        if (sawsEncPW == null) {
            System.exit(-1);
        }
        String signer = "saws";
        KeyStore signKeystore = null;
        try {
            signKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            try {
                signKeystore.load(new BufferedInputStream(new FileInputStream(signingKeyfile)), sawsPW.toCharArray());
            } catch (IOException e) {
                this.showMessage("Reading signing keystore error. The signing keystore has been tampered with.", SAWSTextOutputCallback.ERROR);
                if (debugLevel >= SAWSConstant.ErrorInfo) {
                    sawsDebugLog.write(e.toString());
                }
                System.exit(-1);
            }
            sawsSigningPrivateKey = (PrivateKey) signKeystore.getKey(signer, sawsPW.toCharArray());
            sawsSigCertificate = signKeystore.getCertificate(signer);
            baSigningPublicKeyCert = sawsSigCertificate.getEncoded();
            sawsSigningPublicKey = sawsSigCertificate.getPublicKey();
            java.security.cert.Certificate caCert = signKeystore.getCertificate("rootca");
            if ((caCert == null) || (rootCA == null)) {
                this.showMessage("The rootCA PKC is missing in the SAWS configuration file or in the signing keystore." + "\nSAWS will stop.", SAWSTextOutputCallback.ERROR);
                System.exit(-1);
            } else {
                rootCAPublicKey = caCert.getPublicKey();
            }
        } catch (Exception e2) {
            this.showMessage("Something wrong with signing keystore reading.", SAWSTextOutputCallback.ERROR);
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e2.toString());
            System.exit(-1);
        }
        KeyStore encKeystore = null;
        try {
            encKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            encKeystore.load(new BufferedInputStream(new FileInputStream(encryptionKeyfile)), sawsEncPW.toCharArray());
            sawsEncryptionPrivateKey = (PrivateKey) encKeystore.getKey(signer, sawsEncPW.toCharArray());
            sawsEncCertificate = encKeystore.getCertificate(signer);
            sawsEncryptionPublicKey = sawsEncCertificate.getPublicKey();
        } catch (Exception e2) {
            this.showMessage("Something wrong with encryption keystore reading.", SAWSTextOutputCallback.ERROR);
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e2.toString());
            System.exit(-1);
        }
        try {
            FileInputStream fis = new FileInputStream(vtPKC);
            BufferedInputStream bis = new BufferedInputStream(fis);
            java.security.cert.CertificateFactory cf = null;
            cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate certTemp = cf.generateCertificate(bis);
            vtEncryptionPublicKey = certTemp.getPublicKey();
        } catch (Exception e2) {
            this.showMessage("SAWS VT encryption key is not correct.", SAWSTextOutputCallback.ERROR);
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e2.toString());
        }
    }

    public PublicKey getrootCAPublicKey() {
        return rootCAPublicKey;
    }

    public PublicKey getvtEncryptionPublicKey() {
        return vtEncryptionPublicKey;
    }

    public PublicKey getsawsEncryptionPublicKey() {
        return sawsEncryptionPublicKey;
    }

    public PrivateKey getsawsEncryptionPrivateKey() {
        return sawsEncryptionPrivateKey;
    }

    public PrivateKey getsawsSigningPrivateKey() {
        return sawsSigningPrivateKey;
    }

    public PublicKey getsawsSigningPublicKey() {
        return sawsSigningPublicKey;
    }

    public byte[] getbaSigningPublicKeyCert() {
        return baSigningPublicKeyCert;
    }

    public String getsigningAlgName() {
        return signingAlgName;
    }

    public SecretKey getsawsTCBSecretKey() {
        sawsTCBSecretKey = generateSecretKey(sawsPW);
        return sawsTCBSecretKey;
    }

    public PBEParameterSpec getparamSpec() {
        byte[] salt = { (byte) 0x11, (byte) 0x23, (byte) 0x53, (byte) 0x65, (byte) 0xbc, (byte) 0xef, (byte) 0xf1, (byte) 0x34 };
        paramSpec = new PBEParameterSpec(salt, 10);
        return paramSpec;
    }

    /**
   * This method generates a symmetric key based on a password. 
   *
   * @param password String is the password used to generate the symmetric key.
   */
    private SecretKey generateSecretKey(String password) {
        SecretKey sk = null;
        try {
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            sk = kf.generateSecret(keySpec);
        } catch (Exception e) {
            this.showMessage("Error when generating PBE TCBSecretKey!", SAWSTextOutputCallback.ERROR);
            if (debugLevel >= SAWSConstant.ErrorInfo) {
                sawsDebugLog.write(e.toString());
            }
            return null;
        }
        return sk;
    }

    /**
   * This method is to create a randomised password 
   *
   * @param password input string
   * 
   * @return String randomised password.
   */
    private String generateRandomPW(String password) {
        java.security.MessageDigest firstHash = null;
        try {
            firstHash = java.security.MessageDigest.getInstance("SHA1");
        } catch (Exception e2) {
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e2.toString());
        }
        byte[] sawsHash = null;
        String temp = password;
        for (int i = 0; i < 10; ++i) {
            sawsHash = firstHash.digest(temp.getBytes());
            temp = new String(sawsHash);
        }
        String pw = new String(Base64.encode(sawsHash));
        return pw;
    }

    /**
     * This method is to create a keystore using bouncy castle API
     *
     * @param keystoreLocation is the path for the keystore. 
     * @param pw is the password used for protecting the keystores. 
     * 
     * @return 0 for success.
     */
    private boolean createKeystore(String keystoreLocation, String pw, CertificateData certificateData) {
        KeyPairGenerator kpg = null;
        X509Certificate cert = null;
        KeyPair kp = null;
        KeyStore ks = null;
        Security.addProvider(new BouncyCastleProvider());
        BufferedOutputStream bos = null;
        boolean created = false;
        try {
            kpg = KeyPairGenerator.getInstance(certificateData.getAlgorithm());
            kpg.initialize(certificateData.getKeySize());
            kp = kpg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            this.showMessage("Invalid encryption algorithm for key pair generation.", SAWSTextOutputCallback.ERROR);
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Invalid encryption algorithm for key pair generation.");
            }
            return false;
        }
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal dn = new X500Principal(certificateData.toSubjectName());
        certGen.setSerialNumber(new BigInteger("1"));
        certGen.setIssuerDN(dn);
        certGen.setNotBefore(new Date());
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.add(Calendar.DATE, certificateData.getValidity());
        certGen.setNotAfter(c.getTime());
        c = null;
        certGen.setSubjectDN(dn);
        certGen.setPublicKey(kp.getPublic());
        certGen.setSignatureAlgorithm("SHA1with" + certificateData.getAlgorithm());
        try {
            cert = certGen.generate(kp.getPrivate(), "BC");
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error creating self-signed certificate. " + e);
            }
            return false;
        }
        try {
            if (cert != null) {
                ks = KeyStore.getInstance("JKS");
                try {
                    ks.load(null, null);
                } catch (Exception e) {
                    if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                        this.sawsDebugLog.write("Error creating keystore: Keystore instance could not be initialized.");
                    }
                    return false;
                }
                ks.setCertificateEntry("saws", cert);
                Certificate[] chain = { cert };
                ks.setKeyEntry("saws", kp.getPrivate(), pw.toCharArray(), chain);
            } else {
                if (this.debugLevel >= SAWSConstant.WarningInfo) {
                    this.sawsDebugLog.write("Public key certificate could not be created.");
                }
                return false;
            }
        } catch (KeyStoreException e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error creating keystore: " + e.getMessage());
            }
            return false;
        }
        try {
            bos = new BufferedOutputStream(new FileOutputStream(new File(keystoreLocation)));
        } catch (FileNotFoundException e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error creating keystore: file was not created or not found.");
            }
            return false;
        }
        try {
            ks.store(bos, pw.toCharArray());
            created = true;
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error writing keystore to file: " + e.getMessage());
            }
            return false;
        }
        return created;
    }

    /**
   * Method that creates public key certificate request file, to be signed by a
   * Certificate Authority.
   */
    public void outputPKCRequest() {
        signingKeyfile = new File(signingKeystoreLocation);
        if (!signingKeyfile.exists()) {
            this.showMessage("The SAWS signing keystore doesn't exist. Please first use SAWS to create a signing keystore. ", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        sawsPW = getSAWSPassword3Attempts(signingKeystoreLocation, numberOfPasswordShares, "signing", false);
        if (sawsPW == null) {
            this.showMessage("The password to the signing keystore is wrong. SAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        boolean b1 = createPKCReqest(signingKeystoreLocation, sawsPW);
        if (b1) {
            this.showMessage("The SAWS PKC request file sawsRequest.csr " + "has been created successfully in the current directory." + "\nPlease pass it to a RootCA for issuing a Public Key Certificate.", SAWSTextOutputCallback.INFORMATION);
        } else {
            this.showMessage("There is something wrong with creating the SAWS PKC request file. ", SAWSTextOutputCallback.WARNING);
        }
    }

    /**
     * Method to generate the public key certificate request file for SAWS 
     * encryption certificate.
     * 
     * @param keystoreLocation The path to the key store.
     * @param pw The key store password.
     * @return True, if the request file has been created; False, otherwise.
     */
    private boolean createPKCReqest(String keystoreLocation, String pw) {
        boolean created = false;
        Security.addProvider(new BouncyCastleProvider());
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new BufferedInputStream(new FileInputStream(keystoreLocation)), pw.toCharArray());
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error reading keystore file when creating PKC request: " + e.getMessage());
            }
            return false;
        }
        Certificate cert = null;
        try {
            cert = ks.getCertificate("saws");
        } catch (KeyStoreException e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error reading certificate from keystore file when creating PKC request: " + e.getMessage());
            }
            return false;
        }
        PKCS10CertificationRequest request = null;
        PublicKey pk = cert.getPublicKey();
        try {
            request = new PKCS10CertificationRequest("SHA1with" + pk.getAlgorithm(), new X500Principal(((X509Certificate) cert).getSubjectDN().toString()), pk, new DERSet(), (PrivateKey) ks.getKey("saws", pw.toCharArray()));
            PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(new FileOutputStream("sawsRequest.csr")));
            pemWrt.writeObject(request);
            pemWrt.close();
            created = true;
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error creating PKC request file: " + e.getMessage());
            }
            return false;
        }
        return created;
    }

    /**
   * Method that exports the Public Key Certificate in the signing key store.
   */
    public void exportSigningPKC() {
        signingKeyfile = new File(signingKeystoreLocation);
        if (!signingKeyfile.exists()) {
            this.showMessage("The SAWS signing keystore doesn't exist. Please first use SAWS to create a signing keystore. ", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        sawsPW = getSAWSPassword3Attempts(signingKeystoreLocation, numberOfPasswordShares, "signing", false);
        if (sawsPW == null) {
            this.showMessage("The password to the signing keystore is wrong. SAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        boolean b1 = exportPKC(signingKeystoreLocation, sawsPW);
        if (b1) {
            this.showMessage("The SAWS Signing PKC file sawsSigningPKC.crt in the current directory has been exported successfully.", SAWSTextOutputCallback.INFORMATION);
        } else {
            this.showMessage("There is something wrong with exporting the SAWS Signing PKC file.", SAWSTextOutputCallback.WARNING);
        }
    }

    /**
     * Auxiliar method that exports the Public Key Certificate in the specified key store.
     * 
     * @param keystoreLocation The path to the key store.
     * @param pw The key store password.
     * @return True, if the PKC file has been exported; False, otherwise.
     */
    private boolean exportPKC(String keystoreLocation, String pw) {
        boolean created = false;
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new BufferedInputStream(new FileInputStream(keystoreLocation)), pw.toCharArray());
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error reading keystore file when exporting PKC: " + e.getMessage());
            }
            return false;
        }
        Certificate cert = null;
        try {
            cert = ks.getCertificate("saws");
        } catch (KeyStoreException e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error reading certificate from keystore file when exporting PKC: " + e.getMessage());
            }
            return false;
        }
        try {
            StringBuffer sb = new StringBuffer("-----BEGIN CERTIFICATE-----\n");
            sb.append(new String(Base64.encode(cert.getEncoded())));
            sb.append("\n-----END CERTIFICATE-----\n");
            OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream("sawsSigningPKC.crt"));
            wr.write(new String(sb));
            wr.flush();
            wr.close();
            created = true;
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error exporting PKC file: " + e.getMessage());
            }
            return false;
        }
        return created;
    }

    /**
   * Method that imports the Public Key Certificate signed by a Certificate
   * Authority to the signing key store.
   */
    public void importSigningPKC() {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader systemIn = new BufferedReader(is);
        this.cbs = new Callback[1];
        this.cbs[0] = new SAWSTextInputCallback("Please input the SAWS PKC file name:", "SAWSPKCFileName");
        try {
            this.callbackHandler.handle(this.cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
        String sIn = ((SAWSTextInputCallback) this.cbs[0]).getText();
        if (sIn == null) {
            this.showMessage("The file name is null. Please restart SAWS and type a valid file name." + "\nSAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        File sPKCFile = new File(sIn);
        if (!sPKCFile.exists()) {
            this.showMessage("This file doesn't exist. SAWS will stop. ", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        sawsPW = getSAWSPassword3Attempts(signingKeystoreLocation, numberOfPasswordShares, "signing", false);
        if (sawsPW == null) {
            this.showMessage("The password to the signing keystore is wrong. SAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        boolean b1 = importPKC(signingKeystoreLocation, sawsPW, sIn, "saws");
        if (b1) {
            this.showMessage("The SAWS PKC has been imported into the signing keystore.", SAWSTextOutputCallback.INFORMATION);
        } else {
            this.showMessage("There is something wrong when importing the SAWS PKC into the signing keystore.", SAWSTextOutputCallback.WARNING);
        }
    }

    /**
     * Method to import a certificate to a specified key store.
     * 
     * @param keystoreLocation The path to the key store file.
     * @param pw The key store password.
     * @param pkcFile The Public Key certificate to be imported.
     * @param alias The alias for the certificate in the keystore.
     * 
     * @return True, if the PKC file has been imported; False, otherwise.
     */
    private boolean importPKC(String keystoreLocation, String pw, String pkcFile, String alias) {
        boolean imported = false;
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new BufferedInputStream(new FileInputStream(keystoreLocation)), pw.toCharArray());
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error reading keystore file when exporting PKC: " + e.getMessage());
            }
            return false;
        }
        Certificate cert = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pkcFile));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            while (bis.available() > 0) {
                cert = cf.generateCertificate(bis);
            }
        } catch (Exception e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error reading certificate from file when importing PKC: " + e.getMessage());
            }
            return false;
        }
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(new File(keystoreLocation)));
        } catch (FileNotFoundException e) {
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error accessing key store file when importing certificate: " + e.getMessage());
            }
            return false;
        }
        try {
            if (alias.equals("rootca")) {
                ks.setCertificateEntry(alias, cert);
            } else {
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(pw.toCharArray()));
                ks.setKeyEntry(alias, pkEntry.getPrivateKey(), pw.toCharArray(), new Certificate[] { cert });
            }
            ks.store(bos, pw.toCharArray());
            imported = true;
        } catch (Exception e) {
            e.printStackTrace();
            if (this.debugLevel >= SAWSConstant.ErrorInfo) {
                this.sawsDebugLog.write("Error writing keystore to file when importing key store: " + e.getMessage());
            }
            return false;
        }
        return imported;
    }

    /**
   * Method that creates the SAWS's encryption key store.
   */
    public void createEncryptionKeystore() {
        InputStreamReader is = new InputStreamReader(System.in);
        encryptionKeyfile = new File(encryptionKeystoreLocation);
        if (encryptionKeyfile.exists()) {
            sawsPW = getSAWSPasswordOnce(numberOfEncPasswordShares, "encryption", false);
            boolean tempB = checkKeystorePassword(encryptionKeystoreLocation, sawsPW);
            String[] options = { "Create new encryption keystore", "Stop SAWS" };
            int selection = this.createConfirmCallback("The encryption keystore already exists, \nand the password to the encryption keystore is " + tempB + "." + "\n\nOption 1: SAWS will create a new encryption keystore and overwrite the old one. " + "\nOption 2: SAWS will stop.\n", options, SAWSChoiceCallback.WARNING, "ExistingEncKeystore");
            if (selection == 1) {
                this.showMessage("SAWS stoped.", SAWSTextOutputCallback.WARNING);
                System.exit(0);
            } else {
                boolean b1 = encryptionKeyfile.delete();
                if (b1) {
                    this.showMessage(encryptionKeystoreLocation + " has been deleted. ", SAWSTextOutputCallback.INFORMATION);
                } else {
                    this.showMessage(encryptionKeystoreLocation + " can't be deleted. ", SAWSTextOutputCallback.WARNING);
                    System.exit(-1);
                }
            }
        }
        sawsPW = null;
        this.cbs = new Callback[1];
        this.cbs[0] = new CertificateDataCallback(SAWSConstant.ENCRYPTION_PURPOSE);
        try {
            this.callbackHandler.handle(this.cbs);
        } catch (Exception e) {
            sawsDebugLog.write(e);
        }
        CertificateData cd = ((CertificateDataCallback) this.cbs[0]).getCertData();
        if (cd == null) {
            this.showMessage("The process of creating the encryption keystore has been canceled. Keystore was not created.", SAWSTextOutputCallback.WARNING);
        } else {
            sawsPW = getSAWSPasswordOnce(numberOfEncPasswordShares, "encryption", true);
            boolean b1 = createKeystore(encryptionKeystoreLocation, sawsPW, cd);
            if (b1) {
                this.showMessage(encryptionKeystoreLocation + " has been created successfully.", SAWSTextOutputCallback.INFORMATION);
            } else {
                this.showMessage("There is something wrong with creating " + encryptionKeystoreLocation, SAWSTextOutputCallback.WARNING);
            }
        }
    }

    /**
   * Method that creates the SAWS's signing key store.
   */
    public void createSigningKeystore() {
        InputStreamReader is = new InputStreamReader(System.in);
        signingKeyfile = new File(signingKeystoreLocation);
        if (signingKeyfile.exists()) {
            sawsPW = getSAWSPasswordOnce(numberOfPasswordShares, "signing", false);
            boolean tempB = checkKeystorePassword(signingKeystoreLocation, sawsPW);
            String[] options = { "Create new signing keystore", "Stop SAWS" };
            int selection = this.createConfirmCallback("The signing keystore already exists, \nand the password to the signing keystore is " + tempB + "." + "\n\nOption 1: SAWS will create a new signing keystore and overwrite the old one. " + "\nOption 2: SAWS will stop.\n", options, SAWSChoiceCallback.WARNING, "ExistingSigKeystore");
            if (selection == 1) {
                this.showMessage("SAWS stoped.", SAWSTextOutputCallback.WARNING);
                System.exit(0);
            } else {
                boolean b1 = signingKeyfile.delete();
                if (b1) {
                    this.showMessage(signingKeystoreLocation + " has been deleted. ", SAWSTextOutputCallback.INFORMATION);
                } else {
                    this.showMessage(signingKeystoreLocation + " can't be deleted. ", SAWSTextOutputCallback.WARNING);
                    System.exit(-1);
                }
            }
        }
        sawsPW = null;
        this.cbs = new Callback[1];
        this.cbs[0] = new CertificateDataCallback(SAWSConstant.SIGNING_PURPOSE);
        try {
            this.callbackHandler.handle(this.cbs);
        } catch (Exception e) {
            sawsDebugLog.write(e);
        }
        CertificateData cd = ((CertificateDataCallback) this.cbs[0]).getCertData();
        if (cd == null) {
            this.showMessage("The process of creating the signing keystore has been canceled. Keystore was not created.", SAWSTextOutputCallback.WARNING);
        } else {
            sawsPW = getSAWSPasswordOnce(numberOfPasswordShares, "signing", true);
            boolean b1 = createKeystore(signingKeystoreLocation, sawsPW, cd);
            if (b1) {
                this.showMessage(signingKeystoreLocation + " has been created successfully.", SAWSTextOutputCallback.INFORMATION);
            } else {
                this.showMessage("There is something wrong with creating " + signingKeystoreLocation, SAWSTextOutputCallback.WARNING);
            }
        }
    }

    /**
   * Method that imports the root CA certificate specified in SAWS's 
   * configuration file (saws.xml).
   */
    public void importRootCA() {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader systemIn = new BufferedReader(is);
        sawsPW = getSAWSPassword3Attempts(signingKeystoreLocation, numberOfPasswordShares, "signing", false);
        if (sawsPW == null) {
            this.showMessage("The password to the signing keystore is wrong. SAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        boolean imported = this.importPKC(signingKeystoreLocation, sawsPW, rootCA, "rootca");
        if (imported) {
            this.showMessage("The root certificate has been imported successfully.", SAWSTextOutputCallback.INFORMATION);
        } else {
            this.showMessage("There is something wrong when importing the SAWS root CA certificate into the signing keystore." + "\nPlease check the path for the root certificate in the configuration file (saws.xml).", SAWSTextOutputCallback.WARNING);
        }
    }

    /**
   * Method that lists all the certificates in the signing key store.
   * The list of certificates will be displayed according to the 
   * Callback handler specified in the configuration file (saws.xml).
   */
    public void listSigningKeystore() {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader systemIn = new BufferedReader(is);
        sawsPW = getSAWSPassword3Attempts(signingKeystoreLocation, numberOfPasswordShares, "signing", false);
        if (sawsPW == null) {
            this.showMessage("The password to the signing keystore is wrong. SAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        try {
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance("JKS");
            } catch (KeyStoreException kse) {
                this.showMessage("Fail when creating a keystore instance.", SAWSTextOutputCallback.ERROR);
            }
            try {
                ks.load(new FileInputStream(signingKeystoreLocation), sawsPW.toCharArray());
            } catch (IOException ioe) {
                this.showMessage("Signing keystore could not be found.", SAWSTextOutputCallback.ERROR);
            }
            StringBuffer certificates = new StringBuffer();
            for (Enumeration e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = (String) e.nextElement();
                Certificate c = ks.getCertificate(alias);
                certificates.append("------ BEGINS Certificate: " + alias + " ------\n" + c.toString() + "\n------- ENDS Certificate: " + alias + " -------\n\n");
            }
            this.showMessage(new String(certificates), SAWSTextOutputCallback.LONG_MESSAGE);
        } catch (Exception err) {
            if (debugLevel >= SAWSConstant.ErrorInfo) {
                sawsDebugLog.write(err);
            }
        }
    }

    /**
     * This method makes 3 (three) attempts of getting the SAWS password, i. e.,
     * the user has 3 (three) chances for typing the password correctly.
     * 
     * @param keystoreLocation Path to the key store file.
     * @param numberOfPasswordShares Number of password shares for the keystore.
     * @param prompt The message to be displayed to the user.
     * @param newPassword Indicates if a new password has to be created or not.
     * 
     * @return The password
     */
    private String getSAWSPassword3Attempts(String keystoreLocation, int numberOfPasswordShares, String prompt, boolean newPassword) {
        String pass = null;
        for (int j = 0; j < 3; ++j) {
            pass = getSAWSPasswordOnce(numberOfPasswordShares, prompt, newPassword);
            boolean tempB = checkKeystorePassword(keystoreLocation, pass);
            if (tempB == true) {
                break;
            } else if (j < 2) {
                this.showMessage("Password to the " + prompt + " keystore is wrong. \nPlease try your password again.", SAWSTextOutputCallback.ERROR);
                continue;
            } else {
                this.showMessage("Password to the " + prompt + " keystore is wrong. \nSAWS will stop.", SAWSTextOutputCallback.ERROR);
                return null;
            }
        }
        return pass;
    }

    /**
     * This method asks the user to input the password for SAWS.
     *
     * @param numberOfPasswordShares Number of password shares for the keystore.
     * @param prompt The message to be displayed to the user.
     * @param newPassword Indicates if a new password has to be created or not.
     * 
     * @return The password
     */
    private String getSAWSPasswordOnce(int numberOfPasswordShares, String prompt, boolean newPassword) {
        String pass = null;
        StringBuffer passBuffer = new StringBuffer("");
        for (int i = 1; i <= numberOfPasswordShares; ++i) {
            SAWSPasswordCallback pc = new SAWSPasswordCallback("Please input the password for the " + prompt + " keystore \nfrom SAWS administrator No. " + i + " out of " + numberOfPasswordShares + ":", numberOfPasswordShares, i, false, prompt);
            if (!newPassword) {
                this.cbs = new Callback[1];
                this.cbs[0] = pc;
            } else {
                this.cbs = new Callback[2];
                this.cbs[0] = pc;
                this.cbs[1] = new SAWSPasswordCallback("Please repeat the password for the " + prompt + " keystore \nfrom SAWS administrator No. " + i + " out of " + numberOfPasswordShares + ":", numberOfPasswordShares, i, false, prompt);
            }
            try {
                this.callbackHandler.handle(cbs);
            } catch (Exception e) {
                sawsDebugLog.write(e);
            }
            char[] pass1 = ((SAWSPasswordCallback) this.cbs[0]).getPassword();
            ((SAWSPasswordCallback) cbs[0]).clearPassword();
            if (newPassword) {
                char[] pass2 = ((SAWSPasswordCallback) this.cbs[1]).getPassword();
                ((SAWSPasswordCallback) cbs[1]).clearPassword();
                if (!Arrays.equals(pass1, pass2)) {
                    this.showMessage("The password and the confirmation are not equal." + "\nSAWS will stop.", SAWSTextOutputCallback.ERROR);
                    System.exit(-1);
                }
            }
            passBuffer.append(pass1);
        }
        String s2 = passBuffer.toString();
        pass = generateRandomPW(s2);
        return pass;
    }

    /**
     * Method that checks if the password for the keystore is valid.
     * 
     * @param keystoreLocation The location of the keystore.
     * @param pw The Password
     * @return True if the password is valid, False, otherwise.
     */
    private boolean checkKeystorePassword(String keystoreLocation, String pw) {
        KeyStore keystore = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(keystoreLocation));
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(new BufferedInputStream(fis), pw.toCharArray());
            fis.close();
        } catch (Exception e) {
            try {
                fis.close();
            } catch (Exception e2) {
            }
            if (debugLevel >= SAWSConstant.ErrorInfo) {
                sawsDebugLog.write(e.toString());
            }
            return false;
        }
        return true;
    }

    /**
   * Method to create the callback (SAWSTextOutputCallback) with the message to be 
   * presented to the user and send it to the callback handler.
   * 
   * @param message The message to be presented.
   * @param type The type of the message (SAWSTextOutputCallback.WARNING, 
   * SAWSTextOutputCallback.ERROR, SAWSTextOutputCallback.INFORMATION, 
   * SAWSTextOutputCallback.LONG_MESSAGE)
   */
    private void showMessage(String message, int type) {
        this.cbs = new Callback[1];
        this.cbs[0] = new SAWSTextOutputCallback(type, message);
        try {
            this.callbackHandler.handle(this.cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
    }

    /**
   * Method that creates a callback (SAWSChoiceCallback) that asks the user
   * to choose one option, and sends it to the callback handler.
   * 
   * @param prompt The message to be presented to the user.
   * @param options The available options for the user.
   * @param type The type of the callback (Warning, Information, etc.)
   * 
   * @return the selected option.
   */
    private int createConfirmCallback(String prompt, String[] options, int type, String key) {
        this.cbs = new Callback[1];
        this.cbs[0] = new SAWSChoiceCallback(prompt, options, key, 0, type);
        try {
            this.callbackHandler.handle(cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
        return ((SAWSChoiceCallback) this.cbs[0]).getSelectedIndex();
    }
}
