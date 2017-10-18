package es.caib.signatura.provider.impl.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.tsp.TSTInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataParser;
import org.bouncycastle.cms.CMSTypedStream;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import es.caib.signatura.api.ParsedCertificate;
import es.caib.signatura.api.SignatureProviderException;
import es.caib.signatura.api.SignatureTimestampException;
import es.caib.signatura.api.SignatureVerifyException;
import es.caib.signatura.impl.MIMEInputStream;
import es.caib.signatura.impl.SigDebug;
import es.caib.signatura.impl.SignaturaProperties;
import es.caib.signatura.impl.SignatureProviderInterface;
import es.caib.signatura.impl.ValidadorProxy;

/**
 * Implementation signature of an S/MIME document.
 * 
 * Diferences between other types of signatures is that it cannot be serialized, it cannot be got the PKCS#7 signature 
 * which is composed from and it cannot be added a timestamp after it has been generated.
 *
 * @author u91940
 *
 */
public class SMIMESignatureImpl implements SignatureProviderInterface {

    private X509Certificate certificateChain[];

    private String contentType;

    private byte[] signatureBytes;

    private Date timeStamp;

    public SMIMESignatureImpl() {
        super();
    }

    public SMIMESignatureImpl(byte[] pkcs7, String contentType, boolean isBinary) throws Exception {
        signatureBytes = pkcs7;
        extractCertificate();
    }

    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getSignatureBytes() {
        return null;
    }

    /**
	 * Obtiene el nombre de la entidad certificadora usada en la firma
	 * 
	 * @return nombre de la entidad certificadora
	 */
    public String getCertCaName() {
        if (certificateChain == null || certificateChain.length == 0) {
            throw new Error("No se encuentra cadena de certificación.");
        }
        return certificateChain[certificateChain.length - 1].getSubjectX500Principal().getName();
    }

    /**
	 * Obtiene el nombre del certificado usado en la firma
	 * 
	 * @return nombre del certificado (CommonName)
	 */
    public String getCertSubjectCommonName() {
        ParsedCertificate Parsed = this.getParsedCertificate();
        return (Parsed.getName());
    }

