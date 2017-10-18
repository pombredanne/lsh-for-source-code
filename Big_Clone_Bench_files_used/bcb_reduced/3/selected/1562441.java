package org.monet.docservice.docprocessor.pdf.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import org.monet.docservice.core.exceptions.ApplicationException;
import org.monet.docservice.core.library.LibraryBase64;
import org.monet.docservice.core.log.Logger;
import org.monet.docservice.docprocessor.configuration.Configuration;
import org.monet.docservice.docprocessor.data.Repository;
import org.monet.docservice.docprocessor.model.PresignedDocument;
import org.monet.docservice.docprocessor.model.SignMetadata;
import org.monet.docservice.docprocessor.pdf.Signer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.xml.xmp.PdfA1Schema;

public class PdfSigner implements Signer {

    private static final int ENCODED_SIGNATURE_LENGTH = 15000;

    private static final String RESOURCES_PRESIGN_DOCUMENT = "/resources/%s/presign/%s.pdf";

    private Configuration configuration;

    private Logger logger;

    private LibraryBase64 libraryBase64;

    private Provider<Repository> repositoryProvider;

    @Inject
    public void injectLogger(Logger logger) {
        this.logger = logger;
    }

    @Inject
    public void injectConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void injectLibraryBase64(LibraryBase64 libraryBase64) {
        this.libraryBase64 = libraryBase64;
    }

