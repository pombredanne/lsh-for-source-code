package br.net.woodstock.rockframework.security.sign.impl;

import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.tsp.TimeStampToken;
import br.net.woodstock.rockframework.office.pdf.PDFException;
import br.net.woodstock.rockframework.security.Alias;
import br.net.woodstock.rockframework.security.digest.DigestType;
import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.security.digest.impl.BasicDigester;
import br.net.woodstock.rockframework.security.sign.DocumentSigner;
import br.net.woodstock.rockframework.security.sign.PKCS7SignatureParameters;
import br.net.woodstock.rockframework.security.sign.Signatory;
import br.net.woodstock.rockframework.security.sign.Signature;
import br.net.woodstock.rockframework.security.sign.SignatureType;
import br.net.woodstock.rockframework.security.sign.SignerException;
import br.net.woodstock.rockframework.security.store.CertificateEntry;
import br.net.woodstock.rockframework.security.store.KeyStoreType;
import br.net.woodstock.rockframework.security.store.PrivateKeyEntry;
import br.net.woodstock.rockframework.security.store.Store;
import br.net.woodstock.rockframework.security.store.StoreEntryType;
import br.net.woodstock.rockframework.security.store.impl.JCAStore;
import br.net.woodstock.rockframework.security.timestamp.TimeStamp;
import br.net.woodstock.rockframework.security.timestamp.impl.BouncyCastleTimeStampHelper;
import br.net.woodstock.rockframework.security.util.BouncyCastleProviderHelper;
import br.net.woodstock.rockframework.util.Assert;
import br.net.woodstock.rockframework.util.Require;
import br.net.woodstock.rockframework.utils.CollectionUtils;
import br.net.woodstock.rockframework.utils.ConditionUtils;
import br.net.woodstock.rockframework.utils.IOUtils;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.OcspClient;
import com.itextpdf.text.pdf.OcspClientBouncyCastle;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.TSAClient;

public class PDFSigner implements DocumentSigner {

    private static final char PDF_SIGNATURE_VERSION = '\0';

    private PKCS7SignatureParameters parameters;

    public PDFSigner(final PKCS7SignatureParameters parameters) {
        super();
        this.parameters = parameters;
    }

    @Override
    public byte[] sign(final byte[] data) {
        Require.notNull(this.parameters, "parameters");
        Assert.notNull(data, "data");
        try {
            byte[] currentData = data;
            for (Alias alias : this.parameters.getAliases()) {
                currentData = this.singleSign(data, alias);
            }
            return currentData;
        } catch (Exception e) {
            throw new SignerException(e);
        }
    }

