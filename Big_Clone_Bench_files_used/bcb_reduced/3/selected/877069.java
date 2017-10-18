package com.intel.gui.editors.keystore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import org.bouncycastle.util.encoders.Base64;

/**
 * 
 * @author Thomas Kentemich
 * @version $Id$
 */
public class CertificateUtility {

    private static final Logger logger = Logger.getLogger("com.intel.gpe");

    /**
     * Test a X509Certifcate[] chain for Issuer/Subject order
     * 
     * @param chain
     *            Certificate chain to check
     * @return boolean true if the array contains an ordered chain of
     *         Subject/Issuer certificates, false otherwise
     */
    public static boolean checkOrder(X509Certificate[] chain) {
        boolean check = true;
        for (int i = 0; i < chain.length - 1; i++) {
            if (!chain[i].getIssuerDN().equals(chain[i + 1].getSubjectDN())) {
                check = false;
                logger.warning("Broken certificate chain at index " + i);
            }
        }
        return check;
    }

    /**
     * Check the certificate revokation list
     */
    public static boolean checkRevokedCertificate(Certificate[] userCert) throws Exception {
        return checkRevokedCertificate(toX509(userCert));
    }

    /**
     * Generate a MD5 fingerprint from a byte array containing a X.509
     * certificate
     * 
     * @param ba
     *            Byte array containing DER encoded X509Certificate.
     * @return Byte array containing MD5 hash of DER encoded certificate.
     */
    public static byte[] generateMD5Fingerprint(byte[] ba) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(ba);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("MD5 algorithm not supported" + nsae.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Generate new RSA keys. !! THIS METHOD RETURNS AN UNENCRYPTED PRIVATE KEY !!
     * 
     * @param algorithm
     *            Key algorithm
     * @param length
     *            Key length
     * @return Unencrypted private/public key pair
     * @exception Exception
     *                Description of the Exception
     */
    public static KeyPair generateNewKeys(String algorithm, int length) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        kpg.initialize(length);
        return kpg.generateKeyPair();
    }

