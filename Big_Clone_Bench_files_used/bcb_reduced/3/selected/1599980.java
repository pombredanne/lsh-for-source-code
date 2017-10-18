package org.apache.harmony.jretools.keytool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import org.apache.harmony.luni.util.Base64;

/**
 * Class for printing to stdout the contents of the keystore or a single
 * certificate. The certificate can be in the keystore (list(..) method) or not
 * (printCert(..) method).
 */
public class KeyStoreCertPrinter {

    /**
     * Prints the contents of the entry associated with the alias given in
     * param. If no alias is specified, the contents of the entire keystore are
     * printed.
     * 
     * @param param
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws UnrecoverableKeyException
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws CertificateException 
     */
    static void list(KeytoolParameters param) throws KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, UnrecoverableKeyException, CertificateException, FileNotFoundException, IOException {
        Enumeration aliases;
        KeyStore keyStore = param.getKeyStore();
        String alias = param.getAlias();
        if (alias != null) {
            aliases = Collections.enumeration(Collections.singleton(alias));
        } else {
            aliases = keyStore.aliases();
            System.out.println("Type of keystore: " + keyStore.getType());
            System.out.println("Keystore provider name: " + keyStore.getProvider().getName());
            int keyStoreSize = keyStore.size();
            System.out.println("\nThe keystore contains " + keyStoreSize + ((keyStoreSize == 1) ? " entry \n" : " entries \n"));
        }
        String mdProvider = (param.getMdProvider() != null) ? param.getMdProvider() : param.getProvider();
        while (aliases.hasMoreElements()) {
            String currentAlias = (String) aliases.nextElement();
            String creationDate = keyStore.getCreationDate(currentAlias).toString();
            boolean trustedEntry = false;
            boolean secretKeyEntry = false;
            String entryType = "Key entry";
            if (keyStore.entryInstanceOf(currentAlias, KeyStore.TrustedCertificateEntry.class)) {
                entryType = "Trusted certificate entry";
                trustedEntry = true;
            } else if (keyStore.entryInstanceOf(currentAlias, KeyStore.PrivateKeyEntry.class)) {
                entryType = "Private key entry";
            } else if (keyStore.entryInstanceOf(currentAlias, KeyStore.SecretKeyEntry.class)) {
                entryType = "Secret key entry";
                secretKeyEntry = true;
            }
            X509Certificate x509cert = ((X509Certificate) keyStore.getCertificate(currentAlias));
            if (param.isVerbose() || param.isRfc()) {
                System.out.println("Alias name: " + currentAlias);
                System.out.println("Date of creation: " + creationDate);
                System.out.println("Type of the entry: " + entryType);
                if (!secretKeyEntry) {
                    Certificate[] certChain = keyStore.getCertificateChain(currentAlias);
                    if (!trustedEntry) {
                        System.out.println("Certificate chain length: " + certChain.length);
                    }
                    if (param.isVerbose()) {
                        System.out.println("Certificate[1]:");
                        printX509CertDetailed(x509cert, mdProvider);
                        if (!trustedEntry) {
                            for (int i = 1; i < certChain.length; i++) {
                                System.out.println("Certificate[" + (i + 1) + "]:");
                                printX509CertDetailed((X509Certificate) certChain[i], mdProvider);
                            }
                        }
                    } else {
                        System.out.println("Certificate[1]:");
                        System.out.println("-----BEGIN CERTIFICATE-----");
                        System.out.println(Base64.encode(x509cert.getEncoded(), "ISO-8859-1"));
                        System.out.println("-----END CERTIFICATE-----");
                        if (!trustedEntry) {
                            for (int i = 1; i < certChain.length; i++) {
                                System.out.println("Certificate[" + (i + 1) + "]:");
                                System.out.println("-----BEGIN CERTIFICATE-----");
                                System.out.println(Base64.encode(certChain[i].getEncoded(), "ISO-8859-1"));
                                System.out.println("-----END CERTIFICATE-----");
                            }
                        }
                    }
                } else {
                    if (alias != null) {
                        char[] keyPass;
                        if ((keyPass = param.getKeyPass()) != null) {
                            Key key = keyStore.getKey(currentAlias, keyPass);
                            System.out.println("Algorithm: " + key.getAlgorithm() + "\nFormat: " + key.getFormat());
                            System.out.println("Key: " + formatBytes(key.getEncoded()));
                        } else {
                            System.out.println("If you want to print the key, " + "please set the entry password using " + "\"-keypass\" option");
                        }
                    }
                }
                System.out.println("\n*******************************" + "*******************************\n");
            } else {
                String commaSpc = ", ";
                System.out.print(currentAlias + commaSpc + creationDate + commaSpc + entryType);
                if (!secretKeyEntry) {
                    System.out.print(commaSpc + "\nCertificate fingerprint (MD5):  ");
                    printMD(x509cert.getEncoded(), "MD5", mdProvider);
                } else {
                    if (alias != null) {
                        char[] keyPass;
                        if ((keyPass = param.getKeyPass()) != null) {
                            Key key = keyStore.getKey(currentAlias, keyPass);
                            System.out.println(key.getAlgorithm() + ", " + key.getFormat() + ", " + formatBytes(key.getEncoded()));
                        } else {
                            System.out.println("If you want to print the key, " + "please set the entry password using " + "\"-keypass\" option");
                        }
                    }
                }
            }
        }
    }

