package net.esle.sinadura.core.firma.timestamp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.esle.sinadura.core.firma.exceptions.SinaduraCoreException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERConstructedSet;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.jce.provider.X509CRLParser;
import org.bouncycastle.jce.provider.X509CertParser;
import org.bouncycastle.util.StreamParsingException;
import com.lowagie.text.ExceptionConverter;

/**
 * @author zylk.net
 */
public class TsaPdfPKCS7 {

    private byte sigAttr[];

    private byte digestAttr[];

    private int version, signerversion;

    private Set digestalgos;

    private Collection certs, crls;

    private X509Certificate signCert;

    private byte[] digest;

    private MessageDigest messageDigest;

    private String digestAlgorithm, digestEncryptionAlgorithm;

    private Signature sig;

    private byte RSAdata[];

    private boolean verified;

    private boolean verifyResult;

    private byte externalDigest[];

    private byte externalRSAdata[];

    private static final String ID_PKCS7_DATA = "1.2.840.113549.1.7.1";

    private static final String ID_PKCS7_SIGNED_DATA = "1.2.840.113549.1.7.2";

    private static final String ID_MD5 = "1.2.840.113549.2.5";

    private static final String ID_MD2 = "1.2.840.113549.2.2";

    private static final String ID_SHA1 = "1.3.14.3.2.26";

    private static final String ID_RSA = "1.2.840.113549.1.1.1";

    private static final String ID_DSA = "1.2.840.10040.4.1";

    private static final String ID_CONTENT_TYPE = "1.2.840.113549.1.9.3";

    private static final String ID_MESSAGE_DIGEST = "1.2.840.113549.1.9.4";

    private static final String ID_SIGNING_TIME = "1.2.840.113549.1.9.5";

    private static final String ID_MD2RSA = "1.2.840.113549.1.1.2";

    private static final String ID_MD5RSA = "1.2.840.113549.1.1.4";

    private static final String ID_SHA1RSA = "1.2.840.113549.1.1.5";

    private static final String ID_ADBE_REVOCATION = "1.2.840.113583.1.1.8";

    private String reason;

    private String location;

    private Calendar signDate;

    private String signName;