    /**
     * Generate new RSA keys. !! THIS METHOD RETURNS AN UNENCRYPTED PRIVATE KEY !!
     * 
     * @param algorithm
     *            Key algorithm
     * @param provider
     *            Provider specification
     * @param length
     *            Key length
     * @return Unencrypted private/public key pair
     * @exception Exception
     *                Description of the Exception
     */
    public static KeyPair generateNewKeys(String algorithm, String provider, int length) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm, provider);
        kpg.initialize(length);
        return kpg.generateKeyPair();
    }

    /**
     * Generate a SHA1 fingerprint from a byte array containing a X.509
     * certificate
     * 
     * @param ba
     *            Byte array containing DER encoded X509Certificate.
     * @return Byte array containing SHA1 hash of DER encoded certificate.
     */
    public static byte[] generateSHA1Fingerprint(byte[] ba) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            return md.digest(ba);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("SHA1 algorithm not supported" + nsae.getLocalizedMessage());
        }
        return null;
    }

    public static X509Certificate genSelfCert(String dn, long validity, PrivateKey privKey, PublicKey pubKey, boolean isCA) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        String sigAlg = "SHA1WithRSA";
        Date firstDate = new Date();
        firstDate.setTime(firstDate.getTime() - 10 * 60 * 1000);
        Date lastDate = new Date();
        lastDate.setTime(lastDate.getTime() + (validity * (24 * 60 * 60 * 1000)));
        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        byte[] serno = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(System.currentTimeMillis());
        random.nextBytes(serno);
        certgen.setSerialNumber((new java.math.BigInteger(serno)).abs());
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(lastDate);
        certgen.setSignatureAlgorithm(sigAlg);
        certgen.setSubjectDN(CertificateUtility.stringToBcX509Name(dn));
        certgen.setIssuerDN(CertificateUtility.stringToBcX509Name(dn));
        certgen.setPublicKey(pubKey);
        BasicConstraints bc = new BasicConstraints(isCA);
        certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, bc);
        try {
            if (isCA == true) {
                SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo((DERSequence) new DERInputStream(new ByteArrayInputStream(pubKey.getEncoded())).readObject());
                SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
                SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((DERSequence) new DERInputStream(new ByteArrayInputStream(pubKey.getEncoded())).readObject());
                AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
                certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), false, ski);
                certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), false, aki);
            }
        } catch (IOException e) {
        }
        X509Certificate selfcert = certgen.generateX509Certificate(privKey);
        return selfcert;
    }

    /**
     * Gets the authorityKeyId attribute of the CertificateUtility class
     *  
     */
    public static byte[] getAuthorityKeyId(X509Certificate cert) throws IOException {
        byte[] extvalue = cert.getExtensionValue("2.5.29.35");
        if (extvalue == null) {
            return null;
        }
        DEROctetString oct = (DEROctetString) (new DERInputStream(new ByteArrayInputStream(extvalue)).readObject());
        AuthorityKeyIdentifier keyId = new AuthorityKeyIdentifier((DERSequence) new DERInputStream(new ByteArrayInputStream(oct.getOctets())).readObject());
        return keyId.getKeyIdentifier();
    }

    /**
     * Generate SHA1 fingerprint in string representation.
     */
    public static String getCertFingerprintAsString(byte[] ba) {
        try {
            X509Certificate cert = getCertfromByteArray(ba);
            byte[] res = generateSHA1Fingerprint(cert.getEncoded());
            return Hex.encode(res);
        } catch (CertificateEncodingException cee) {
            System.err.println("Error encoding X509 certificate." + cee.getLocalizedMessage());
        } catch (CertificateException cee) {
            System.err.println("Error decoding X509 certificate." + cee.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Creates X509Certificate from byte[].
     * 
     * @param cert
     *            byte array containing certificate in DER-format
     * @return X509Certificate
     * @exception CertificateException
     *                if the byte array does not contain a proper certificate.
     */
    public static X509Certificate getCertfromByteArray(byte[] cert) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));
        return x509cert;
    }

    /**
     * Reads a certificate in PEM-format from an InputStream. The stream may
     * contain other things, the first certificate in the stream is read.
     * 
     * @param certstream
     *            Description of the Parameter
     * @return X509Certificate
     * @exception IOException
     *                if the stream cannot be read.
     * @exception CertificateException
     *                if the stream does not contain a correct certificate.
     */
    public static X509Certificate getCertfromPEM(InputStream certstream) throws IOException, CertificateException {
        String beginKey = "-----BEGIN CERTIFICATE-----";
        String endKey = "-----END CERTIFICATE-----";
        BufferedReader bufRdr = new BufferedReader(new InputStreamReader(certstream));
        ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        PrintStream opstr = new PrintStream(ostr);
        String temp;
        while ((temp = bufRdr.readLine()) != null && !temp.equals(beginKey)) {
            continue;
        }
        if (temp == null) {
            throw new IOException("Error in " + certstream.toString() + ", missing " + beginKey + " boundary");
        }
        while ((temp = bufRdr.readLine()) != null && !temp.equals(endKey)) {
            opstr.print(temp);
        }
        if (temp == null) {
            throw new IOException("Error in " + certstream.toString() + ", missing " + endKey + " boundary");
        }
        opstr.close();
        byte[] certbuf = Base64.decode(ostr.toByteArray());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certbuf));
        return x509cert;
    }

    /**
     * Reads a certificate in PEM-format from a file. The file may contain other
     * things, the first certificate in the file is read.
     * 
     * @param certFile
     *            the file containing the certificate in PEM-format
     * @return X509Certificate
     * @exception IOException
     *                if the filen cannot be read.
     * @exception CertificateException
     *                if the filen does not contain a correct certificate.
     */
    public static X509Certificate getCertfromPEM(String certFile) throws IOException, CertificateException {
        InputStream inStrm = new FileInputStream(certFile);
        X509Certificate cert = getCertfromPEM(inStrm);
        return cert;
    }

    /**
     * Get the common name from a X509 Certificate
     * 
     * @param x509
     *            input certificate
     * @return common name as String
     */
    public static String getCommonName(X509Certificate x509) {
        if (x509 == null) {
            return null;
        }
        String principal = x509.getSubjectDN().toString();
        int index1 = 0;
        int index2 = 0;
        String cn = null;
        index1 = principal.indexOf("CN=");
        index2 = principal.indexOf(",", index1);
        if (index2 > 0) {
            cn = principal.substring(index1 + 3, index2);
        } else {
            cn = principal.substring(index1 + 3, principal.length() - 1);
        }
        return cn;
    }

    /**
     * Creates X509CRL from byte[].
     * 
     * @param crl
     *            byte array containing CRL in DER-format
     * @return X509CRL
     * @exception CertificateException
     *                if the byte arrayen does not contani a correct CRL.
     * @exception CRLException
     *                if the byte arrayen does not contani a correct CRL.
     */
    public static X509CRL getCRLfromByteArray(byte[] crl) throws CertificateException, CRLException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509CRL x509crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(crl));
        return x509crl;
    }

    /**
     * Generate SHA1 fingerprint of certificate in string representation.
     * 
     * @param cert
     *            X509Certificate.
     * @return String containing hex format of SHA1 fingerprint.
     */
    public static String getFingerprintAsString(X509Certificate cert) {
        try {
            byte[] res = generateSHA1Fingerprint(cert.getEncoded());
            return Hex.encode(res);
        } catch (CertificateEncodingException cee) {
            System.err.println("Error encoding X509 certificate." + cee.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Generate SHA1 fingerprint of CRL in string representation.
     * 
     * @param crl
     *            X509CRL.
     * @return String containing hex format of SHA1 fingerprint.
     */
    public static String getFingerprintAsString(X509CRL crl) {
        try {
            byte[] res = generateSHA1Fingerprint(crl.getEncoded());
            return Hex.encode(res);
        } catch (CRLException ce) {
            System.err.println("Error encoding X509 CRL." + ce.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Gets a specified part of a DN.
     * 
     * @param dn
     *            String containing DN, The DN string has the format "C=SE,
     *            O=xx, OU=yy, CN=zz".
     * @param dnpart
     *            String specifying which part of the DN to get, should be "CN"
     *            or "OU" etc.
     * @return String containing dnpart or null if dnpart is not present
     */
    public static String getPartFromDN(String dn, String dnpart) {
        String trimmeddn = dn.trim();
        String part = null;
        String o = null;
        StringTokenizer st = new StringTokenizer(trimmeddn, ",=");
        while (st.hasMoreTokens()) {
            o = st.nextToken();
            if (o.trim().equalsIgnoreCase(dnpart)) {
                part = st.nextToken();
            }
        }
        return part;
    }

    /**
     * Gets a specified part of a DN.
     * 
     * @param dn
     *            String containing DN, The DN string has the format "C=SE,
     *            O=xx, OU=yy, CN=zz".
     * @param dnpart
     *            String specifying which part of the DN to get, should be "CN"
     *            or "OU" etc.
     * @return String containing dnpart or null if dnpart is not present
     */
    public static Vector getPartsFromDN(String dn, String dnpart) {
        String trimmeddn = dn.trim();
        Vector part = new Vector();
        String o = null;
        StringTokenizer st = new StringTokenizer(trimmeddn, ",=");
        while (st.hasMoreTokens()) {
            o = st.nextToken();
            if (o.trim().equalsIgnoreCase(dnpart)) {
                part.addElement(st.nextToken());
            }
        }
        return part;
    }

    /**
     * Gets the subjectKeyId attribute of the CertificateUtility class
     * 
     * @param cert
     *            Description of the Parameter
     * @return The subjectKeyId value
     * @exception IOException
     *                Description of the Exception
     */
    public static byte[] getSubjectKeyId(X509Certificate cert) throws IOException {
        byte[] extvalue = cert.getExtensionValue("2.5.29.14");
        if (extvalue == null) {
            return null;
        }
        DEROctetString oct = (DEROctetString) (new DERInputStream(new ByteArrayInputStream(extvalue)).readObject());
        SubjectKeyIdentifier keyId = new SubjectKeyIdentifier(oct);
        return keyId.getKeyIdentifier();
    }

    /**
     * Imports a X509Certificate in PEM encoding;
     * 
     * @param filename
     *            name of the PEM file
     * @return Description of the Return Value
     * @exception Exception
     *                Description of the Exception
     */
    public static Certificate importTrustedCertifcate(File filename) throws Exception {
        DataInputStream dis = new DataInputStream(new FileInputStream(filename));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Certificate cert = null;
        while (bais.available() > 0) {
            cert = cf.generateCertificate(bais);
        }
        return cert;
    }

    /**
     * Checks if a certificate is self signed by verifying if subject and issuer
     * are the same.
     * 
     * @param cert
     *            the certificate that skall be checked.
     * @return boolean true if the certificate has the same issuer and subject,
     *         false otherwise.
     */
    public static boolean isSelfSigned(X509Certificate cert) {
        boolean ret = cert.getSubjectDN().equals(cert.getIssuerDN());
        return ret;
    }

    /**
     * Sort a X509Certifcate[] chain according to Subject and Issuer principals
     * 
     * @param in
     *            Certificate chain to convert
     * @return X509 certificate chain
     */
    public static X509Certificate[] sort(X509Certificate in[]) {
        X509Certificate x509Cert[] = null;
        if (in == null) {
            return null;
        } else if (in.length == 0) {
            x509Cert = new X509Certificate[0];
            return x509Cert;
        }
        LinkedList list = new LinkedList();
        LinkedHashSet inSet = new LinkedHashSet();
        for (int i = 0; i < in.length; i++) {
            inSet.add(in[i]);
        }
        int setSize = inSet.size();
        if (setSize != in.length) {
            logger.warning("Size " + setSize + " of certificate set different from size " + in.length + " of certificate array");
        }
        list.addLast(in[0]);
        inSet.remove(in[0]);
        while (!inSet.isEmpty()) {
            int size = inSet.size();
            Iterator iter = inSet.iterator();
            while (iter.hasNext()) {
                X509Certificate next = (X509Certificate) iter.next();
                if (((X509Certificate) list.getLast()).getIssuerDN().equals(next.getSubjectDN())) {
                    list.addLast(next);
                    inSet.remove(next);
                } else if (next.getIssuerDN().equals(((X509Certificate) list.getFirst()).getSubjectDN())) {
                    list.addFirst(next);
                    inSet.remove(next);
                }
            }
            if (inSet.size() == size) {
                break;
            }
        }
        Object obj[] = list.toArray();
        x509Cert = new X509Certificate[obj.length];
        for (int i = 0; i < obj.length; i++) {
            x509Cert[i] = (X509Certificate) obj[i];
        }
        if (x509Cert.length != setSize) {
            logger.warning("Size " + setSize + " of certificate set different from size " + x509Cert.length + " of SORTED certificate array");
            return null;
        }
        if (!checkOrder(x509Cert)) {
            logger.warning("Sorting of certificates failed");
            return null;
        }
        return x509Cert;
    }

    /**
     * Every DN-string should look the same. Creates a name string ordered and
     * looking like we want it...
     * 
     * @param dn
     *            String containing DN
     * @return String containing DN
     */
    public static String stringToBCDNString(String dn) {
        String name = stringToBcX509Name(dn).toString();
        return name;
    }

    /**
     * Creates a (Bouncycastle) X509Name object from a string with a DN.
     * <p>
     * 
     * Known OID (with order) are:
     * 
     * <pre>
     * 
     *  
     *   CN, SN, OU, O, L, ST, DC, C
     *  
     *  
     *  
     *  
     *  
     *  @param  dn  String containing DN that will be transformed into X509Name,
     *        The DN string has the format &quot;CN=zz,OU=yy,O=foo,C=SE&quot;. Unknown OIDs
     *        in the string will be silently dropped.
     *  @return     X509Name
     * 
     * 
     */
    public static X509Name stringToBcX509Name(String dn) {
        String trimmeddn = dn.trim();
        StringTokenizer st = new StringTokenizer(trimmeddn, ",=");
        Hashtable dntable = new Hashtable();
        String o = null;
        DERObjectIdentifier oid = null;
        Collection coll = new ArrayList();
        while (st.hasMoreTokens()) {
            o = st.nextToken();
            if (o.trim().equalsIgnoreCase("C")) {
                oid = X509Name.C;
                coll.add(X509Name.C);
            } else if (o.trim().equalsIgnoreCase("DC")) {
                oid = X509Name.DC;
                coll.add(X509Name.DC);
            } else if (o.trim().equalsIgnoreCase("ST")) {
                oid = X509Name.ST;
                coll.add(X509Name.ST);
            } else if (o.trim().equalsIgnoreCase("L")) {
                oid = X509Name.L;
                coll.add(X509Name.L);
            } else if (o.trim().equalsIgnoreCase("O")) {
                oid = X509Name.O;
                coll.add(X509Name.O);
            } else if (o.trim().equalsIgnoreCase("OU")) {
                oid = X509Name.OU;
                coll.add(X509Name.OU);
            } else if (o.trim().equalsIgnoreCase("SN")) {
                oid = X509Name.SN;
                coll.add(X509Name.SN);
            } else if (o.trim().equalsIgnoreCase("CN")) {
                oid = X509Name.CN;
                coll.add(X509Name.CN);
            } else if (o.trim().equalsIgnoreCase("EmailAddress")) {
                oid = X509Name.EmailAddress;
                coll.add(X509Name.EmailAddress);
            } else {
                oid = null;
            }
            if (oid != null) {
                dntable.put(oid, st.nextToken());
            }
        }
        Vector order = new Vector();
        order.add(X509Name.EmailAddress);
        order.add(X509Name.CN);
        order.add(X509Name.SN);
        order.add(X509Name.OU);
        order.add(X509Name.O);
        order.add(X509Name.L);
        order.add(X509Name.ST);
        order.add(X509Name.DC);
        order.add(X509Name.C);
        order.retainAll(coll);
        return new X509Name(order, dntable);
    }

    /**
     * Tokenizes DN.
     * 
     * @param dn
     *            String containing DN, The DN string has the format "C=SE,
     *            O=xx, OU=yy, CN=zz".
     * @return String containing dnpart or null if dnpart is not present
     */
    public static Vector tokenizeDN(String dn) {
        String trimmeddn = dn.trim();
        Vector part = new Vector();
        StringTokenizer st = new StringTokenizer(trimmeddn, ",=");
        while (st.hasMoreTokens()) {
            st.nextToken();
            part.addElement(st.nextToken());
        }
        return part;
    }

    /**
     * Private helper to convert a Certificate[] to a X509Certifcate []
     * 
     * @param in
     *            Certificate chain to convert
     * @return X509 certificate chain
     */
    public static X509Certificate[] toX509(Certificate in[]) {
        X509Certificate x509Cert[] = null;
        if (in != null) {
            x509Cert = new X509Certificate[in.length];
            for (int j = 0; j < in.length; j++) {
                if (in[j] instanceof X509Certificate) {
                    x509Cert[j] = (X509Certificate) in[j];
                }
            }
        }
        return x509Cert;
    }

    /**
     * Convert a collection of X509Certificates to a corresponding
     * X509Certificate[] chain
     * 
     * @param coll
     *            Collection of X509Certificates to convert
     * @return Chain of X509 certificates
     */
    public static X509Certificate[] toX509Chain(Collection coll) {
        Iterator iter = coll.iterator();
        X509Certificate unsortedCerts[] = new X509Certificate[coll.size()];
        int k = 0;
        while (iter.hasNext()) {
            unsortedCerts[k] = (X509Certificate) iter.next();
            k++;
        }
        X509Certificate x509Cert[] = sort(unsortedCerts);
        return x509Cert;
    }

    /**
     * Constructor for the CertifcateUtility object
     */
    private CertificateUtility() {
    }
}