    /**
     * Prints the detailed description of a certificate in a human-readable
     * format: its owner and issuer, serial number, validity period and
     * fingerprints. providerName is needed to generate MessageDigest. If it is
     * null, a default one is used.
     * 
     * @param x509cert
     * @param providerName
     * @throws CertificateEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    static void printX509CertDetailed(X509Certificate x509cert, String providerName) throws CertificateEncodingException, NoSuchAlgorithmException, NoSuchProviderException {
        System.out.println("Owner: " + x509cert.getSubjectX500Principal());
        System.out.println("Issuer: " + x509cert.getIssuerX500Principal());
        System.out.println("Serial number: " + Integer.toHexString(x509cert.getSerialNumber().intValue()));
        System.out.println("Validity period \n\t from:  " + x509cert.getNotBefore() + "\n\t until: " + x509cert.getNotAfter());
        byte[] encodedCert;
        try {
            encodedCert = x509cert.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new CertificateEncodingException("Failed to encode the certificate", e);
        }
        String strMD5 = "MD5";
        String strSHA1 = "SHA1";
        System.out.print("Certificate fingerprints: " + "\n\t " + strMD5 + ":  ");
        printMD(encodedCert, strMD5, providerName);
        System.out.print("\t " + strSHA1 + ": ");
        printMD(encodedCert, strSHA1, providerName);
    }

    private static void printMD(byte[] encoding, String mdAlgorithm, String providerName) throws NoSuchAlgorithmException, NoSuchProviderException {
        byte[] digest;
        try {
            if (providerName != null) {
                digest = MessageDigest.getInstance(mdAlgorithm, providerName).digest(encoding);
            } else {
                digest = MessageDigest.getInstance(mdAlgorithm).digest(encoding);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("The algorithm " + mdAlgorithm + " is not found in the environment.", e);
        } catch (NoSuchProviderException e) {
            throw (NoSuchProviderException) new NoSuchProviderException("The provider " + providerName + " is not found in the environment.").initCause(e);
        }
        System.out.println(formatBytes(digest));
    }

    /**
     * Reads an X.509 certificate from the file specified in param and prints it
     * in a human-readable format. If param.getFileName() returns null, the
     * certificate is read from the standard input. The input data is awaited
     * for 3 seconds. If the data is not entered, an exception is thrown.
     * 
     * @param param
     * @throws KeytoolException
     * @throws IOException
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    static void printCert(KeytoolParameters param) throws FileNotFoundException, CertificateException, IOException, KeytoolException, NoSuchAlgorithmException, NoSuchProviderException {
        String provider = param.getProvider();
        String certProvider = (param.getCertProvider() != null) ? param.getCertProvider() : provider;
        String mdProvider = (param.getMdProvider() != null) ? param.getMdProvider() : provider;
        Collection certCollection = CertReader.readCerts(param.getFileName(), false, certProvider);
        Iterator certIter = certCollection.iterator();
        int counter = 1;
        while (certIter.hasNext()) {
            X509Certificate cert = (X509Certificate) certIter.next();
            System.out.println("\nCertificate[" + counter + "]:");
            printX509CertDetailed(cert, mdProvider);
            ++counter;
        }
    }

    private static String formatBytes(byte[] bytes) {
        int i;
        StringBuffer buffer = new StringBuffer(59);
        int length;
        String currentByte;
        for (i = 0; i < bytes.length - 1; i++) {
            currentByte = Integer.toHexString(bytes[i]).toUpperCase();
            if ((length = currentByte.length()) > 1) {
                buffer.append(currentByte.substring(length - 2) + ":");
            } else {
                buffer.append("0" + currentByte + ":");
            }
        }
        currentByte = Integer.toHexString(bytes[i]).toUpperCase();
        if ((length = currentByte.length()) > 1) {
            buffer.append(currentByte.substring(length - 2));
        } else {
            buffer.append("0" + currentByte);
        }
        return new String(buffer);
    }
}
