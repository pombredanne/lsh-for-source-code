package edu.upmc.opi.caBIG.caTIES.common;

import gov.nih.nci.cagrid.gridca.common.CertUtil;
import gov.nih.nci.cagrid.gridca.common.KeyUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import org.bouncycastle.asn1.x509.X509Name;
import sun.misc.BASE64Encoder;
import sun.security.util.ManifestDigester;

public class CaTIES_JARSigner {

    private String alias;

    private PrivateKey privateKey;

    private X509Certificate[] certChain;

    public CaTIES_JARSigner(String alias, PrivateKey privateKey, X509Certificate[] certChain) {
        this.alias = alias;
        this.privateKey = privateKey;
        this.certChain = certChain;
    }

    public static Manifest getExistingManifestFile(JarFile jarFile) {
        JarEntry je = jarFile.getJarEntry("META-INF/MANIFEST.MF");
        if (je != null) {
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                je = (JarEntry) entries.nextElement();
                if ("META-INF/MANIFEST.MF".equalsIgnoreCase(je.getName())) break; else je = null;
            }
        }
        Manifest manifest = new Manifest();
        if (je != null) {
            try {
                InputStream is = jarFile.getInputStream(je);
                manifest.read(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return manifest;
    }

    public static Manifest getManifestFile(JarFile jarFile) {
        Manifest manifest = new Manifest();
        return manifest;
    }

    private static Map pruneManifest(Manifest manifest, JarFile jarFile) throws IOException {
        Map map = manifest.getEntries();
        Iterator elements = map.keySet().iterator();
        while (elements.hasNext()) {
            String element = (String) elements.next();
            if (jarFile.getEntry(element) == null) elements.remove();
        }
        return map;
    }

    private static Map createEntries(Manifest manifest, JarFile jarFile) throws IOException {
        Map entries = null;
        if (manifest.getEntries().size() > 0) entries = pruneManifest(manifest, jarFile); else {
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            attributes.putValue("Created-By", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
            entries = manifest.getEntries();
        }
        return entries;
    }

    private static BASE64Encoder b64Encoder = new BASE64Encoder();

    private static String updateDigest(MessageDigest digest, InputStream inputStream) throws IOException {
        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) digest.update(buffer, 0, read);
        inputStream.close();
        return b64Encoder.encode(digest.digest());
    }

    private static Map updateManifestDigest(Manifest manifest, JarFile jarFile, MessageDigest messageDigest, Map entries) throws IOException {
        Enumeration jarElements = jarFile.entries();
        while (jarElements.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) jarElements.nextElement();
            if (jarEntry.getName().startsWith("META-INF")) continue; else if (manifest.getAttributes(jarEntry.getName()) != null) {
                Attributes attributes = manifest.getAttributes(jarEntry.getName());
                attributes.putValue("SHA1-Digest", updateDigest(messageDigest, jarFile.getInputStream(jarEntry)));
            } else if (!jarEntry.isDirectory()) {
                Attributes attributes = new Attributes();
                attributes.putValue("SHA1-Digest", updateDigest(messageDigest, jarFile.getInputStream(jarEntry)));
                entries.put(jarEntry.getName(), attributes);
            }
        }
        return entries;
    }

    private byte[] serialiseManifest(Manifest manifest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        manifest.write(baos);
        baos.flush();
        baos.close();
        return baos.toByteArray();
    }

    private CaTIES_SignatureFile createSignatureFile(Manifest manifest, MessageDigest messageDigest) throws IOException {
        ManifestDigester manifestDigester = new ManifestDigester(serialiseManifest(manifest));
        return new CaTIES_SignatureFile(new MessageDigest[] { messageDigest }, manifest, manifestDigester, this.alias, true);
    }

    private static void writeJarEntry(JarEntry je, JarFile jarFile, JarOutputStream jos) throws IOException {
        jos.putNextEntry(je);
        byte[] buffer = new byte[2048];
        int read = 0;
        InputStream is = jarFile.getInputStream(je);
        while ((read = is.read(buffer)) > 0) jos.write(buffer, 0, read);
        jos.closeEntry();
    }

    public void signJarFile(JarFile jarFile, OutputStream outputStream) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, CertificateException {
        Manifest manifest = getManifestFile(jarFile);
        Map entries = createEntries(manifest, jarFile);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        updateManifestDigest(manifest, jarFile, messageDigest, entries);
        CaTIES_SignatureFile signatureFile = createSignatureFile(manifest, messageDigest);
        CaTIES_SignatureFile.Block block = signatureFile.generateBlock(privateKey, certChain, true);
        String manifestFileName = "META-INF/MANIFEST.MF";
        JarOutputStream jos = new JarOutputStream(outputStream);
        JarEntry manifestFile = new JarEntry(manifestFileName);
        jos.putNextEntry(manifestFile);
        byte manifestBytes[] = serialiseManifest(manifest);
        jos.write(manifestBytes, 0, manifestBytes.length);
        jos.closeEntry();
        String signatureFileName = signatureFile.getMetaName();
        JarEntry signatureFileEntry = new JarEntry(signatureFileName);
        jos.putNextEntry(signatureFileEntry);
        signatureFile.write(jos);
        jos.closeEntry();
        String signatureBlockName = block.getMetaName();
        JarEntry signatureBlockEntry = new JarEntry(signatureBlockName);
        jos.putNextEntry(signatureBlockEntry);
        block.write(jos);
        jos.closeEntry();
        Enumeration metaEntries = jarFile.entries();
        while (metaEntries.hasMoreElements()) {
            JarEntry metaEntry = (JarEntry) metaEntries.nextElement();
            if (metaEntry.getName().startsWith("META-INF") && !(manifestFileName.equalsIgnoreCase(metaEntry.getName()) || signatureFileName.equalsIgnoreCase(metaEntry.getName()) || signatureBlockName.equalsIgnoreCase(metaEntry.getName()) || metaEntry.getName().toLowerCase().endsWith(".dsa") || metaEntry.getName().toLowerCase().endsWith(".sf"))) {
                writeJarEntry(metaEntry, jarFile, jos);
            }
        }
        Enumeration allEntries = jarFile.entries();
        ArrayList duplicatesList = new ArrayList();
        while (allEntries.hasMoreElements()) {
            JarEntry entry = (JarEntry) allEntries.nextElement();
            if (!entry.getName().startsWith("META-INF") && !duplicatesList.contains(entry.getName())) {
                writeJarEntry(entry, jarFile, jos);
                duplicatesList.add(entry.getName());
            }
        }
        jos.flush();
        jos.finish();
        jarFile.close();
    }

    /**
	 * @param args
	 */
    public static void testKeyTool(String[] args) {
        try {
            FileInputStream fileIn = new FileInputStream("keytoolgenerated.keystore");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            char[] password = { 'p', 'a', 's', 's', 'w', 'd' };
            keyStore.load(fileIn, password);
            Certificate[] chain = keyStore.getCertificateChain("alias");
            X509Certificate certChain[] = new X509Certificate[chain.length];
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (int count = 0; count < chain.length; count++) {
                ByteArrayInputStream certIn = new ByteArrayInputStream(chain[0].getEncoded());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certIn);
                certChain[count] = cert;
            }
            Key key = keyStore.getKey("alias", password);
            KeyFactory keyFactory = KeyFactory.getInstance(key.getAlgorithm());
            KeySpec keySpec = keyFactory.getKeySpec(key, DSAPrivateKeySpec.class);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            CaTIES_JARSigner jarSigner = new CaTIES_JARSigner("alias", privateKey, certChain);
            JarFile jarFile = new JarFile("MyJar.jar");
            OutputStream outStream = new FileOutputStream("MySignedJar.jar");
            jarSigner.signJarFile(jarFile, outStream);
            fileIn.close();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            KeyPair caKeyPair;
            caKeyPair = KeyUtil.generateRSAKeyPair1024();
            PrivateKey privateKey = caKeyPair.getPrivate();
            PublicKey publicKey = caKeyPair.getPublic();
            GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            date.add(Calendar.MINUTE, -5);
            Date start = new Date(date.getTimeInMillis());
            date.add(Calendar.MINUTE, 5);
            date.add(Calendar.DAY_OF_MONTH, 365);
            Date end = new Date(date.getTimeInMillis());
            X509Certificate caCertificate = CertUtil.generateCACertificate(new X509Name("O=UPMC,CN=fred"), start, end, caKeyPair, 10);
            X509Certificate[] certChain = { caCertificate };
            CaTIES_JARSigner jarSigner = new CaTIES_JARSigner("alias", privateKey, certChain);
            JarFile jarFile = new JarFile("jdom-1.0.jar");
            OutputStream outStream = new FileOutputStream("kevin.jar");
            jarSigner.signJarFile(jarFile, outStream);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