    public String getCertSubjectAlternativeNames() {
        StringBuffer altNameSB = new StringBuffer("");
        String altNameString = null;
        try {
            Collection generalNames = certificateChain[0].getSubjectAlternativeNames();
            Iterator itr = generalNames.iterator();
            while (itr.hasNext()) {
                List list = (List) itr.next();
                int tagNo = ((Integer) list.get(0)).intValue();
                switch(tagNo) {
                    case 0:
                        altNameSB.append("," + "otherName=");
                        break;
                    case 1:
                        altNameSB.append("," + "rfc822Name=");
                        break;
                    case 2:
                        altNameSB.append("," + "dNSName=");
                        break;
                    case 3:
                        altNameSB.append("," + "x400Address=");
                        break;
                    case 4:
                        altNameSB.append("," + "directoryName=");
                        break;
                    case 5:
                        altNameSB.append("," + "ediPartyName=");
                        break;
                    case 6:
                        altNameSB.append("," + "uniformResourceIdentifier=");
                        break;
                    case 7:
                        altNameSB.append("," + "iPAddress=");
                        break;
                    case 8:
                        altNameSB.append("," + "registeredID=");
                        break;
                }
                altNameSB.append(list.get(1).toString());
                if (altNameSB.length() > 0) {
                    altNameString = altNameSB.substring(1, altNameSB.length());
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return altNameString;
    }

    private void extractCertificate() throws Exception {
        ByteArrayInputStream voidContentData = new ByteArrayInputStream(new byte[0]);
        CMSTypedStream typedIn = new CMSTypedStream(voidContentData);
        CMSSignedDataParser parser = new CMSSignedDataParser(typedIn, signatureBytes);
        parser.getSignedContent().drain();
        CertStore certs = null;
        try {
            if (Security.getProvider("BC") == null) {
                Provider p = (Provider) this.getClass().getClassLoader().loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
                Security.addProvider(p);
            }
            certs = parser.getCertificatesAndCRLs("Collection", "BC");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SignerInformationStore signersStore = parser.getSignerInfos();
        Collection signers = signersStore.getSigners();
        Iterator it = signers.iterator();
        if (it.hasNext()) {
            X509Certificate userCertificate = null;
            SignerInformation signer = null;
            signer = (SignerInformation) it.next();
            Collection certCollection = certs.getCertificates(signer.getSID());
            Iterator certIt = certCollection.iterator();
            if (certIt.hasNext()) {
                userCertificate = (X509Certificate) certIt.next();
            }
            certCollection = certs.getCertificates(null);
            certIt = certCollection.iterator();
            LinkedList allCertificates = new LinkedList();
            while (certIt.hasNext()) {
                allCertificates.addFirst(certIt.next());
            }
            boolean finishExtraction = allCertificates.size() == 0;
            X509Certificate currentCertificate = userCertificate;
            LinkedList certificateChainList = new LinkedList();
            certificateChainList.addFirst(userCertificate);
            while (!finishExtraction) {
                ListIterator iterator = allCertificates.listIterator();
                boolean nextCertificate = false;
                X509Certificate certificateFromIterator = null;
                while (iterator.hasNext() && !nextCertificate) {
                    certificateFromIterator = (X509Certificate) iterator.next();
                    nextCertificate = certificateFromIterator.getSubjectDN().toString().compareTo(currentCertificate.getIssuerDN().toString()) == 0;
                }
                if (nextCertificate) {
                    certificateChainList.addLast(certificateFromIterator);
                    currentCertificate = certificateFromIterator;
                }
                finishExtraction = !nextCertificate || currentCertificate.getIssuerDN().toString().compareTo(currentCertificate.getSubjectDN().toString()) == 0;
            }
            certificateChain = (X509Certificate[]) certificateChainList.toArray(new X509Certificate[certificateChainList.size()]);
            if (signer.getUnsignedAttributes() != null) {
                Attribute att = signer.getUnsignedAttributes().get(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14"));
                if (att != null) {
                    ASN1Set values = att.getAttrValues();
                    if (values.size() > 0) {
                        DEREncodable token = values.getObjectAt(0);
                        CMSSignedDataParser parserTS = new CMSSignedDataParser(token.getDERObject().getDEREncoded());
                        CMSTypedStream content = parserTS.getSignedContent();
                        ASN1InputStream tsInput = new ASN1InputStream(content.getContentStream());
                        DERObject obj = tsInput.readObject();
                        TSTInfo tstInfo = new TSTInfo((ASN1Sequence) obj);
                        timeStamp = tstInfo.getGenTime().getDate();
                    }
                }
            }
        } else {
            throw new SignatureVerifyException(new Exception("No signer"));
        }
    }

    private boolean isInvertedCertificateChain(X509Certificate[] certificateChain) {
        return isRootCA(certificateChain[0]);
    }

    private void invertCertificateChain(X509Certificate[] certificateChain) {
        for (int i = 0; i < certificateChain.length / 2; i++) {
            X509Certificate certificate = certificateChain[i];
            certificateChain[i] = certificateChain[certificateChain.length - 1 - i];
            certificateChain[certificateChain.length - 1 - i] = certificate;
        }
    }

    private boolean isRootCA(X509Certificate cert) {
        boolean isRootCa = false;
        try {
            SignaturaProperties signaturaProperties = new SignaturaProperties();
            String[] roots = signaturaProperties.getRootCAs();
            String commonName = getCACommonName(cert);
            for (int i = 0; i < roots.length && !isRootCa; i++) {
                isRootCa = roots[i].compareTo(commonName) == 0;
            }
        } catch (Exception e) {
            return true;
        }
        return isRootCa;
    }

    private static String getCACommonName(X509Certificate cert) throws IOException, CertificateEncodingException {
        String ou = "";
        byte b[] = cert.getEncoded();
        ASN1InputStream asn1is = new ASN1InputStream(b);
        DERObject obj = asn1is.readObject();
        X509CertificateStructure certificate = new X509CertificateStructure((ASN1Sequence) obj);
        asn1is.close();
        X509Name name = certificate.getIssuer();
        java.util.Vector v = name.getOIDs();
        java.util.Vector value = name.getValues();
        for (int i = 0; i < v.size(); i++) {
            if (v.get(i).equals(X509Name.CN)) {
                if (SigDebug.isActive()) SigDebug.write("getCACommonName(): " + value.get(i).toString());
                return value.get(i).toString();
            }
            if (v.get(i).equals(X509Name.OU)) ou = value.get(i).toString();
        }
        if (SigDebug.isActive()) SigDebug.write("getCACommonName(): " + ou);
        return ou;
    }

    protected X509Certificate[] getTimeStampCertificates(Collection certCollection) throws Exception {
        Iterator certIt = certCollection.iterator();
        Vector v = new Vector();
        while (certIt.hasNext()) {
            v.add(certIt.next());
        }
        return (X509Certificate[]) v.toArray(new X509Certificate[v.size()]);
    }

    public Date getDate() throws SignatureTimestampException {
        return timeStamp;
    }

    public boolean verify() throws SignatureVerifyException {
        try {
            certificateChain[0].checkValidity();
        } catch (CertificateExpiredException cee) {
            return false;
        } catch (CertificateNotYetValidException cve) {
            return false;
        }
        boolean isVerified = true;
        try {
            X509Certificate certificateChain[] = getCertificateChain();
            X509Certificate validationCertificateChain[] = new X509Certificate[certificateChain.length];
            for (int i = 0; i < validationCertificateChain.length; i++) {
                validationCertificateChain[i] = certificateChain[i];
            }
            ValidadorProxy validador = new ValidadorProxy();
            if (validador.isValidadorInstalado()) {
                ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                try {
                    SignaturaProperties properties = new SignaturaProperties();
                    if (!properties.needsRecognizedCertificate(contentType)) isVerified = validador.validarAutenticacion(validationCertificateChain); else isVerified = validador.validarFirma(validationCertificateChain);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                } finally {
                    Thread.currentThread().setContextClassLoader(prevCL);
                }
            }
            return isVerified;
        } catch (SignatureVerifyException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
    }

    public X509Certificate getCert() {
        return certificateChain[0];
    }

    public boolean verifyAPosterioriTimestamp(InputStream contentStream) throws SignatureProviderException, IOException, SignatureVerifyException {
        InputStream mimeContentStream = new MIMEInputStream(contentStream, contentType);
        return verifyAPosterioriTimestampRaw(mimeContentStream);
    }

    protected boolean verifyAPosterioriTimestampRaw(InputStream contentStream) throws SignatureProviderException, IOException, SignatureVerifyException {
        boolean verified = true;
        try {
            CMSTypedStream typedIn = new CMSTypedStream(contentStream);
            SignaturaProperties signaturaProperties = new SignaturaProperties();
            boolean needsTimestamp = signaturaProperties.needsTimeStamp(contentType);
            CMSSignedDataParser parser = new CMSSignedDataParser(typedIn, signatureBytes);
            CMSTypedStream da = parser.getSignedContent();
            parser.getSignedContent().drain();
            CertStore mainCerts = parser.getCertificatesAndCRLs("Collection", "BC");
            SignerInformationStore signersStore = parser.getSignerInfos();
            Collection signers = signersStore.getSigners();
            Iterator it = signers.iterator();
            byte[] digest;
            if (it.hasNext()) {
                SignerInformation signer = (SignerInformation) it.next();
                verified = verified && signer.verify(certificateChain[0], "BC");
                if (!verified) {
                    verified = signer.verify(certificateChain[certificateChain.length - 1], "BC");
                }
                digest = signer.getContentDigest();
                boolean hasTimestamp = false;
                boolean timeStampVerified = false;
                if (signer.getUnsignedAttributes() != null) {
                    Attribute att = signer.getUnsignedAttributes().get(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14"));
                    if (att != null) {
                        hasTimestamp = true;
                        ASN1Set values = att.getAttrValues();
                        if (values.size() > 0) {
                            DEREncodable token = values.getObjectAt(0);
                            try {
                                CMSSignedData cmsSD = new CMSSignedData(token.getDERObject().getDEREncoded());
                                TimeStampToken tst = new TimeStampToken(cmsSD);
                                try {
                                    timeStampVerified = verifyTimestamp(tst, signer, digest);
                                } catch (Exception e) {
                                    throw new SignatureVerifyException(e);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                if (!hasTimestamp) {
                    return false;
                }
                verified = verified && ((timeStampVerified && hasTimestamp && needsTimestamp) || (!needsTimestamp));
            } else {
                throw new SignatureVerifyException(new Exception("No signer"));
            }
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
        return verified;
    }

    public boolean verify(InputStream contentStream) throws SignatureProviderException, IOException, SignatureVerifyException {
        InputStream mimeContentStream = new MIMEInputStream(contentStream, contentType);
        return verifyRaw(mimeContentStream);
    }

    protected boolean verifyRaw(InputStream contentStream) throws SignatureProviderException, IOException, SignatureVerifyException {
        boolean verified = true;
        try {
            CMSTypedStream typedIn = new CMSTypedStream(contentStream);
            SignaturaProperties signaturaProperties = new SignaturaProperties();
            boolean needsTimestamp = signaturaProperties.needsTimeStamp(contentType);
            CMSSignedDataParser parser = new CMSSignedDataParser(typedIn, signatureBytes);
            CMSTypedStream da = parser.getSignedContent();
            parser.getSignedContent().drain();
            CertStore mainCerts = parser.getCertificatesAndCRLs("Collection", "BC");
            SignerInformationStore signersStore = parser.getSignerInfos();
            Collection signers = signersStore.getSigners();
            Iterator it = signers.iterator();
            byte[] digest;
            if (it.hasNext()) {
                SignerInformation signer = (SignerInformation) it.next();
                for (int i = 0; i < certificateChain.length; i++) {
                    X509Certificate certificate = certificateChain[i];
                }
                verified = verified && signer.verify(getCert(), "BC");
                if (!verified) {
                    verified = signer.verify(certificateChain[certificateChain.length - 1], "BC");
                }
                digest = signer.getContentDigest();
                boolean hasTimestamp = false;
                boolean timeStampVerified = false;
                if (signer.getUnsignedAttributes() != null) {
                    Attribute att = signer.getUnsignedAttributes().get(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14"));
                    if (att != null) {
                        hasTimestamp = true;
                        ASN1Set values = att.getAttrValues();
                        if (values.size() > 0) {
                            DEREncodable token = values.getObjectAt(0);
                            try {
                                CMSSignedData cmsSD = new CMSSignedData(token.getDERObject().getDEREncoded());
                                TimeStampToken tst = new TimeStampToken(cmsSD);
                                try {
                                    timeStampVerified = verifyTimestamp(tst, signer, digest);
                                } catch (Exception e) {
                                    throw new SignatureVerifyException(e);
                                }
                            } catch (Exception e) {
                                throw new SignatureVerifyException(e);
                            }
                        }
                    }
                }
                verified = verified && ((needsTimestamp && hasTimestamp && timeStampVerified) || (!needsTimestamp));
            } else {
                throw new SignatureVerifyException(new Exception("No signer"));
            }
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
        return verified;
    }

    protected boolean verifyTimestamp(TimeStampToken tst, SignerInformation si, byte[] documentDigest) throws SignatureProviderException, IOException, SignatureVerifyException {
        boolean timeStampVerified = false;
        try {
            byte signatureDigest[] = SHA1Util.digest(si.getSignature());
            if (tst != null) {
                CertStore certs = tst.getCertificatesAndCRLs("Collection", "BC");
                if (certs != null) {
                    Collection certificates = certs.getCertificates(tst.getSID());
                    if (certificates != null && certificates.size() > 0) {
                        X509Certificate timeStampCertificate = getTimeStampCertificates(certificates)[0];
                        try {
                            tst.validate(timeStampCertificate, "BC");
                            timeStampVerified = true;
                            TimeStampTokenInfo tsTokenInfo = tst.getTimeStampInfo();
                            byte[] hashTimeStamp = tsTokenInfo.getMessageImprintDigest();
                            timeStampVerified = timeStampVerified && hashTimeStamp.length == signatureDigest.length;
                            for (int i = 0; i < signatureDigest.length && timeStampVerified; i++) {
                                timeStampVerified = timeStampVerified && (hashTimeStamp[i] == signatureDigest[i]);
                            }
                        } catch (Exception e) {
                            throw new SignatureVerifyException(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
        return timeStampVerified;
    }

    public ParsedCertificate getParsedCertificate() {
        try {
            return new ParsedCertificateImpl(certificateChain, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setContentType(String contentType) throws Exception {
        this.contentType = contentType;
    }

    /**
	 * Crea un nuevo objeto a partir de los atributos de la clase. Es el
	 * constructor que debe usar cada implementación de la interfaz
	 * <code>Signature</code> para crear una firma. Se extrae el certificado
	 * de la firma y se guarda en la propiedad <code>transient</code>
	 * certificate para usarla en los métodos que dan información concreta del
	 * certificado
	 * 
	 * @param signatureBytes
	 *            array de bytes con la firma digital generada por la api del
	 *            proveedor de firma electrónica
	 * 
	 */
    public void setSignedData(byte[] pkcs7) throws Exception {
        signatureBytes = pkcs7;
        extractCertificate();
    }

    ;

    public byte[] getPkcs7() {
        return null;
    }
}
