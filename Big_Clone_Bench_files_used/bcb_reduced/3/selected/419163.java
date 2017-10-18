package es.caib.signatura.provider.impl.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import es.caib.signatura.impl.SMIMEPkcs7InputStream;
import es.caib.signatura.api.Signature;
import es.caib.signatura.impl.RawSignature;
import es.caib.signatura.impl.SMIMEGenerator;
import es.caib.signatura.impl.SMIMEInputStream;

/**
 * S/MIME generator implementation.
 * This class implements the same functionallity of SMIMEGeneratorImpl.
 * 
 * @author 3digits
 */
public class CopyOfSMIMEGeneratorImpl implements SMIMEGenerator {

    public CopyOfSMIMEGeneratorImpl() {
    }

    /**
	 * {@inheritDoc}
	 */
    public InputStream generateSMIME(InputStream document, Signature signature) throws IOException {
        InputStream in;
        if (signature instanceof RawSignature) {
            byte[] pkcs7 = encapsulateData(signature.getPkcs7(), document);
            in = new SMIMEPkcs7InputStream(pkcs7);
        } else {
            in = new SMIMEInputStream(signature, document);
        }
        return in;
    }

    /**
	 * This method encapsulates the signed data into the signature.
	 * It returns a PKCS#7 signature with the data that has been signed encapsulated into it.
	 * If the byte array <code>pkcs7</code> is not a correct signature the method returns null.
	 * It doesn't throws some exceptions because the method is private and it is suposed
	 * that signatures will be correct (Signature implementations check it).
	 * 
	 * @param pkcs7 byte array that contains a PKCS#7 dettached signature.
	 * @param data InputStream with the data to encapsulate into the signature.
	 * @return PKCS#7 signature with the signed data attached into it. 
	 * 
	 * @throws IOException when I/O errors occurs.
	 */
    private static byte[] encapsulateData(byte[] pkcs7, InputStream data) throws IOException {
        try {
            CMSSignedData cmsSignedData = new CMSSignedData(pkcs7);
            CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
            cmsSignedDataGenerator.addSigners(cmsSignedData.getSignerInfos());
            cmsSignedDataGenerator.addCertificatesAndCRLs(cmsSignedData.getCertificatesAndCRLs("Collection", "BC"));
            CMSProcessable cmsProcessable = new CopyOfSMIMEGeneratorImpl().new ProcessableInputStream(data);
            cmsSignedData = cmsSignedDataGenerator.generate(cmsProcessable, true, "BC");
            return cmsSignedData.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    class ProcessableInputStream implements CMSProcessable {

        private DigestInputStream in;

        MessageDigest digester;

        byte digestResult[];

        public void write(OutputStream out) throws IOException, CMSException {
            byte b[] = new byte[8192];
            int read = in.read(b);
            while (read > 0) {
                out.write(b, 0, read);
                read = in.read(b);
            }
            out.close();
            in.close();
            digestResult = digester.digest();
        }

        public Object getContent() {
            return in;
        }

        public ProcessableInputStream(InputStream datain) throws NoSuchAlgorithmException, NoSuchProviderException {
            super();
            digester = MessageDigest.getInstance("SHA-1", "BC");
            in = new DigestInputStream(datain, digester);
            digestResult = null;
        }

        public byte[] getDigest() {
            return digestResult;
        }
    }
}