    private byte[] singleSign(final byte[] data, final Alias alias) {
        Assert.notEmpty(data, "data");
        try {
            Store store = this.parameters.getStore();
            PrivateKeyEntry privateEntry = (PrivateKeyEntry) store.get(alias, StoreEntryType.PRIVATE_KEY);
            if (privateEntry == null) {
                throw new SignerException("Private key '" + alias.getName() + " not found in store");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrivateKey privateKey = privateEntry.getValue();
            Certificate[] chain = privateEntry.getChain();
            X509Certificate certificate = (X509Certificate) chain[0];
            DigestType digestType = this.getDigestTypeFromSignature(certificate.getSigAlgName());
            Calendar calendar = Calendar.getInstance();
            PdfReader reader = new PdfReader(data);
            PdfStamper stamper = PdfStamper.createSignature(reader, outputStream, PDFSigner.PDF_SIGNATURE_VERSION, null, true);
            PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
            appearance.setCrypto(privateKey, chain, null, PdfSignatureAppearance.SELF_SIGNED);
            appearance.setContact(this.parameters.getContactInfo());
            appearance.setLocation(this.parameters.getLocation());
            appearance.setReason(this.parameters.getReason());
            appearance.setSignDate(calendar);
            PdfSignature signature = new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
            signature.setReason(appearance.getReason());
            signature.setLocation(appearance.getLocation());
            signature.setContact(appearance.getContact());
            signature.setDate(new PdfDate(appearance.getSignDate()));
            if (ConditionUtils.isNotEmpty(this.parameters.getName())) {
                signature.setName(this.parameters.getName());
            } else {
                signature.setName(BouncyCastleProviderHelper.getName(certificate.getSubjectX500Principal()));
            }
            appearance.setCryptoDictionary(signature);
            int contentSize = 0x2502;
            HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
            exc.put(PdfName.CONTENTS, new Integer(contentSize));
            appearance.preClose(exc);
            Digester digester = new BasicDigester(digestType);
            byte[] rangeStream = IOUtils.toByteArray(appearance.getRangeStream());
            byte[] hash = digester.digest(rangeStream);
            TSAClient tsc = null;
            if (this.parameters.getTimeStampClient() != null) {
                tsc = new DelegateITextTSAClient(this.parameters.getTimeStampClient());
            }
            byte[] oscp = null;
            if (ConditionUtils.isNotEmpty(chain)) {
                String oscpUrl = PdfPKCS7.getOCSPURL(certificate);
                X509Certificate parentCertificate = null;
                for (Certificate c : chain) {
                    if (!certificate.equals(c)) {
                        parentCertificate = (X509Certificate) c;
                        break;
                    }
                }
                if (parentCertificate != null) {
                    if ((oscpUrl != null) && (oscpUrl.trim().length() > 0)) {
                        OcspClient ocspClient = new OcspClientBouncyCastle(certificate, parentCertificate, oscpUrl);
                        oscp = ocspClient.getEncoded();
                    }
                }
            }
            PdfPKCS7 pkcs7 = new PdfPKCS7(privateKey, chain, null, digestType.getAlgorithm(), null, false);
            byte[] authenticatedAttributes = pkcs7.getAuthenticatedAttributeBytes(hash, calendar, oscp);
            pkcs7.update(authenticatedAttributes, 0, authenticatedAttributes.length);
            pkcs7.setLocation(this.parameters.getLocation());
            pkcs7.setReason(this.parameters.getReason());
            if (tsc == null) {
                pkcs7.setSignDate(calendar);
            }
            byte[] encodedPkcs7 = pkcs7.getEncodedPKCS7(hash, calendar, tsc, oscp);
            byte[] output = new byte[(contentSize - 2) / 2];
            System.arraycopy(encodedPkcs7, 0, output, 0, encodedPkcs7.length);
            PdfDictionary newDictionary = new PdfDictionary();
            PdfString content = new PdfString(output);
            content.setHexWriting(true);
            newDictionary.put(PdfName.CONTENTS, content);
            appearance.close(newDictionary);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new SignerException(e);
        }
    }

    @Override
    public boolean verify(final byte[] data, final byte[] signature) {
        try {
            Signature[] signatures = this.getSignatures(signature);
            if (ConditionUtils.isNotEmpty(signatures)) {
                for (Signature s : signatures) {
                    if (!s.getValid().booleanValue()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            throw new SignerException(e);
        }
    }

    @Override
    public Signature[] getSignatures(final byte[] data) {
        try {
            PdfReader reader = new PdfReader(data);
            AcroFields fields = reader.getAcroFields();
            Collection<Signature> signatures = new ArrayList<Signature>();
            if (fields != null) {
                List<String> list = fields.getSignatureNames();
                if ((list != null) && (!list.isEmpty())) {
                    for (String str : list) {
                        PdfPKCS7 pk = fields.verifySignature(str);
                        PdfString string = fields.getSignatureDictionary(str).getAsString(PdfName.CONTENTS);
                        byte[] content = string.getBytes();
                        X509Certificate certificate = pk.getSigningCertificate();
                        Certificate[] chain = pk.getSignCertificateChain();
                        byte[] encoded = content;
                        TimeStamp timeStamp = null;
                        String location = pk.getLocation();
                        String reason = pk.getReason();
                        Date date = pk.getSignDate().getTime();
                        Boolean valid = Boolean.TRUE;
                        Signatory signatory = this.toSignatory(certificate);
                        Store store = new JCAStore(KeyStoreType.JKS);
                        store.add(new CertificateEntry(new Alias(certificate.getSerialNumber().toString()), certificate));
                        Object[] fails = PdfPKCS7.verifyCertificates(chain, store.toKeyStore(), pk.getCRLs(), pk.getSignDate());
                        if (ConditionUtils.isNotEmpty(fails)) {
                            valid = Boolean.FALSE;
                        }
                        TimeStampToken timeStampToken = pk.getTimeStampToken();
                        if (timeStampToken != null) {
                            timeStamp = BouncyCastleTimeStampHelper.toTimeStamp(timeStampToken);
                            timeStampToken.getTimeStampInfo();
                            if (valid.booleanValue()) {
                                boolean ok = pk.verifyTimestampImprint();
                                valid = Boolean.valueOf(ok);
                            }
                        }
                        Signature sig = new Signature();
                        sig.setDate(date);
                        sig.setEncoded(encoded);
                        sig.setLocation(location);
                        sig.setReason(reason);
                        sig.setSignatories(new ArrayList<Signatory>());
                        sig.getSignatories().add(signatory);
                        sig.setTimeStamp(timeStamp);
                        sig.setValid(valid);
                        signatures.add(sig);
                    }
                }
            }
            return CollectionUtils.toArray(signatures, Signature.class);
        } catch (Exception e) {
            throw new PDFException(e);
        }
    }

    protected Signatory toSignatory(final X509Certificate certificate) {
        X500Principal x500Subject = certificate.getSubjectX500Principal();
        X500Principal x500Issuer = certificate.getIssuerX500Principal();
        String subject = BouncyCastleProviderHelper.getName(x500Subject);
        String issuer = BouncyCastleProviderHelper.getName(x500Issuer);
        Signatory signatory = new Signatory();
        signatory.setCertificate(certificate);
        signatory.setIssuer(issuer);
        signatory.setSubject(subject);
        return signatory;
    }

    protected SignatureType getSignatureType(final String signatureAlgorithm) {
        SignatureType type = SignatureType.getSignType(signatureAlgorithm);
        if (type == null) {
            type = SignatureType.SHA1_RSA;
        }
        return type;
    }

    protected DigestType getDigestTypeFromSignature(final String signatureAlgorithm) {
        SignatureType signatureType = this.getSignatureType(signatureAlgorithm);
        DigestType digestType = signatureType.getDigestType();
        return digestType;
    }
}
