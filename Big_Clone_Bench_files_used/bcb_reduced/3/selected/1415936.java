package com.lowagie.text.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.esle.sinadura.core.exceptions.OCSPException;
import net.esle.sinadura.core.exceptions.RevokedException;
import net.esle.sinadura.core.exceptions.SignException;
import net.esle.sinadura.core.sign.SignaturePreferences;
import net.esle.sinadura.core.validate.CertificatePathBuilder;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;

public class PdfReaderExtended extends PdfReader {

    private static Logger logger = Logger.getLogger(PdfReaderExtended.class.getName());

    private static final int MAX_DATA_PDF = 925;

    private String path = null;

    private String pathDestino = null;

    private boolean signed = false;

    private String alias = "";

    private PrivateKey priv = null;

    private Certificate[] chain = null;

    private String tsaURL = null;

    private String tsaProxy = null;

    private int tsaPort = 0;

    private boolean addOCSP = false;

    protected PdfReaderExtended() {
    }

    /** Reads and parses a PDF document.
     * @param filename the file name of the document
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */
    public PdfReaderExtended(String filename, byte ownerPassword[]) throws IOException {
        super(filename, ownerPassword);
    }

    /** Reads and parses a PDF document.
     * @param pdfIn the byte array with the document
     * @throws IOException on error
     */
    public PdfReaderExtended(byte pdfIn[]) throws IOException {
        this(pdfIn, null);
    }

    /** Reads and parses a PDF document.
     * @param pdfIn the byte array with the document
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */
    public PdfReaderExtended(byte pdfIn[], byte ownerPassword[]) throws IOException {
        super(pdfIn, ownerPassword);
    }

    /** Reads and parses a PDF document.
     * @param filename the file name of the document
     * @param certificate the certificate to read the document
     * @param certificateKey the private key of the certificate
     * @param certificateKeyProvider the security provider for certificateKey
     * @throws IOException on error
     */
    public PdfReaderExtended(String filename, Certificate certificate, Key certificateKey, String certificateKeyProvider) throws IOException {
        super(filename, certificate, certificateKey, certificateKeyProvider);
    }

    /** Reads and parses a PDF document.
     * @param url the URL of the document
     * @throws IOException on error
     */
    public PdfReaderExtended(URL url) throws IOException {
        this(url, null);
    }

    /** Reads and parses a PDF document.
     * @param url the URL of the document
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */
    public PdfReaderExtended(URL url, byte ownerPassword[]) throws IOException {
        super(url, ownerPassword);
    }

    /**
     * Reads and parses a PDF document.
     * @param is the <CODE>InputStream</CODE> containing the document. The stream is read to the
     * end but is not closed
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */
    public PdfReaderExtended(InputStream is, byte ownerPassword[]) throws IOException {
        super(is, ownerPassword);
    }

    /**
     * Reads and parses a PDF document.
     * @param is the <CODE>InputStream</CODE> containing the document. The stream is read to the
     * end but is not closed
     * @throws IOException on error
     */
    public PdfReaderExtended(InputStream is) throws IOException {
        this(is, null);
    }

    /**
     * Reads and parses a pdf document. Contrary to the other constructors only the xref is read
     * into memory. The reader is said to be working in "partial" mode as only parts of the pdf
     * are read as needed. The pdf is left open but may be closed at any time with
     * <CODE>PdfReader.close()</CODE>, reopen is automatic.
     * @param raf the document location
     * @param ownerPassword the password or <CODE>null</CODE> for no password
     * @throws IOException on error
     */
    public PdfReaderExtended(RandomAccessFileOrArray raf, byte ownerPassword[]) throws IOException {
        super(raf, ownerPassword);
    }

    /** Creates an independent duplicate.
     * @param reader the <CODE>PdfReader</CODE> to duplicate
     */
    public PdfReaderExtended(PdfReader reader) {
        super(reader);
    }

    /** Reads and parses a PDF document.
     * @param filename the file name of the document
     * @throws IOException on error
     */
    public PdfReaderExtended(String path) throws IOException {
        super(path);
    }

    /** Overwrited to generate an AcroFieldsExtended instead of an AcroFields   
     * 
     */
    public AcroFields getAcroFields() {
        return new AcroFieldsExtended(this, null);
    }

