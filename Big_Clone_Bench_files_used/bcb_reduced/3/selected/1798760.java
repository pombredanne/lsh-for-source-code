package br.gov.component.demoiselle.security.certificate.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import br.gov.component.demoiselle.security.certificate.CertificateValidatorException;
import br.gov.component.demoiselle.security.certificate.IValidator;
import br.gov.component.demoiselle.security.certificate.extension.BasicCertificate;
import br.gov.component.demoiselle.security.certificate.extension.ICPBR_CRL;

/**
 * 
 * @author CETEC/CTCTA
 *
 */
public class CRLValidator implements IValidator {

    private static final String CRL_PATH_KEY = "demoiselle.component.security.certificate.crl.path";

    private static final String CRL_INDEX_NAME_KEY = "demoiselle.component.security.certificate.crl.index";

    private static final String DEFAULT_INDEX_NAME = ".index";

    public void validate(X509Certificate x509) throws CertificateValidatorException {
        String crlPath = (String) System.getProperties().get(CRL_PATH_KEY);
        String crlIndex = (String) System.getProperties().get(CRL_INDEX_NAME_KEY);
        if (crlIndex == null || crlIndex.equals("")) {
            crlIndex = DEFAULT_INDEX_NAME;
        }
        BasicCertificate cert = new BasicCertificate(x509);
        List<String> ListaURLCRL;
        try {
            ListaURLCRL = cert.getCRLDistributionPoint();
        } catch (IOException e1) {
            throw new CertificateValidatorException("Could not get the CRL List from Certificate " + e1);
        }
        boolean clrOK = false;
        for (String URLCRL : ListaURLCRL) {
            String tmpFileName = URLCRL;
            String fileNameCRL = doHash(tmpFileName, "MD5");
            File fileCRL = new File(crlPath, fileNameCRL.toString());
            if (fileCRL.exists()) {
                ICPBR_CRL crl = null;
                try {
                    crl = new ICPBR_CRL(new FileInputStream(fileCRL));
                } catch (FileNotFoundException e) {
                    clrOK = false;
                    crl = null;
                    e.printStackTrace();
                } catch (CertificateException e) {
                    clrOK = false;
                    crl = null;
                    e.printStackTrace();
                } catch (CRLException e) {
                    clrOK = false;
                    crl = null;
                    e.printStackTrace();
                } catch (Exception e) {
                    clrOK = false;
                    crl = null;
                    e.printStackTrace();
                }
                if (crl != null) {
                    if (crl.getCRL().getNextUpdate().before(new Date())) {
                        clrOK = false;
                        System.err.println("This CRL is out of date!:" + fileNameCRL + "\n will try the next, if it exists.");
                    } else {
                        if (crl.getCRL().isRevoked(x509)) {
                            throw new CertificateValidatorException("Certificate Revoked in CRL");
                        } else {
                            clrOK = true;
                        }
                    }
                }
            }
            if (clrOK) {
                break;
            }
        }
        if (!clrOK) {
            boolean storedOK = false;
            for (String URLCRL : ListaURLCRL) {
                String tmpFileName = URLCRL;
                String fileNameCRL = doHash(tmpFileName, "MD5");
                File fileIndex = new File(crlPath, crlIndex);
                if (!fileIndex.exists()) {
                    try {
                        fileIndex.createNewFile();
                    } catch (Exception e) {
                        throw new CertificateValidatorException("Error creating index file " + fileIndex, e);
                    }
                }
                Properties prop = new Properties();
                try {
                    prop.load(new FileInputStream(fileIndex));
                } catch (Exception e) {
                    throw new CertificateValidatorException("Error on load index file " + fileIndex, e);
                }
                prop.put(fileNameCRL.toString(), URLCRL);
                try {
                    prop.store(new FileOutputStream(fileIndex), null);
                    storedOK = true;
                } catch (Exception e) {
                    storedOK = false;
                    e.printStackTrace();
                }
            }
            if (!storedOK) {
                throw new CertificateValidatorException("Error on store some index file !");
            }
            throw new CertificateValidatorException("Can NOT validate if Certificate is Revoked!, none valid list was found, try Update It");
        }
    }

    private static String doHash(String frase, String algorithm) {
        try {
            String ret;
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(frase.getBytes());
            BigInteger bigInt = new BigInteger(1, md.digest());
            ret = bigInt.toString(16);
            return ret;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
