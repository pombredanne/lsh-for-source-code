package it.trento.comune.j4sign.examples;

import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import it.trento.comune.j4sign.cms.ExternalSignatureCMSSignedDataGenerator;
import it.trento.comune.j4sign.cms.ExternalSignatureSignerInfoGenerator;
import it.trento.comune.j4sign.pcsc.CardInfo;
import it.trento.comune.j4sign.pcsc.PCSCHelper;
import it.trento.comune.j4sign.pkcs11.PKCS11Signer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * A command line interface program for testing the generation of a CMS signed
 * message, using a pkcs11 token (usually a SmartCard). This is a complete
 * example covering all aspects of digital signature, from token management to
 * CMS message generation and verification. <br>
 * This is a command line program because the need to concentrate on algorithmic
 * issues, whithout the burden of GUI stuff.
 * <p>
 * Multiple signatures are permitted, each with different token types; the
 * generated CMS message keeps signers informations at the same level (similar
 * to a paper document with multiple signatures). I call this arrangement
 * "combined signatures", in contrast with "nested signatures" (like a signed
 * paper document put in a signed envelope). A hierarchical type of multiple
 * signatures, ("CounterSignature", OID: 1.2.840.113549.1.9.6) is also
 * contemplated by CMS standard in RFC 3852, but it is not currently supported.
 * <p>
 * The example adopts a special class for securing token PIN management
 * (Unfortunately the java language does not provide any standard method to hide
 * character input). This could cause some problem on IDEs, where the default
 * input - output streams are not the standard console, then password hiding can
 * be disabled. <br>
 * <p>
 * <b>N.B. note that in this example signature verification only ensures signed
 * data integrity; a complete verification to ensure non-repudiation requires
 * checking the full certification path including the CA root certificate, and
 * CRL verification on the CA side. <br>
 * (Good stuff for a next release ...) </b>
 * 
 * @author Roberto Resoli
 */
public class CLITest {

    private String cryptokiLib = null;

    private boolean forcingCryptoki = false;

    String signDN;

    KeyPair signKP;

    X509Certificate signCert;

    String origDN;

    KeyPair origKP;

    X509Certificate origCert;

    String reciDN;

    KeyPair reciKP;

    X509Certificate reciCert;

    KeyPair dsaSignKP;

    X509Certificate dsaSignCert;

    KeyPair dsaOrigKP;

    X509Certificate dsaOrigCert;

    byte[] msgBytes = { 'C', 'I', 'A', 'O' };

    private static int WRAP_AFTER = 16;

    private boolean makeDigestOnToken = false;

    private String digestAlg = CMSSignedDataGenerator.DIGEST_SHA256;

    private String encAlg = CMSSignedDataGenerator.ENCRYPTION_RSA;

    private static String PROPERTIES_FILE = "clitest.properties";

    public CLITest() {
        super();
        loadProperties();
    }

