package es.caib.signatura.provider.impl.mscryptoapi;

import java.io.*;
import java.util.Date;
import java.util.ArrayList;
import java.util.Vector;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.ProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.*;
import java.security.*;
import org.bouncycastle.cms.*;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.util.encoders.Base64;
import es.caib.signatura.api.SignaturePrivKeyException;
import es.caib.signatura.provider.impl.mscryptoapi.mscrypto.MSCryptoFunctions;
import es.caib.signatura.provider.impl.mscryptoapi.mscrypto.MSKeyMgrProvider;
import es.caib.signatura.provider.impl.mscryptoapi.mscrypto.MSRSASignProvider;
import es.caib.signatura.provider.impl.mscryptoapi.mscrypto.MSTrustMgrProvider;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;
import es.caib.signatura.api.Signature;
import es.caib.signatura.api.SignatureCertNotFoundException;
import es.caib.signatura.api.SignatureException;
import es.caib.signatura.api.SignatureTimestampException;
import es.caib.signatura.api.Signer;
import es.caib.signatura.impl.CMSSignatureRawv2;
import es.caib.signatura.impl.CMSSignaturev2;
import es.caib.signatura.impl.SigDebug;
import es.caib.signatura.impl.ValidadorProxy;
import es.caib.signatura.impl.MIMEInputStream;
import es.caib.signatura.impl.SignerProviderInterface;
import es.caib.signatura.provider.impl.common.PDFSigner;
import es.caib.signatura.provider.impl.common.ParsedCertificateImpl;
import es.caib.signatura.provider.impl.common.TimeStampManager;

/**
 * Implementation of the interface <code>Signer</code> using the Microsoft CryptoApi as signature provider,
 * through the Java API developed by Boyter.
 * 
 * @author Jesús Reyes (3dígits)
 * @see Signer
 * @see Signature
 * @version 0.9.0
 */
public class MscryptoapiSigner implements SignerProviderInterface {

    public static final String SECUREDEVICE = "SD\1";

    public static final String NONSECUREDEVICE = "ND\1";

    private TrustManagerFactory tmf = null;

    private KeyManagerFactory kmf = null;

    private X509TrustManager xtm = null;

    private X509KeyManager xkm = null;

    private MSCryptoFunctions mscf = new MSCryptoFunctions();

    public MscryptoapiSigner() {
        MSTrustMgrProvider.install();
        MSKeyMgrProvider.install();
        try {
            tmf = TrustManagerFactory.getInstance("MSTMF");
            tmf.init((KeyStore) null);
            kmf = KeyManagerFactory.getInstance("MSKMF");
            kmf.init(null, null);
        } catch (Exception ex) {
        }
        xtm = (X509TrustManager) tmf.getTrustManagers()[0];
        xkm = (X509KeyManager) kmf.getKeyManagers()[0];
    }

