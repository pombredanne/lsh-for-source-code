package com.android.sdklib.internal.build;

import com.android.sdklib.internal.build.SignedJarBuilder.IZipEntryFilter.ZipAbortException;
import sun.misc.BASE64Encoder;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A Jar file builder with signature support.
 */
public class SignedJarBuilder {

    private static final String DIGEST_ALGORITHM = "SHA1";

    private static final String DIGEST_ATTR = "SHA1-Digest";

    private static final String DIGEST_MANIFEST_ATTR = "SHA1-Digest-Manifest";

    /** Write to another stream and also feed it to the Signature object. */
    private static class SignatureOutputStream extends FilterOutputStream {

        private Signature mSignature;

        private int mCount = 0;

        public SignatureOutputStream(OutputStream out, Signature sig) {
            super(out);
            mSignature = sig;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                mSignature.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            super.write(b);
            mCount++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                mSignature.update(b, off, len);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            super.write(b, off, len);
            mCount += len;
        }

        public int size() {
            return mCount;
        }
    }

    private JarOutputStream mOutputJar;

    private PrivateKey mKey;

    private X509Certificate mCertificate;

    private Manifest mManifest;

    private BASE64Encoder mBase64Encoder;

    private MessageDigest mMessageDigest;

    private byte[] mBuffer = new byte[4096];

    /**
     * Classes which implement this interface provides a method to check whether a file should
     * be added to a Jar file.
     */
    public interface IZipEntryFilter {

        /**
         * An exception thrown during packaging of a zip file into APK file.
         * This is typically thrown by implementations of
         * {@link IZipEntryFilter#checkEntry(String)}.
         */
        public static class ZipAbortException extends Exception {

            private static final long serialVersionUID = 1L;

            public ZipAbortException() {
                super();
            }

            public ZipAbortException(String format, Object... args) {
                super(String.format(format, args));
            }

            public ZipAbortException(Throwable cause, String format, Object... args) {
                super(String.format(format, args), cause);
            }

            public ZipAbortException(Throwable cause) {
                super(cause);
            }
        }

        /**
         * Checks a file for inclusion in a Jar archive.
         * @param archivePath the archive file path of the entry
         * @return <code>true</code> if the file should be included.
         * @throws ZipAbortException if writing the file should be aborted.
         */
        public boolean checkEntry(String archivePath) throws ZipAbortException;
    }

    /**
     * Creates a {@link SignedJarBuilder} with a given output stream, and signing information.
     * <p/>If either <code>key</code> or <code>certificate</code> is <code>null</code> then
     * the archive will not be signed.
     * @param out the {@link OutputStream} where to write the Jar archive.
     * @param key the {@link PrivateKey} used to sign the archive, or <code>null</code>.
     * @param certificate the {@link X509Certificate} used to sign the archive, or
     * <code>null</code>.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public SignedJarBuilder(OutputStream out, PrivateKey key, X509Certificate certificate) throws IOException, NoSuchAlgorithmException {
        mOutputJar = new JarOutputStream(out);
        mOutputJar.setLevel(9);
        mKey = key;
        mCertificate = certificate;
        if (mKey != null && mCertificate != null) {
            mManifest = new Manifest();
            Attributes main = mManifest.getMainAttributes();
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android)");
            mBase64Encoder = new BASE64Encoder();
            mMessageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        }
    }

    /**
     * Writes a new {@link File} into the archive.
     * @param inputFile the {@link File} to write.
     * @param jarPath the filepath inside the archive.
     * @throws IOException
     */
    public void writeFile(File inputFile, String jarPath) throws IOException {
        FileInputStream fis = new FileInputStream(inputFile);
        try {
            JarEntry entry = new JarEntry(jarPath);
            entry.setTime(inputFile.lastModified());
            writeEntry(fis, entry);
        } finally {
            fis.close();
        }
    }