    private void loadProperties() {
        Properties props = new Properties();
        String propertiesFile = PROPERTIES_FILE;
        System.out.println("Trying to load properties from: '" + propertiesFile + "'");
        try {
            InputStream in = getClass().getResourceAsStream("/" + propertiesFile);
            if (in != null) {
                props.load(in);
                in.close();
            } else System.out.println("'" + propertiesFile + "' not found!");
        } catch (IOException e) {
            System.out.println(e);
        }
        if (props.size() > 0) {
            Iterator i = props.entrySet().iterator();
            System.out.println("loaded properties:");
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                System.out.println((me.getKey().toString() + ": " + me.getValue()));
            }
            if (props.getProperty("digest.algorithm") != null) this.digestAlg = props.getProperty("digest.algorithm");
            if (props.getProperty("digest.ontoken") != null) this.makeDigestOnToken = Boolean.valueOf(props.getProperty("digest.ontoken")).booleanValue();
            if (props.getProperty("encryption.algorithm") != null) this.encAlg = props.getProperty("encryption.algorithm");
        }
    }

    /**
	 * @param cryptokiLib
	 */
    public CLITest(String cryptokiLib) {
        this();
        this.cryptokiLib = cryptokiLib;
        this.forcingCryptoki = true;
    }

    /**
	 * the main method Adds BouncyCastle cryptographic provider, instantiates
	 * the CLITest class, and launches the signature process. The class require
	 * no arguments; the message to sign is the fixed word "CIAO".
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        Security.insertProviderAt(new BouncyCastleProvider(), 3);
        CLITest rt = null;
        if (args.length == 1) rt = new CLITest(args[0]); else rt = new CLITest();
        rt.testExternalSignature();
    }

    /**
	 * The cryptoki library currently used, as set in
	 * {@link #detectCardAndCriptoki()}method.
	 * 
	 * @return the cryptoki native library to use to access the current PKCS#11
	 *         token.
	 */
    public String getCryptokiLib() {
        return cryptokiLib;
    }

    /**
	 * Sets th cryptoki library to use to access the current PKCS#11 token; This
	 * method is used internally in {@link #detectCardAndCriptoki()}method.
	 * 
	 * @param lib
	 */
    private void setCryptokiLib(String lib) {
        this.cryptokiLib = lib;
    }

    /**
	 * This triggers the PCSC wrapper stuff; a {@link PCSCHelper}class is used
	 * to detect reader and token presence, trying also to provide a candidate
	 * PKCS#11 cryptoki for it.
	 * 
	 * @return true if a token with corresponding candidate cryptoki was
	 *         detected.
	 * @throws IOException
	 */
    private boolean detectCardAndCriptoki() throws IOException {
        CardInfo ci = null;
        boolean cardPresent = false;
        System.out.println("\n\n========= DETECTING CARD ===========");
        PCSCHelper pcsc = new PCSCHelper(true);
        List cards = pcsc.findCards();
        cardPresent = !cards.isEmpty();
        if (!isForcingCryptoki()) {
            if (cardPresent) {
                ci = (CardInfo) cards.get(0);
                System.out.println("\n\nFor signing we will use card: '" + ci.getProperty("description") + "' with criptoki '" + ci.getProperty("lib") + "'");
                System.out.println(ci.getProperty("lib"));
                setCryptokiLib(ci.getProperty("lib"));
            } else System.out.println("Sorry, no card detected!");
        } else System.out.println("\n\nFor signing we are forcing use of cryptoki: '" + getCryptokiLib() + "'");
        System.out.println("=================================");
        return (getCryptokiLib() != null);
    }

    /**
	 * Test (possibly multiple) digital signatures using PKCS#11 tokens. After
	 * correct verification of all signatures, the CMS signed message is saved
	 * on the filesystem under the users's home directory.
	 * 
	 */
    public void testExternalSignature() {
        try {
            System.out.println("==================================================");
            System.out.println("========= CMS (PKCS7) Signed message test ========\n\n");
            System.out.print("The test message to sign is:\t");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(this.msgBytes);
            System.out.println(baos.toString());
            System.out.print("As exadecimal string:\t\t");
            System.out.println(formatAsString(this.msgBytes, " ", WRAP_AFTER));
            System.out.println();
            CMSProcessable msg = new CMSProcessableByteArray(this.msgBytes);
            ExternalSignatureCMSSignedDataGenerator gen = new ExternalSignatureCMSSignedDataGenerator();
            ArrayList certList = new ArrayList();
            ExternalSignatureSignerInfoGenerator sig = null;
            String answer = "STARTVALUE";
            String prompt = "Do you want to sign this message?\n" + baos.toString() + "\nType Y or N:";
            int i = 0;
            BufferedReader input_;
            PrintWriter output_;
            {
                try {
                    output_ = new PrintWriter(System.out, true);
                    input_ = new BufferedReader(new InputStreamReader(System.in));
                } catch (Throwable thr) {
                    thr.printStackTrace();
                    output_ = new PrintWriter(System.out, true);
                    input_ = new BufferedReader(new InputStreamReader(System.in));
                }
            }
            while (!answer.equals("N")) {
                while (!answer.equals("N") && !answer.equals("Y")) {
                    output_.print(prompt);
                    output_.flush();
                    answer = input_.readLine().toUpperCase();
                    output_.flush();
                }
                if (answer.equals("Y")) {
                    System.out.println("========================");
                    System.out.println("ADDING SIGNATURE " + i);
                    if (detectCardAndCriptoki()) {
                        System.out.println("Starting signing process.");
                        sig = getSignerInfoGenerator(msg, this.digestAlg, this.encAlg, this.makeDigestOnToken, certList);
                        if (sig != null) gen.addSignerInf(sig);
                    }
                    prompt = "Add another signature?\n" + "\nType Y or N:";
                    answer = "STARTVALUE";
                }
                i++;
            }
            if (certList.size() != 0) {
                CertStore store = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
                System.out.println("Adding certificates ... ");
                gen.addCertificatesAndCRLs(store);
                System.out.println("Generating CMSSignedData ");
                CMSSignedData s = gen.generate(msg, true);
                System.out.println("\nStarting CMSSignedData verification ... ");
                CertStore certs = s.getCertificatesAndCRLs("Collection", "BC");
                SignerInformationStore signers = s.getSignerInfos();
                Collection c = signers.getSigners();
                System.out.println(c.size() + " signers found.");
                Iterator it = c.iterator();
                i = 0;
                while (it.hasNext()) {
                    SignerInformation signer = (SignerInformation) it.next();
                    Collection certCollection = certs.getCertificates(signer.getSID());
                    if (certCollection.size() == 1) {
                        X509Certificate cert = (X509Certificate) certCollection.toArray()[0];
                        System.out.println(i + ") Verifiying signature from:\n" + cert.getSubjectDN());
                        if (signer.verify(cert, "BC")) {
                            System.out.println("SIGNATURE " + i + " OK!");
                        } else System.err.println("SIGNATURE " + i + " Failure!");
                    } else System.out.println("There is not exactly one certificate for this signer!");
                    i++;
                }
                String filePath = System.getProperty("user.home") + System.getProperty("file.separator") + "ciao.txt.p7m";
                System.out.println("\nSAVING FILE TO: " + filePath);
                FileOutputStream fos = new FileOutputStream(filePath);
                fos.write(s.getEncoded());
                fos.flush();
                fos.close();
            }
        } catch (Exception ex) {
            System.err.println("EXCEPTION:\n" + ex);
        }
    }

    private long algToMechanism(boolean digestOnToken, String digestAlg, String encryptionAlg) {
        long mechanism = -1L;
        if (CMSSignedDataGenerator.ENCRYPTION_RSA.equals(encryptionAlg)) if (digestOnToken) {
            if (CMSSignedDataGenerator.DIGEST_MD5.equals(digestAlg)) mechanism = PKCS11Constants.CKM_MD5_RSA_PKCS; else if (CMSSignedDataGenerator.DIGEST_SHA1.equals(digestAlg)) mechanism = PKCS11Constants.CKM_SHA1_RSA_PKCS; else if (CMSSignedDataGenerator.DIGEST_SHA256.equals(digestAlg)) mechanism = PKCS11Constants.CKM_SHA256_RSA_PKCS;
        } else mechanism = PKCS11Constants.CKM_RSA_PKCS;
        return mechanism;
    }

    /**
	 * Implements a single signature, returning the
	 * {@link ExternalSignatureSignerInfoGenerator}that encapsulates all signer
	 * informations.
	 * 
	 * 
	 * @param msg
	 *            the content to sign
	 * @param certList
	 *            the list which the signer certificate is to be added to.
	 * @return the <code>ExternalSignatureSignerInfoGenerator</code> containing
	 *         all signer informations.
	 */
    ExternalSignatureSignerInfoGenerator getSignerInfoGenerator(CMSProcessable msg, String digestAlg, String encryptionAlg, boolean digestOnToken, ArrayList certList) {
        ExternalSignatureSignerInfoGenerator signerGenerator = new ExternalSignatureSignerInfoGenerator(digestAlg, encryptionAlg);
        byte[] certBytes = null;
        byte[] signedBytes = null;
        byte[] bytesToSign = null;
        byte[] dInfoBytes = null;
        try {
            long mechanism = -1L;
            long certHandle = -1L;
            long t = -1L;
            mechanism = algToMechanism(digestOnToken, digestAlg, encryptionAlg);
            if (mechanism != -1L) {
                PKCS11Signer signAgent = new PKCS11Signer(getCryptokiLib(), System.out);
                System.out.println("Finding a token supporting required mechanism and " + "containing a suitable " + "certificate...");
                t = signAgent.findSuitableToken(mechanism);
                if (t != -1L) {
                    signAgent.setMechanism(mechanism);
                    signAgent.setTokenHandle(t);
                    signAgent.openSession();
                    certHandle = signAgent.findCertificateWithNonRepudiationCritical();
                    if (certHandle >= 0) certBytes = signAgent.getDEREncodedCertificate(certHandle); else System.out.println("\nNo suitable  certificate on token!");
                    signAgent.closeSession();
                    System.out.println("Cert session Closed.");
                    if (certBytes != null) {
                        System.out.println("======== Certificate found =========");
                        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(certBytes);
                        java.security.cert.X509Certificate javaCert = (java.security.cert.X509Certificate) cf.generateCertificate(bais);
                        signerGenerator.setCertificate(javaCert);
                        System.out.println("Calculating bytes to sign ...");
                        bytesToSign = signerGenerator.getBytesToSign(PKCSObjectIdentifiers.data, msg, "BC");
                        byte[] rawDigest = null;
                        byte[] paddedBytes = null;
                        rawDigest = applyDigest(digestAlg, bytesToSign);
                        System.out.println("Raw digest bytes:\n" + formatAsString(rawDigest, " ", WRAP_AFTER));
                        System.out.println("Encapsulating in a DigestInfo...");
                        dInfoBytes = encapsulateInDigestInfo(digestAlg, rawDigest);
                        System.out.println("DigestInfo bytes:\n" + formatAsString(dInfoBytes, " ", WRAP_AFTER));
                        if (!digestOnToken) {
                            System.out.println("Adding Pkcs1 padding...");
                            paddedBytes = applyPkcs1Padding(128, dInfoBytes);
                            System.out.println("Padded DigestInfo bytes:\n" + formatAsString(paddedBytes, " ", WRAP_AFTER));
                        }
                        System.out.println("============ Encrypting with pkcs11 token ============");
                        signAgent.setMechanism(mechanism);
                        signAgent.setTokenHandle(t);
                        signAgent.openSession(readPassword());
                        if (certHandle == -1L) certHandle = signAgent.findCertificateWithNonRepudiationCritical();
                        long privateKeyHandle = signAgent.findSignatureKeyFromCertificateHandle(certHandle);
                        if (privateKeyHandle >= 0) {
                            if (!digestOnToken) {
                                signedBytes = signAgent.signDataSinglePart(privateKeyHandle, dInfoBytes);
                            } else signedBytes = signAgent.signDataSinglePart(privateKeyHandle, bytesToSign);
                        } else System.out.println("\nNo suitable private key and certificate on token!");
                        signAgent.closeSession();
                        System.out.println("Sign session Closed.");
                        signAgent.libFinalize();
                        System.out.println("Criptoki library finalized.");
                    }
                }
            } else System.out.println("Mechanism currently not supported");
            if ((certBytes != null) && (signedBytes != null)) {
                System.out.println("======== Encryption completed =========");
                System.out.println("\nBytes:\n" + formatAsString(bytesToSign, " ", WRAP_AFTER));
                if (dInfoBytes != null) System.out.println("DigestInfo bytes:\n" + formatAsString(dInfoBytes, " ", WRAP_AFTER));
                System.out.println("Encryption result:\n" + formatAsString(signedBytes, " ", WRAP_AFTER) + "\n");
                java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(certBytes);
                java.security.cert.X509Certificate javaCert = (java.security.cert.X509Certificate) cf.generateCertificate(bais);
                PublicKey pubKey = javaCert.getPublicKey();
                try {
                    System.out.println("Decrypting...");
                    Cipher c = Cipher.getInstance("RSA/ECB/PKCS1PADDING", "BC");
                    c.init(Cipher.DECRYPT_MODE, pubKey);
                    byte[] decBytes = c.doFinal(signedBytes);
                    System.out.println("Decrypted bytes (should match DigestInfo bytes):\n" + formatAsString(decBytes, " ", WRAP_AFTER));
                } catch (NoSuchAlgorithmException e1) {
                    e1.printStackTrace();
                } catch (NoSuchPaddingException e1) {
                    e1.printStackTrace();
                } catch (InvalidKeyException e2) {
                    e2.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                }
                if (signerGenerator.getCertificate() != null) signerGenerator.setCertificate(javaCert);
                signerGenerator.setSignedBytes(signedBytes);
                certList.add(javaCert);
            } else signerGenerator = null;
        } catch (Exception e) {
            System.out.println(e);
        } catch (Throwable e) {
            System.out.println(e);
        }
        return signerGenerator;
    }

    /**
	 * Formats a byte[] as an hexadecimal String, interleaving bytes with a
	 * separator string.
	 * 
	 * @param bytes
	 *            the byte[] to format.
	 * @param byteSeparator
	 *            the string to be used to separate bytes.
	 * 
	 * @return the formatted string.
	 */
    public String formatAsString(byte[] bytes, String byteSeparator, int wrapAfter) {
        int n, x;
        String w = new String();
        String s = new String();
        String separator = null;
        for (n = 0; n < bytes.length; n++) {
            x = (int) (0x000000FF & bytes[n]);
            w = Integer.toHexString(x).toUpperCase();
            if (w.length() == 1) w = "0" + w;
            if ((n % wrapAfter) == (wrapAfter - 1)) separator = "\n"; else separator = byteSeparator;
            s = s + w + ((n + 1 == bytes.length) ? "" : separator);
        }
        return s;
    }

    /**
	 * Uses the {@link PasswordMasker}class to securely read characters from the
	 * console; it's a pity that java language does not includes this in its
	 * standard set of features.
	 * 
	 * @return the characters read.
	 * @throws IOException
	 * @throws InterruptedException
	 */
    char[] readPassword() throws IOException, InterruptedException {
        System.out.println("Getting password ...\n");
        char[] password = null;
        System.out.println("N.B.: If you are not running the example from the console, password hiding could not work well;");
        PrintWriter output_ = null;
        BufferedReader input_ = null;
        try {
            output_ = new PrintWriter(System.out, true);
            input_ = new BufferedReader(new InputStreamReader(System.in));
        } catch (Throwable thr) {
            thr.printStackTrace();
            output_ = new PrintWriter(System.out, true);
            input_ = new BufferedReader(new InputStreamReader(System.in));
        }
        String answer = "STARTVALUE";
        while (answer != null && !answer.equals("") && !answer.equals("N") && !answer.equals("Y")) {
            output_.println("Do you want to enable password hiding? [Y]");
            output_.println("IF YOU ANSWER 'N' PASSWORD WILL BE VISIBLE WHILE YOU TYPE IT!!!!");
            output_.flush();
            answer = input_.readLine().toUpperCase();
            if (answer == null || "".equals(answer)) answer = "Y";
            output_.flush();
        }
        if (answer.equals("N")) {
            output_.print("Enter user-PIN (VISIBLE) and press [return key]: ");
            output_.flush();
            password = input_.readLine().toCharArray();
        } else {
            password = PasswordMasker.readConsoleSecure("Enter user-PIN (hidden) and press [return key]: ");
        }
        return password;
    }

    private byte[] applyDigest(String digestAlg, byte[] bytes) throws NoSuchAlgorithmException {
        System.out.println("Applying digest algorithm...");
        MessageDigest md = MessageDigest.getInstance(digestAlg);
        md.update(bytes);
        return md.digest();
    }

    private byte[] encapsulateInDigestInfo(String digestAlg, byte[] digestBytes) throws IOException {
        byte[] bcDigestInfoBytes = null;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        DERObjectIdentifier digestObjId = new DERObjectIdentifier(digestAlg);
        AlgorithmIdentifier algId = new AlgorithmIdentifier(digestObjId, null);
        DigestInfo dInfo = new DigestInfo(algId, digestBytes);
        dOut.writeObject(dInfo);
        return bOut.toByteArray();
    }

    private byte[] applyPkcs1Padding(int resultLength, byte[] srcBytes) {
        int paddingLength = resultLength - srcBytes.length;
        byte[] dstBytes = new byte[resultLength];
        dstBytes[0] = 0x00;
        dstBytes[1] = 0x01;
        for (int i = 2; i < (paddingLength - 1); i++) {
            dstBytes[i] = (byte) 0xFF;
        }
        dstBytes[paddingLength - 1] = 0x00;
        for (int i = 0; i < srcBytes.length; i++) {
            dstBytes[paddingLength + i] = srcBytes[i];
        }
        return dstBytes;
    }

    public boolean isForcingCryptoki() {
        return forcingCryptoki;
    }
}
