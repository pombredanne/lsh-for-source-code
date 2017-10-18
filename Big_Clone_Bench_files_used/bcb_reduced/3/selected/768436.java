package org.hardtokenmgmt.hosting.web.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.util.encoders.Base64;

class SignatureFile {

    static class Block {

        public String getMetaName() {
            return blockFileName;
        }

        public void write(OutputStream out) throws IOException {
            out.write(signedData.getEncoded());
        }

        CMSSignedData signedData;

        private String blockFileName;

        Block(SignatureFile sfg, PrivateKey privateKey, X509Certificate certChain[]) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, CertificateException, NoSuchProviderException, CMSException, CertStoreException, InvalidAlgorithmParameterException {
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addCertificatesAndCRLs(getCertStore(certChain));
            generator.addSigner(privateKey, (X509Certificate) certChain[0], CMSSignedDataGenerator.DIGEST_SHA1);
            String keyAlgorithm = privateKey.getAlgorithm();
            String digestAlgorithm;
            if (keyAlgorithm.equalsIgnoreCase("DSA")) {
                generator.addSigner(privateKey, (X509Certificate) certChain[0], CMSSignedDataGenerator.DIGEST_SHA1);
                digestAlgorithm = "SHA1";
            } else if (keyAlgorithm.equalsIgnoreCase("RSA")) {
                generator.addSigner(privateKey, (X509Certificate) certChain[0], CMSSignedDataGenerator.DIGEST_MD5);
                digestAlgorithm = "MD5";
            } else {
                throw new RuntimeException("private key is not a DSA or RSA key");
            }
            String signatureAlgorithm = digestAlgorithm + "with" + keyAlgorithm;
            blockFileName = "META-INF/" + sfg.getBaseName() + "." + keyAlgorithm;
            Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initSign(privateKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sfg.write(baos);
            byte bytes[] = baos.toByteArray();
            CMSProcessable content = new CMSProcessableByteArray(bytes);
            signedData = generator.generate(content, true, "BC");
        }

        private CertStore getCertStore(X509Certificate[] certChain) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
            ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
            for (int i = 0, length = certChain == null ? 0 : certChain.length; i < length; i++) {
                list.add(certChain[i]);
            }
            return CertStore.getInstance("Collection", new CollectionCertStoreParameters(list), "BC");
        }
    }

    public SignatureFile(MessageDigest digests[], Manifest mf, ManifestDigester md, String baseName, boolean signManifest) {
        this.baseName = baseName;
        String version = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        sf = new Manifest();
        Attributes mattr = sf.getMainAttributes();
        mattr.putValue(java.util.jar.Attributes.Name.SIGNATURE_VERSION.toString(), "1.0");
        mattr.putValue("Created-By", version + " (" + javaVendor + ")");
        if (signManifest) {
            for (int i = 0; i < digests.length; i++) mattr.putValue(digests[i].getAlgorithm() + "-Digest-Manifest", new String(Base64.encode(md.manifestDigest(digests[i]))));
        }
        Map<String, Attributes> entries = sf.getEntries();
        Iterator<Entry<String, Attributes>> mit = mf.getEntries().entrySet().iterator();
        do {
            if (!mit.hasNext()) break;
            java.util.Map.Entry<String, Attributes> e = mit.next();
            String name = (String) e.getKey();
            ManifestDigester.Entry mde = md.get(name, false);
            if (mde != null) {
                Attributes attr = new Attributes();
                for (int i = 0; i < digests.length; i++) attr.putValue(digests[i].getAlgorithm() + "-Digest", new String(Base64.encode(mde.digest(digests[i]))));
                entries.put(name, attr);
            }
        } while (true);
    }

    public void write(OutputStream out) throws IOException {
        sf.write(out);
    }

    public String getMetaName() {
        return "META-INF/" + baseName + ".SF";
    }

    public String getBaseName() {
        return baseName;
    }

    public Block generateBlock(PrivateKey privateKey, X509Certificate certChain[]) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, CertificateException, NoSuchProviderException, CertStoreException, InvalidAlgorithmParameterException, CMSException {
        return new Block(this, privateKey, certChain);
    }

    private Manifest sf;

    private String baseName;
}
