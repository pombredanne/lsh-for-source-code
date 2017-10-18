package com.pkcs11.support.sign;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.ess.ESSCertID;
import org.bouncycastle.asn1.ess.SigningCertificate;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author iozen
 */
public class CMSPackage {

    /** Field description */
    private X509Certificate certificate;

    /** Field description */
    private CMSProcessable content;

    /** Field description */
    private DigestInfo contentDigestInfo;

    /** Field description */
    private byte[] dataToBeSignedDigest;

    /** Field description */
    private Date signDate;

    /** Field description */
    private ASN1Set signedAttr;

    /** Field description */
    private boolean withContent;

    /**
     * Constructs ...
     *
     *
     * @param certificate
     * @param signDate
     * @param content
     */
    public CMSPackage(X509Certificate certificate, Date signDate, byte[] content) {
        this.content = new CMSProcessableByteArray(content);
        this.withContent = true;
        this.certificate = certificate;
        this.signDate = signDate;
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Constructs ...
     *
     *
     * @param certificate
     * @param signDate
     * @param content
     * @param withContent
     */
    public CMSPackage(X509Certificate certificate, Date signDate, byte[] content, boolean withContent) {
        this.content = new CMSProcessableByteArray(content);
        this.withContent = withContent;
        this.certificate = certificate;
        this.signDate = signDate;
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Creates multiple signed CMSSignedData from signedData and existing CMSSignedData object
     *
     *
     * @param signedData
     * @param oldSignedData
     *
     * @return CMSSignedData
     *
     * @throws CMSException
     * @throws CRLException
     * @throws CertStoreException
     * @throws CertificateEncodingException
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public CMSSignedData getCMSPackage(byte[] signedData, CMSSignedData oldSignedData) throws NoSuchAlgorithmException, NoSuchProviderException, CMSException, InvalidAlgorithmParameterException, CertStoreException, CertificateEncodingException, IOException, CRLException {
        List signersCertList = new ArrayList();
        signersCertList.add(certificate);
        CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(signersCertList), "BC");
        CertStore existingCertStore = oldSignedData.getCertificatesAndCRLs("Collection", "BC");
        List certs = new ArrayList();
        List crls = new ArrayList();
        addCertificatesAndCRLs(existingCertStore, certs, crls);
        addCertificatesAndCRLs(certStore, certs, crls);
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        Iterator<SignerInformation> signerInfIterator = oldSignedData.getSignerInfos().getSigners().iterator();
        while (signerInfIterator.hasNext()) {
            SignerInformation signer = signerInfIterator.next();
            digestAlgs.add(new AlgorithmIdentifier(new DERObjectIdentifier(signer.getDigestAlgOID()), new DERNull()));
            signerInfos.add(signer.toSignerInfo());
        }
        digestAlgs.add(new AlgorithmIdentifier(new DERObjectIdentifier(CMSSignedGenerator.DIGEST_SHA1), new DERNull()));
        signerInfos.add(getSignerInfo(signedData));
        DERObjectIdentifier contentTypeOID = new DERObjectIdentifier(CMSSignedDataGenerator.DATA);
        ASN1Set certificates = null;
        if (!certs.isEmpty()) {
            ASN1EncodableVector v = new ASN1EncodableVector();
            Iterator it = certs.iterator();
            while (it.hasNext()) {
                v.add((DEREncodable) it.next());
            }
            certificates = new DERSet(v);
        }
        ASN1Set certrevlist = null;
        if (!crls.isEmpty()) {
            ASN1EncodableVector v = new ASN1EncodableVector();
            Iterator it = crls.iterator();
            while (it.hasNext()) {
                v.add((DEREncodable) it.next());
            }
            certrevlist = new DERSet(v);
        }
        ContentInfo encInfo;
        if (withContent) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            try {
                content.write(bOut);
            } catch (IOException e) {
                throw new CMSException("encapsulation error.", e);
            }
            ASN1OctetString octs = new BERConstructedOctetString(bOut.toByteArray());
            encInfo = new ContentInfo(contentTypeOID, octs);
        } else {
            encInfo = new ContentInfo(contentTypeOID, null);
        }
        SignedData sd = new SignedData(new DERSet(digestAlgs), encInfo, certificates, certrevlist, new DERSet(signerInfos));
        ContentInfo contentInfo = new ContentInfo(PKCSObjectIdentifiers.signedData, sd);
        return new CMSSignedData(content, contentInfo);
    }

