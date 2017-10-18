package es.caib.signatura.provider.impl.pkcs11;

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
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStore;
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
import sun.security.pkcs11.SunPKCS11;
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

public class PKCS11Signer implements SignerProviderInterface {

    private char pin[];

    private SunPKCS11 provider;

    private String configFile;

    private String providerDesc;

    private KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (pin == null) {
            try {
                return getKeyStore(null);
            } catch (IOException e) {
                if (e.getCause() instanceof LoginException) {
                    return askForPin();
                } else throw e;
            } catch (KeyStoreException e) {
                if (e.getCause() instanceof LoginException) {
                    return askForPin();
                } else throw e;
            }
        } else {
            try {
                return getKeyStore(pin);
            } catch (KeyStoreException e) {
                disposeProvider();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                return getKeyStore();
            }
        }
    }

    /**
	 * Shows a dialog asking for the card pin.
	 * 
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 */
    private KeyStore askForPin() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        char newPin[] = PinDialog.getPIN(this.providerDesc);
        try {
            KeyStore ks = getKeyStore(newPin);
            pin = newPin;
            return ks;
        } catch (IOException e2) {
            e2.printStackTrace();
            if (e2.getCause() instanceof LoginException) {
                JOptionPane.showMessageDialog(null, "Wrong PIN", "Cryptographic card", JOptionPane.WARNING_MESSAGE);
            }
            throw e2;
        }
    }

    private KeyStore getKeyStore(char pinToUse[]) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS11", getProvider());
        try {
            ks.load(null, pinToUse);
        } catch (ProviderException e) {
            e.printStackTrace();
            throw new KeyStoreException("Unable to get KeyStore", e);
        }
        return ks;
    }

    public PKCS11Signer(String cfgFile, String providerDesc) {
        configFile = cfgFile;
        this.providerDesc = providerDesc;
    }

    private SunPKCS11 getProvider() {
        if (provider == null) {
            provider = new SunPKCS11(configFile);
            Security.addProvider(provider);
        }
        return provider;
    }

    private void disposeProvider() {
        pin = null;
        closeSession();
    }

    /**
	 * 
	 */
    private void closeSession() {
        try {
            if (provider != null) provider.logout();
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public String[] getCertList(boolean recognized) throws SignatureCertNotFoundException, SignaturePrivKeyException {
        KeyStore ks = null;
        try {
            ks = getKeyStore();
            Enumeration enumeration = ks.aliases();
            Vector keys = new Vector();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                X509Certificate[] certs = (X509Certificate[]) ks.getCertificateChain(alias);
                ParsedCertificateImpl parsed;
                try {
                    certs[0].checkValidity();
                    parsed = new ParsedCertificateImpl(certs, true);
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
                            SigDebug.write("PKCS11 Certificate " + parsed.getCommonName() + " refused. Searching " + (recognized ? "recognized" : "advanced") + " and the certificate is not.");
                        }
                    }
                } catch (CertificateException e) {
                } catch (IOException e) {
                }
            }
            return (String[]) keys.toArray(new String[keys.size()]);
        } catch (KeyStoreException e) {
            disposeProvider();
            throw new SignatureCertNotFoundException();
        } catch (NoSuchAlgorithmException e) {
            throw new SignaturePrivKeyException();
        } catch (CertificateException e) {
            throw new SignaturePrivKeyException();
        } catch (IOException e) {
            throw new SignatureCertNotFoundException();
        } catch (ProviderException e) {
            disposeProvider();
            throw new SignatureCertNotFoundException();
        }
    }

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
        CMSSignedData signedData = gen.generate(in, getProvider().getName());
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
            if (password == null) throw new SignaturePrivKeyException();
            char passchars[] = password.toCharArray();
            if (passchars == null && pin != null || passchars.length != pin.length) throw new SignatureException("Incorrect password");
            for (int i = 0; pin != null && i < passchars.length; i++) if (passchars[i] != pin[i]) throw new SignatureException("Incorrect password");
            KeyStore ks = getKeyStore();
            Enumeration enumeration = ks.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                X509Certificate certs[] = (X509Certificate[]) ks.getCertificateChain(alias);
                ParsedCertificateImpl parsed;
                try {
                    certs[0].checkValidity();
                    parsed = new ParsedCertificateImpl(certs, true);
                    if (parsed.getCommonName().equals(certificateName) && ((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized))) {
                        try {
                            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
                            return generate(privateKey, certs, contentType, contentStream, timeStamp, rawSign);
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
            disposeProvider();
            throw new SignaturePrivKeyException();
        } catch (NoSuchAlgorithmException e) {
            throw new SignaturePrivKeyException();
        } catch (CertificateException e) {
            throw new SignaturePrivKeyException();
        } catch (ProviderException e) {
            throw new SignatureCertNotFoundException();
        } catch (IOException e) {
            throw new SignatureCertNotFoundException();
        } finally {
            try {
                if (provider != null) provider.logout();
            } catch (LoginException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public OutputStream signPDF(InputStream contentStream, String certificateName, String password, String contentType, boolean recognized, String url, int position) throws IOException, SignatureException {
        try {
            if (password == null) throw new SignaturePrivKeyException();
            char passchars[] = password.toCharArray();
            if (passchars == null && pin != null || passchars.length != pin.length) throw new SignatureException("Incorrect password");
            for (int i = 0; pin != null && i < passchars.length; i++) if (passchars[i] != pin[i]) throw new SignatureException("Incorrect password");
            KeyStore ks = getKeyStore();
            Enumeration enumeration = ks.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                X509Certificate certs[] = (X509Certificate[]) ks.getCertificateChain(alias);
                ParsedCertificateImpl parsed;
                try {
                    certs[0].checkValidity();
                    parsed = new ParsedCertificateImpl(certs, true);
                    if (parsed.getCommonName().equals(certificateName) && ((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized))) {
                        try {
                            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
                            OutputStream out = PDFSigner.sign(contentStream, privateKey, certs, url, position);
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
            disposeProvider();
            throw new SignaturePrivKeyException();
        } catch (NoSuchAlgorithmException e) {
            throw new SignaturePrivKeyException();
        } catch (CertificateException e) {
            throw new SignaturePrivKeyException();
        } catch (ProviderException e) {
            throw new SignatureCertNotFoundException();
        } catch (IOException e) {
            throw new SignatureCertNotFoundException();
        } finally {
            try {
                if (provider != null) provider.logout();
            } catch (LoginException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public Date getCurrentDate(String certificateName, String password, boolean recognized) throws SignatureTimestampException, SignatureException, IOException {
        TimeStampManager tsm = new TimeStampManager();
        try {
            KeyStore ks = getKeyStore();
            Enumeration enumeration = ks.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = (String) enumeration.nextElement();
                X509Certificate certs[] = (X509Certificate[]) ks.getCertificateChain(alias);
                ParsedCertificateImpl parsed;
                certs[0].checkValidity();
                parsed = new ParsedCertificateImpl(certs, true);
                if (parsed.getCommonName().equals(certificateName) && ((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized))) {
                    return tsm.getTimeStamp(certs[0]);
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
            digester = MessageDigest.getInstance("SHA-1", "BC");
            in = new DigestInputStream(datain, digester);
            digestResult = null;
        }

        public byte[] getDigest() {
            return digestResult;
        }
    }

    protected void finalize() {
        closeSession();
    }
}
