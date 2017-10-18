package es.caib.signatura.provider.impl.bccryptoapi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import javax.security.auth.login.LoginException;
import javax.swing.JOptionPane;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedDataStreamGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import es.caib.signatura.api.Signature;
import es.caib.signatura.api.SignatureCertNotFoundException;
import es.caib.signatura.api.SignatureException;
import es.caib.signatura.api.SignaturePrivKeyException;
import es.caib.signatura.api.SignatureTimestampException;
import es.caib.signatura.impl.CMSSignatureRawv2;
import es.caib.signatura.impl.CMSSignaturev2;
import es.caib.signatura.impl.MIMEInputStream;
import es.caib.signatura.impl.SigDebug;
import es.caib.signatura.impl.SignaturaProperties;
import es.caib.signatura.impl.SignerProviderInterface;
import es.caib.signatura.provider.impl.common.PDFSigner;
import es.caib.signatura.provider.impl.common.ParsedCertificateImpl;
import es.caib.signatura.provider.impl.common.TimeStampManager;

/**
 * Implementation of the interface <code>Signer</code> using the Bouncy Castle crypto API as provider.
 * This implementation is created to be able to sign with certificates contained into keystores of type JKS.
 * By default, it is searched a keystore file at the path <user.home>/.keystore. To change the path of the
 * keystore or its password, system properties caib-crypto-keystore and caib-crypto-keystore-password
 * are used respectively.
 * 
 * @author 3digits
 *
 */
public class BcCryptoApiSigner implements SignerProviderInterface {

    private String keyStorePath;

    private char[] keyStorePassword;

    private KeyStore keyStore;

