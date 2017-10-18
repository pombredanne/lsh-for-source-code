package es.caib.signatura.provider.impl.common;

import es.caib.signatura.api.Signature;
import es.caib.signatura.impl.GeneradorSMIMEParalelo;
import es.caib.signatura.impl.MIMEInputStream;
import es.caib.signatura.impl.SMIMEInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Properties;
import javax.mail.Session;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInformationStore;

/**
 * Implementation of the S/MIME generator with N signatures in parallel.
 * 
 * @author Fernando Guardiola Ruiz
 */
public class GeneradorSMIMEParaleloImpl implements GeneradorSMIMEParalelo {

    public GeneradorSMIMEParaleloImpl() {
    }

    /**
   * It generates a PKCS#7 multipart/signed S/MIME from a <code>contentStream</code> and an array that contains
   * diferent signatures of that content.
   * 
   * @param contentStream Data content of the original message.
   * @param signatures Array with the signatures from which the PKCS#7 object is generated.
   * @return the S/MIME document.
   * @throws Exception Si se produce algun error generando el S/MIME. 
   */
    public SMIMEInputStream generarSMIMEParalelo(InputStream contentStream, Signature[] signatures) throws Exception {
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        String contentType = "";
        ArrayList listSigners = new ArrayList();
        ArrayList listCertsAndCrl = new ArrayList();
        contentType = signatures[0].getContentType();
        for (int is = 0; is < signatures.length; is++) {
            Signature signature = signatures[is];
            CMSSignedData cmsSignedData = new CMSSignedData(signature.getPkcs7());
            SignerInformationStore signerInformationStore = cmsSignedData.getSignerInfos();
            listSigners.addAll(signerInformationStore.getSigners());
            CertStore certStore = cmsSignedData.getCertificatesAndCRLs("Collection", "BC");
            CollectionCertStoreParameters collectionCertStoreParams = (CollectionCertStoreParameters) certStore.getCertStoreParameters();
            listCertsAndCrl.addAll(collectionCertStoreParams.getCollection());
        }
        SignerInformationStore signerInformationStoreParalell = new SignerInformationStore(listSigners);
        CollectionCertStoreParameters collectionCertStoreParamsParalell = new CollectionCertStoreParameters(listCertsAndCrl);
        CertStore certStoreParallel = CertStore.getInstance("Collection", collectionCertStoreParamsParalell);
        CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
        cmsSignedDataGenerator.addSigners(signerInformationStoreParalell);
        cmsSignedDataGenerator.addCertificatesAndCRLs(certStoreParallel);
        MIMEInputStream mimeInputStream = new MIMEInputStream(contentStream, contentType);
        CMSProcessable cmsProcessable = new ProcessableInputStream(mimeInputStream);
        CMSSignedData cmsSignedDataParallel = cmsSignedDataGenerator.generate(cmsProcessable, "BC");
        return new SMIMEInputStream(cmsSignedDataParallel.getEncoded(), contentStream, contentType);
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