    /**
     * Creates CMSSignedData from signedData
     *
     *
     * @param signedData
     *
     * @return CMSSignedData
     *
     * @throws CMSException
     * @throws CRLException
     * @throws CertStoreException
     * @throws CertificateEncodingException
     * @throws IOException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public CMSSignedData getCMSPackage(byte[] signedData) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, CertificateEncodingException, IOException, CertStoreException, CRLException, CMSException {
        List signersCertList = new ArrayList();
        signersCertList.add(certificate);
        CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(signersCertList), "BC");
        List certs = new ArrayList();
        List crls = new ArrayList();
        addCertificatesAndCRLs(certStore, certs, crls);
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        DERObjectIdentifier contentTypeOID = new DERObjectIdentifier(CMSSignedDataGenerator.DATA);
        digestAlgs.add(new AlgorithmIdentifier(new DERObjectIdentifier(CMSSignedGenerator.DIGEST_SHA1), new DERNull()));
        signerInfos.add(getSignerInfo(signedData));
        ASN1Set certificates = null;
        if (!certs.isEmpty()) {
            ASN1EncodableVector v = new ASN1EncodableVector();
            Iterator it = certs.iterator();
            while (it.hasNext()) {
                v.add((DEREncodable) it.next());
            }
            certificates = new DERSet(v);
        }
        ASN1Set certrevlist = null;
        if (!crls.isEmpty()) {
            ASN1EncodableVector v = new ASN1EncodableVector();
            Iterator it = crls.iterator();
            while (it.hasNext()) {
                v.add((DEREncodable) it.next());
            }
            certrevlist = new DERSet(v);
        }
        ContentInfo encInfo;
        if (withContent) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            try {
                content.write(bOut);
            } catch (IOException e) {
                throw new CMSException("encapsulation error.", e);
            }
            ASN1OctetString octs = new BERConstructedOctetString(bOut.toByteArray());
            encInfo = new ContentInfo(contentTypeOID, octs);
        } else {
            encInfo = new ContentInfo(contentTypeOID, null);
        }
        SignedData sd = new SignedData(new DERSet(digestAlgs), encInfo, certificates, certrevlist, new DERSet(signerInfos));
        ContentInfo contentInfo = new ContentInfo(PKCSObjectIdentifiers.signedData, sd);
        return new CMSSignedData(content, contentInfo);
    }

    /**
     * Creates the SignerInfo object from signedData
     *
     *
     * @param signedData
     *
     * @return SignerInfo
     *
     * @throws CertificateEncodingException
     * @throws IOException
     */
    private SignerInfo getSignerInfo(byte[] signedData) throws CertificateEncodingException, IOException {
        AlgorithmIdentifier digAlgId = new AlgorithmIdentifier(new DERObjectIdentifier(CMSSignedGenerator.DIGEST_SHA1), new DERNull());
        AlgorithmIdentifier encAlgId = new AlgorithmIdentifier(new DERObjectIdentifier(CMSSignedGenerator.ENCRYPTION_RSA), new DERNull());
        ASN1OctetString encDigest = new DEROctetString(signedData);
        ByteArrayInputStream bIn = new ByteArrayInputStream(certificate.getTBSCertificate());
        ASN1InputStream aIn = new ASN1InputStream(bIn);
        TBSCertificateStructure tbs = TBSCertificateStructure.getInstance(aIn.readObject());
        IssuerAndSerialNumber encSid;
        SubjectKeyIdentifier ski = getSubjectKeyIdentifier();
        SignerIdentifier sid;
        if (ski == null) {
            encSid = new IssuerAndSerialNumber(tbs.getIssuer(), certificate.getSerialNumber());
            sid = new SignerIdentifier(encSid);
        } else {
            sid = new SignerIdentifier(new DEROctetString(ski));
        }
        ASN1Set unsignedAttr = null;
        return new SignerInfo(sid, digAlgId, signedAttr, encAlgId, encDigest, unsignedAttr);
    }

