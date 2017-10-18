package pdfsignature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import visual.ProgressBarEdu;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
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
import com.itextpdf.text.pdf.TSAClientBouncyCastle;
import exceptions.NotEnoughSpaceException;

/**
 * Sign a PDF document
 * @author eduard
 *
 */
public class PDFFileSign {

    /**
	 * Sign a PDF File and creates a new PDF Signed
	 * @param inFile     The PDF file to sign
	 * @param outFile    The PDF file signed
	 * @param certChain  The certificate chain used to sign
	 * @param privKey    The private key
	 * @param tsaClie    The Time Stamp Authority client
	 * @param tsaAccount The account of the Time Stamp Authority (usually null)
	 * @param tsaPwd     The password of the Time Stamp Authority account (usually null)
	 * @param Rctng      The rectangle of the signature
	 * @param location   The location of the signature
	 * @param reason     The reason of the signature
	 * @throws IOException
	 * @throws DocumentException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateParsingException
	 * @throws SignatureException
	 * @throws NotEnoughSpaceException
	 */
    public static void signFile(String inFile, String outFile, Certificate[] certChain, PrivateKey privKey, TSAClient tsaCli, Rectangle Rctng, String location, String reason) throws IOException, DocumentException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, CertificateParsingException, SignatureException, NotEnoughSpaceException {
        PdfReader reader = new PdfReader(inFile);
        FileOutputStream fout = new FileOutputStream(outFile);
        PdfStamper stp = PdfStamper.createSignature(reader, fout, '\0');
        PdfSignatureAppearance sap = stp.getSignatureAppearance();
        sap.setCrypto(null, certChain, null, PdfSignatureAppearance.WINCER_SIGNED);
        sap.setVisibleSignature(Rctng, 1, "Signature");
        sap.setLocation(location);
        sap.setReason(reason);
        PdfSignature pdfSig = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));
        pdfSig.setReason(sap.getReason());
        pdfSig.setLocation(sap.getLocation());
        pdfSig.setContact(sap.getContact());
        pdfSig.setDate(new PdfDate(sap.getSignDate()));
        sap.setCryptoDictionary(pdfSig);
        int contentEstimated = 15000;
        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(contentEstimated * 2 + 2));
        sap.preClose(exc);
        PdfPKCS7 sgn = new PdfPKCS7(privKey, certChain, null, "SHA1", null, false);
        InputStream data = sap.getRangeStream();
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        byte buf[] = new byte[8192];
        int n;
        while ((n = data.read(buf)) > 0) {
            messageDigest.update(buf, 0, n);
        }
        byte hash[] = messageDigest.digest();
        Calendar cal = Calendar.getInstance();
        byte[] ocsp = null;
        if (certChain.length >= 2) {
            String url = PdfPKCS7.getOCSPURL((X509Certificate) certChain[0]);
            if (url != null && url.length() > 0) ocsp = new OcspClientBouncyCastle((X509Certificate) certChain[0], (X509Certificate) certChain[1], url).getEncoded();
        }
        byte sh[] = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp);
        sgn.update(sh, 0, sh.length);
        byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, tsaCli, ocsp);
        if (contentEstimated + 2 < encodedSig.length) throw new NotEnoughSpaceException();
        byte[] paddedSig = new byte[contentEstimated];
        System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);
        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
        sap.close(dic2);
    }

    /**
	 * Sign a PDF File and creates a new PDF Signed
	 * @param inFile     The PDF file to sign
	 * @param outFile    The PDF file signed
	 * @param certChain  The certificate chain used to sign
	 * @param privKey    The private key
	 * @param tsaURL     The URL of the Time Stamp Authority
	 * @param tsaAccount The account of the Time Stamp Authority (usually null)
	 * @param tsaPwd     The password of the Time Stamp Authority account (usually null)
	 * @param Rctng      The rectangle of the signature
	 * @param location   The location of the signature
	 * @param reason     The reason of the signature
	 * @throws IOException
	 * @throws DocumentException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateParsingException
	 * @throws SignatureException
	 * @throws NotEnoughSpaceException
	 */
    public static void signFile(String inFile, String outFile, Certificate[] certChain, PrivateKey privKey, String tsaURL, String tsaAccount, String tsaPwd, Rectangle Rctng, String location, String reason) throws IOException, DocumentException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, CertificateParsingException, SignatureException, NotEnoughSpaceException {
        TSAClient tsaCli = new TSAClientBouncyCastle(tsaURL, tsaAccount, tsaPwd);
        signFile(inFile, outFile, certChain, privKey, tsaCli, Rctng, location, reason);
    }

    public static void signFolder(String inFolder, String outFolder, Certificate[] certChain, PrivateKey privKey, String tsaURL, String tsaAccount, String tsaPwd, Rectangle Rctng, String location, String reason) throws IOException, DocumentException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, CertificateParsingException, SignatureException, NotEnoughSpaceException {
        boolean errada = false;
        TSAClient tsaCli = new TSAClientBouncyCastle(tsaURL, tsaAccount, tsaPwd);
        File dir = new File(inFolder);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return (name.endsWith(".pdf") || name.endsWith(".PDF")) && !name.contains("_signed.");
            }
        };
        String[] fitxers = dir.list(filter);
        ProgressBarEdu progress = new ProgressBarEdu("Signing PDF files", fitxers.length);
        for (int i = 0; i < fitxers.length; i++) {
            if (!errada) {
                String inputFileName = inFolder + "\\" + fitxers[i];
                String OUT_FILE = outFolder + "\\" + fitxers[i];
                progress.mostrar("Signing " + fitxers[i], i);
                if (fitxers[i].endsWith(".PDF")) OUT_FILE = OUT_FILE.replace(".PDF", "_signed.pdf");
                if (fitxers[i].endsWith(".pdf")) OUT_FILE = OUT_FILE.replace(".pdf", "_signed.pdf");
                signFile(inputFileName, OUT_FILE, certChain, privKey, tsaCli, Rctng, location, reason);
            }
        }
        progress.amagar();
    }

    public static void main(String[] args) {
        String CERT_PATH = "D:/DATOS_EDU/CERTIFICATS_2010/eescrihuela286s_firma.p12";
        String CERT_PASSW = "6526231";
        String TSA_URL = "http://tss.accv.es:8318/tsa";
        String TSA_ACCNT = null;
        String TSA_PASSW = null;
        String IN_FILE = "c:/a.pdf";
        String OUT_FILE = "c:/a_signed_X1.pdf";
        KeyStore ks;
        try {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(CERT_PATH), CERT_PASSW.toCharArray());
            String alias = (String) ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey) ks.getKey(alias, CERT_PASSW.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);
            PDFFileSign.signFile(IN_FILE, OUT_FILE, chain, pk, TSA_URL, TSA_ACCNT, TSA_PASSW, new Rectangle(365, 20, 580, 120), "Tavernes de Valldigna", "Test PDF Signature");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (NotEnoughSpaceException e) {
            e.printStackTrace();
        }
    }
}