    /**
	 * Gets the keystore. System properties caib-crypto-keystore and caib-crypto-keystore-password
	 * are used to get the path of the keystore and its password respectively. If the system property
	 * caib-crypto-keystore is not initialized then the keystore is searched at the path: <user.home>/.keystore
	 */
    private KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (keyStorePassword == null) {
            try {
                return getKeyStore(null);
            } catch (IOException e) {
                if (e.getCause() instanceof LoginException) {
                    return askForPin();
                } else throw e;
            } catch (KeyStoreException e) {
                if (e.getCause() instanceof LoginException) {
                    return askForPin();
                } else {
                    throw e;
                }
            }
        } else {
            try {
                return getKeyStore(keyStorePassword);
            } catch (KeyStoreException e) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                return getKeyStore();
            }
        }
    }

    /**
	 * Asks for the keystore password and returns it.
	 */
    private KeyStore askForPin() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        char newPin[] = PinDialog.getPIN();
        try {
            KeyStore ks = getKeyStore(newPin);
            keyStorePassword = newPin;
            return ks;
        } catch (IOException e2) {
            e2.printStackTrace();
            if (e2.getCause() instanceof LoginException) {
                JOptionPane.showMessageDialog(null, "The keystore password is not correct.", "Keystore", JOptionPane.WARNING_MESSAGE);
            }
            throw e2;
        }
    }

    /**
	 * Obtiene el keystore usando la contraseña pasada por parámetro.
	 */
    private KeyStore getKeyStore(char pinToUse[]) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance("JKS");
        try {
            FileInputStream f = new FileInputStream(keyStorePath);
            ks.load(f, pinToUse);
        } catch (ProviderException e) {
            e.printStackTrace();
            throw new KeyStoreException("Unable to get KeyStore", e);
        }
        return ks;
    }

    /**
	 * Constructor.
	 */
    public BcCryptoApiSigner() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        keyStorePath = System.getProperty("caib-crypto-keystore");
        if (keyStorePath == null) {
            keyStorePath = System.getProperty("user.home") + "/.keystore";
        }
        keyStorePassword = null;
        String password = System.getProperty("caib-crypto-keystore-password");
        if (password != null) {
            keyStorePassword = password.toCharArray();
        }
        if (SigDebug.isActive()) SigDebug.write("BcSigner: Path keystore = " + keyStorePath);
        keyStore = getKeyStore();
    }

    /**
	 * Returns a list of the alias of the certificates available in the keystore.
	 */
    public String[] getCertList(boolean recognized) throws SignatureCertNotFoundException, SignaturePrivKeyException {
        try {
            Enumeration enumeration = keyStore.aliases();
            Vector keys = new Vector();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                Certificate[] certs = keyStore.getCertificateChain(alias);
                if (certs == null || certs.length == 0) continue;
                X509Certificate[] x509Certs = new X509Certificate[certs.length];
                for (int i = 0; i < certs.length; i++) {
                    if (certs[i] instanceof X509Certificate) x509Certs[i] = (X509Certificate) certs[i];
                }
                ParsedCertificateImpl parsed;
                try {
                    x509Certs[0].checkValidity();
                    parsed = new ParsedCertificateImpl(x509Certs, true);
                    if (parsed.isRecognized() && recognized || parsed.isAdvanced() && !recognized) {
                        SignaturaProperties signaturaProperties = new SignaturaProperties();
                        if (signaturaProperties.enDesenvolupament()) {
                            keys.add(parsed.getCommonName());
                        } else {
                            if (!parsed.isTest()) {
                                keys.add(parsed.getCommonName());
                            }
                        }
                    } else {
                        if (SigDebug.isActive()) {
                            SigDebug.write("Certificate " + parsed.getCommonName() + " refused. Searching " + (recognized ? "recognized" : "advanced") + " and the certificate is not.");
                        }
                    }
                } catch (CertificateException e) {
                } catch (IOException e) {
                }
            }
            return (String[]) keys.toArray(new String[keys.size()]);
        } catch (KeyStoreException e) {
            throw new SignatureCertNotFoundException();
        } catch (ProviderException e) {
            throw new SignatureCertNotFoundException();
        }
    }

    /**
	 * Generates the signature.
	 */
    private Signature generate(PrivateKey key, X509Certificate certs[], String contentType, InputStream stream, boolean timeStamp, boolean raw) throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSigner(key, certs[0], CMSSignedDataStreamGenerator.DIGEST_SHA1);
        Vector v = new Vector();
        for (int i = 0; i < certs.length; i++) {
            v.add(certs[i]);
        }
        CollectionCertStoreParameters param = new CollectionCertStoreParameters(v);
        CertStore certStore = CertStore.getInstance("Collection", param);
        gen.addCertificatesAndCRLs(certStore);
        ProcessableInputStream in;
        if (raw) {
            in = new ProcessableInputStream(stream);
        } else {
            in = new ProcessableInputStream(new MIMEInputStream(stream, contentType));
        }
        CMSSignedData signedData = gen.generate(in, BouncyCastleProvider.PROVIDER_NAME);
        if (timeStamp) {
            TimeStampManager tsm = new TimeStampManager();
            CMSSignedData signedDataTimestamp = tsm.addTimestamp(certs[0], signedData);
            if (signedDataTimestamp != null) {
                signedData = signedDataTimestamp;
            }
        }
        if (raw) {
            return new CMSSignatureRawv2(signedData.getEncoded(), contentType);
        } else {
            return new CMSSignaturev2(signedData.getEncoded(), contentType);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public Signature sign(InputStream contentStream, String certificateName, String password, String contentType, boolean recognized, boolean timeStamp, boolean rawSign) throws IOException, SignatureException {
        try {
            Enumeration enumeration = keyStore.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                Certificate[] certs = keyStore.getCertificateChain(alias);
                X509Certificate[] x509Certs = new X509Certificate[certs.length];
                for (int i = 0; i < certs.length; i++) {
                    if (certs[i] instanceof X509Certificate) x509Certs[i] = (X509Certificate) certs[i];
                }
                ParsedCertificateImpl parsed;
                try {
                    x509Certs[0].checkValidity();
                    parsed = new ParsedCertificateImpl(x509Certs, true);
                    if (parsed.getCommonName().equals(certificateName) && ((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized))) {
                        try {
                            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
                            return generate(privateKey, x509Certs, contentType, contentStream, timeStamp, rawSign);
                        } catch (UnrecoverableKeyException e) {
                            throw new SignatureException("Incorrect password", e);
                        } catch (Exception e) {
                            throw new SignatureException("Error generating signature", e);
                        }
                    } else {
                        if (SigDebug.isActive()) {
                            SigDebug.write("Certificate " + parsed.getCommonName() + " refused. Searching " + (recognized ? "recognized" : "advanced") + " and the certificate is not.");
                        }
                    }
                } catch (CertificateException e) {
                } catch (IOException e) {
                }
            }
            throw new SignatureException("Private key not found");
        } catch (KeyStoreException e) {
            throw new SignaturePrivKeyException();
        } catch (ProviderException e) {
            throw new SignatureCertNotFoundException();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public OutputStream signPDF(InputStream contentStream, String certificateName, String password, String contentType, boolean recognized, String url, int position) throws IOException, SignatureException {
        try {
            if (password == null) throw new SignaturePrivKeyException();
            char passchars[] = password.toCharArray();
            if (passchars == null && keyStorePassword != null || passchars.length != keyStorePassword.length) throw new SignatureException("Incorrect password");
            for (int i = 0; keyStorePassword != null && i < passchars.length; i++) if (passchars[i] != keyStorePassword[i]) throw new SignatureException("Incorrect password");
            Enumeration enumeration = keyStore.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                Certificate[] certs = keyStore.getCertificateChain(alias);
                X509Certificate[] x509Certs = new X509Certificate[certs.length];
                for (int i = 0; i < certs.length; i++) {
                    if (certs[i] instanceof X509Certificate) x509Certs[i] = (X509Certificate) certs[i];
                }
                ParsedCertificateImpl parsed;
                try {
                    x509Certs[0].checkValidity();
                    parsed = new ParsedCertificateImpl(x509Certs, true);
                    if (parsed.getCommonName().equals(certificateName) && ((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized))) {
                        try {
                            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
                            OutputStream out = PDFSigner.sign(contentStream, privateKey, x509Certs, url, position);
                            return out;
                        } catch (UnrecoverableKeyException e) {
                            throw new SignatureException("Incorrect password", e);
                        } catch (Exception e) {
                            throw new SignatureException("Error generating the signature", e);
                        }
                    } else {
                        if (SigDebug.isActive()) {
                            SigDebug.write("Certificate " + parsed.getCommonName() + " refused. Searching " + (recognized ? "recognized" : "advanced") + " and the certificate is not.");
                        }
                    }
                } catch (CertificateException e) {
                } catch (IOException e) {
                }
            }
            throw new SignatureException("Private key not found");
        } catch (KeyStoreException e) {
            throw new SignaturePrivKeyException();
        } catch (ProviderException e) {
            throw new SignatureCertNotFoundException();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public Date getCurrentDate(String certificateName, String password, boolean recognized) throws SignatureTimestampException, SignatureException, IOException {
        TimeStampManager tsm = new TimeStampManager();
        try {
            Enumeration enumeration = keyStore.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                Certificate[] certs = keyStore.getCertificateChain(alias);
                X509Certificate[] x509Certs = new X509Certificate[certs.length];
                for (int i = 0; i < certs.length; i++) {
                    if (certs[i] instanceof X509Certificate) x509Certs[i] = (X509Certificate) certs[i];
                }
                ParsedCertificateImpl parsed;
                x509Certs[0].checkValidity();
                parsed = new ParsedCertificateImpl(x509Certs, true);
                if (parsed.getCommonName().equals(certificateName) && ((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized))) {
                    return tsm.getTimeStamp(x509Certs[0]);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("Unable to get date", e);
        }
        throw new SignatureException("Certificate not found.");
    }

    public String getVersion() {
        return "1.0";
    }

    class ProcessableInputStream implements CMSProcessable {

        private DigestInputStream in;

        MessageDigest digester;

        byte digestResult[];

        public void write(OutputStream out) throws IOException, CMSException {
            byte b[] = new byte[8192];
            int read = in.read(b);
            while (read > 0) {
                out.write(b, 0, read);
                read = in.read(b);
            }
            out.close();
            in.close();
            digestResult = digester.digest();
        }

        public Object getContent() {
            return in;
        }

        public ProcessableInputStream(InputStream datain) throws NoSuchAlgorithmException, NoSuchProviderException {
            super();
            digester = MessageDigest.getInstance("SHA-1", BouncyCastleProvider.PROVIDER_NAME);
            in = new DigestInputStream(datain, digester);
            digestResult = null;
        }

        public byte[] getDigest() {
            return digestResult;
        }
    }
}