    /**
	 * Sign the current PdfReader with the given preferences
	 * 
	 * @param signaturePreferences
	 * @throws SignException
	 * @throws OCSPException
	 * @throws RevokedException
	 */
    public void sign(SignaturePreferences signaturePreferences) throws SignException, OCSPException, RevokedException {
        try {
            this.alias = signaturePreferences.getAlias();
            this.addOCSP = signaturePreferences.getAddOCSP();
            KeyStore.PrivateKeyEntry keyEntry = null;
            KeyStore ks = signaturePreferences.getKs();
            try {
                keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(this.alias, signaturePreferences.getPasswordProtection());
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (UnrecoverableEntryException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            this.priv = keyEntry.getPrivateKey();
            this.chain = null;
            try {
                this.chain = ks.getCertificateChain(this.alias);
            } catch (KeyStoreException e) {
                throw new SignException(e.getMessage(), e);
            }
            logger.info("Chain size before: " + this.chain.length);
            if (signaturePreferences.getKsCache() != null) {
                try {
                    this.chain = CertificatePathBuilder.completeChain((X509Certificate[]) this.chain, signaturePreferences.getKsCache());
                } catch (ClassCastException e) {
                    logger.log(Level.WARNING, "could not complete the chain", e);
                }
                logger.info("Chain size after: " + this.chain.length);
            }
            com.lowagie.text.pdf.TSAClient tsc = null;
            if (signaturePreferences.getTsurl() != null && !signaturePreferences.getTsurl().equals("")) {
                tsc = new TSAClientBouncyCastle(signaturePreferences.getTsurl(), null, null);
            }
            PdfStamper stp = null;
            try {
                stp = PdfStamper.createSignature(this, signaturePreferences.getOutput(), '\0', null, true);
            } catch (DocumentException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            PdfSignatureAppearance sap = stp.getSignatureAppearance();
            sap.setCrypto(null, this.chain, null, PdfSignatureAppearance.SELF_SIGNED);
            if (signaturePreferences.getReason() != null && !signaturePreferences.getReason().equals("")) sap.setReason(signaturePreferences.getReason());
            if (signaturePreferences.getLocation() != null && !signaturePreferences.getLocation().equals("")) sap.setLocation(signaturePreferences.getLocation());
            if (signaturePreferences.getImage() != null) {
                sap.setImage(signaturePreferences.getImage());
            }
            if (signaturePreferences.getVisible()) {
                sap.setVisibleSignature(new Rectangle(signaturePreferences.getStartX(), signaturePreferences.getStartY(), signaturePreferences.getEndX(), signaturePreferences.getEndY()), 1, null);
            }
            sap.setCertificationLevel(signaturePreferences.getCertified());
            PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));
            dic.setReason(sap.getReason());
            dic.setLocation(sap.getLocation());
            dic.setContact(sap.getContact());
            dic.setDate(new PdfDate(sap.getSignDate()));
            sap.setCryptoDictionary(dic);
            int CONTENT_ESTIMATED = 15000;
            HashMap exc = new HashMap();
            exc.put(PdfName.CONTENTS, new Integer(CONTENT_ESTIMATED * 2 + 2));
            try {
                sap.preClose(exc);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (DocumentException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            PdfPKCS7Extended sgn = null;
            try {
                sgn = new PdfPKCS7Extended(this.priv, this.chain, null, "SHA1", null, false);
            } catch (InvalidKeyException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (NoSuchProviderException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            InputStream data = sap.getRangeStream();
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            byte buf[] = new byte[8192];
            int n;
            try {
                while ((n = data.read(buf)) > 0) {
                    messageDigest.update(buf, 0, n);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            byte hash[] = messageDigest.digest();
            Calendar cal = Calendar.getInstance();
            byte[] ocsp = null;
            if (signaturePreferences.getAddOCSP()) {
                if (this.chain.length >= 2) {
                    String url = null;
                    try {
                        url = PdfPKCS7.getOCSPURL((X509Certificate) this.chain[0]);
                    } catch (CertificateParsingException e) {
                        logger.log(Level.SEVERE, "", e);
                        throw new OCSPException(e);
                    }
                    if (url != null && url.length() > 0) {
                        logger.info("starting ocsp request");
                        try {
                            ocsp = new OcspClientBouncyCastle((X509Certificate) this.chain[0], (X509Certificate) this.chain[1], url).getEncoded();
                        } catch (RuntimeException e) {
                            if (e.getMessage().equals("OCSP Status is revoked!")) {
                                logger.log(Level.SEVERE, "", e);
                                throw new RevokedException(e);
                            } else {
                                logger.log(Level.SEVERE, "", e);
                                throw new OCSPException(e);
                            }
                        }
                    } else {
                        logger.severe("could not get ocsp url from the certificate.");
                        throw new OCSPException(new Exception());
                    }
                } else {
                    logger.severe("not enought certificates in the chain, needs the signer issuer.");
                    throw new OCSPException(new Exception());
                }
            }
            byte sh[] = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp);
            try {
                sgn.update(sh, 0, sh.length);
            } catch (SignatureException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, tsc, ocsp);
            if (CONTENT_ESTIMATED + 2 < encodedSig.length) {
                logger.severe("Not enough space");
                throw new SignException("Not enough space", new Exception());
            }
            byte[] paddedSig = new byte[CONTENT_ESTIMATED];
            System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);
            PdfDictionary dic2 = new PdfDictionary();
            dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
            try {
                sap.close(dic2);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            } catch (DocumentException e) {
                logger.log(Level.SEVERE, "", e);
                throw new SignException(e.getMessage(), e);
            }
            logger.info("signed");
        } catch (RuntimeException e) {
            throw new SignException(e.getMessage(), e);
        }
    }
}