    /**
     * Copies the content of a Jar/Zip archive into the receiver archive.
     * <p/>An optional {@link IZipEntryFilter} allows to selectively choose which files
     * to copy over.
     * @param input the {@link InputStream} for the Jar/Zip to copy.
     * @param filter the filter or <code>null</code>
     * @throws IOException
     * @throws ZipAbortException if the {@link IZipEntryFilter} filter indicated that the write
     *                           must be aborted.
     */
    public void writeZip(InputStream input, IZipEntryFilter filter) throws IOException, ZipAbortException {
        ZipInputStream zis = new ZipInputStream(input);
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || name.startsWith("META-INF/")) {
                    continue;
                }
                if (filter != null && filter.checkEntry(name) == false) {
                    continue;
                }
                JarEntry newEntry;
                if (entry.getMethod() == JarEntry.STORED) {
                    newEntry = new JarEntry(entry);
                } else {
                    newEntry = new JarEntry(name);
                }
                writeEntry(zis, newEntry);
                zis.closeEntry();
            }
        } finally {
            zis.close();
        }
    }

    /**
     * Closes the Jar archive by creating the manifest, and signing the archive.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public void close() throws IOException, GeneralSecurityException {
        if (mManifest != null) {
            mOutputJar.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
            mManifest.write(mOutputJar);
            Signature signature = Signature.getInstance("SHA1with" + mKey.getAlgorithm());
            signature.initSign(mKey);
            mOutputJar.putNextEntry(new JarEntry("META-INF/CERT.SF"));
            writeSignatureFile(new SignatureOutputStream(mOutputJar, signature));
            mOutputJar.putNextEntry(new JarEntry("META-INF/CERT." + mKey.getAlgorithm()));
            writeSignatureBlock(signature, mCertificate, mKey);
        }
        mOutputJar.close();
    }

    /**
     * Adds an entry to the output jar, and write its content from the {@link InputStream}
     * @param input The input stream from where to write the entry content.
     * @param entry the entry to write in the jar.
     * @throws IOException
     */
    private void writeEntry(InputStream input, JarEntry entry) throws IOException {
        mOutputJar.putNextEntry(entry);
        int count;
        while ((count = input.read(mBuffer)) != -1) {
            mOutputJar.write(mBuffer, 0, count);
            if (mMessageDigest != null) {
                mMessageDigest.update(mBuffer, 0, count);
            }
        }
        mOutputJar.closeEntry();
        if (mManifest != null) {
            Attributes attr = mManifest.getAttributes(entry.getName());
            if (attr == null) {
                attr = new Attributes();
                mManifest.getEntries().put(entry.getName(), attr);
            }
            attr.putValue(DIGEST_ATTR, mBase64Encoder.encode(mMessageDigest.digest()));
        }
    }

    /** Writes a .SF file with a digest to the manifest. */
    private void writeSignatureFile(SignatureOutputStream out) throws IOException, GeneralSecurityException {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android)");
        BASE64Encoder base64 = new BASE64Encoder();
        MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
        PrintStream print = new PrintStream(new DigestOutputStream(new ByteArrayOutputStream(), md), true, "UTF-8");
        mManifest.write(print);
        print.flush();
        main.putValue(DIGEST_MANIFEST_ATTR, base64.encode(md.digest()));
        Map<String, Attributes> entries = mManifest.getEntries();
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            print.print("Name: " + entry.getKey() + "\r\n");
            for (Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
                print.print(att.getKey() + ": " + att.getValue() + "\r\n");
            }
            print.print("\r\n");
            print.flush();
            Attributes sfAttr = new Attributes();
            sfAttr.putValue(DIGEST_ATTR, base64.encode(md.digest()));
            sf.getEntries().put(entry.getKey(), sfAttr);
        }
        sf.write(out);
        if ((out.size() % 1024) == 0) {
            out.write('\r');
            out.write('\n');
        }
    }

    /** Write the certificate file with a digital signature. */
    private void writeSignatureBlock(Signature signature, X509Certificate publicKey, PrivateKey privateKey) throws IOException, GeneralSecurityException {
        SignerInfo signerInfo = new SignerInfo(new X500Name(publicKey.getIssuerX500Principal().getName()), publicKey.getSerialNumber(), AlgorithmId.get(DIGEST_ALGORITHM), AlgorithmId.get(privateKey.getAlgorithm()), signature.sign());
        PKCS7 pkcs7 = new PKCS7(new AlgorithmId[] { AlgorithmId.get(DIGEST_ALGORITHM) }, new ContentInfo(ContentInfo.DATA_OID, null), new X509Certificate[] { publicKey }, new SignerInfo[] { signerInfo });
        pkcs7.encodeSignedData(mOutputJar);
    }
}
