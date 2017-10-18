package net.sourceforge.keytool.plugin.certificate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import net.sourceforge.keytool.plugin.KeystoreFile;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * Tools to handle common certificate operations.
 * 
 */
public final class CertTools {

    private static final int KEY_SIZE = 1024;

    private static final int RADIX = 16;

    private static final int NO_OF_ROLLS = 4;

    private static final int SECOND_BYTE_ADDER = 0x0F;

    private static final int FIRST_BYTE_ADDER = 0xF0;

    private static X509V1CertificateGenerator v1CertGen = new X509V1CertificateGenerator();

    private static X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static final long SIX_YEARS = (1000L * 60 * 60 * 24 * 365 * 6);

    static {
        Security.addProvider(getBouncyCastle());
    }

    private static Provider getBouncyCastle() {
        return new BouncyCastleProvider();
    }

    /**
	 * inhibits creation of new CertTools
	 */
    private CertTools() {
    }

    public static byte[] generateFingerprint(byte[] ba, Algorithm algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm.getAlgorithm());
            return md.digest(ba);
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        return null;
    }

    public static String generateFingerprintInHex(byte[] ba, Algorithm algorithm) {
        return hexToString(generateFingerprint(ba, algorithm));
    }

    public static String generateFingerprintInHex(Certificate certificate, Algorithm algorithm) throws CertificateEncodingException {
        return hexToString(generateFingerprint(certificate.getEncoded(), algorithm));
    }

    public static String hexToString(byte buffer[]) {
        StringBuffer hexString = new StringBuffer(2 * buffer.length);
        for (int i = 0; i < buffer.length; i++) {
            appendHexPair(buffer[i], hexString);
        }
        return hexString.toString();
    }

    public static String generateFingerprintInHex(KeystoreFile file, String alias, Algorithm algorithm) throws KeyStoreException, CertificateEncodingException {
        Certificate certificate = getCertificate(file, alias).getCertificate();
        if (certificate != null) {
            return generateFingerprintInHex(certificate.getEncoded(), algorithm);
        }
        return null;
    }

    private static void appendHexPair(byte b, StringBuffer hexString) {
        char firstByte = HEX_CHARS[(b & FIRST_BYTE_ADDER) >> NO_OF_ROLLS];
        char secondByte = HEX_CHARS[b & SECOND_BYTE_ADDER];
        if (hexString.length() > 0) {
            hexString.append(":");
        }
        hexString.append(firstByte);
        hexString.append(secondByte);
    }

    public static void loadKeystoreFile(KeystoreFile keystoreFile) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        String keystoreFilename = keystoreFile.getKeystorefilename();
        String keystorePassword = keystoreFile.getPassword();
        keystoreFile.getKeystore().load(new FileInputStream(keystoreFilename), keystorePassword.toCharArray());
    }

    public static KeystoreFile loadKeystoreFile(String keystoreFilename, KeystoreType keystoreType, String keystorePassword) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore = KeyStore.getInstance(keystoreType.getKeystoreType());
        keystore.load(new FileInputStream(keystoreFilename), keystorePassword.toCharArray());
        KeystoreFile keystoreFile = new KeystoreFile(keystore, keystoreFilename, keystorePassword);
        return keystoreFile;
    }

    public static String getSerialNumber(Certificate certificate) throws FileNotFoundException, CertificateException, CRLException {
        if (certificate instanceof X509Certificate) {
            X509Certificate x509certificate = (X509Certificate) certificate;
            return x509certificate.getSerialNumber().toString(RADIX);
        }
        return null;
    }

    public static CompleteCertificate getCertificate(KeystoreFile file, String alias) throws KeyStoreException {
        CompleteCertificate c = new CompleteCertificate();
        c.setCertificate(file.getKeystore().getCertificate(alias));
        c.setKeyEntry(file.getKeystore().isKeyEntry(alias));
        c.setAlias(alias);
        c.setKeystoreFile(file);
        return c;
    }

    public static Certificate loadCertificate(String filename) throws CertificateException, IOException {
        FileInputStream fis = new FileInputStream(filename);
        BufferedInputStream bis = new BufferedInputStream(fis);
        return loadCertificate(bis);
    }

    public static Certificate loadCertificate(InputStream inputStream) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(inputStream);
        return cert;
    }

    public static CompleteCertificate createCertificate(CertificatePerson person, Date notBefore, Date notAfter) {
        int serialNumber = makeSerialNumber();
        return createCertificate(person, serialNumber, notBefore, notAfter);
    }

    private static int makeSerialNumber() {
        int serialNumber = (int) System.currentTimeMillis();
        if (serialNumber < 0) {
            serialNumber *= -1;
        }
        return serialNumber;
    }

    public static CompleteCertificate createCertificate(CertificatePerson person, int serialNumber, Date notBefore, Date notAfter) {
        try {
            KeyPair keypair = getKeypair();
            PublicKey publicKey = keypair.getPublic();
            PrivateKey privateKey = keypair.getPrivate();
            X509Certificate certificate = createSelfIssuerCertificate(person, publicKey, privateKey, serialNumber, notBefore, notAfter);
            return new CompleteCertificate(person, publicKey, privateKey, serialNumber, certificate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static KeyPair getKeypair() throws NoSuchAlgorithmException {
        java.security.SecureRandom sr;
        sr = new java.security.SecureRandom();
        KeyPairGenerator keyGen;
        keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(KEY_SIZE, sr);
        KeyPair keypair = keyGen.generateKeyPair();
        return keypair;
    }

    public static void exportCertificate(Certificate certificate, String filename) throws CertificateEncodingException, IOException {
        FileOutputStream fos = new FileOutputStream(new File(filename));
        fos.write(certificate.getEncoded());
        fos.flush();
        fos.close();
    }

    /**
     * we generate a certificate signed by our CA's intermediate certficate
     * @param notBefore 
     * @param notAfter 
     */
    public static CompleteCertificate createClientCertificate(CertificatePerson person, CompleteCertificate issuer, Date notBefore, Date notAfter) throws Exception {
        CompleteCertificate personCertificate = createCertificate(person, notBefore, notAfter);
        Hashtable<DERObjectIdentifier, String> attrs = new Hashtable<DERObjectIdentifier, String>();
        Vector<DERObjectIdentifier> order = new Vector<DERObjectIdentifier>();
        populate(person, attrs, order);
        Hashtable<DERObjectIdentifier, String> attrsIssuer = new Hashtable<DERObjectIdentifier, String>();
        Vector<DERObjectIdentifier> orderIssuer = new Vector<DERObjectIdentifier>();
        populate(issuer.getCertificatePerson(), attrsIssuer, orderIssuer);
        v3CertGen.setSerialNumber(BigInteger.valueOf(personCertificate.getSerialNumber()));
        v3CertGen.setIssuerDN(new X509Principal(orderIssuer, attrsIssuer));
        v3CertGen.setNotBefore(notBefore);
        v3CertGen.setNotAfter(notAfter);
        v3CertGen.setSubjectDN(new X509Principal(order, attrs));
        v3CertGen.setPublicKey(personCertificate.getPublicKey());
        v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");
        X509Certificate cert = v3CertGen.generateX509Certificate(issuer.getPrivateKey(), "BC");
        CompleteCertificate clientCertificate = new CompleteCertificate();
        clientCertificate.setCertificate(cert);
        clientCertificate.setPrivateKey(personCertificate.getPrivateKey());
        clientCertificate.setIssuer(issuer);
        return clientCertificate;
    }

    private static void populate(CertificatePerson person, Hashtable<DERObjectIdentifier, String> attrs, Vector<DERObjectIdentifier> order) {
        if (person.getCountryCode() != null) {
            order.addElement(X509Principal.C);
            attrs.put(X509Principal.C, person.getCountryCode());
        }
        if (person.getOrganization() != null) {
            order.addElement(X509Principal.O);
            attrs.put(X509Principal.O, person.getOrganization());
        }
        if (person.getLocation() != null) {
            order.addElement(X509Principal.L);
            attrs.put(X509Principal.L, person.getLocation());
        }
        if (person.getName() != null) {
            order.addElement(X509Principal.CN);
            attrs.put(X509Principal.CN, person.getName());
        }
        if (person.getEmail() != null) {
            order.addElement(X509Principal.EmailAddress);
            attrs.put(X509Principal.EmailAddress, person.getEmail());
        }
    }

    public static X509Certificate createSelfIssuerCertificate(CertificatePerson person, PublicKey pubKey, PrivateKey privKey, int serialNumber) throws Exception {
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + SIX_YEARS);
        return createSelfIssuerCertificate(person, pubKey, privKey, serialNumber, notBefore, notAfter);
    }

    public static X509Certificate createSelfIssuerCertificate(CertificatePerson person, PublicKey pubKey, PrivateKey privKey, int serialNumber, Date notBefore, Date notAfter) throws Exception {
        String issuer = person.toString();
        String subject = issuer;
        v1CertGen.setSerialNumber(BigInteger.valueOf(serialNumber));
        v1CertGen.setIssuerDN(new X509Principal(issuer));
        v1CertGen.setNotBefore(notBefore);
        v1CertGen.setNotAfter(notAfter);
        v1CertGen.setSubjectDN(new X509Principal(subject));
        v1CertGen.setPublicKey(pubKey);
        v1CertGen.setSignatureAlgorithm("SHA1WITHRSA");
        X509Certificate cert = v1CertGen.generateX509Certificate(privKey);
        cert.verify(pubKey);
        return cert;
    }

    public static void exportPersonalCertToPFX(CompleteCertificate cc, String filename) {
        exportPersonalCertToPFX((X509Certificate) cc.getCertificate(), cc.getPrivateKey(), filename, cc.getPassword());
    }

    public static void exportPersonalCertToPFX(X509Certificate cert, PrivateKey privateKey, String filename, String password) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            try {
                String alias = Long.toHexString(SecureRandom.getInstance("SHA1PRNG").nextLong());
                KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
                char[] pwdArray = password.toCharArray();
                ks.load(null, pwdArray);
                X509Certificate[] certs = new X509Certificate[1];
                certs[0] = cert;
                Key key = privateKey;
                if (key != null) {
                    ks.setKeyEntry(alias, key, pwdArray, certs);
                }
                ks.store(fos, pwdArray);
            } finally {
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CompleteCertificate loadPFX(String filename, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        FileInputStream fis = new FileInputStream(filename);
        ks.load(fis, password.toCharArray());
        Enumeration<String> aliases = ks.aliases();
        if (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = ks.getCertificate(alias);
            Key key = ks.getKey(alias, password.toCharArray());
            CompleteCertificate complete = new CompleteCertificate();
            complete.setCertificate(certificate);
            complete.setPrivateKey(makePrivateKey(key.getEncoded()));
            return complete;
        }
        return null;
    }

    public static void addCertificateAndSaveKeystore(CompleteCertificate completeCertificate, KeystoreFile keystoreFile) throws Exception {
        KeyStore keystore = keystoreFile.getKeystore();
        keystore.setCertificateEntry(completeCertificate.getAlias(), completeCertificate.getCertificate());
        Certificate[] chain = makeCertificateChain(completeCertificate);
        if (completeCertificate.isKeyEntry()) {
            keystore.setKeyEntry(completeCertificate.getAlias(), completeCertificate.getPrivateKey(), keystoreFile.getPassword().toCharArray(), chain);
        }
        keystore.store(new FileOutputStream(keystoreFile.getKeystorefilename()), keystoreFile.getPassword().toCharArray());
        loadKeystoreFile(keystoreFile);
    }

    private static Certificate[] makeCertificateChain(CompleteCertificate completeCertificate) {
        Certificate[] chain;
        if (completeCertificate.getIssuer() == null) {
            chain = new Certificate[] { completeCertificate.getCertificate() };
        } else {
            ArrayList<Certificate> chains = new ArrayList<Certificate>();
            chains.add(completeCertificate.getCertificate());
            CompleteCertificate issuer = completeCertificate.getIssuer();
            while (issuer != null) {
                chains.add(issuer.getCertificate());
                issuer = issuer.getIssuer();
            }
            chain = chains.toArray(new Certificate[chains.size()]);
        }
        return chain;
    }

    public static void addCertificateToNewKeystore(CompleteCertificate completeCertificate, final String alias, String password, String keystorePassword, String keystoreFilename, KeystoreType keystoreType) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, NoSuchProviderException {
        KeyStore newKeystore = KeyStore.getInstance(keystoreType.getKeystoreType());
        newKeystore.load(null, keystorePassword.toCharArray());
        Certificate[] chain = makeCertificateChain(completeCertificate);
        newKeystore.setKeyEntry(alias, completeCertificate.getPrivateKey(), password.toCharArray(), chain);
        FileOutputStream fileOutputStream = new FileOutputStream(keystoreFilename);
        newKeystore.store(fileOutputStream, keystorePassword.toCharArray());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private static PrivateKey makePrivateKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
        PrivateKey pKey = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
        return pKey;
    }

    public static CompleteCertificate getCompleteCertificate(KeystoreFile file, String alias, String certificatePassword) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyStore keystore = file.getKeystore();
        Certificate certificate = keystore.getCertificate(alias);
        CompleteCertificate complete = new CompleteCertificate();
        complete.setCertificate(certificate);
        complete.setKeyEntry(keystore.isKeyEntry(alias));
        if (complete.isKeyEntry()) {
            Key key = keystore.getKey(alias, certificatePassword.toCharArray());
            PrivateKey pKey = makePrivateKey(key.getEncoded());
            complete.setPrivateKey(pKey);
        }
        complete.setPassword(certificatePassword);
        complete.setKeystoreFile(file);
        complete.setAlias(alias);
        complete.setPublicKey(certificate.getPublicKey());
        return complete;
    }

    public static Certificate[] getCertificateChain(CompleteCertificate completeCertificate) throws KeyStoreException {
        return completeCertificate.getKeystoreFile().getKeystore().getCertificateChain(completeCertificate.getAlias());
    }

    public static String generateCSR(CompleteCertificate complete) throws CertificateEncodingException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
        return certificatonRequestAsCSR(generateCertificateRequest(complete));
    }

    public static PKCS10CertificationRequest generateCertificateRequest(CompleteCertificate complete) throws CertificateEncodingException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
        PKCS10CertificationRequest request = null;
        X509Principal _x509Principal = new X509Principal(complete.getX509Certificate().getSubjectDN().getName());
        if (complete.getPublicKey() != null || complete.getPrivateKey() != null) {
            request = new PKCS10CertificationRequest("SHA1WITHRSA", _x509Principal, complete.getPublicKey(), null, complete.getPrivateKey());
            if (!request.verify()) {
                System.out.println("The certificate request verification failed");
            }
        } else {
            throw new RuntimeException("Could not generate CSR missing public/private key");
        }
        return request;
    }

    public static String certificatonRequestAsCSR(PKCS10CertificationRequest request) {
        byte buf[] = request.getEncoded();
        StringBuffer buff = new StringBuffer();
        buff.append("-----BEGIN NEW CERTIFICATE REQUEST-----\n");
        buff.append(new sun.misc.BASE64Encoder().encode(buf));
        buff.append("\n-----END NEW CERTIFICATE REQUEST-----\n");
        return buff.toString();
    }
}
