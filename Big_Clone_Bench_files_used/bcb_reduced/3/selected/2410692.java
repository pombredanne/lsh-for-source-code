package org.dbe.zidbe;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.dbe.zidbe.util.CertsKeysProxiesFileReaderWriter;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.bc.BouncyCastleCertProcessingFactory;

/**
 * The CRES class corresponds to the simple implementation of the CREdential
 * Server (CRES). TODO: Remove the private keys from the memory after creation
 * and load them from their files when needed
 *
 * @author Jm
 * @author <a href="mailto:Dominik.Dahlem@cs.tcd.ie">Dominik Dahlem</a>
 */
public class CRES {

    public ZIDBEProperties props;

    private X509Name defUserX509Name;

    private X509Name defLocalCAX509Name;

    private X509Name defSERVENTX509Name;

    private final java.security.SecureRandom _rand = new java.security.SecureRandom();

    public CRES(ZIDBEProperties props) {
        this.props = props;
        defUserX509Name = createX509Name(props._defUserSubject);
        defLocalCAX509Name = createX509Name(props._defLocalCASubject);
        defSERVENTX509Name = createX509Name(props._defSERVENTSubject);
        System.out.println("Constructing the local CRES");
        if (!isUserIDBEInstalled()) {
            createUserIDBE();
        }
        if (!isProxyIDBEValid()) {
            createTempDefaultUserIDBEProxy();
        }
        if (!isSERVENTCertAndKeyInstalled()) {
            createSERVENTCertAndKey();
        }
        if (props._wantLocalCA) {
            System.out.println("A local CA is wanted... starts" + " the local CA check");
            if (!isLocalCAInstalled()) {
                createLocalCA();
            }
        } else {
            System.out.println("No local CA is wanted.");
        }
    }

