package org.hardtokenmgmt.hosting.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.util.encoders.Base64;

/**
 * Utility that signs a jar with a given key store. Much of the code is inspired by a great
 * article by Raffi Krikorian published by ONJava.com.
 * 
 * 
 *
 * @version $Id$
 */
@SuppressWarnings(value = { "all" })
public class JarSigner {

    private String alias;

    private KeyStore ks;

    private char[] ksPwd;

    public JarSigner(KeyStore ks, String alias, char[] ksPwd) {
        this.alias = alias;
        this.ks = ks;
        this.ksPwd = ksPwd;
    }

    /** 
     * Retrieve the manifest from a jar file -- this will either
	 * load a pre-existing META-INF/MANIFEST.MF, or create a new
	 * one 
	 */
    private static Manifest getManifestFile(JarInputStream jarInputStream) throws IOException {
        JarEntry je = null;
        while ((je = jarInputStream.getNextJarEntry()) != null) {
            if ("META-INF/MANIFEST.MF".equalsIgnoreCase(je.getName())) {
                break;
            } else {
                je = null;
            }
        }
        Manifest manifest = new Manifest();
        if (je != null) {
            manifest.read(jarInputStream);
        }
        return manifest;
    }

    /** Make sure that the manifest entries are ready for the signed
	 * JAR manifest file. if we already have a manifest, then we
	 * make sure that all the elements are valid. if we do not
	 * have a manifest, then we create a new signed JAR manifest
	 * file by adding the appropriate headers. */
    private static Map<String, Attributes> createEntries(Manifest manifest, JarInputStream jarInputStream) throws IOException {
        Map<String, Attributes> entries = null;
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        attributes.putValue("Created-By", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        entries = manifest.getEntries();
        return entries;
    }

    /**
	 *  Helper function to update the digest
	 */
    private static String updateDigest(MessageDigest digest, InputStream inputStream) throws IOException {
        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        return new String(Base64.encode(digest.digest()));
    }

    /** Update the attributes in the manifest to have the
	 * appropriate message digests. we store the new entries into
	 * the entries Map and return it (we do not compute the digests
	 * for those entries in the META-INF directory). */
    private static Map<?, ?> updateManifestDigest(Manifest manifest, JarInputStream jarInputStream, MessageDigest messageDigest, Map<String, Attributes> entries) throws IOException {
        JarEntry jarEntry = null;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            try {
                if (jarEntry.getName().startsWith("META-INF")) {
                    continue;
                } else if (manifest.getAttributes(jarEntry.getName()) != null) {
                    Attributes attributes = manifest.getAttributes(jarEntry.getName());
                    attributes.putValue("SHA1-Digest", updateDigest(messageDigest, jarInputStream));
                } else if (!jarEntry.isDirectory()) {
                    Attributes attributes = new Attributes();
                    attributes.putValue("SHA1-Digest", updateDigest(messageDigest, jarInputStream));
                    entries.put(jarEntry.getName(), attributes);
                }
            } finally {
                jarInputStream.closeEntry();
            }
        }
        return entries;
    }

    /** A small helper function that will convert a manifest into an
	 * array of bytes. */
    private byte[] serialiseManifest(Manifest manifest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        manifest.write(baos);
        baos.flush();
        baos.close();
        return baos.toByteArray();
    }

    /** create a signature file object out of the manifest and the
	 * message digest. */
    private SignatureFile createSignatureFile(Manifest manifest, MessageDigest messageDigest) throws IOException {
        ManifestDigester manifestDigester = new ManifestDigester(serialiseManifest(manifest));
        return new SignatureFile(new MessageDigest[] { messageDigest }, manifest, manifestDigester, this.alias, true);
    }

    /** a helper function that can take entries from one jar file and
	 * write it to another jar stream. */
    private static void writeJarEntry(JarEntry je, JarInputStream jarInputStream, JarOutputStream jos) throws IOException {
        jos.putNextEntry(je);
        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = jarInputStream.read(buffer)) > 0) {
            jos.write(buffer, 0, read);
        }
        jos.closeEntry();
    }

    /** the actual JAR signing method -- this is the method which
	 * will be called by those wrapping the JARSigner class.
	 */
    public void signJarFile(byte[] jarData, OutputStream outputStream) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchProviderException, CertStoreException, InvalidAlgorithmParameterException, CMSException {
        Manifest manifest = getManifestFile(new JarInputStream(new ByteArrayInputStream(jarData.clone())));
        Map<String, Attributes> entries = createEntries(manifest, new JarInputStream(new ByteArrayInputStream(jarData.clone())));
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        updateManifestDigest(manifest, new JarInputStream(new ByteArrayInputStream(jarData.clone())), messageDigest, entries);
        SignatureFile signatureFile = createSignatureFile(manifest, messageDigest);
        SignatureFile.Block block = signatureFile.generateBlock((PrivateKey) ks.getKey(alias, ksPwd), getCertChain());
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
        JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(jarData.clone()));
        JarEntry entry = null;
        while ((entry = jarInputStream.getNextJarEntry()) != null) {
            if (entry.getName().startsWith("META-INF") && !(manifestFileName.equalsIgnoreCase(entry.getName()) || signatureFileName.equalsIgnoreCase(entry.getName()) || signatureBlockName.equalsIgnoreCase(entry.getName()))) {
                writeJarEntry(entry, jarInputStream, jos);
            }
        }
        jarInputStream = new JarInputStream(new ByteArrayInputStream(jarData.clone()));
        entry = null;
        while ((entry = jarInputStream.getNextJarEntry()) != null) {
            if (!entry.getName().startsWith("META-INF")) {
                writeJarEntry(entry, jarInputStream, jos);
            }
        }
        jos.flush();
        jos.finish();
        jarInputStream.close();
    }

    private X509Certificate[] certchain = null;

    private X509Certificate[] getCertChain() throws KeyStoreException {
        if (certchain == null) {
            Certificate[] certs = ks.getCertificateChain(alias);
            certchain = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; i++) {
                certchain[i] = (X509Certificate) certs[i];
            }
        }
        return certchain;
    }
}