    public String[] getCertList(boolean recognized) throws SignatureCertNotFoundException, SignaturePrivKeyException {
        try {
            X509Certificate[] issuerCerts = xtm.getAcceptedIssuers();
            if (issuerCerts == null || issuerCerts.length == 0) {
                return new String[0];
            }
            Principal[] issuers = new Principal[issuerCerts.length];
            if (issuers == null || issuers.length == 0) {
                return new String[0];
            }
            for (int i = 0; i < issuerCerts.length; i++) {
                issuers[i] = issuerCerts[i].getSubjectDN();
            }
            Vector keys = new Vector();
            String[] aliases = xkm.getClientAliases("X509", issuers);
            if (aliases == null || aliases.length == 0) throw new SignatureCertNotFoundException();
            ValidadorProxy validador = new ValidadorProxy();
            for (int i = 0; i < aliases.length; i++) {
                X509Certificate[] certs = null;
                certs = xkm.getCertificateChain(aliases[i]);
                ParsedCertificateImpl parsed;
                certs[0].checkValidity();
                boolean exportable = mscf.MSisPrivateKeyExportable(aliases[i]);
                if (!exportable || !recognized) {
                    parsed = new ParsedCertificateImpl(certs, true);
                    if (parsed.isRecognized() && recognized || parsed.isAdvanced() && !recognized) {
                        if (validador.isEnDesenvolupament()) {
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
                }
            }
            return (String[]) keys.toArray(new String[keys.size()]);
        } catch (CertificateException e) {
            throw new SignatureCertNotFoundException();
        } catch (IOException e) {
            throw new SignatureCertNotFoundException();
        } catch (ProviderException e) {
            throw new SignatureCertNotFoundException();
        }
    }

    public Signature sign(InputStream contentStream, String certificateName, String password, String contentType, boolean recognized, boolean timeStamp, boolean rawSign) throws IOException, SignatureException {
        Signature signatureData = null;
        String alias = this.getAliasFromCN(certificateName, recognized);
        if (alias == null) throw new SignatureException("The alias of the certificate " + certificateName + " has not been found.");
        MSRSASignProvider.install();
        try {
            PrivateKey pKey = new PrivateKey(password + Character.toString('\0') + alias);
            if (pKey == null) {
                throw new SignatureException("Unable to acces to the private key of the certificate with the alias " + alias);
            }
            X509Certificate[] certChain = xkm.getCertificateChain(alias);
            CMSSignedData cmsSignedData = this.buildPKCS7(contentStream, pKey, certChain, contentType, timeStamp, rawSign);
            if (cmsSignedData != null) {
                if (rawSign) {
                    signatureData = new CMSSignatureRawv2(cmsSignedData.getEncoded(), contentType);
                } else {
                    signatureData = new CMSSignaturev2(cmsSignedData.getEncoded(), contentType);
                }
            } else {
                throw new SignatureException("Error creating PKCS#7");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SignatureException(e.getMessage());
        }
        return signatureData;
    }

    public OutputStream signPDF(InputStream contentStream, String certificateName, String password, String contentType, boolean recognized, String url, int position) throws IOException, SignatureException {
        String alias = this.getAliasFromCN(certificateName, recognized);
        if (alias == null) {
            throw new SignatureException("The alias of the certificate " + certificateName + " has not been found.");
        }
        MSRSASignProvider.install();
        try {
            PrivateKey pKey = new PrivateKey(password + Character.toString('\0') + alias);
            if (pKey == null) {
                throw new SignatureException("Unable to acces to the private key of the certificate with the alias " + alias);
            }
            X509Certificate[] certChain = xkm.getCertificateChain(alias);
            OutputStream out = PDFSigner.sign(contentStream, pKey, certChain, url, position);
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SignatureException(e.getMessage());
        }
    }

    /**
	 * Signs the document and builds a PKCS#7 signature with the certificate chain used included in it.
	 * 
	 * @param content document to sign.
	 * @param pKey private key of the certificate to sign.
	 * @param cert certificate chain used to sign.
	 * @param contentType content type used to sign.
	 * @param timestamp indicates if a timestamp token has to be added.
	 * @param rawSign if true then the signature has to be from the original content; otherwise, the signature
	 * has to be from a S/MIME document with the original content.
	 * 
	 * @return The generated signature.
	 */
    private CMSSignedData buildPKCS7(InputStream content, PrivateKey pKey, X509Certificate[] cert, String contentType, boolean timeStamp, boolean rawSign) throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ArrayList certList = new ArrayList();
        for (int i = 0; i < cert.length; i++) {
            certList.add(cert[i]);
        }
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        CertStore certs = null;
        try {
            certs = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList));
        } catch (InvalidAlgorithmParameterException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return null;
        }
        try {
            gen.addCertificatesAndCRLs(certs);
            gen.addSigner(pKey, cert[0], CMSSignedDataGenerator.DIGEST_SHA1);
        } catch (CertStoreException e2) {
            e2.printStackTrace();
            return null;
        } catch (CMSException e2) {
            e2.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        CMSSignedData signedData = null;
        try {
            ProcessableInputStream in;
            if (rawSign) {
                in = new ProcessableInputStream(content);
            } else {
                in = new ProcessableInputStream(new MIMEInputStream(content, contentType));
            }
            signedData = gen.generate(in, "MicrosoftRSASign");
            if (timeStamp) {
                try {
                    TimeStampManager tsm = new TimeStampManager();
                    CMSSignedData signedDataTimestamp = tsm.addTimestamp(cert[0], signedData);
                    if (signedDataTimestamp != null) {
                        signedData = signedDataTimestamp;
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e4) {
            if (e4 instanceof LoginException) {
                throw e4;
            }
            e4.printStackTrace();
            return null;
        }
        return signedData;
    }

    public void generateSMIME(InputStream document, Signature signature, OutputStream smime) throws IOException {
        String smimeText = new String("MIME-Version: 1.0\r\n");
        smimeText = smimeText + "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=\"sha1\"; ";
        smimeText = smimeText + "boundary=\"govern-de-les-illes-balears-boundary\"\r\n\r\n";
        smimeText = smimeText + "--govern-de-les-illes-balears-boundary\r\n";
        smime.write(smimeText.getBytes("UTF-8"));
        MIMEInputStream mime = new MIMEInputStream(document, signature.getContentType());
        int n;
        byte[] byteBuffer = new byte[256];
        while ((n = mime.read(byteBuffer)) != -1) {
            smime.write(byteBuffer, 0, n);
        }
        smimeText = "\r\n--govern-de-les-illes-balears-boundary\r\n";
        smimeText = smimeText + "Content-Type: application/pkcs7-signature; name=\"smime.p7s\" smime-type=signed-data\r\n";
        smimeText = smimeText + "Content-Transfer-Encoding: base64\r\n";
        smimeText = smimeText + "Content-Disposition: attachment; filename=\"smime.p7s\"\r\n";
        smimeText = smimeText + "Content-Description: S/MIME Cryptographic Signature\r\n\r\n";
        smime.write(smimeText.getBytes("UTF-8"));
        byte[] p7Base64 = Base64.encode(signature.getPkcs7());
        StringReader reader = new StringReader(new String(p7Base64, "UTF-8"));
        StringBuffer text = new StringBuffer();
        n = 0;
        char[] buffer = new char[64];
        while ((n = reader.read(buffer, 0, 64)) > -1) {
            text.append(buffer, 0, n);
            text.append("\r\n");
        }
        text.append("\r\n--govern-de-les-illes-balears-boundary--\r\n");
        smime.write(text.toString().getBytes("UTF-8"));
        smime.flush();
        smime.close();
        document.close();
    }

    public Date getCurrentDate(String certificateName, String password, boolean recognized) throws SignatureTimestampException, SignatureException, IOException {
        TimeStampManager tsm = new TimeStampManager();
        try {
            String alias = this.getAliasFromCN(certificateName, recognized);
            if (alias == null) throw new SignatureException("The alias of the certificate " + certificateName + " has not been found.");
            MSRSASignProvider.install();
            X509Certificate[] certChain = xkm.getCertificateChain(alias);
            return tsm.getTimeStamp(certChain[0]);
        } catch (TSPException e) {
            throw new SignatureException("Unable to get the date.", e);
        }
    }

    /**
	 * Gets the alias from the common name of the certificate.
	 * 
	 * @param cn Common Name of the certificate
	 * @param recognized Indicates whether the certificate has to be recognized or not.
	 * @return The alias of the certificate.
	 */
    private String getAliasFromCN(String cn, boolean recognized) {
        boolean found = false;
        int i;
        X509Certificate[] issuerCerts = xtm.getAcceptedIssuers();
        Principal[] issuers = new Principal[issuerCerts.length];
        for (i = 0; i < issuerCerts.length; i++) issuers[i] = issuerCerts[i].getSubjectDN();
        String[] aliases = xkm.getClientAliases("X509", issuers);
        if (aliases.length == 0) return null;
        i = 0;
        while (!found && i < aliases.length) {
            X509Certificate[] certChain = xkm.getCertificateChain(aliases[i]);
            try {
                boolean exportable = mscf.MSisPrivateKeyExportable(aliases[i]);
                ParsedCertificateImpl parsed = new ParsedCertificateImpl(certChain, !exportable);
                if (((parsed.isRecognized() && recognized) || (parsed.isAdvanced() && !recognized)) && parsed.getCommonName().equals(cn)) {
                    return aliases[i];
                } else {
                    if (SigDebug.isActive() && parsed.getCommonName().equals(cn)) {
                        SigDebug.write("Certificate " + parsed.getCommonName() + " refused. Searching " + (recognized ? "recognized" : "advanced") + " certificates and it is not.");
                    }
                }
            } catch (IOException e) {
            } catch (CertificateException e) {
            }
            i++;
        }
        return null;
    }

    public String getVersion() {
        return "0.9.0";
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
    }
}