    /**
	 * @param contentsKey
	 * @param certsKey
	 * @param provider
	 * @throws SecurityException
	 * @throws InvalidKeyException
	 * @throws CertificateException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws StreamParsingException
	 */
    public TsaPdfPKCS7(byte[] contentsKey, byte[] certsKey, String provider) throws SecurityException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, IOException {
        X509CertParser cr = new X509CertParser();
        cr.engineInit(new ByteArrayInputStream(certsKey));
        try {
            this.certs = cr.engineReadAll();
        } catch (StreamParsingException e) {
            e.printStackTrace();
        }
        this.signCert = (X509Certificate) this.certs.iterator().next();
        this.crls = new ArrayList();
        ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(contentsKey));
        this.digest = ((DEROctetString) in.readObject()).getOctets();
        if (provider == null) this.sig = Signature.getInstance("SHA1withRSA"); else this.sig = Signature.getInstance("SHA1withRSA", provider);
        this.sig.initVerify(this.signCert.getPublicKey());
    }

    /**
	 * @param contentsKey
	 * @param provider
	 * @throws SecurityException
	 * @throws CRLException
	 * @throws InvalidKeyException
	 * @throws CertificateException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 */
    public TsaPdfPKCS7(byte[] contentsKey, String provider) throws SecurityException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        ASN1InputStream din = new ASN1InputStream(new ByteArrayInputStream(contentsKey));
        DERObject pkcs;
        try {
            pkcs = din.readObject();
        } catch (IOException e) {
            throw new SecurityException("can't decode PKCS7SignedData object");
        }
        if (!(pkcs instanceof ASN1Sequence)) {
            throw new SecurityException("Not a valid PKCS#7 object - not a sequence");
        }
        ASN1Sequence signedData = (ASN1Sequence) pkcs;
        DERObjectIdentifier objId = (DERObjectIdentifier) signedData.getObjectAt(0);
        if (!objId.getId().equals(ID_PKCS7_SIGNED_DATA)) throw new SecurityException("Not a valid PKCS#7 object - not signed data");
        ASN1Sequence content = (ASN1Sequence) ((DERTaggedObject) signedData.getObjectAt(1)).getObject();
        this.version = ((DERInteger) content.getObjectAt(0)).getValue().intValue();
        this.digestalgos = new HashSet();
        Enumeration e = ((ASN1Set) content.getObjectAt(1)).getObjects();
        while (e.hasMoreElements()) {
            ASN1Sequence s = (ASN1Sequence) e.nextElement();
            DERObjectIdentifier o = (DERObjectIdentifier) s.getObjectAt(0);
            this.digestalgos.add(o.getId());
        }
        X509CertParser cr = new X509CertParser();
        cr.engineInit(new ByteArrayInputStream(contentsKey));
        try {
            this.certs = cr.engineReadAll();
        } catch (StreamParsingException e2) {
            e2.printStackTrace();
        }
        X509CRLParser cl = new X509CRLParser();
        cl.engineInit(new ByteArrayInputStream(contentsKey));
        try {
            this.crls = cl.engineReadAll();
        } catch (StreamParsingException e1) {
            e1.printStackTrace();
        }
        ASN1Sequence rsaData = (ASN1Sequence) content.getObjectAt(2);
        if (rsaData.size() > 1) {
            DEROctetString rsaDataContent = (DEROctetString) ((DERTaggedObject) rsaData.getObjectAt(1)).getObject();
            this.RSAdata = rsaDataContent.getOctets();
        }
        int next = 3;
        while (content.getObjectAt(next) instanceof DERTaggedObject) ++next;
        ASN1Set signerInfos = (ASN1Set) content.getObjectAt(next);
        if (signerInfos.size() != 1) throw new SecurityException("This PKCS#7 object has multiple SignerInfos - only one is supported at this time");
        ASN1Sequence signerInfo = (ASN1Sequence) signerInfos.getObjectAt(0);
        this.signerversion = ((DERInteger) signerInfo.getObjectAt(0)).getValue().intValue();
        ASN1Sequence issuerAndSerialNumber = (ASN1Sequence) signerInfo.getObjectAt(1);
        BigInteger serialNumber = ((DERInteger) issuerAndSerialNumber.getObjectAt(1)).getValue();
        for (Iterator i = this.certs.iterator(); i.hasNext(); ) {
            X509Certificate cert = (X509Certificate) i.next();
            if (serialNumber.equals(cert.getSerialNumber())) {
                this.signCert = cert;
                break;
            }
        }
        if (this.signCert == null) {
            throw new SecurityException("Can't find signing certificate with serial " + serialNumber.toString(16));
        }
        this.digestAlgorithm = ((DERObjectIdentifier) ((ASN1Sequence) signerInfo.getObjectAt(2)).getObjectAt(0)).getId();
        next = 3;
        if (signerInfo.getObjectAt(next) instanceof ASN1TaggedObject) {
            ASN1TaggedObject tagsig = (ASN1TaggedObject) signerInfo.getObjectAt(next);
            ASN1Sequence sseq = (ASN1Sequence) tagsig.getObject();
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ASN1OutputStream dout = new ASN1OutputStream(bOut);
            try {
                ASN1EncodableVector attribute = new ASN1EncodableVector();
                for (int k = 0; k < sseq.size(); ++k) {
                    attribute.add(sseq.getObjectAt(k));
                }
                dout.writeObject(new DERSet(attribute));
                dout.close();
            } catch (IOException ioe) {
            }
            this.sigAttr = bOut.toByteArray();
            for (int k = 0; k < sseq.size(); ++k) {
                ASN1Sequence seq2 = (ASN1Sequence) sseq.getObjectAt(k);
                if (((DERObjectIdentifier) seq2.getObjectAt(0)).getId().equals(ID_MESSAGE_DIGEST)) {
                    ASN1Set set = (ASN1Set) seq2.getObjectAt(1);
                    this.digestAttr = ((DEROctetString) set.getObjectAt(0)).getOctets();
                    break;
                }
            }
            if (this.digestAttr == null) throw new SecurityException("Authenticated attribute is missing the digest.");
            ++next;
        }
        this.digestEncryptionAlgorithm = ((DERObjectIdentifier) ((ASN1Sequence) signerInfo.getObjectAt(next++)).getObjectAt(0)).getId();
        this.digest = ((DEROctetString) signerInfo.getObjectAt(next)).getOctets();
        if (this.RSAdata != null || this.digestAttr != null) {
            if (provider == null || provider.startsWith("SunPKCS11")) this.messageDigest = MessageDigest.getInstance(getHashAlgorithm()); else this.messageDigest = MessageDigest.getInstance(getHashAlgorithm(), provider);
        }
        if (provider == null) this.sig = Signature.getInstance(getDigestAlgorithm()); else this.sig = Signature.getInstance(getDigestAlgorithm(), provider);
        this.sig.initVerify(this.signCert.getPublicKey());
    }

    /**
	 * @param privKey
	 * @param certChain
	 * @param crlList
	 * @param hashAlgorithm
	 * @param provider
	 * @param hasRSAdata
	 * @throws SecurityException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 */
    public TsaPdfPKCS7(PrivateKey privKey, Certificate[] certChain, CRL[] crlList, String hashAlgorithm, String provider, boolean hasRSAdata) throws SecurityException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        if (hashAlgorithm.equals("MD5")) {
            this.digestAlgorithm = ID_MD5;
        } else if (hashAlgorithm.equals("MD2")) {
            this.digestAlgorithm = ID_MD2;
        } else if (hashAlgorithm.equals("SHA")) {
            this.digestAlgorithm = ID_SHA1;
        } else if (hashAlgorithm.equals("SHA1")) {
            this.digestAlgorithm = ID_SHA1;
        } else {
            throw new NoSuchAlgorithmException("Unknown Hash Algorithm " + hashAlgorithm);
        }
        this.version = this.signerversion = 1;
        this.certs = new ArrayList();
        this.crls = new ArrayList();
        this.digestalgos = new HashSet();
        this.digestalgos.add(this.digestAlgorithm);
        this.signCert = (X509Certificate) certChain[0];
        for (int i = 0; i < certChain.length; i++) {
            this.certs.add(certChain[i]);
        }
        if (crlList != null) {
            for (int i = 0; i < crlList.length; i++) {
                this.crls.add(crlList[i]);
            }
        }
        if (privKey != null) {
            this.digestEncryptionAlgorithm = privKey.getAlgorithm();
            if (this.digestEncryptionAlgorithm.equals("RSA")) {
                this.digestEncryptionAlgorithm = ID_RSA;
            } else if (this.digestEncryptionAlgorithm.equals("DSA")) {
                this.digestEncryptionAlgorithm = ID_DSA;
            } else {
                throw new NoSuchAlgorithmException("Unknown Key Algorithm " + this.digestEncryptionAlgorithm);
            }
        }
        if (hasRSAdata) {
            this.RSAdata = new byte[0];
            if (provider == null || provider.startsWith("SunPKCS11")) this.messageDigest = MessageDigest.getInstance(getHashAlgorithm()); else this.messageDigest = MessageDigest.getInstance(getHashAlgorithm(), provider);
        }
        if (privKey != null) {
            if (provider == null) {
                this.sig = Signature.getInstance(getDigestAlgorithm());
            } else {
                this.sig = Signature.getInstance(getDigestAlgorithm(), provider);
            }
            this.sig.initSign(privKey);
        }
    }

    /**
	 * @param buf
	 * @param off
	 * @param len
	 * @throws SignatureException
	 */
    public void update(byte[] buf, int off, int len) throws SignatureException {
        if (this.RSAdata != null || this.digestAttr != null) this.messageDigest.update(buf, off, len); else this.sig.update(buf, off, len);
    }

    /**
	 * @throws SignatureException
	 * @return
	 */
    public boolean verify() throws SignatureException {
        if (this.verified) return this.verifyResult;
        if (this.sigAttr != null) {
            this.sig.update(this.sigAttr);
            if (this.RSAdata != null) {
                byte msd[] = this.messageDigest.digest();
                this.messageDigest.update(msd);
            }
            this.verifyResult = (Arrays.equals(this.messageDigest.digest(), this.digestAttr) && this.sig.verify(this.digest));
        } else {
            if (this.RSAdata != null) this.sig.update(this.messageDigest.digest());
            this.verifyResult = this.sig.verify(this.digest);
        }
        this.verified = true;
        return this.verifyResult;
    }

    /**
	 * @return
	 */
    public Certificate[] getCertificates() {
        return (X509Certificate[]) this.certs.toArray(new X509Certificate[this.certs.size()]);
    }

    /**
	 * @return
	 */
    public Collection getCRLs() {
        return this.crls;
    }

    /**
	 * @return
	 */
    public X509Certificate getSigningCertificate() {
        return this.signCert;
    }

    /**
	 * @return
	 */
    public int getVersion() {
        return this.version;
    }

    /**
	 * @return
	 */
    public int getSigningInfoVersion() {
        return this.signerversion;
    }

    /**
	 * @return
	 */
    public String getDigestAlgorithm() {
        String dea = this.digestEncryptionAlgorithm;
        if (this.digestEncryptionAlgorithm.equals(ID_RSA) || this.digestEncryptionAlgorithm.equals(ID_MD5RSA) || this.digestEncryptionAlgorithm.equals(ID_MD2RSA) || this.digestEncryptionAlgorithm.equals(ID_SHA1RSA)) {
            dea = "RSA";
        } else if (this.digestEncryptionAlgorithm.equals(ID_DSA)) {
            dea = "DSA";
        }
        return getHashAlgorithm() + "with" + dea;
    }

    /**
	 * @return
	 */
    public String getHashAlgorithm() {
        String da = this.digestAlgorithm;
        if (this.digestAlgorithm.equals(ID_MD5) || this.digestAlgorithm.equals(ID_MD5RSA)) {
            da = "MD5";
        } else if (this.digestAlgorithm.equals(ID_MD2) || this.digestAlgorithm.equals(ID_MD2RSA)) {
            da = "MD2";
        } else if (this.digestAlgorithm.equals(ID_SHA1) || this.digestAlgorithm.equals(ID_SHA1RSA)) {
            da = "SHA1";
        }
        return da;
    }

    /**
	 * @return
	 */
    public static KeyStore loadCacertsKeyStore() {
        return loadCacertsKeyStore(null);
    }

    /**
	 * @param provider
	 * @return
	 */
    public static KeyStore loadCacertsKeyStore(String provider) {
        File file = new File(System.getProperty("java.home"), "lib");
        file = new File(file, "security");
        file = new File(file, "cacerts");
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            KeyStore k;
            if (provider == null) k = KeyStore.getInstance("JKS"); else k = KeyStore.getInstance("JKS", provider);
            k.load(fin, null);
            return k;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    /**
	 * @param cert
	 * @param crls
	 * @param calendar
	 * @return
	 */
    public static String verifyCertificate(X509Certificate cert, Collection crls, Calendar calendar) {
        if (calendar == null) calendar = new GregorianCalendar();
        if (cert.hasUnsupportedCriticalExtension()) return "Has unsupported critical extension";
        try {
            cert.checkValidity(calendar.getTime());
        } catch (Exception e) {
            return e.getMessage();
        }
        if (crls != null) {
            for (Iterator it = crls.iterator(); it.hasNext(); ) {
                if (((CRL) it.next()).isRevoked(cert)) return "Certificate revoked";
            }
        }
        return null;
    }

    /**
	 * @param certs
	 * @param keystore
	 * @param crls
	 * @param calendar
	 * @return
	 */
    public static Object[] verifyCertificates(Certificate certs[], KeyStore keystore, Collection crls, Calendar calendar) {
        if (calendar == null) calendar = new GregorianCalendar();
        for (int k = 0; k < certs.length; ++k) {
            X509Certificate cert = (X509Certificate) certs[k];
            String err = verifyCertificate(cert, crls, calendar);
            if (err != null) return new Object[] { cert, err };
            try {
                for (Enumeration aliases = keystore.aliases(); aliases.hasMoreElements(); ) {
                    try {
                        String alias = (String) aliases.nextElement();
                        if (!keystore.isCertificateEntry(alias)) continue;
                        X509Certificate certStoreX509 = (X509Certificate) keystore.getCertificate(alias);
                        if (verifyCertificate(certStoreX509, crls, calendar) != null) continue;
                        try {
                            cert.verify(certStoreX509.getPublicKey());
                            return null;
                        } catch (Exception e) {
                            continue;
                        }
                    } catch (Exception ex) {
                    }
                }
            } catch (Exception e) {
            }
            int j;
            for (j = 0; j < certs.length; ++j) {
                if (j == k) continue;
                X509Certificate certNext = (X509Certificate) certs[j];
                try {
                    cert.verify(certNext.getPublicKey());
                    break;
                } catch (Exception e) {
                }
            }
            if (j == certs.length) return new Object[] { cert, "Cannot be verified against the KeyStore or the certificate chain" };
        }
        return new Object[] { null, "Invalid state. Possible circular certificate chain" };
    }

    /**
	 * @param enc
	 * @return a DERObject
	 */
    private static DERObject getIssuer(byte[] enc) {
        try {
            ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
            ASN1Sequence seq = (ASN1Sequence) in.readObject();
            return (DERObject) seq.getObjectAt(seq.getObjectAt(0) instanceof DERTaggedObject ? 3 : 2);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * @param enc
	 * @return
	 */
    private static DERObject getSubject(byte[] enc) {
        try {
            ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
            ASN1Sequence seq = (ASN1Sequence) in.readObject();
            return (DERObject) seq.getObjectAt(seq.getObjectAt(0) instanceof DERTaggedObject ? 5 : 4);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * @param cert
	 * @return
	 */
    public static X509Name getIssuerFields(X509Certificate cert) {
        try {
            return new X509Name((ASN1Sequence) getIssuer(cert.getTBSCertificate()));
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * @param cert
	 * @return
	 */
    public static X509Name getSubjectFields(X509Certificate cert) {
        try {
            return new X509Name((ASN1Sequence) getSubject(cert.getTBSCertificate()));
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * @return
	 */
    public byte[] getEncodedPKCS1() {
        try {
            if (this.externalDigest != null) this.digest = this.externalDigest; else this.digest = this.sig.sign();
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject(new DEROctetString(this.digest));
            dout.close();
            return bOut.toByteArray();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * @param digest
	 * @param RSAdata
	 * @param digestEncryptionAlgorithm
	 */
    public void setExternalDigest(byte digest[], byte RSAdata[], String digestEncryptionAlgorithm) {
        this.externalDigest = digest;
        this.externalRSAdata = RSAdata;
        if (digestEncryptionAlgorithm != null) {
            if (digestEncryptionAlgorithm.equals("RSA")) {
                this.digestEncryptionAlgorithm = ID_RSA;
            } else if (digestEncryptionAlgorithm.equals("DSA")) {
                this.digestEncryptionAlgorithm = ID_DSA;
            } else throw new ExceptionConverter(new NoSuchAlgorithmException("Unknown Key Algorithm " + digestEncryptionAlgorithm));
        }
    }

    /**
	 * @return
	 * @throws SinaduraCoreException
	 */
    public byte[] getEncodedPKCS7() throws SinaduraCoreException {
        return getEncodedPKCS7(null, null, null);
    }

    /**
	 * @param secondDigest
	 * @param signingTime
	 * @return
	 * @throws SinaduraCoreException
	 */
    public byte[] getEncodedPKCS7(byte secondDigest[], Calendar signingTime) throws SinaduraCoreException {
        return getEncodedPKCS7(secondDigest, signingTime, null);
    }

    /**
	 * @param secondDigest
	 * @param signingTime
	 * @param tsaClient
	 * @return
	 * @throws SinaduraCoreException
	 */
    public byte[] getEncodedPKCS7(byte secondDigest[], Calendar signingTime, TSAClient tsaClient) throws SinaduraCoreException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try {
            if (this.externalDigest != null) {
                this.digest = this.externalDigest;
                if (this.RSAdata != null) this.RSAdata = this.externalRSAdata;
            } else if (this.externalRSAdata != null && this.RSAdata != null) {
                this.RSAdata = this.externalRSAdata;
                this.sig.update(this.RSAdata);
                this.digest = this.sig.sign();
            } else {
                if (this.RSAdata != null) {
                    this.RSAdata = this.messageDigest.digest();
                    this.sig.update(this.RSAdata);
                }
                this.digest = this.sig.sign();
            }
            DERConstructedSet digestAlgorithms = new DERConstructedSet();
            for (Iterator it = this.digestalgos.iterator(); it.hasNext(); ) {
                ASN1EncodableVector algos = new ASN1EncodableVector();
                algos.add(new DERObjectIdentifier((String) it.next()));
                algos.add(new DERNull());
                digestAlgorithms.addObject(new DERSequence(algos));
            }
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new DERObjectIdentifier(ID_PKCS7_DATA));
            if (this.RSAdata != null) v.add(new DERTaggedObject(0, new DEROctetString(this.RSAdata)));
            DERSequence contentinfo = new DERSequence(v);
            v = new ASN1EncodableVector();
            for (Iterator i = this.certs.iterator(); i.hasNext(); ) {
                ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(((X509Certificate) i.next()).getEncoded()));
                v.add(tempstream.readObject());
            }
            DERSet dercertificates = new DERSet(v);
            ASN1EncodableVector signerinfo = new ASN1EncodableVector();
            signerinfo.add(new DERInteger(this.signerversion));
            v = new ASN1EncodableVector();
            v.add(getIssuer(this.signCert.getTBSCertificate()));
            v.add(new DERInteger(this.signCert.getSerialNumber()));
            signerinfo.add(new DERSequence(v));
            v = new ASN1EncodableVector();
            v.add(new DERObjectIdentifier(this.digestAlgorithm));
            v.add(new DERNull());
            signerinfo.add(new DERSequence(v));
            if (secondDigest != null && signingTime != null) {
                ASN1EncodableVector attribute = new ASN1EncodableVector();
                v = new ASN1EncodableVector();
                v.add(new DERObjectIdentifier(ID_CONTENT_TYPE));
                v.add(new DERSet(new DERObjectIdentifier(ID_PKCS7_DATA)));
                attribute.add(new DERSequence(v));
                v = new ASN1EncodableVector();
                v.add(new DERObjectIdentifier(ID_SIGNING_TIME));
                v.add(new DERSet(new DERUTCTime(signingTime.getTime())));
                attribute.add(new DERSequence(v));
                v = new ASN1EncodableVector();
                v.add(new DERObjectIdentifier(ID_MESSAGE_DIGEST));
                v.add(new DERSet(new DEROctetString(secondDigest)));
                attribute.add(new DERSequence(v));
                if (!this.crls.isEmpty()) {
                    v = new ASN1EncodableVector();
                    v.add(new DERObjectIdentifier(ID_ADBE_REVOCATION));
                    ASN1EncodableVector v2 = new ASN1EncodableVector();
                    for (Iterator i = this.crls.iterator(); i.hasNext(); ) {
                        ASN1InputStream t = new ASN1InputStream(new ByteArrayInputStream((((X509CRL) i.next()).getEncoded())));
                        v2.add(t.readObject());
                    }
                    v.add(new DERSet(new DERSequence(new DERTaggedObject(true, 0, new DERSequence(v2)))));
                    attribute.add(new DERSequence(v));
                }
                signerinfo.add(new DERTaggedObject(false, 0, new DERSet(attribute)));
            }
            v = new ASN1EncodableVector();
            v.add(new DERObjectIdentifier(this.digestEncryptionAlgorithm));
            v.add(new DERNull());
            signerinfo.add(new DERSequence(v));
            signerinfo.add(new DEROctetString(this.digest));
            if (tsaClient != null) {
                byte[] tsImprint = MessageDigest.getInstance("SHA-1").digest(this.digest);
                byte[] tsToken = tsaClient.getTimeStampToken(this, tsImprint);
                if (tsToken != null) {
                    ASN1EncodableVector unauthAttributes = buildUnauthenticatedAttributes(tsToken);
                    if (unauthAttributes != null) {
                        signerinfo.add(new DERTaggedObject(false, 1, new DERSet(unauthAttributes)));
                    }
                }
            }
            ASN1EncodableVector body = new ASN1EncodableVector();
            body.add(new DERInteger(this.version));
            body.add(digestAlgorithms);
            body.add(contentinfo);
            body.add(new DERTaggedObject(false, 0, dercertificates));
            if (!this.crls.isEmpty()) {
                v = new ASN1EncodableVector();
                for (Iterator i = this.crls.iterator(); i.hasNext(); ) {
                    ASN1InputStream t = new ASN1InputStream(new ByteArrayInputStream((((X509CRL) i.next()).getEncoded())));
                    v.add(t.readObject());
                }
                DERSet dercrls = new DERSet(v);
                body.add(new DERTaggedObject(false, 1, dercrls));
            }
            body.add(new DERSet(new DERSequence(signerinfo)));
            ASN1EncodableVector whole = new ASN1EncodableVector();
            whole.add(new DERObjectIdentifier(ID_PKCS7_SIGNED_DATA));
            whole.add(new DERTaggedObject(0, new DERSequence(body)));
            ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject(new DERSequence(whole));
            dout.close();
        } catch (CertificateEncodingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (SignatureException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (CRLException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (IOException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (ProviderException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        }
        return bOut.toByteArray();
    }

    /**
	 * @param secondDigest
	 * @param signingTime
	 * @return
	 */
    public byte[] getAuthenticatedAttributeBytes(byte secondDigest[], Calendar signingTime) {
        try {
            ASN1EncodableVector attribute = new ASN1EncodableVector();
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new DERObjectIdentifier(ID_CONTENT_TYPE));
            v.add(new DERSet(new DERObjectIdentifier(ID_PKCS7_DATA)));
            attribute.add(new DERSequence(v));
            v = new ASN1EncodableVector();
            v.add(new DERObjectIdentifier(ID_SIGNING_TIME));
            v.add(new DERSet(new DERUTCTime(signingTime.getTime())));
            attribute.add(new DERSequence(v));
            v = new ASN1EncodableVector();
            v.add(new DERObjectIdentifier(ID_MESSAGE_DIGEST));
            v.add(new DERSet(new DEROctetString(secondDigest)));
            attribute.add(new DERSequence(v));
            if (!this.crls.isEmpty()) {
                v = new ASN1EncodableVector();
                v.add(new DERObjectIdentifier(ID_ADBE_REVOCATION));
                ASN1EncodableVector v2 = new ASN1EncodableVector();
                for (Iterator i = this.crls.iterator(); i.hasNext(); ) {
                    ASN1InputStream t = new ASN1InputStream(new ByteArrayInputStream((((X509CRL) i.next()).getEncoded())));
                    v2.add(t.readObject());
                }
                v.add(new DERSet(new DERSequence(new DERTaggedObject(true, 0, new DERSequence(v2)))));
                attribute.add(new DERSequence(v));
            }
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject(new DERSet(attribute));
            dout.close();
            return bOut.toByteArray();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * @return
	 */
    public String getReason() {
        return this.reason;
    }

    /**
	 * @param reason
	 */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
	 * @return
	 */
    public String getLocation() {
        return this.location;
    }

    /**
	 * @param location
	 */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
	 * @return
	 */
    public Calendar getSignDate() {
        return this.signDate;
    }

    /**
	 * @param signDate
	 */
    public void setSignDate(Calendar signDate) {
        this.signDate = signDate;
    }

    /**
	 * @return
	 */
    public String getSignName() {
        return this.signName;
    }

    /**
	 * @param signName
	 */
    public void setSignName(String signName) {
        this.signName = signName;
    }

    /**
	 * @author zylk.net
	 */
    public static class X509Name {

        /**
		 * country code - StringType(SIZE(2))
		 */
        public static final DERObjectIdentifier C = new DERObjectIdentifier("2.5.4.6");

        /**
		 * organization - StringType(SIZE(1..64))
		 */
        public static final DERObjectIdentifier O = new DERObjectIdentifier("2.5.4.10");

        /**
		 * organizational unit name - StringType(SIZE(1..64))
		 */
        public static final DERObjectIdentifier OU = new DERObjectIdentifier("2.5.4.11");

        /**
		 * Title
		 */
        public static final DERObjectIdentifier T = new DERObjectIdentifier("2.5.4.12");

        /**
		 * common name - StringType(SIZE(1..64))
		 */
        public static final DERObjectIdentifier CN = new DERObjectIdentifier("2.5.4.3");

        /**
		 * device serial number name - StringType(SIZE(1..64))
		 */
        public static final DERObjectIdentifier SN = new DERObjectIdentifier("2.5.4.5");

        /**
		 * locality name - StringType(SIZE(1..64))
		 */
        public static final DERObjectIdentifier L = new DERObjectIdentifier("2.5.4.7");

        /**
		 * state, or province name - StringType(SIZE(1..64))
		 */
        public static final DERObjectIdentifier ST = new DERObjectIdentifier("2.5.4.8");

        /** Naming attribute of type X520name */
        public static final DERObjectIdentifier SURNAME = new DERObjectIdentifier("2.5.4.4");

        /** Naming attribute of type X520name */
        public static final DERObjectIdentifier GIVENNAME = new DERObjectIdentifier("2.5.4.42");

        /** Naming attribute of type X520name */
        public static final DERObjectIdentifier INITIALS = new DERObjectIdentifier("2.5.4.43");

        /** Naming attribute of type X520name */
        public static final DERObjectIdentifier GENERATION = new DERObjectIdentifier("2.5.4.44");

        /** Naming attribute of type X520name */
        public static final DERObjectIdentifier UNIQUE_IDENTIFIER = new DERObjectIdentifier("2.5.4.45");

        /**
		 * Email address (RSA PKCS#9 extension) - IA5String.
		 * <p>
		 * Note: if you're trying to be ultra orthodox, don't use this! It shouldn't be in here.
		 */
        public static final DERObjectIdentifier EmailAddress = new DERObjectIdentifier("1.2.840.113549.1.9.1");

        /**
		 * email address in Verisign certificates
		 */
        public static final DERObjectIdentifier E = EmailAddress;

        /** object identifier */
        public static final DERObjectIdentifier DC = new DERObjectIdentifier("0.9.2342.19200300.100.1.25");

        /** LDAP User id. */
        public static final DERObjectIdentifier UID = new DERObjectIdentifier("0.9.2342.19200300.100.1.1");

        /** A HashMap with default symbols */
        public static HashMap DefaultSymbols = new HashMap();

        static {
            DefaultSymbols.put(C, "C");
            DefaultSymbols.put(O, "O");
            DefaultSymbols.put(T, "T");
            DefaultSymbols.put(OU, "OU");
            DefaultSymbols.put(CN, "CN");
            DefaultSymbols.put(L, "L");
            DefaultSymbols.put(ST, "ST");
            DefaultSymbols.put(SN, "SN");
            DefaultSymbols.put(EmailAddress, "E");
            DefaultSymbols.put(DC, "DC");
            DefaultSymbols.put(UID, "UID");
            DefaultSymbols.put(SURNAME, "SURNAME");
            DefaultSymbols.put(GIVENNAME, "GIVENNAME");
            DefaultSymbols.put(INITIALS, "INITIALS");
            DefaultSymbols.put(GENERATION, "GENERATION");
        }

        /** A HashMap with values */
        public HashMap values = new HashMap();

        /**
		 * @param seq
		 */
        public X509Name(ASN1Sequence seq) {
            Enumeration e = seq.getObjects();
            while (e.hasMoreElements()) {
                ASN1Set set = (ASN1Set) e.nextElement();
                for (int i = 0; i < set.size(); i++) {
                    ASN1Sequence s = (ASN1Sequence) set.getObjectAt(i);
                    String id = (String) DefaultSymbols.get(s.getObjectAt(0));
                    if (id == null) continue;
                    ArrayList vs = (ArrayList) this.values.get(id);
                    if (vs == null) {
                        vs = new ArrayList();
                        this.values.put(id, vs);
                    }
                    vs.add(((DERString) s.getObjectAt(1)).getString());
                }
            }
        }

        /**
		 * @param dirName
		 */
        public X509Name(String dirName) {
            X509NameTokenizer nTok = new X509NameTokenizer(dirName);
            while (nTok.hasMoreTokens()) {
                String token = nTok.nextToken();
                int index = token.indexOf('=');
                if (index == -1) {
                    throw new IllegalArgumentException("badly formated directory string");
                }
                String id = token.substring(0, index).toUpperCase();
                String value = token.substring(index + 1);
                ArrayList vs = (ArrayList) this.values.get(id);
                if (vs == null) {
                    vs = new ArrayList();
                    this.values.put(id, vs);
                }
                vs.add(value);
            }
        }

        /**
		 * @param name
		 * @return
		 */
        public String getField(String name) {
            ArrayList vs = (ArrayList) this.values.get(name);
            return vs == null ? null : (String) vs.get(0);
        }

        /**
		 * @param name
		 * @return
		 */
        public ArrayList getFieldArray(String name) {
            ArrayList vs = (ArrayList) this.values.get(name);
            return vs == null ? null : vs;
        }

        /**
		 * @return
		 */
        public HashMap getFields() {
            return this.values;
        }

        /**
		 * @see java.lang.Object#toString()
		 */
        @Override
        public String toString() {
            return this.values.toString();
        }
    }

    /**
	 * @author zylk.net
	 */
    public static class X509NameTokenizer {

        private String oid;

        private int index;

        private StringBuffer buf = new StringBuffer();

        /**
		 * @param oid
		 */
        public X509NameTokenizer(String oid) {
            this.oid = oid;
            this.index = -1;
        }

        /**
		 * @return
		 */
        public boolean hasMoreTokens() {
            return (this.index != this.oid.length());
        }

        /**
		 * @return
		 */
        public String nextToken() {
            if (this.index == this.oid.length()) {
                return null;
            }
            int end = this.index + 1;
            boolean quoted = false;
            boolean escaped = false;
            this.buf.setLength(0);
            while (end != this.oid.length()) {
                char c = this.oid.charAt(end);
                if (c == '"') {
                    if (!escaped) {
                        quoted = !quoted;
                    } else {
                        this.buf.append(c);
                    }
                    escaped = false;
                } else {
                    if (escaped || quoted) {
                        this.buf.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == ',') {
                        break;
                    } else {
                        this.buf.append(c);
                    }
                }
                end++;
            }
            this.index = end;
            return this.buf.toString().trim();
        }
    }

    /**
	 * @param timeStampToken
	 * @return ASN1EncodableVector
	 * @throws IOException
	 */
    private ASN1EncodableVector buildUnauthenticatedAttributes(byte[] timeStampToken) throws IOException {
        if (timeStampToken == null) return null;
        String ID_TIME_STAMP_TOKEN = "1.2.840.113549.1.9.16.2.14";
        ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(timeStampToken));
        ASN1EncodableVector unauthAttributes = new ASN1EncodableVector();
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERObjectIdentifier(ID_TIME_STAMP_TOKEN));
        ASN1Sequence seq = (ASN1Sequence) tempstream.readObject();
        v.add(new DERSet(seq));
        unauthAttributes.add(new DERSequence(v));
        return unauthAttributes;
    }
}