    /**
     * Create a temporary proxy of the default user's IDBE
     *
     * @return void
     */
    private void createTempDefaultUserIDBEProxy() {
        System.out.println("Creating the temporary proxy of the user's " + "default IDBE");
        BouncyCastleCertProcessingFactory proxyFacto = BouncyCastleCertProcessingFactory.getDefault();
        try {
            X509Certificate userIDBEMainCert = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(props._defUserIDBE);
            PrivateKey userIDBEPrivateKey = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(props._defUserKey);
            KeyPair newKeyPair;
            newKeyPair = CertUtil.generateKeyPair(props._defAsymAlgo, props._defKeyLength);
            byte[] proxyRequest = proxyFacto.createCertificateRequest(userIDBEMainCert, newKeyPair);
            InputStream proxyRequestStream = new ByteArrayInputStream(proxyRequest);
            X509Certificate newProxy = proxyFacto.createCertificate(proxyRequestStream, userIDBEMainCert, userIDBEPrivateKey, (((int) props._defProxyLifetime) / 1000), GSIConstants.GSI_3_IMPERSONATION_PROXY);
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(newProxy, props._defUserProxyIDBE);
            CertsKeysProxiesFileReaderWriter.writePrivateKeyToPEMFile(newKeyPair.getPrivate(), props._defUserProxyIDBEKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an impersonation proxy for the specified IDBE and for the
     * specified lifetime in the full path of the specified directory with
     * the specified file name
     * @return void
     */
    public void createImpersonationIDBEProxy(File idbeFullPathFile, int lifetime, String newProxyDir, String newProxyFileName) {
        BouncyCastleCertProcessingFactory proxyFacto = BouncyCastleCertProcessingFactory.getDefault();
        try {
            X509Certificate userIDBE = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(idbeFullPathFile.getAbsolutePath());
            PrivateKey userIDBEPrivateKey = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(supposedAssociatedPrivateKeyFullPathFile(idbeFullPathFile.getAbsolutePath()));
            KeyPair newKeyPair;
            newKeyPair = CertUtil.generateKeyPair(props._defAsymAlgo, props._defKeyLength);
            byte[] proxyRequest = proxyFacto.createCertificateRequest(userIDBE, newKeyPair);
            InputStream proxyRequestStream = new ByteArrayInputStream(proxyRequest);
            X509Certificate newProxy = proxyFacto.createCertificate(proxyRequestStream, userIDBE, userIDBEPrivateKey, lifetime, GSIConstants.GSI_3_IMPERSONATION_PROXY);
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(newProxy, newProxyDir + "/" + newProxyFileName);
            CertsKeysProxiesFileReaderWriter.writePrivateKeyToPEMFile(newKeyPair.getPrivate(), supposedAssociatedPrivateKeyFullPathFile(newProxyDir + "/" + newProxyFileName));
            System.out.println("New impersonation Proxy IDBE created in " + newProxyDir + "!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an impersonation proxy for the specified IDBE and for the
     * specified lifetime in the full path of the specified directory with
     * the same file name as the IDBE
     * @return void
     */
    public void createImpersonationIDBEProxy(File idbeFullPathFile, int lifetime, String newProxyDir) {
        BouncyCastleCertProcessingFactory proxyFacto = BouncyCastleCertProcessingFactory.getDefault();
        try {
            X509Certificate userIDBE = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(idbeFullPathFile.getAbsolutePath());
            PrivateKey userIDBEPrivateKey = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(supposedAssociatedPrivateKeyFullPathFile(idbeFullPathFile.getAbsolutePath()));
            KeyPair newKeyPair;
            newKeyPair = CertUtil.generateKeyPair(props._defAsymAlgo, props._defKeyLength);
            byte[] proxyRequest = proxyFacto.createCertificateRequest(userIDBE, newKeyPair);
            InputStream proxyRequestStream = new ByteArrayInputStream(proxyRequest);
            X509Certificate newProxy = proxyFacto.createCertificate(proxyRequestStream, userIDBE, userIDBEPrivateKey, lifetime, GSIConstants.GSI_3_IMPERSONATION_PROXY);
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(newProxy, newProxyDir + supposedAssociatedProxyFileName(idbeFullPathFile.getName()));
            CertsKeysProxiesFileReaderWriter.writePrivateKeyToPEMFile(newKeyPair.getPrivate(), supposedAssociatedPrivateKeyFullPathFile(newProxyDir + supposedAssociatedProxyFileName(idbeFullPathFile.getName())));
            System.out.println("New impersonation Proxy IDBE created!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static X509Name createX509Name(String subject) {
        return new X509Name(subject);
    }

    /**
     * Create the local user's IDBE in the directories given in the properties.
     * This creates both the PEM private key (UNENCRYPTED) and the PEM
     * self-signed certificate for one year. TODO: The only algorithm available
     * in GSI is RSA
     */
    private void createUserIDBE() {
        System.out.println("Creating the local user IDBE");
        try {
            KeyPair newKeyPair;
            newKeyPair = CertUtil.generateKeyPair(props._defAsymAlgo, props._defKeyLength);
            CertsKeysProxiesFileReaderWriter.writePrivateKeyToPEMFile(newKeyPair.getPrivate(), props._defUserKey);
            System.out.println("UNENCRYPTED user's private key " + "stored in the file " + props._defUserKey);
            X509Certificate newCert = signCertificate(defUserX509Name, defUserX509Name, newKeyPair.getPublic(), newKeyPair, props._defMainCertLifetime);
            System.out.println("The local user's self-cert subject is " + newCert.getSubjectDN().getName());
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(newCert, props._defUserIDBE);
            System.out.println("User's self-signed proxy stored in file " + props._defUserIDBE);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * @param
     * @return
     * @throws
     */
    private X509Certificate signCertificate(X509Name issuerDN, X509Name subjectDN, PublicKey subjectPublicKey, KeyPair issuerKeyPair, long timeValidity) throws SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, IOException {
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.reset();
        certGen.setIssuerDN(issuerDN);
        certGen.setSubjectDN(subjectDN);
        certGen.setNotBefore(new Date(System.currentTimeMillis()));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + timeValidity));
        certGen.setPublicKey(subjectPublicKey);
        certGen.setSignatureAlgorithm(props._defSignAlgo);
        certGen.setSerialNumber(new BigInteger(new Long(_rand.nextLong()).toString()));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, createSubjectKeyId(subjectPublicKey));
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, createAuthorityKeyId(issuerKeyPair.getPublic()));
        certGen.addExtension(X509Extensions.BasicConstraints, false, new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, false, new KeyUsage(KeyUsage.dataEncipherment | KeyUsage.digitalSignature));
        X509Certificate newCert = certGen.generateX509Certificate(issuerKeyPair.getPrivate(), "BC");
        newCert.checkValidity(new Date());
        newCert.verify(issuerKeyPair.getPublic());
        return newCert;
    }

    /**
     * Create the local CA in the directories given in the properties. This
     * creates both the PEM private key (UNENCRYPTED) and the PEM self-signed
     * certificate for one year. TODO: The only algorithm available in GSI is
     * RSA
     */
    private void createLocalCA() {
        System.out.println("Creating the local CA");
        try {
            KeyPair newKeyPair;
            newKeyPair = CertUtil.generateKeyPair(props._defAsymAlgo, props._defKeyLength);
            CertsKeysProxiesFileReaderWriter.writePrivateKeyToPEMFile(newKeyPair.getPrivate(), props._defCAKey);
            System.out.println("UNENCRYPTED CA's private key stored in the file " + props._defCAKey);
            X509Certificate newCert = signCertificate(defLocalCAX509Name, defLocalCAX509Name, newKeyPair.getPublic(), newKeyPair, props._defMainCertLifetime);
            System.out.println("The local CA's self-cert subject is " + newCert.getSubjectDN().getName());
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(newCert, props._defCACert);
            System.out.println("Local CA's self-signed proxy stored in file " + props._defCACert);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Create the SERVENT key and cert in the directories given in the
     * properties. This creates both the PEM private key (UNENCRYPTED) and the
     * PEM self-signed certificate for one year. TODO: The only algorithm
     * available in GSI is RSA
     */
    private void createSERVENTCertAndKey() {
        System.out.println("Creating the SERVENT key and cert.");
        try {
            KeyPair newKeyPair;
            newKeyPair = CertUtil.generateKeyPair(props._defAsymAlgo, props._defKeyLength);
            CertsKeysProxiesFileReaderWriter.writePrivateKeyToPEMFile(newKeyPair.getPrivate(), props._defSERVENTKey);
            System.out.println("UNENCRYPTED SERVENT's private" + " key stored in the file " + props._defSERVENTKey);
            X509Certificate newCert = signCertificate(defSERVENTX509Name, defSERVENTX509Name, newKeyPair.getPublic(), newKeyPair, props._defMainCertLifetime);
            System.out.println("SERVENT's self-cert subject is " + newCert.getSubjectDN().getName());
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(newCert, props._defSERVENTCert);
            System.out.println("SERVENT's self-signed proxy stored in file " + props._defSERVENTCert);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Check if the default user's proxy IDBE is valid in the directories
     * specified in the properties ( USER_DEF_PROXY_IDBE_FILENAME &
     * USER_DEF_PROXY_IDBE_KEY_FILENAME are checked)
     *
     * @return boolean exist (true if the files exist)
     */
    private boolean isProxyIDBEValid() {
        String defProxyIDBEPath = props._defUserProxyIDBE;
        String defProxyIDBEKeyPath = props._defUserProxyIDBEKey;
        boolean valid = false;
        try {
            PrivateKey key = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(defProxyIDBEKeyPath);
            if (key == null) {
                return false;
            }
            System.out.println("The default user's proxy key PEM file exists!");
            X509Certificate cert = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(defProxyIDBEPath);
            if (cert == null) {
                throw new NullPointerException("The cert in the local user's proxy IDBE PEM" + " file is null!");
            }
            System.out.println("The default user's proxy cert PEM file exists!");
            Date certValidityFinalDate = cert.getNotAfter();
            System.out.println("User's proxy IDBE valid until: " + certValidityFinalDate);
            Date today = new Date(System.currentTimeMillis());
            System.out.println("Now is: " + today);
            if (!today.before(certValidityFinalDate)) {
                System.out.println("The proxy IDBE is NOT valid, another " + "proxy must be created.");
                valid = false;
            } else {
                valid = true;
                System.out.println("The default user's proxy IDBE is still valid.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The local user's proxy IDBE isn't set up correctly" + " --> A new key and cert are generated from scratch.");
        }
        return valid;
    }

    /**
     * Check if the local default user's IDBE is installed in the directories
     * specified in the properties ( USER_DEF_PROXY_IDBE_FILENAME &
     * USER_DEF_PROXY_IDBE_KEY_FILENAME are checked)
     *
     * @return boolean exist (true if the files exist)
     */
    private boolean isUserIDBEInstalled() {
        String defIDBEPath = props._defUserIDBE;
        String defIDBEKeyPath = props._defUserKey;
        boolean exist = false;
        try {
            PrivateKey key = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(defIDBEKeyPath);
            if (key == null) {
                return false;
            }
            System.out.println("The default user's key PEM file exists!");
            X509Certificate cert = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(defIDBEPath);
            if (cert == null) {
                throw new NullPointerException("The cert in the local user's PEM file is null!");
            }
            System.out.println("The default user's cert PEM file exists!");
            exist = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The local user's IDBE isn't set up correctly" + " --> A new key and cert are generated from scratch.");
        }
        return exist;
    }

    /**
     * Check if the SERVENT cert and key are installed in the directories
     * specified in the properties ( SERVENT_KEY_FILENAME &
     * SERVENT_CERT_FILENAME are checked)
     *
     * @return boolean exist (true if the files exist)
     */
    private boolean isSERVENTCertAndKeyInstalled() {
        String certSERVENTPath = props._defSERVENTCert;
        String keySERVENTPath = props._defSERVENTKey;
        boolean exist = false;
        try {
            PrivateKey key = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(keySERVENTPath);
            if (key == null) {
                return false;
            }
            System.out.println("The default SERVENT's key PEM file exists!");
            X509Certificate cert = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(certSERVENTPath);
            if (cert == null) {
                throw new NullPointerException("The cert in the SERVENT's PEM file is null!");
            }
            System.out.println("The default SERVENT's cert PEM file exists!");
            exist = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The SERVENT's cert and key aren't set up correctly" + " --> A new key and cert are generated from scratch.");
        }
        return exist;
    }

    /**
     * Check if the local CA is installed in the directories specified in the
     * properties ( CA_KEY_FILENAME & CA_CERT_FILENAME are checked)
     *
     * @return boolean exist (true if the files exist)
     */
    private boolean isLocalCAInstalled() {
        String defCACertPath = props._defCACert;
        String defCAKeyPath = props._defCAKey;
        boolean exist = false;
        try {
            PrivateKey key = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(defCAKeyPath);
            if (key == null) {
                return false;
            }
            System.out.println("The local CA's key PEM file exists!");
            X509Certificate cert = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(defCACertPath);
            if (cert == null) {
                throw new NullPointerException("The cert in the local CA's PEM file is null!");
            }
            System.out.println("The CA's cert PEM file exists!");
            exist = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The local CA isn't set up correctly" + " --> A new key and cert are generated from scratch.");
        }
        return exist;
    }

    /**
     *
     * @param pubKey
     * @return
     */
    private static SubjectKeyIdentifier createSubjectKeyId(PublicKey pubKey) {
        try {
            ByteArrayInputStream bIn = new ByteArrayInputStream(pubKey.getEncoded());
            SubjectPublicKeyInfo info = new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(bIn).readObject());
            return new SubjectKeyIdentifier(info);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param pubKey
     * @return
     * @throws IOException
     */
    private static AuthorityKeyIdentifier createAuthorityKeyId(PublicKey pubKey) throws IOException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(pubKey.getEncoded());
        SubjectPublicKeyInfo info = new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(bIn).readObject());
        return new AuthorityKeyIdentifier(info);
    }

    /**
     * Create a PKCS10 certificate request in the specified PEM file
     */
    public void createPEMPKCS10CertificationRequest(String pkcs10CRFile, X509Name subject, KeyPair subjectKeyPair) {
        try {
            PKCS10CertificationRequest newReq = new PKCS10CertificationRequest(props._defSignAlgo, subject, subjectKeyPair.getPublic(), null, subjectKeyPair.getPrivate());
            CertsKeysProxiesFileReaderWriter.writePKCS10CRToPEMFile(newReq, pkcs10CRFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("CANNOT create new PKCS10 certificate request " + "in " + pkcs10CRFile + " for subject: " + subject);
            System.exit(-1);
        }
        System.out.println("New PKCS10 certificate request created in " + pkcs10CRFile + " for subject: " + subject);
    }

    /**
     * Sign a PKCS10 Certificate Request. All input files must be PEM encoded.
     * @param pkcs10RqPath Full path to the certificate request PEM file
     * @param localCACertPath Full path to the CA certificate used for signature
     * @param localCAKeyPath Full path to the CA key file used for signature
     * @param certRqSignedPath Full path to the output X509 signed certificate
     * @param localCAPwd CA passphrase TODO: No password used yet.
     */
    public void signCertRequest(String pkcs10RqPath, String localCACertPath, String localCAKeyPath, String certRqSignedPath, String localCAPwd, long validity) {
        System.out.println("Signing PKCS10CR = " + pkcs10RqPath + " with local CA = " + localCACertPath + " and CA key=" + localCAKeyPath + " for the resulting X509 signed cert here =" + certRqSignedPath);
        try {
            X509Certificate localCACert = CertsKeysProxiesFileReaderWriter.readCertFromPEMFile(localCACertPath);
            localCACert.checkValidity();
            PrivateKey localCAPrivateKey = CertsKeysProxiesFileReaderWriter.readPrivateKeyFromPEMFile(localCAKeyPath);
            localCAPrivateKey.getFormat();
            KeyPair localCAKeyPair = new KeyPair(localCACert.getPublicKey(), localCAPrivateKey);
            PKCS10CertificationRequest certRq = CertsKeysProxiesFileReaderWriter.readPKCS10CRfromPEM(pkcs10RqPath);
            certRq.getPublicKey();
            X509Certificate certRqSigned = signCertificate(defLocalCAX509Name, certRq.getCertificationRequestInfo().getSubject(), certRq.getPublicKey(), localCAKeyPair, validity);
            CertsKeysProxiesFileReaderWriter.writeX509CertToPEMFile(certRqSigned, certRqSignedPath);
        } catch (Exception e) {
            System.out.println("An error has occured during the signing of a PKCS10CR!");
            e.printStackTrace();
        }
    }

    /**
     * Returns the SHA-1 hash of the given public key
     * @param pub
     * @return String SHA-1(pub) in Base64
     */
    public static String hashSHA1PublicKey(PublicKey pub) {
        String hashSHA1 = null;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA");
            hash.update(pub.getEncoded());
            ByteArrayInputStream bis = new ByteArrayInputStream(Base64.encode(hash.digest()));
            InputStreamReader irr = new InputStreamReader(bis);
            BufferedReader r = new BufferedReader(irr);
            StringBuffer buff = new StringBuffer();
            String line;
            while ((line = r.readLine()) != null) {
                buff.append(line + "\n");
            }
            hashSHA1 = buff.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashSHA1;
    }

    public static String supposedAssociatedPrivateKeyFullPathFile(String certFullPathFile) {
        return certFullPathFile.substring(0, certFullPathFile.length() - 4) + "key.pem";
    }

    public static String supposedAssociatedProxyFileName(String idbeFileName) {
        String supposedProxyIDBEFileName = idbeFileName.substring(0, idbeFileName.length() - 4);
        return supposedProxyIDBEFileName + "proxy.pem";
    }
}
