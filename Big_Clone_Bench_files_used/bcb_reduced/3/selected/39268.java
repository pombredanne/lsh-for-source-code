package fipa.adst.util.compiler;

import jade.util.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class SimpleCompiler {

    /**
	 * Main class of the self-protected agents compiler.
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        SimpleCompiler compiler = new SimpleCompiler();
        switch(args.length) {
            case 0:
                System.err.println("No arguments provided.\n");
                System.err.println("Syntax:  Compiler [path] target.jar \n");
                break;
            case 1:
                if (args[0].endsWith(".jar")) {
                    compiler.compile(System.getProperty("user.dir"), args[0].trim());
                } else {
                    System.err.println("Bad argument.\n");
                    System.err.println("Syntax:  Compiler [path] target.jar \n");
                }
                break;
            case 2:
                if (args[1].endsWith(".jar")) {
                    File directory = new File(args[0].trim());
                    if (directory.isDirectory()) {
                        compiler.compile(args[0].trim(), args[1].trim());
                    } else {
                        System.err.println("The path provided is incorrect.\n");
                    }
                } else {
                    System.err.println("Bad argument.\n");
                    System.err.println("Syntax:  Compiler [path] target.jar \n");
                }
                break;
        }
    }

    private void compile(String path, String target) {
        String sourceJarFile = path + File.separatorChar + PATH_CODE_MANAGER + File.separatorChar + NAME_CODE_MANAGER;
        String destJarFile = path + File.separatorChar + target;
        String certificateJarPath = PATH_CERTIFICATE + File.separatorChar + NAME_CERTIFICATE;
        String codesPath = path + File.separatorChar + PATH_CODES;
        String codesJarPath = PATH_CODES;
        String itineraryPath = path + File.separatorChar + PATH_ITINERARY + File.separatorChar + NAME_ITINERARY;
        String itineraryJarPath = PATH_ITINERARY + File.separatorChar + NAME_ITINERARY;
        PrivateKey privKey;
        ZipEntry ze;
        byte[] hash, signature, ciphered;
        FileInputStream fis;
        FileOutputStream fos;
        JarOutputStream jos;
        JarInputStream jis;
        try {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("Creating disposable key pair...");
            KeyPair pair = createKeyPair();
            if (pair != null) {
                privKey = pair.getPrivate();
                X509Certificate cert = createX509Certificate(pair);
                System.out.println("Calculating code manager hash...");
                fis = new FileInputStream(sourceJarFile);
                FileInputStream itIS = new FileInputStream(itineraryPath);
                hash = calculateHash(fis, itIS, cert);
                fis.close();
                itIS.close();
                fos = new FileOutputStream(destJarFile);
                jos = new JarOutputStream(fos);
                System.out.println("Adding code manager to the agent Jar...");
                copyJarEntries(sourceJarFile, jos);
                System.out.println("Adding disposable public key and certificate...");
                ze = new ZipEntry(certificateJarPath);
                jos.putNextEntry(ze);
                ByteArrayInputStream bais = new ByteArrayInputStream(cert.getEncoded());
                copyIStoOS(bais, jos, 4096);
                jos.closeEntry();
                bais.close();
                System.out.println("Adding agent itinerary...");
                ze = new ZipEntry(itineraryJarPath);
                jos.putNextEntry(ze);
                fis = new FileInputStream(itineraryPath);
                copyIStoOS(fis, jos, 4096);
                jos.closeEntry();
                fis.close();
                System.out.println("Processing each agent task...");
                File codeFolder = new File(codesPath);
                File[] files;
                String fileStr, pubKeyPath;
                files = codeFolder.listFiles();
                int pos;
                ByteArrayOutputStream baos;
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
                System.out.println("Securing tasks...");
                for (int i = 0; i < files.length; i++) {
                    fileStr = files[i].getName();
                    if (files[i].isFile() && fileStr.endsWith(PLAIN_CODE_EXTENSION)) {
                        pos = fileStr.lastIndexOf(PLAIN_CODE_EXTENSION);
                        fileStr = fileStr.substring(0, pos);
                        fis = new FileInputStream(files[i]);
                        baos = new ByteArrayOutputStream();
                        copyIStoOS(fis, baos, 4096);
                        signature = sign(baos.toByteArray(), privKey, cert, true);
                        pubKeyPath = codesPath + File.separatorChar + fileStr + PUBLIC_KEY_EXTENSION;
                        fis = new FileInputStream(pubKeyPath);
                        X509Certificate hostCert = (X509Certificate) certFactory.generateCertificate(fis);
                        ciphered = cipher(signature, hash, hostCert);
                        signature = null;
                        fis.close();
                        ze = new ZipEntry(codesJarPath + File.separatorChar + fileStr + CIPHERED_CODE_EXTENSION);
                        jos.putNextEntry(ze);
                        bais = new ByteArrayInputStream(ciphered);
                        copyIStoOS(bais, jos, 4096);
                        jos.closeEntry();
                        bais.close();
                        bais = null;
                        System.out.println("Task secured: " + files[i].getName());
                    }
                }
                System.out.println("Agent compiled.");
                jos.close();
                fos.close();
            }
        } catch (FileNotFoundException fnfe) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + fnfe);
        } catch (CertificateEncodingException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (InvalidKeyException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (IllegalStateException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (NoSuchProviderException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (NoSuchAlgorithmException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (SignatureException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (IOException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: " + e);
        } catch (CertificateException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: External certificate: " + e);
        } catch (CMSException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: Cipher: " + e);
        }
    }

    private void copyJarEntries(String sourceJarFile, JarOutputStream jos) throws IOException {
        FileInputStream fis = new FileInputStream(sourceJarFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry zeSrc, zeDst;
        while ((zeSrc = zis.getNextEntry()) != null) {
            zeDst = new ZipEntry(zeSrc.getName());
            jos.putNextEntry(zeDst);
            copyIStoOS(zis, jos, 4096);
            zis.closeEntry();
            jos.closeEntry();
        }
        zis.close();
        fis.close();
    }

    public byte[] sign(byte[] data, PrivateKey key, X509Certificate cert, boolean includeData) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, CMSException {
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        generator.addSigner(key, cert, CMSSignedDataGenerator.DIGEST_SHA256);
        CMSProcessable content = new CMSProcessableByteArray(data);
        CMSSignedData signedData = generator.generate(content, includeData, "BC");
        byte[] result = signedData.getEncoded();
        return result;
    }

    private byte[] cipher(byte[] data, byte[] hashCodeManager, X509Certificate cert) throws NoSuchAlgorithmException, NoSuchProviderException, CMSException, IOException {
        byte[] dataToCipher = new byte[hashCodeManager.length + data.length];
        System.arraycopy(hashCodeManager, 0, dataToCipher, 0, hashCodeManager.length);
        System.arraycopy(data, 0, dataToCipher, hashCodeManager.length, data.length);
        CMSEnvelopedDataGenerator gen = new CMSEnvelopedDataGenerator();
        gen.addKeyTransRecipient(cert);
        CMSProcessable dataToProcess = new CMSProcessableByteArray(dataToCipher);
        CMSEnvelopedData envelopedData = gen.generate(dataToProcess, CMSEnvelopedDataGenerator.AES128_CBC, "BC");
        return envelopedData.getEncoded();
    }

    private byte[] calculateHash(InputStream jarIS, InputStream itIS, X509Certificate cert) throws CertificateEncodingException, IOException, NoSuchAlgorithmException {
        JarInputStream jis = new JarInputStream(jarIS);
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        ZipEntry ze;
        int readed = 0;
        byte[] buffer = new byte[4096];
        byte[] finalDigest = new byte[HASH_ALGORITHM_LENGTH / 8];
        while ((readed = itIS.read(buffer)) >= 0) md.update(buffer, 0, readed);
        finalDigest = xor(finalDigest, md.digest());
        md.update(cert.getEncoded());
        finalDigest = xor(finalDigest, md.digest());
        while ((ze = jis.getNextEntry()) != null) {
            if ((!ze.isDirectory()) && (!ze.getName().endsWith(".scode"))) {
                md.reset();
                while ((readed = jis.read(buffer)) >= 0) md.update(buffer, 0, readed);
                finalDigest = xor(finalDigest, md.digest());
            }
        }
        return finalDigest;
    }

    private byte[] xor(byte[] first, byte[] second) {
        byte[] result = new byte[first.length];
        for (int i = 0; i < first.length; i++) {
            result[i] = (byte) (first[i] ^ second[i]);
        }
        return result;
    }

    /**
	 * Method to create a self-signed X.509 certificate.  
	 * @param keyPair Public key to include in the certificate and private key to sign it.
	 * @return Generated certificate. 
	 * @throws SignatureException 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchProviderException 
	 * @throws IllegalStateException 
	 * @throws InvalidKeyException 
	 * @throws CertificateEncodingException 
	 */
    private X509Certificate createX509Certificate(KeyPair keyPair) throws CertificateEncodingException, InvalidKeyException, IllegalStateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X509Name("CN=SelfSignedCertificate"));
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 31536000000L));
        certGen.setSubjectDN(new X509Name("CN=SelfSignedCertificate"));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.rfc822Name, "selfprotectedagent@deic.uab.cat")));
        return certGen.generate(keyPair.getPrivate(), "BC");
    }

    private KeyPair createKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        SecureRandom random;
        random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(2048, random);
        return keyGen.generateKeyPair();
    }

    private void copyIStoOS(InputStream is, OutputStream os, int bufSize) {
        byte[] buffer = new byte[bufSize];
        int len;
        try {
            while ((len = is.read(buffer)) >= 0) os.write(buffer, 0, len);
        } catch (IOException ioe) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "Compiler: CopyIStoOS: " + ioe);
        }
    }

    private static final String PATH_CODE_MANAGER = "code-manager";

    private static final String NAME_CODE_MANAGER = "cm.jar";

    private static final String PATH_CERTIFICATE = "conf";

    private static final String NAME_CERTIFICATE = "agent.cert";

    private static final String PATH_CODES = "codes";

    private static final String PATH_ITINERARY = "conf";

    private static final String NAME_ITINERARY = "itinerary.conf";

    private static final String PLAIN_CODE_EXTENSION = ".jar";

    private static final String CIPHERED_CODE_EXTENSION = ".scode";

    private static final String PUBLIC_KEY_EXTENSION = ".cert";

    private static final String HASH_ALGORITHM = "sha-256";

    private static final int HASH_ALGORITHM_LENGTH = 256;

    private static final int MAX_SIZE_TO_ENCRYPT = 245;

    Logger _logger = Logger.getMyLogger(getClass().getName());

    ;
}
