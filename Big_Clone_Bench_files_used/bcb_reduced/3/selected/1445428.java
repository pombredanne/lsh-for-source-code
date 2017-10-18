package edu.cps.messa.im;

import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.List;
import java.util.Vector;
import edu.cps.messa.Logger;

/**
 * Unique identifier for instante messaging users. Usually, the email. It
 * includes some useful user information, like public key.
 * This information is to be published along with the location information
 * @author Alvaro J. Iradier
 */
public class MessaPublicId implements java.io.Serializable {

    private CertPath certPath;

    private byte[] email_hash;

    public MessaPublicId(String email) {
        certPath = null;
        email_hash = sha1hash(email);
    }

    public static byte[] sha1hash(String str) {
        byte[] email_hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            email_hash = md.digest(str.getBytes("UTF-8"));
            email_hash = md.digest(email_hash);
        } catch (Exception ex) {
            Logger.errorMessage("Exception: " + ex.getMessage());
            email_hash = str.getBytes();
        }
        return email_hash;
    }

    /**
     * @return Returns the public key
     */
    public PublicKey getPublicKey() {
        List certs = getCertificatePath().getCertificates();
        Certificate cert = (Certificate) certs.get(certs.size() - 1);
        return cert.getPublicKey();
    }

    /**
     * 
     * @return The user certificate path
     */
    public CertPath getCertificatePath() {
        return certPath;
    }

    public void setCertificatePath(CertPath path) {
        certPath = path;
    }

    /**
     * Sets a self signed certificate for the given public key 
     * @param cert
     * @throws CertificateException
     */
    public void setPublicKey(Certificate cert) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        Vector certChain = new Vector();
        certChain.add(cert);
        setCertificatePath(certFactory.generateCertPath(certChain));
    }

    /**
     * @return the user public key Fingerprint
     */
    public String getKeyFingerprint() throws Exception {
        Logger.infoMessage("Creating key digest");
        MessageDigest mesdig;
        mesdig = MessageDigest.getInstance("MD5");
        byte[] result = mesdig.digest(getPublicKey().getEncoded());
        String fingerprint = new String("");
        for (int jj = 0; jj < result.length; ++jj) {
            char bytes[] = new char[2];
            bytes[0] = "0123456789ABCDEF".charAt((result[jj] >> 4) & 0x0F);
            bytes[1] = "0123456789ABCDEF".charAt(result[jj] & 0x0F);
            fingerprint = fingerprint.concat(new String(bytes) + ":");
        }
        return fingerprint;
    }
}
