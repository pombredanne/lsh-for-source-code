package jp.go.aist.sot.client.security;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import static jp.go.aist.sot.common.SOTSignOnToolConstants.CSR_HEADER;
import static jp.go.aist.sot.common.SOTSignOnToolConstants.CSR_FOOTER;
import jp.go.aist.sot.utils.Base64Util;
import jp.go.aist.sot.utils.SOTProxyCertInfo;
import jp.go.aist.sot.utils.SOTProxyPolicy;

public class ProxyCertGenerator {

    private static final boolean CRITICAL = true;

    private static final long SKEW = 300000;

    private X509V3CertificateGenerator generator;

    public ProxyCertGenerator() {
    }

    public X509Certificate generate(String csrPem, long certTerm, X509Certificate issuerCert, PrivateKey issuerKey) throws GeneralSecurityException {
        this.generator = new X509V3CertificateGenerator();
        PKCS10CertificationRequest pkcs10Req = getPKCS10CertificationRequest(csrPem);
        String newCn = calcCommonName(pkcs10Req.getPublicKey());
        BigInteger serialNum = new BigInteger(newCn);
        TBSCertificateStructure issuerTbsCert = getTBSCertificate(issuerCert);
        addProxyCertInfoExtension();
        X509Extensions extensions = issuerTbsCert.getExtensions();
        if (extensions != null) {
            X509Extension ext = null;
            ext = extensions.getExtension(X509Extensions.KeyUsage);
            addKeyUsage(ext);
            ext = extensions.getExtension(X509Extensions.ExtendedKeyUsage);
            addExtendedKeyUsage(ext);
        }
        X509Name issuerDn = issuerTbsCert.getSubject();
        X509Name subjectDn = buildSubjectDn(issuerDn, newCn);
        generator.setSubjectDN(subjectDn);
        generator.setIssuerDN(issuerDn);
        generator.setSerialNumber(serialNum);
        generator.setPublicKey(pkcs10Req.getPublicKey());
        generator.setSignatureAlgorithm(issuerCert.getSigAlgName());
        generator.setNotBefore(new Date(System.currentTimeMillis() - SKEW));
        generator.setNotAfter(new Date(System.currentTimeMillis() + certTerm));
        return generator.generateX509Certificate(issuerKey);
    }

    private static String calcCommonName(PublicKey pubKey) {
        byte[] data = pubKey.getEncoded();
        byte[] digest = null;
        try {
            MessageDigest msgDgst = MessageDigest.getInstance("SHA-1");
            msgDgst.update(data);
            digest = msgDgst.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        int subHash = 0;
        subHash = digest[0] + (digest[1] + (digest[2] + (digest[3] >>> 1) * 256) * 256) * 256;
        return Integer.toString(Math.abs(subHash));
    }

    private void addProxyCertInfoExtension() {
        SOTProxyPolicy policy = new SOTProxyPolicy(SOTProxyPolicy.INHERITALL);
        SOTProxyCertInfo proxyCertInfo = new SOTProxyCertInfo(policy);
        generator.addExtension("1.3.6.1.5.5.7.1.14", CRITICAL, proxyCertInfo);
    }

    private static byte clearBits(byte source, int mask) {
        return ((source & mask) != 0) ? (byte) (source ^ mask) : source;
    }

    private void addExtendedKeyUsage(X509Extension ext) {
        if (ext == null) return;
        generator.addExtension(X509Extensions.ExtendedKeyUsage, ext.isCritical(), duplicate(getExtentionObject(ext)));
    }

    private void addKeyUsage(X509Extension ext) {
        if (ext == null) return;
        DERBitString bits = (DERBitString) getExtentionObject(ext);
        byte data = bits.getBytes()[0];
        data = clearBits(data, KeyUsage.nonRepudiation);
        data = clearBits(data, KeyUsage.keyCertSign);
        byte[] newData = new byte[1];
        newData[0] = data;
        bits = new DERBitString(newData, bits.getPadBits());
        generator.addExtension(X509Extensions.KeyUsage, ext.isCritical(), bits);
    }

    private static X509Name buildSubjectDn(X509Name issuerDn, String cnName) {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(X509Name.CN);
        v.add(new DERPrintableString(cnName));
        DERSet rdn = new DERSet(new DERSequence(v));
        ASN1Sequence seq = (ASN1Sequence) duplicate(issuerDn.getDERObject());
        ASN1EncodableVector newSeq = new ASN1EncodableVector();
        int size = seq.size();
        for (int i = 0; i < size; i++) {
            newSeq.add(seq.getObjectAt(i));
        }
        newSeq.add(rdn);
        return new X509Name(new DERSequence(newSeq));
    }

    private static byte[] loadCSR(String req) {
        if ((req == null) || (req.equals(""))) {
            throw new IllegalArgumentException("Empty PEM CSR.");
        }
        int headerIndex = req.indexOf(CSR_HEADER);
        int footerIndex = req.indexOf(CSR_FOOTER);
        if (headerIndex < 0) {
            throw new IllegalArgumentException("Missing CSR Header.");
        }
        if (footerIndex < 0) {
            throw new IllegalArgumentException("Missing CSR Footer.");
        }
        String body = req.substring(headerIndex + CSR_HEADER.length(), footerIndex);
        if (body.equals("")) {
            throw new IllegalArgumentException("No data between header and footer.");
        }
        return Base64Util.decode(body.replaceAll("\n", ""));
    }

    private static PKCS10CertificationRequest getPKCS10CertificationRequest(String req) {
        return new PKCS10CertificationRequest(loadCSR(req));
    }

    private static DERObject duplicate(DERObject obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DEROutputStream derOut = new DEROutputStream(out);
            derOut.writeObject(obj);
            return toDer(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static TBSCertificateStructure getTBSCertificate(X509Certificate cert) throws GeneralSecurityException {
        DERObject derObject = toDer(cert.getTBSCertificate());
        return TBSCertificateStructure.getInstance(derObject);
    }

    private static DERObject toDer(byte[] data) {
        try {
            DERInputStream derInput = new DERInputStream(new ByteArrayInputStream(data));
            return derInput.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DERObject getExtentionObject(X509Extension ext) {
        return toDer(ext.getValue().getOctets());
    }
}