    /**
     * creates the data which will be signed
     *
     *
     * @return byte[]
     *
     * @throws CMSException
     * @throws CertificateEncodingException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public byte[] getSigndata() throws NoSuchAlgorithmException, NoSuchProviderException, IOException, CMSException, CertificateEncodingException {
        String digestAlgOID = CMSSignedGenerator.DIGEST_SHA1;
        String encAlgOID = CMSSignedGenerator.ENCRYPTION_RSA;
        MessageDigest dig = MessageDigest.getInstance(digestAlgOID, "BC");
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        content.write(bo);
        dig.update(bo.toByteArray());
        byte[] contentDigest = dig.digest();
        DEREncodableVector v = new ASN1EncodableVector();
        v.add(new Attribute(PKCSObjectIdentifiers.pkcs_9_at_contentType, new DERSet(PKCSObjectIdentifiers.data)));
        v.add(new Attribute(PKCSObjectIdentifiers.pkcs_9_at_signingTime, new DERSet(new DERUTCTime(signDate))));
        v.add(new Attribute(PKCSObjectIdentifiers.pkcs_9_at_messageDigest, new DERSet(new DEROctetString(contentDigest))));
        if (certificate != null) {
            dig.reset();
            ESSCertID essCertid = new ESSCertID(dig.digest(certificate.getEncoded()));
            v.add(new Attribute(PKCSObjectIdentifiers.id_aa_signingCertificate, new DERSet(new SigningCertificate(essCertid))));
        }
        signedAttr = new DERSet(v);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(signedAttr);
        byte[] dataToBeSigned = bOut.toByteArray();
        dig.reset();
        dig.update(dataToBeSigned);
        dataToBeSignedDigest = dig.digest();
        AlgorithmIdentifier algId = new AlgorithmIdentifier(new DERObjectIdentifier(digestAlgOID), new DERNull());
        contentDigestInfo = new DigestInfo(algId, dataToBeSignedDigest);
        return dataToBeSigned;
    }

    /**
     * get Subject Key Identifier from signing certificate
     *
     *
     * @return SubjectKeyIdentifier
     */
    private SubjectKeyIdentifier getSubjectKeyIdentifier() {
        byte[] derValue = certificate.getExtensionValue(X509Extensions.SubjectKeyIdentifier.getId());
        if ((derValue == null) || (derValue.length == 0)) {
            return null;
        }
        SubjectKeyIdentifier ski = null;
        try {
            ski = new SubjectKeyIdentifierStructure(derValue);
        } catch (IOException e) {
            return null;
        }
        return ski;
    }

    /**
     * Add the certificates and CRLs in certStore to lists
     *
     *
     * @param certStore
     * @param certs
     * @param crls
     *
     * @throws CRLException
     * @throws CertStoreException
     * @throws CertificateEncodingException
     * @throws IOException
     */
    private void addCertificatesAndCRLs(CertStore certStore, List certs, List crls) throws CertStoreException, CertificateEncodingException, IOException, CRLException {
        Iterator it = certStore.getCertificates(null).iterator();
        while (it.hasNext()) {
            X509Certificate c = (X509Certificate) it.next();
            ByteArrayInputStream bIn = new ByteArrayInputStream(c.getEncoded());
            ASN1InputStream aIn = new ASN1InputStream(bIn);
            certs.add(new X509CertificateStructure((ASN1Sequence) aIn.readObject()));
        }
        it = certStore.getCRLs(null).iterator();
        while (it.hasNext()) {
            X509CRL c = (X509CRL) it.next();
            ByteArrayInputStream bIn = new ByteArrayInputStream(c.getEncoded());
            ASN1InputStream aIn = new ASN1InputStream(bIn);
            crls.add(new CertificateList((ASN1Sequence) aIn.readObject()));
        }
    }
}
