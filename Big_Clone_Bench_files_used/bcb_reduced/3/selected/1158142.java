package org.rip.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyStoreUtils {

    private static Logger LOG = LoggerFactory.getLogger(KeyStoreUtils.class);

    private static final String PASSWORD = "password";

    public static KeyStore openDefaultKeyStore() {
        File file = new File("src/main/resources/keystore.jks");
        LOG.info("Loading KeyStore {} ...", file.getName());
        try {
            InputStream in = new FileInputStream(file);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(in, PASSWORD.toCharArray());
            in.close();
            return ks;
        } catch (Exception e) {
            LOG.error("Error loading keystore", e);
        }
        return null;
    }

    public static void updateKeyStore(String host, X509Certificate[] chain) {
        LOG.info("updateKeyStore begin");
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (int i = 0; i < chain.length; i++) {
                X509Certificate cert = chain[i];
                LOG.info(" " + (i + 1) + " Subject " + cert.getSubjectDN());
                LOG.info("   Issuer  " + cert.getIssuerDN());
                sha1.update(cert.getEncoded());
                LOG.info("   sha1    " + toHexString(sha1.digest()));
                md5.update(cert.getEncoded());
                LOG.info("   md5     " + toHexString(md5.digest()));
                String alias = host + "-" + (i + 1);
                KeyStore ks = openDefaultKeyStore();
                ks.setCertificateEntry(alias, cert);
                OutputStream out = new FileOutputStream("src/main/resources/keystore.jks");
                ks.store(out, PASSWORD.toCharArray());
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            LOG.error("updateKeyStore exception", e);
        }
        LOG.info("updateKeyStore end");
    }

    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }

    public static void dumpKeyStore() {
        try {
            File file = new File("src/main/resources/keystore.jks");
            FileInputStream is = new FileInputStream(file);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            String password = "password";
            keystore.load(is, password.toCharArray());
            Enumeration<String> e = keystore.aliases();
            while (e.hasMoreElements()) {
                String alias = e.nextElement();
                LOG.info("Alias:{} Key:{} Cert:{}", new Object[] { alias, keystore.isKeyEntry(alias), keystore.isCertificateEntry(alias) });
            }
            is.close();
        } catch (java.security.cert.CertificateException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (FileNotFoundException e) {
        } catch (KeyStoreException e) {
        } catch (IOException e) {
        }
    }

    public static void addToKeyStore(File keystoreFile, char[] keystorePassword, String alias, Certificate cert) {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream in = new FileInputStream(keystoreFile);
            keystore.load(in, keystorePassword);
            in.close();
            keystore.setCertificateEntry(alias, cert);
            FileOutputStream out = new FileOutputStream(keystoreFile);
            keystore.store(out, keystorePassword);
            out.close();
        } catch (java.security.cert.CertificateException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (FileNotFoundException e) {
        } catch (KeyStoreException e) {
        } catch (IOException e) {
        }
    }

    public static Certificate getCertificate() {
        try {
            File file = new File("src/main/resources/keystore.jks");
            FileInputStream is = new FileInputStream(file);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, "password".toCharArray());
            return keystore.getCertificate("myalias");
        } catch (KeyStoreException e) {
        } catch (java.security.cert.CertificateException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (java.io.IOException e) {
        }
        return null;
    }
}
