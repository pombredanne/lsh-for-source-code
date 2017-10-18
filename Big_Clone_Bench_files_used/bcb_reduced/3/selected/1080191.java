package es.caib.signatura.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;
import es.caib.signatura.api.SMIMEParser;
import es.caib.signatura.api.Signature;
import es.caib.signatura.api.SignatureCertNotFoundException;
import es.caib.signatura.api.SignatureException;
import es.caib.signatura.api.SignaturePrivKeyException;
import es.caib.signatura.api.SignatureProviderException;
import es.caib.signatura.api.SignatureTimestampException;
import es.caib.signatura.api.SignatureVerifyException;
import es.caib.signatura.api.Signer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;

/**
 * Implementator class of the Signer interface.
 *
 */
public class CAIBSigner implements Signer {

    private static LinkedHashMap realSigners = null;

    private SignaturaProperties properties = null;

    private static ClassLoaderFactory factory;

    private static Object lock = new Object();

    private SignerProviderInterface getSigner(String certificateName, String contentType) throws SignatureCertNotFoundException {
        boolean recognized = properties.needsRecognizedCertificate(contentType);
        Iterator signersIterator = realSigners.values().iterator();
        while (signersIterator.hasNext()) {
            SignerProviderInterface s = (SignerProviderInterface) signersIterator.next();
            String[] certs;
            try {
                certs = s.getCertList(recognized);
                for (int j = 0; j < certs.length; j++) {
                    if (certs[j].equals(certificateName)) return s;
                }
            } catch (SignatureCertNotFoundException e) {
            } catch (SignaturePrivKeyException e) {
            }
        }
        throw new SignatureCertNotFoundException();
    }

    public String[] getCertList(String contentType) throws SignatureCertNotFoundException, SignaturePrivKeyException {
        synchronized (lock) {
            Vector v = new Vector();
            Collection signersCollection = realSigners.values();
            if (signersCollection != null) {
                Iterator signersIterator = signersCollection.iterator();
                if (signersIterator != null) {
                    while (signersIterator.hasNext()) {
                        SignerProviderInterface s = (SignerProviderInterface) signersIterator.next();
                        String certs[];
                        boolean recognized = properties.needsRecognizedCertificate(contentType);
                        try {
                            certs = s.getCertList(recognized);
                            for (int j = 0; j < certs.length; j++) {
                                if (!v.contains(certs[j])) v.add(certs[j]);
                            }
                        } catch (SignatureCertNotFoundException e) {
                        } catch (SignaturePrivKeyException e) {
                            e.printStackTrace();
                        } catch (Throwable e) {
                            System.err.println("CANNOT LOAD on signature provider " + s.getClass().getName());
                            e.printStackTrace();
                        }
                    }
                }
            }
            return (String[]) v.toArray(new String[v.size()]);
        }
    }

    public Signature sign(String fileName, String certificateName, String password, String contentType) throws IOException, SignatureException {
        return sign(new FileInputStream(fileName), certificateName, password, contentType);
    }

    public Signature sign(InputStream contentStream, String certificateName, String password, String contentType) throws IOException, SignatureException {
        synchronized (lock) {
            boolean recognized = properties.needsRecognizedCertificate(contentType);
            boolean timeStamp = properties.needsTimeStamp(contentType);
            boolean rawSign = properties.needsRawSignature(contentType);
            SignerProviderInterface signer = getSigner(certificateName, contentType);
            if (SigDebug.isActive()) SigDebug.write("Checking if password is null.");
            if (password == null) {
                password = "";
            }
            if (SigDebug.isActive()) {
                SigDebug.write("CAIBSigned.sign: password " + (password.length() == 0 ? "is" : "is not") + " null.");
            }
            return signer.sign(contentStream, certificateName, password, contentType, recognized, timeStamp, rawSign);
        }
    }

    public Signature sign(URL url, String certificateName, String password, String contentType) throws IOException, SignatureException {
        return sign(url.openStream(), certificateName, password, contentType);
    }

    public OutputStream signPDF(InputStream contentStream, String certificateName, String password, String contentType, String url, int position) throws IOException, SignatureException {
        synchronized (lock) {
            boolean recognized = properties.needsRecognizedCertificate(contentType);
            SignerProviderInterface signer = getSigner(certificateName, contentType);
            if (SigDebug.isActive()) SigDebug.write("Checking if password is null.");
            if (password == null) {
                password = "";
            }
            if (SigDebug.isActive()) {
                SigDebug.write("CAIBSigned.sign: password " + (password.length() == 0 ? "is" : "is not") + " null.");
            }
            return signer.signPDF(contentStream, certificateName, password, contentType, recognized, url, position);
        }
    }

