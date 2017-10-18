package es.eucm.eadventure.editor.control.security.jarsigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import sun.misc.BASE64Encoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ManifestDigester;
import sun.security.util.SignatureFileVerifier;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.NetscapeCertTypeExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertInfo;

/**
 * Class to sign JAR files.
 */
public class JSSigner {

    private static final String META_INF = "META-INF/";

    private static ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

    private static byte[] buffer = new byte[8192];

    private static final String SIG_PREFIX = META_INF + "SIG-";

    static String digestalg = "SHA1";

    static boolean signManifest = true;

    static X509Certificate[] certChain;

    /**
     * Main method to perform the signature program
     * 
     * @param param
     * @throws JarSignerException
     */
    public static void signJar(JSParameters param) throws JarSignerException {
        boolean aliasUsed = false;
        X509Certificate tsaCert = null;
        ZipFile zipFile = null;
        String sigfile = null;
        if (sigfile == null) {
            sigfile = param.getAlias();
            aliasUsed = true;
        }
        if (sigfile.length() > 8) {
            sigfile = sigfile.substring(0, 8).toUpperCase();
        } else {
            sigfile = sigfile.toUpperCase();
        }
        StringBuilder tmpSigFile = new StringBuilder(sigfile.length());
        for (int j = 0; j < sigfile.length(); j++) {
            char c = sigfile.charAt(j);
            if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '-') || (c == '_'))) {
                if (aliasUsed) {
                    c = '_';
                } else {
                    throw new JarSignerException("signature filename must consist of the following characters: A-Z, 0-9, _ or -");
                }
            }
            tmpSigFile.append(c);
        }
        sigfile = tmpSigFile.toString();
        String tmpJarName;
        if (param.getSignedJARName() == null) tmpJarName = param.getJarName() + ".sig"; else tmpJarName = param.getSignedJARName();
        File jarFile = new File(param.getJarName());
        File signedJarFile = new File(tmpJarName);
        try {
            String nombre = param.getJarName();
            zipFile = new ZipFile(nombre);
        } catch (IOException ioe) {
            throw new JarSignerException("unable to open jar file: " + param.getJarName());
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(signedJarFile);
        } catch (IOException ioe) {
            throw new JarSignerException("unable to create: " + tmpJarName);
        }
        PrintStream ps = new PrintStream(fos);
        ZipOutputStream zos = new ZipOutputStream(ps);
        String sfFilename = (META_INF + sigfile + ".SF").toUpperCase();
        String bkFilename = (META_INF + sigfile + ".DSA").toUpperCase();
        Manifest manifest = new Manifest();
        Map<String, Attributes> mfEntries = manifest.getEntries();
        Attributes oldAttr = null;
        boolean mfCreated = false;
        boolean mfModified = false;
        byte[] mfRawBytes = null;
        try {
            MessageDigest digests[] = { MessageDigest.getInstance(digestalg) };
            ZipEntry mfFile;
            if ((mfFile = getManifestFile(zipFile)) != null) {
                mfRawBytes = getBytes(zipFile, mfFile);
                manifest.read(new ByteArrayInputStream(mfRawBytes));
                oldAttr = (Attributes) (manifest.getMainAttributes().clone());
            } else {
                Attributes mattr = manifest.getMainAttributes();
                mattr.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
                String javaVendor = System.getProperty("java.vendor");
                String jdkVersion = System.getProperty("java.version");
                mattr.putValue("Created by", jdkVersion + " (" + javaVendor + ")");
                mfFile = new ZipEntry(JarFile.MANIFEST_NAME);
                mfCreated = true;
            }
            BASE64Encoder encoder = new JarBASE64Encoder();
            Vector<ZipEntry> mfFiles = new Vector<ZipEntry>();
            for (Enumeration<? extends ZipEntry> enum_ = zipFile.entries(); enum_.hasMoreElements(); ) {
                ZipEntry ze = enum_.nextElement();
                if (ze.getName().startsWith(META_INF)) {
                    mfFiles.addElement(ze);
                    if (signatureRelated(ze.getName())) {
                        continue;
                    }
                }
                if (manifest.getAttributes(ze.getName()) != null) {
                    if (updateDigests(ze, zipFile, digests, encoder, manifest) == true) {
                        mfModified = true;
                    }
                } else if (!ze.isDirectory()) {
                    Attributes attrs = getDigestAttributes(ze, zipFile, digests, encoder);
                    mfEntries.put(ze.getName(), attrs);
                    mfModified = true;
                }
            }
            if (mfModified) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                manifest.write(baos);
                byte[] newBytes = baos.toByteArray();
                if (mfRawBytes != null && oldAttr.equals(manifest.getMainAttributes())) {
                    mfRawBytes = newBytes;
                    int newPos = findHeaderEnd(newBytes);
                    int oldPos = findHeaderEnd(mfRawBytes);
                    if (newPos == oldPos) {
                        System.arraycopy(mfRawBytes, 0, newBytes, 0, oldPos);
                    } else {
                        byte[] lastBytes = new byte[oldPos + newBytes.length - newPos];
                        System.arraycopy(mfRawBytes, 0, lastBytes, 0, oldPos);
                        System.arraycopy(newBytes, newPos, lastBytes, oldPos, newBytes.length - newPos);
                        newBytes = lastBytes;
                    }
                }
                mfRawBytes = newBytes;
            }
            if (mfModified) {
                mfFile = new ZipEntry(JarFile.MANIFEST_NAME);
            }
            if (param.isVerbose()) {
                if (mfCreated) {
                    System.out.println((" adding: ") + mfFile.getName());
                } else if (mfModified) {
                    System.out.println((" updating: ") + mfFile.getName());
                }
            }
            zos.putNextEntry(mfFile);
            zos.write(mfRawBytes);
            ManifestDigester manDig = new ManifestDigester(mfRawBytes);
            SignatureFile sf = new SignatureFile(digests, manifest, manDig, sigfile, signManifest);
            SignatureFile.Block block = null;
            try {
                block = sf.generateBlock(null, certChain, true, null, tsaCert, null, param, zipFile);
            } catch (SocketTimeoutException e) {
                throw new JarSignerException("unable to sign jar: " + "no response from the Timestamping Authority. " + "When connecting from behind a firewall then an HTTP proxy may need to be specified. " + "Supply the following options to jarsigner: " + "\n  -J-Dhttp.proxyHost=<hostname> " + "\n  -J-Dhttp.proxyPort=<portnumber> ", e);
            } catch (InvalidKeyException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            } catch (UnrecoverableKeyException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            } catch (SignatureException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            } catch (CertificateException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            } catch (KeyStoreException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            } catch (JarSignerException e) {
                throw new JarSignerException("unable to sign jar: " + e.getMessage());
            }
            sfFilename = sf.getMetaName();
            bkFilename = block.getMetaName();
            ZipEntry sfFile = new ZipEntry(sfFilename);
            ZipEntry bkFile = new ZipEntry(bkFilename);
            long time = System.currentTimeMillis();
            sfFile.setTime(time);
            bkFile.setTime(time);
            zos.putNextEntry(sfFile);
            sf.write(zos);
            zos.putNextEntry(bkFile);
            block.write(zos);
            for (int i = 0; i < mfFiles.size(); i++) {
                ZipEntry ze = mfFiles.elementAt(i);
                if (!ze.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME) && !ze.getName().equalsIgnoreCase(sfFilename) && !ze.getName().equalsIgnoreCase(bkFilename)) {
                    writeEntry(zipFile, zos, ze);
                }
            }
            for (Enumeration<? extends ZipEntry> enum_ = zipFile.entries(); enum_.hasMoreElements(); ) {
                ZipEntry ze = enum_.nextElement();
                if (!ze.getName().startsWith(META_INF)) {
                    writeEntry(zipFile, zos, ze);
                }
            }
        } catch (IOException ioe) {
            throw new JarSignerException("unable to sign jar: " + ioe, ioe);
        } catch (NoSuchAlgorithmException e) {
            throw new JarSignerException("unable to sign jar: " + e, e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    throw new JarSignerException("Exception with zipFile");
                }
                zipFile = null;
            }
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    throw new JarSignerException("Exception with zipFile");
                }
            }
        }
        if (param.getSignedJARName() == null) {
            if (!signedJarFile.renameTo(jarFile)) {
                File origJar = new File(param.getJarName() + ".orig");
                if (jarFile.renameTo(origJar)) {
                    if (signedJarFile.renameTo(jarFile)) {
                        origJar.delete();
                    } else {
                        MessageFormat form = new MessageFormat("attempt to rename signedJarFile to jarFile failed");
                        Object[] source = { signedJarFile, jarFile };
                        throw new JarSignerException(form.format(source));
                    }
                } else {
                    MessageFormat form = new MessageFormat("attempt to rename jarFile to origJar failed");
                    Object[] source = { jarFile, origJar };
                    throw new JarSignerException(form.format(source));
                }
            }
        }
    }

    /**
     * Obtained the Manifest file
     * 
     * @param zf
     *            ZipFile
     * @return ZipEntry
     */
    private static ZipEntry getManifestFile(ZipFile zf) {
        ZipEntry ze = zf.getEntry(JarFile.MANIFEST_NAME);
        if (ze == null) {
            Enumeration<? extends ZipEntry> enum_ = zf.entries();
            while (enum_.hasMoreElements() && ze == null) {
                ze = enum_.nextElement();
                if (!JarFile.MANIFEST_NAME.equalsIgnoreCase(ze.getName())) {
                    ze = null;
                }
            }
        }
        return ze;
    }

    /**
     * Get Bytes from a file
     * 
     * @param zf
     *            ZipFile
     * @param ze
     *            ZipEntry
     * @return Array with bytes
     * @throws IOException
     */
    private static synchronized byte[] getBytes(ZipFile zf, ZipEntry ze) throws IOException {
        int n;
        InputStream is = null;
        try {
            is = zf.getInputStream(ze);
            baos.reset();
            long left = ze.getSize();
            while ((left > 0) && (n = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, n);
                left -= n;
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return baos.toByteArray();
    }

    /**
     * signature-related files include: . META-INF/MANIFEST.MF . META-INF/SIG-*
     * . META-INF/*.SF . META-INF/*.DSA . META-INF/*.RSA
     */
    private static boolean signatureRelated(String name) {
        String ucName = name.toUpperCase();
        if (ucName.equals(JarFile.MANIFEST_NAME) || ucName.equals(META_INF) || (ucName.startsWith(SIG_PREFIX) && ucName.indexOf("/") == ucName.lastIndexOf("/"))) {
            return true;
        }
        if (ucName.startsWith(META_INF) && SignatureFileVerifier.isBlockOrSF(ucName)) {
            return (ucName.indexOf("/") == ucName.lastIndexOf("/"));
        }
        return false;
    }

    private static boolean updateDigests(ZipEntry ze, ZipFile zf, MessageDigest[] digests, BASE64Encoder encoder, Manifest mf) throws IOException {
        boolean update = false;
        Attributes attrs = mf.getAttributes(ze.getName());
        String[] base64Digests = getDigests(ze, zf, digests, encoder);
        for (int i = 0; i < digests.length; i++) {
            String name = digests[i].getAlgorithm() + "-Digest";
            String mfDigest = attrs.getValue(name);
            if (mfDigest == null && digests[i].getAlgorithm().equalsIgnoreCase("SHA")) {
                mfDigest = attrs.getValue("SHA-Digest");
            }
            if (mfDigest == null) {
                attrs.putValue(name, base64Digests[i]);
                update = true;
            } else {
                if (!mfDigest.equalsIgnoreCase(base64Digests[i])) {
                    attrs.putValue(name, base64Digests[i]);
                    update = true;
                }
            }
        }
        return update;
    }

    private static synchronized String[] getDigests(ZipEntry ze, ZipFile zf, MessageDigest[] digests, BASE64Encoder encoder) throws IOException {
        int n, i;
        InputStream is = null;
        try {
            is = zf.getInputStream(ze);
            long left = ze.getSize();
            while ((left > 0) && (n = is.read(buffer, 0, buffer.length)) != -1) {
                for (i = 0; i < digests.length; i++) {
                    digests[i].update(buffer, 0, n);
                }
                left -= n;
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        String[] base64Digests = new String[digests.length];
        for (i = 0; i < digests.length; i++) {
            base64Digests[i] = encoder.encode(digests[i].digest());
        }
        return base64Digests;
    }

    private static Attributes getDigestAttributes(ZipEntry ze, ZipFile zf, MessageDigest[] digests, BASE64Encoder encoder) throws IOException {
        String[] base64Digests = getDigests(ze, zf, digests, encoder);
        Attributes attrs = new Attributes();
        for (int i = 0; i < digests.length; i++) {
            attrs.putValue(digests[i].getAlgorithm() + "-Digest", base64Digests[i]);
        }
        return attrs;
    }

    /**
     * Find the position of an empty line inside bs
     */
    private static int findHeaderEnd(byte[] bs) {
        if (bs.length > 1 && bs[0] == '\r' && bs[1] == '\n') {
            return 0;
        }
        for (int i = 0; i < bs.length - 3; i++) {
            if (bs[i] == '\r' && bs[i + 1] == '\n' && bs[i + 2] == '\r' && bs[i + 3] == '\n') {
                return i;
            }
        }
        return 0;
    }

    private static void writeEntry(ZipFile zf, ZipOutputStream os, ZipEntry ze) throws IOException {
        ZipEntry ze2 = new ZipEntry(ze.getName());
        ze2.setMethod(ze.getMethod());
        ze2.setTime(ze.getTime());
        ze2.setComment(ze.getComment());
        ze2.setExtra(ze.getExtra());
        if (ze.getMethod() == ZipEntry.STORED) {
            ze2.setSize(ze.getSize());
            ze2.setCrc(ze.getCrc());
        }
        os.putNextEntry(ze2);
        writeBytes(zf, ze, os);
    }

    /**
     * Writes all the bytes for a given entry to the specified output stream.
     */
    private static synchronized void writeBytes(ZipFile zf, ZipEntry ze, ZipOutputStream os) throws IOException {
        int n;
        InputStream is = null;
        try {
            is = zf.getInputStream(ze);
            long left = ze.getSize();
            while ((left > 0) && (n = is.read(buffer, 0, buffer.length)) != -1) {
                os.write(buffer, 0, n);
                left -= n;
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}

/**
 * This is a BASE64Encoder that does not insert a default newline at the end of
 * every output line. This is necessary because java.util.jar does its own line
 * management (see Manifest.make72Safe()). Inserting additional new lines can
 * cause line-wrapping problems (see CR 6219522).
 */
class JarBASE64Encoder extends BASE64Encoder {

    /**
     * Encode the suffix that ends every output line.
     */
    @Override
    protected void encodeLineSuffix(OutputStream aStream) throws IOException {
    }
}

class SignatureFile {

    /** SignatureFile */
    Manifest sf;

    /** .SF base name */
    String baseName;

    public SignatureFile(MessageDigest digests[], Manifest mf, ManifestDigester md, String baseName, boolean signManifest) {
        this.baseName = baseName;
        String version = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        sf = new Manifest();
        Attributes mattr = sf.getMainAttributes();
        BASE64Encoder encoder = new JarBASE64Encoder();
        mattr.putValue(Attributes.Name.SIGNATURE_VERSION.toString(), "1.0");
        mattr.putValue("Created-By", version + " (" + javaVendor + ")");
        if (signManifest) {
            for (int i = 0; i < digests.length; i++) {
                mattr.putValue(digests[i].getAlgorithm() + "-Digest-Manifest", encoder.encode(md.manifestDigest(digests[i])));
            }
        }
        ManifestDigester.Entry mde = md.get(ManifestDigester.MF_MAIN_ATTRS, false);
        if (mde != null) {
            for (int i = 0; i < digests.length; i++) {
                mattr.putValue(digests[i].getAlgorithm() + "-Digest-" + ManifestDigester.MF_MAIN_ATTRS, encoder.encode(mde.digest(digests[i])));
            }
        } else {
            throw new IllegalStateException("ManifestDigester failed to create " + "Manifest-Main-Attribute entry");
        }
        Map<String, Attributes> entries = sf.getEntries();
        Iterator<Map.Entry<String, Attributes>> mit = mf.getEntries().entrySet().iterator();
        while (mit.hasNext()) {
            Map.Entry<String, Attributes> e = mit.next();
            String name = e.getKey();
            mde = md.get(name, false);
            if (mde != null) {
                Attributes attr = new Attributes();
                for (int i = 0; i < digests.length; i++) {
                    attr.putValue(digests[i].getAlgorithm() + "-Digest", encoder.encode(mde.digest(digests[i])));
                }
                entries.put(name, attr);
            }
        }
    }

    /**
     * Writes the SignatureFile to the specified OutputStream.
     * 
     * @param out
     *            the output stream
     * @exception IOException
     *                if an I/O error has occurred
     */
    public void write(OutputStream out) throws IOException {
        sf.write(out);
    }

    /**
     * get .SF file name
     */
    public String getMetaName() {
        return "META-INF/" + baseName + ".SF";
    }

    /**
     * get base file name
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Check if userCert is designed to be a code signer
     * 
     * @param userCert
     *            the certificate to be examined
     * @param bad
     *            3 booleans to show if the KeyUsage, ExtendedKeyUsage,
     *            NetscapeCertType has codeSigning flag turned on. If null, the
     *            class field badKeyUsage, badExtendedKeyUsage,
     *            badNetscapeCertType will be set.
     */
    void checkCertUsage(X509Certificate userCert, boolean[] bad) {
        if (bad != null) {
            bad[0] = bad[1] = bad[2] = false;
        }
        boolean[] keyUsage = userCert.getKeyUsage();
        if (keyUsage != null) {
            if (keyUsage.length < 1 || !keyUsage[0]) {
                if (bad != null) {
                    bad[0] = true;
                } else {
                    System.out.println("badkeyusage ERROR - The signer certificate's KeyUsage extension doesn't allow code signing.");
                }
            }
        }
        try {
            List<String> xKeyUsage = userCert.getExtendedKeyUsage();
            if (xKeyUsage != null) {
                if (!xKeyUsage.contains("2.5.29.37.0") && !xKeyUsage.contains("1.3.6.1.5.5.7.3.3")) {
                    if (bad != null) {
                        bad[1] = true;
                    } else {
                        System.out.println("badExtendedKeyUsage ERROR - The signer certificate's ExtendedKeyUsage extension doesn't allow code signing. ");
                    }
                }
            }
        } catch (java.security.cert.CertificateParsingException e) {
            e.printStackTrace();
        }
        try {
            byte[] netscapeEx = userCert.getExtensionValue("2.16.840.1.113730.1.1");
            if (netscapeEx != null) {
                DerInputStream in = new DerInputStream(netscapeEx);
                byte[] encoded = in.getOctetString();
                encoded = new DerValue(encoded).getUnalignedBitString().toByteArray();
                NetscapeCertTypeExtension extn = new NetscapeCertTypeExtension(encoded);
                Boolean val = (Boolean) extn.get(NetscapeCertTypeExtension.OBJECT_SIGNING);
                if (!val) {
                    if (bad != null) {
                        bad[2] = true;
                    } else {
                        System.out.println("badNetscapeCertType ERROR - The signer certificate's NetscapeCertType extension doesn't allow code signing.");
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    public Block generateBlock(String sigalg, X509Certificate[] certChain, boolean externalSF, String tsaUrl, X509Certificate tsaCert, TimestampedSigner signingMechanism, JSParameters param, ZipFile zipFile) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, CertificateException, UnrecoverableKeyException, KeyStoreException, JarSignerException {
        long SIX_MONTHS = 180 * 24 * 60 * 60 * 1000L;
        java.security.cert.Certificate[] cs = null;
        try {
            cs = param.getKeyStore().getCertificateChain(param.getAlias());
        } catch (KeyStoreException kse) {
        }
        if (cs == null) {
            MessageFormat form = new MessageFormat(("Certificate chain not found for: alias.  alias must reference a valid KeyStore key entry containing a private key and corresponding public key certificate chain."));
            Object[] source = { param.getAlias(), param.getAlias() };
            throw new JarSignerException(form.format(source));
        }
        certChain = new X509Certificate[cs.length];
        for (int i = 0; i < cs.length; i++) {
            if (!(cs[i] instanceof X509Certificate)) {
                throw new JarSignerException(("found non-X.509 certificate in signer's chain"));
            }
            certChain[i] = (X509Certificate) cs[i];
        }
        X509Certificate userCert = (X509Certificate) param.getKeyStore().getCertificate(param.getAlias());
        try {
            userCert.checkValidity();
            if (userCert.getNotAfter().getTime() < System.currentTimeMillis() + SIX_MONTHS) {
                System.out.println("hasExpiringCert ERROR - This jar contains entries whose signer certificate will expire within six months. ");
            }
        } catch (CertificateExpiredException cee) {
            System.out.println("hasExpiredCert ERROR - The signer certificate has expired. ");
        } catch (CertificateNotYetValidException cnyve) {
            System.out.println("notYetValidCert ERROR - The signer certificate is not yet valid. ");
        }
        checkCertUsage(userCert, null);
        if (!userCert.equals(certChain[0])) {
            X509Certificate[] certChainTmp = new X509Certificate[certChain.length];
            certChainTmp[0] = userCert;
            Principal issuer = userCert.getIssuerDN();
            for (int i = 1; i < certChain.length; i++) {
                int j;
                for (j = 0; j < certChainTmp.length; j++) {
                    if (certChainTmp[j] == null) continue;
                    Principal subject = certChainTmp[j].getSubjectDN();
                    if (issuer.equals(subject)) {
                        certChain[i] = certChainTmp[j];
                        issuer = certChainTmp[j].getIssuerDN();
                        certChainTmp[j] = null;
                        break;
                    }
                }
                if (j == certChainTmp.length) {
                    throw new JarSignerException(("incomplete certificate chain"));
                }
            }
            certChain = certChainTmp;
        }
        PrivateKey privateKey = null;
        Key key = null;
        key = param.getKeyStore().getKey(param.getAlias(), param.getKeyPass());
        if (!(key instanceof PrivateKey)) {
            MessageFormat form = new MessageFormat(("key associated with alias not a private key"));
            Object[] source = { param.getAlias() };
            throw new JarSignerException(form.format(source));
        } else {
            privateKey = (PrivateKey) key;
        }
        return new Block(this, privateKey, sigalg, certChain, externalSF, tsaUrl, tsaCert, signingMechanism, param, zipFile);
    }

    public static class Block {

        private byte[] block;

        private String blockFileName;

        Block(SignatureFile sfg, PrivateKey privateKey, String sigalg, X509Certificate[] certChain, boolean externalSF, String tsaUrl, X509Certificate tsaCert, TimestampedSigner signingMechanism, JSParameters args, ZipFile zipFile) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, CertificateException, JarSignerException {
            Principal issuerName = certChain[0].getIssuerDN();
            if (!(issuerName instanceof X500Name)) {
                X509CertInfo tbsCert = new X509CertInfo(certChain[0].getTBSCertificate());
                issuerName = (Principal) tbsCert.get(CertificateIssuerName.NAME + "." + CertificateIssuerName.DN_NAME);
            }
            String digestAlgorithm;
            String signatureAlgorithm;
            String keyAlgorithm = privateKey.getAlgorithm();
            if (sigalg == null) {
                if (keyAlgorithm.equalsIgnoreCase("DSA")) digestAlgorithm = "SHA1"; else if (keyAlgorithm.equalsIgnoreCase("RSA")) digestAlgorithm = "SHA1"; else {
                    throw new JarSignerException("private key is not a DSA or " + "RSA key");
                }
                signatureAlgorithm = digestAlgorithm + "with" + keyAlgorithm;
            } else {
                signatureAlgorithm = sigalg;
            }
            String sigAlgUpperCase = signatureAlgorithm.toUpperCase();
            if ((sigAlgUpperCase.endsWith("WITHRSA") && !keyAlgorithm.equalsIgnoreCase("RSA")) || (sigAlgUpperCase.endsWith("WITHDSA") && !keyAlgorithm.equalsIgnoreCase("DSA"))) {
                throw new SignatureException("private key algorithm is not compatible with signature algorithm");
            }
            blockFileName = "META-INF/" + sfg.getBaseName() + "." + keyAlgorithm;
            Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initSign(privateKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sfg.write(baos);
            byte[] content = baos.toByteArray();
            sig.update(content);
            byte[] signature = sig.sign();
            if (signingMechanism == null) {
                signingMechanism = new TimestampedSigner();
            }
            URI tsaUri = null;
            try {
                if (tsaUrl != null) {
                    tsaUri = new URI(tsaUrl);
                }
            } catch (URISyntaxException e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }
            JarSignerParameters params = new JarSignerParameters(tsaUri, tsaCert, signature, signatureAlgorithm, certChain, content, zipFile);
            block = signingMechanism.generateSignedData(params, externalSF, (tsaUrl != null || tsaCert != null));
        }

        public String getMetaName() {
            return blockFileName;
        }

        /**
         * Writes the block file to the specified OutputStream.
         * 
         * @param out
         *            the output stream
         * @exception IOException
         *                if an I/O error has occurred
         */
        public void write(OutputStream out) throws IOException {
            out.write(block);
        }
    }
}

class JarSignerParameters {

    private URI tsa;

    private X509Certificate tsaCertificate;

    private byte[] signature;

    private String signatureAlgorithm;

    private X509Certificate[] signerCertificateChain;

    private byte[] content;

    private ZipFile source;

    /**
     * Create a new object.
     */
    JarSignerParameters(URI tsa, X509Certificate tsaCertificate, byte[] signature, String signatureAlgorithm, X509Certificate[] signerCertificateChain, byte[] content, ZipFile source) {
        if (signature == null || signatureAlgorithm == null || signerCertificateChain == null) {
            throw new NullPointerException();
        }
        this.tsa = tsa;
        this.tsaCertificate = tsaCertificate;
        this.signature = signature;
        this.signatureAlgorithm = signatureAlgorithm;
        this.signerCertificateChain = signerCertificateChain;
        this.content = content;
        this.source = source;
    }

    /**
     * Retrieves the identifier for a Timestamping Authority (TSA).
     * 
     * @return The TSA identifier. May be null.
     */
    public URI getTimestampingAuthority() {
        return tsa;
    }

    /**
     * Retrieves the certificate for a Timestamping Authority (TSA).
     * 
     * @return The TSA certificate. May be null.
     */
    public X509Certificate getTimestampingAuthorityCertificate() {
        return tsaCertificate;
    }

    /**
     * Retrieves the signature.
     * 
     * @return The non-null signature bytes.
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Retrieves the name of the signature algorithm.
     * 
     * @return The non-null string name of the signature algorithm.
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Retrieves the signer's X.509 certificate chain.
     * 
     * @return The non-null array of X.509 public-key certificates.
     */
    public X509Certificate[] getSignerCertificateChain() {
        return signerCertificateChain;
    }

    /**
     * Retrieves the content that was signed.
     * 
     * @return The content bytes. May be null.
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Retrieves the original source ZIP file before it was signed.
     * 
     * @return The original ZIP file. May be null.
     */
    public ZipFile getSource() {
        return source;
    }
}