    @Inject
    public void injectRepository(Provider<Repository> repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    public PresignedDocument prepareDocument(String documentId, byte[] aCertificate, SignMetadata signMetadata) {
        logger.debug("prepareDocument(%s, %s)", documentId, aCertificate);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(aCertificate));
            return prepareDocument(documentId, certificate, signMetadata);
        } catch (CertificateException e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(String.format("Error processing certificate"));
        }
    }

    private PresignedDocument prepareDocument(String documentId, Certificate certificate, SignMetadata signMetadata) {
        logger.debug("prepareDocument(%s, %s)", documentId, certificate);
        InputStream document = getDocumentFile(documentId);
        String instanceId = UUID.randomUUID().toString();
        File preparedDoc = getPresignedDocumentFile(documentId, instanceId);
        byte[] hash = getDocumentHash(document, preparedDoc, certificate, signMetadata, documentId);
        PresignedDocument info = new PresignedDocument();
        info.setDocumentId(documentId);
        info.setInstanceId(instanceId);
        info.setHash(libraryBase64.encode(hash));
        return info;
    }

    public byte[] getDocumentHash(InputStream document, File preparedDoc, Certificate certificate, SignMetadata signMetadata, String documentId) {
        logger.debug("getDocumentHash(%s, %s, %s)", document, preparedDoc, certificate);
        try {
            PdfReader pdfReader = new PdfReader(document);
            FileOutputStream fout = new FileOutputStream(preparedDoc);
            PdfStamper pdfStamper = PdfStamper.createSignature(pdfReader, fout, '\0', null, true);
            if (getPDFXConformance(pdfReader) == PdfWriter.PDFA1B) pdfStamper.getWriter().setPDFXConformance(PdfWriter.PDFA1B);
            PdfSignatureAppearance sap = pdfStamper.getSignatureAppearance();
            Certificate[] certs = new Certificate[1];
            certs[0] = certificate;
            sap.setCrypto(null, certs, null, PdfSignatureAppearance.WINCER_SIGNED);
            sap.setVisibleSignature(signMetadata.getSignField());
            sap.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
            addSignMetadata(certificate, sap, signMetadata);
            HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
            exc.put(PdfName.CONTENTS, Integer.valueOf(ENCODED_SIGNATURE_LENGTH + 2));
            sap.preClose(exc);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            byte buf[] = new byte[8192];
            int n;
            InputStream inp = sap.getRangeStream();
            while ((n = inp.read(buf)) > 0) {
                messageDigest.update(buf, 0, n);
            }
            byte[] hash = messageDigest.digest();
            PdfDictionary dic2 = new PdfDictionary();
            dic2.put(PdfName.CONTENTS, new PdfString(PdfSignerUtils.getPlaceHolderArr(ENCODED_SIGNATURE_LENGTH)).setHexWriting(true));
            sap.close(dic2);
            return hash;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(String.format("Error calculating document '%s' sign field '%s' hash", documentId, signMetadata.getSignField()));
        }
    }

    private int getPDFXConformance(PdfReader pdfReader) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(pdfReader.getMetadata()));
            NodeList nodeList = document.getElementsByTagName(PdfA1Schema.CONFORMANCE);
            Node node1 = nodeList.item(0);
            if (node1.getTextContent().equals("A")) return PdfWriter.PDFA1A; else if (node1.getTextContent().equals("B")) return PdfWriter.PDFA1B;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PdfWriter.PDFXNONE;
    }

    private void addSignMetadata(Certificate certificate, PdfSignatureAppearance sap, SignMetadata signMetadata) {
        logger.debug("addSignMetadata(%s, %s)", certificate, sap);
        Calendar date = Calendar.getInstance();
        sap.setSignDate(date);
        sap.setReason(signMetadata.getReason());
        sap.setLocation(signMetadata.getLocation());
        sap.setContact(signMetadata.getContact());
        sap.setAcro6Layers(true);
        sap.setRenderingMode(PdfSignatureAppearance.RenderingMode.NAME_AND_DESCRIPTION);
        PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKMS, PdfName.ADBE_PKCS7_SHA1);
        dic.setDate(new PdfDate(date));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate) certificate).getField("CN"));
        dic.setReason(signMetadata.getReason());
        dic.setLocation(signMetadata.getLocation());
        dic.setContact(signMetadata.getContact());
        sap.setCryptoDictionary(dic);
    }

    private File getPresignedDocumentFile(String documentId, String instanceId) {
        logger.debug("getPresignedDocumentFile(%s, %s)", documentId, instanceId);
        File file = new File(this.configuration.getPath(Configuration.PATH_TEMP) + String.format(RESOURCES_PRESIGN_DOCUMENT, documentId, instanceId));
        file.getParentFile().mkdirs();
        return file;
    }

    private InputStream getDocumentFile(String documentId) {
        logger.debug("getDocumentFile(%s)", documentId);
        return repositoryProvider.get().getDocumentData(documentId);
    }

    public void signDocument(String documentId, String instanceId, byte[] pkcs7Block) {
        logger.debug("signDocument(%s, %s, %s)", documentId, instanceId, pkcs7Block);
        File presignedDoc = getPresignedDocumentFile(documentId, instanceId);
        try {
            String sigAreaHex = PdfSignerUtils.byteArrayToHexString(pkcs7Block);
            byte[] placeHolder = PdfSignerUtils.byteArrayToHexString(PdfSignerUtils.getPlaceHolderArr(ENCODED_SIGNATURE_LENGTH)).getBytes();
            byte paddedHexArea[] = new byte[placeHolder.length];
            for (int i = 0; i < paddedHexArea.length; i++) {
                paddedHexArea[i] = 0x30;
            }
            System.arraycopy(sigAreaHex.getBytes(), 0, paddedHexArea, 0, sigAreaHex.getBytes().length);
            PdfSignerUtils.replace(presignedDoc, placeHolder, paddedHexArea);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ApplicationException("Creation of signed document failed.");
        }
        boolean isValid = false;
        try {
            isValid = verifyDocument(presignedDoc);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (!isValid) throw new ApplicationException("Creation of signed document failed: invalid document generated."); else {
            String contentType = repositoryProvider.get().getDocumentDataContentType(documentId);
            try {
                repositoryProvider.get().saveDocumentData(documentId, new FileInputStream(presignedDoc), contentType);
                presignedDoc.delete();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                presignedDoc.delete();
            }
        }
    }

    private boolean verifyDocument(File documentFile) throws IOException, SignatureException {
        logger.debug("verifyDocument(%s)", documentFile);
        boolean isValid = false;
        PdfReader reader = null;
        try {
            reader = new PdfReader(documentFile.getAbsolutePath());
            AcroFields af = reader.getAcroFields();
            ArrayList<String> names = (ArrayList<String>) af.getSignatureNames();
            isValid = true;
            for (String name : names) {
                PdfPKCS7 pk = af.verifySignature(name);
                isValid &= pk.verify();
            }
        } finally {
            if (reader != null) reader.close();
        }
        return isValid;
    }
}