    public boolean verify(InputStream contentStream, Signature signatureData) throws SignatureProviderException, IOException, SignatureVerifyException {
        try {
            boolean isVerified = false;
            isVerified = signatureData.verify(contentStream);
            return isVerified;
        } catch (SignatureVerifyException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
    }

    public boolean verifyAPosterioriTimeStamp(InputStream contentStream, Signature signatureData) throws SignatureProviderException, IOException, SignatureVerifyException {
        boolean isVerified = false;
        try {
            isVerified = signatureData.verifyAPosterioriTimestamp(contentStream);
        } catch (SignatureVerifyException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureVerifyException(e);
        }
        return isVerified;
    }

    public boolean verify(String fileName, Signature signatureData) throws FileNotFoundException, SignatureProviderException, IOException, SignatureVerifyException {
        return verify(new FileInputStream(fileName), signatureData);
    }

    public boolean verify(URL url, Signature signatureData) throws SignatureProviderException, IOException, SignatureVerifyException {
        return verify(url.openStream(), signatureData);
    }

    public boolean verifyAPosterioriTimeStamp(String fileName, Signature signatureData) throws FileNotFoundException, SignatureProviderException, IOException, SignatureVerifyException {
        return verifyAPosterioriTimeStamp(new FileInputStream(fileName), signatureData);
    }

    public boolean verifyAPosterioriTimeStamp(URL url, Signature signatureData) throws SignatureProviderException, IOException, SignatureVerifyException {
        return verifyAPosterioriTimeStamp(url.openStream(), signatureData);
    }

    public void generateSMIME(InputStream document, Signature signature, OutputStream smime) throws IOException {
        SMIMEGenerator smimeGenerator;
        try {
            Class c = factory.getMasterClassLoader().loadClass("es.caib.signatura.provider.impl.common.SMIMEGeneratorImpl");
            smimeGenerator = (SMIMEGenerator) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        InputStream in = smimeGenerator.generateSMIME(document, signature);
        byte b[] = new byte[8192];
        int read;
        while ((read = in.read(b)) > 0) {
            smime.write(b, 0, read);
        }
        in.close();
    }

    public void generateSMIMEParalell(InputStream document, Signature[] signatures, OutputStream smime) throws IOException, SignatureException {
        try {
            Class c = factory.getMasterClassLoader().loadClass("es.caib.signatura.provider.impl.common.GeneradorSMIMEParaleloImpl");
            GeneradorSMIMEParalelo generadorSMIMEParalelo = (GeneradorSMIMEParalelo) c.newInstance();
            SMIMEInputStream in = generadorSMIMEParalelo.generarSMIMEParalelo(document, signatures);
            byte b[] = new byte[8192];
            int read;
            while ((read = in.read(b)) > 0) {
                smime.write(b, 0, read);
            }
            in.close();
        } catch (IOException iox) {
            throw iox;
        } catch (Exception e) {
            throw new SignatureException(e.getMessage());
        }
    }

    public Date getCurrentDate(String certificateName, String password, String contentType) throws SignatureTimestampException, SignatureException, IOException {
        if (password == null) password = "";
        if (SigDebug.isActive()) {
            SigDebug.write("CAIBSigned.getCurrentDate: password " + (password.length() == 0 ? "is" : "is not") + " null.");
        }
        boolean recognized = properties.needsRecognizedCertificate(contentType);
        return getSigner(certificateName, contentType).getCurrentDate(certificateName, password, recognized);
    }

    public CAIBSigner(Map signerConfiguration) throws FileNotFoundException {
        CAIBSecurityManager.register();
        if (factory == null) {
            factory = ClassLoaderFactory.getFactory();
            realSigners = new LinkedHashMap();
        }
        try {
            if (Security.getProvider("BC") == null) {
                ClassLoader c = factory.getFactory().getMasterClassLoader();
                Provider p = (Provider) factory.getFactory().getMasterClassLoader().loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
                Security.addProvider(p);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            if (signerConfiguration == null) {
                properties = new SignaturaProperties();
            } else {
                properties = new SignaturaProperties(signerConfiguration);
            }
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
        if (SigDebug.getLevel() == SigDebug.DEBUG_LEVEL_NONE) {
            SigDebug.setLevel(properties.getProperty("debugLevel"));
        }
        if (System.getProperty("es.caib.provider.unactive") == null || System.getProperty("es.caib.provider.unactive").indexOf("tradise") == -1) {
            try {
                addTradiseSigner("tradise");
                SigDebug.write("[DBG] ACTIVAT TRADISE.");
            } catch (FileNotFoundException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
            File dirDev = new File(factory.getLibraryDir(), "tradise-dev");
            if (dirDev.isDirectory()) {
                try {
                    addTradiseSigner("tradise-dev");
                    SigDebug.write("[DBG] ACTIVAT TRADISE TEST.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (System.getProperty("es.caib.provider.unactive") == null || System.getProperty("es.caib.provider.unactive").indexOf("PKCS11") == -1) {
            String drivers[] = properties.getPKCS11Drivers();
            for (int i = 0; i < drivers.length; i++) {
                addPKCS11Signer(drivers[i]);
                SigDebug.write("[DBG] ACTIVAT PKCS11 " + drivers[i] + ".");
            }
        }
        if (System.getProperty("es.caib.provider.unactive") == null || System.getProperty("es.caib.provider.unactive").indexOf("MSCRYPTOAPI") == -1) {
            try {
                addMscryptoapiSigner();
                SigDebug.write("[DBG] ACTIVAT MSCRYPTOAPI.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (System.getProperty("es.caib.provider.unactive") == null || System.getProperty("es.caib.provider.unactive").indexOf("BC") == -1) {
            try {
                addBcCryptoApiSigner();
                SigDebug.write("[DBG] ACTIVAT BC.");
            } catch (Exception e) {
                if (e instanceof FileNotFoundException && System.getProperty("caib-crypto-keystore") == null) {
                    SigDebug.write("[DBG] The user has not a keystore file in the default location (<user.home>/.keystore).");
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addPKCS11Signer(String libraryName) {
        File f = new File(libraryName);
        if (f.isAbsolute()) {
            String name = f.getName();
            if (realSigners.get(name) == null) {
                addPKCS11Signer(name, f);
            }
        } else {
            if (realSigners.get(libraryName) == null) {
                String path = System.getProperty("java.library.path");
                StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
                while (tokenizer.hasMoreTokens()) {
                    File next = new File(tokenizer.nextToken());
                    f = new File(next, libraryName);
                    if (addPKCS11Signer(libraryName, f)) return;
                    f = new File(next, libraryName + ".dll");
                    if (addPKCS11Signer(libraryName, f)) return;
                }
            }
        }
    }

    private boolean addPKCS11Signer(String libraryName, File f) {
        if (f.canRead()) {
            String providerName = properties.getPKCS11DriversDescription(libraryName);
            try {
                File cfg = File.createTempFile(libraryName, "pkcs11.cfg");
                PrintWriter writer = new PrintWriter(new FileWriter(cfg));
                writer.println("name=" + libraryName);
                writer.println("library=" + f.getAbsolutePath());
                writer.close();
                cfg.deleteOnExit();
                Class c = factory.getMasterClassLoader().loadClass("es.caib.signatura.provider.impl.pkcs11.PKCS11Signer");
                Constructor constructor = c.getConstructor(new Class[] { String.class, String.class });
                realSigners.put(libraryName, constructor.newInstance(new Object[] { cfg.getAbsolutePath(), providerName }));
                return true;
            } catch (Throwable e) {
                return false;
            }
        } else return false;
    }

    private void addTradiseSigner(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException, FileNotFoundException {
        if (realSigners.get(name) == null) {
            ClassLoader loader = factory.getClassLoader(name);
            Class c = loader.loadClass("es.caib.signatura.provider.impl.tradise.TradiseSigner");
            realSigners.put(name, c.newInstance());
        }
    }

    public String getAPIVersion() {
        try {
            InputStream inputStream = CAIBSigner.class.getResourceAsStream("version.properties");
            if (inputStream == null) {
                throw new FileNotFoundException();
            }
            Properties tempProperties = new Properties();
            tempProperties.load(inputStream);
            inputStream.close();
            return tempProperties.getProperty("Version");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addBcCryptoApiSigner() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class c = factory.getMasterClassLoader().loadClass("es.caib.signatura.provider.impl.bccryptoapi.BcCryptoApiSigner");
        realSigners.put("bccryptoapi", c.newInstance());
        if (SigDebug.isActive()) SigDebug.write("CAIBSigner: addBcCryptoApiSigner: Keystore certificates loaded.");
    }

    /**
	 * Añade mscryptoapi como proveedor de firma de la CAIB. Sólo se hace si se puede cargar la biblioteca dinámica mscryptofunctions.dll,
	 * que debe estar en el directorio JAVA_HOME\lib\signaturacaib\ del PC con windows del usuario
	 * @author Jesus Reyes (3digits)
	 */
    private void addMscryptoapiSigner() {
        try {
            String libraryName = System.getProperty("java.home") + "\\lib\\signaturacaib\\mscryptofunctions.dll";
            if (SigDebug.isActive()) SigDebug.write("CAIBSigner: addMscryptoapiSigner: Path mscryptofunctions = " + libraryName);
            File f = new File(libraryName);
            if (f.canRead()) {
                Class c = factory.getMasterClassLoader().loadClass("es.caib.signatura.provider.impl.mscryptoapi.MscryptoapiSigner");
                realSigners.put("mscryptoapi", c.newInstance());
                if (SigDebug.isActive()) SigDebug.write("CAIBSigner: addMscryptoapiSigner: mscryptofunctions.dll loaded.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("mscryptofunctions.dll cannot be loaded.");
            return;
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

    public SMIMEParser getSMIMEParser(InputStream smime) throws InstantiationException, IllegalAccessException, IOException, SignatureException {
        return new SMIMEParserProxy(smime);
    }
}
