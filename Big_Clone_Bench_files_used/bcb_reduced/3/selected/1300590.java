package tools.keytool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import tools.keytool.gui.JKSManagerGUI;
import codec.Base64;
import codec.asn1.DEREncoder;
import codec.pkcs12.PFX;
import codec.pkcs8.EncryptedPrivateKeyInfo;
import codec.x501.Name;
import codec.x509.X509Certificate;

public class Conversions {

    public static final String DIGEST = "SHA1";

    public static void jks2pkcs12(HashMap<String, KeystoreEntryDefinition> content, String basename) {
        KeystoreEntryDefinition entry;
        KeyStore.PrivateKeyEntry entr;
        X509Certificate x509;
        Certificate[] chain;
        DEREncoder enc;
        PrivateKey key;
        char[] keyPass;
        String alias;
        Iterator it;
        PFX pkcs12;
        int ans;
        File f;
        it = content.keySet().iterator();
        JKSManagerGUI.addStatusInfo("Checking whether .p12 files already exist...");
        while (it.hasNext()) {
            alias = it.next().toString();
            f = new File(basename + "_" + alias + ".p12");
            if (f.exists()) {
                JKSManagerGUI.addStatusInfo("\nABORTING: file " + basename + "_" + alias + ".p12" + " does already exist!\n");
                JKSManagerGUI.allowStatusWindowToClose(true);
                return;
            }
        }
        JKSManagerGUI.addStatusInfo("OK\n");
        try {
            it = content.values().iterator();
            while (it.hasNext()) {
                entry = (KeystoreEntryDefinition) it.next();
                if (entry.needsCorrectKeyParameters()) {
                    ans = JKSManagerGUI.showConfirmUserDialog("The protection password of the entry with alias: \"" + entry.getNewAlias() + "\" is different from the keystore " + " password! Do you wish to skip this entry and continue?", "Skip Entry: \"" + entry.getNewAlias() + "\"");
                    if (ans == 0) {
                        JKSManagerGUI.addStatusInfo("\nEntry with alias \"" + entry.getNewAlias() + "\" is " + "with unresolved key pass parameters ... Skipping\n");
                        continue;
                    }
                    JKSManagerGUI.addStatusInfo("\nABORTING: The protection password of the " + "entry with alias: \"" + entry.getNewAlias() + "\" is " + "different from the keystore password and you haven't" + "provided it!");
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                if (entry.getType() != KeystoreEntryDefinition.PRIVATE_KEY_ENTRY) {
                    continue;
                }
                entr = (PrivateKeyEntry) entry.getEntry();
                chain = entry.getCertificateChain();
                if (chain == null) {
                    JKSManagerGUI.addStatusInfo("\nABORTING: The certificate chain of alias \"" + entry.getNewAlias() + "\" cannot be null!\n");
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                JKSManagerGUI.addStatusInfo("Extracting the certificate of alias: \"" + entry.getNewAlias() + "\" ... ");
                try {
                    x509 = new X509Certificate(chain[0].getEncoded());
                } catch (CertificateEncodingException cee) {
                    JKSManagerGUI.addStatusInfo(" \nABORTING: The exported certificate of alias \"" + entry.getNewAlias() + "\" is not of type x509!");
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                JKSManagerGUI.addStatusInfo("OK\n");
                JKSManagerGUI.addStatusInfo("Extracting the private key of alias: \"" + entry.getNewAlias() + "\" ... ");
                key = entr.getPrivateKey();
                JKSManagerGUI.addStatusInfo("OK\n");
                keyPass = ((KeyStore.PasswordProtection) entry.getProtectionParams()).getPassword();
                JKSManagerGUI.addStatusInfo("Creating the PKCS12 information ... ");
                byte[] b = getFingerprint(x509);
                if (b != null) {
                    pkcs12 = new PFX(key, x509, null, keyPass, entry.getNewAlias(), b);
                } else {
                    pkcs12 = new PFX(key, x509, null, keyPass, entry.getNewAlias(), null);
                }
                enc = new DEREncoder(new FileOutputStream(basename + "_" + entry.getNewAlias() + ".p12"));
                JKSManagerGUI.addStatusInfo("OK\n");
                JKSManagerGUI.addStatusInfo("Writing the file: " + basename + "_" + entry.getNewAlias() + ".p12" + " ... ");
                pkcs12.encode(enc);
                enc.close();
                JKSManagerGUI.addStatusInfo("OK\n");
            }
            JKSManagerGUI.addStatusInfo("DONE!");
            JKSManagerGUI.allowStatusWindowToClose(true);
        } catch (Exception e) {
            JKSManagerGUI.addStatusInfo("\nABORTING: The following exception occurred: \n" + e.getClass() + ": " + e.getMessage());
            JKSManagerGUI.allowStatusWindowToClose(true);
            return;
        }
    }

    public static void jks2ldif(HashMap<String, KeystoreEntryDefinition> content, String ldifLocation, String objectClass) {
        KeystoreEntryDefinition entry;
        Iterator it;
        boolean firstKey = true;
        boolean append = false;
        try {
            it = content.values().iterator();
            while (it.hasNext()) {
                entry = (KeystoreEntryDefinition) it.next();
                try {
                    writeLDIFInfo(entry, ldifLocation, objectClass, append, firstKey);
                } catch (IllegalArgumentException iae) {
                    JKSManagerGUI.addStatusInfo(iae.getMessage());
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                if (firstKey) {
                    firstKey = false;
                }
                if (!append) {
                    append = true;
                }
            }
            JKSManagerGUI.addStatusInfo("\nDONE!");
            JKSManagerGUI.allowStatusWindowToClose(true);
        } catch (Exception e) {
            JKSManagerGUI.addStatusInfo("\nABORTING: " + e.getMessage() + "!");
            JKSManagerGUI.allowStatusWindowToClose(true);
        }
    }

    public static void jks2pem(HashMap<String, KeystoreEntryDefinition> content, String pemLocation) {
        KeystoreEntryDefinition entry;
        HashMap<String, String> certs;
        HashMap<String, String> keys;
        FileWriter fileWriter = null;
        Certificate[] chain;
        Certificate cert;
        PrivateKey key;
        char[] keypass;
        String alias;
        Iterator it;
        String str;
        int ans;
        String keyStr = null;
        try {
            fileWriter = new FileWriter(pemLocation);
        } catch (IOException fnfe) {
            JKSManagerGUI.addStatusInfo("\nABORTING: The file " + pemLocation + " cannot be opened!");
            JKSManagerGUI.allowStatusWindowToClose(true);
            return;
        }
        certs = new HashMap<String, String>();
        keys = new HashMap<String, String>();
        it = content.keySet().iterator();
        try {
            while (it.hasNext()) {
                alias = it.next().toString();
                entry = (KeystoreEntryDefinition) content.get(alias);
                if (entry.needsCorrectKeyParameters()) {
                    ans = JKSManagerGUI.showConfirmUserDialog("The protection password of the entry with alias: \"" + entry.getNewAlias() + "\" is different from the keystore " + " password! Do you wish to skip this entry and continue?", "Skip Entry: " + entry.getNewAlias());
                    if (ans == 0) {
                        JKSManagerGUI.addStatusInfo("\nEntry with alias \"" + alias + "\" is " + "with unresolved key pass parameters ... Skipping\n");
                        continue;
                    }
                    JKSManagerGUI.addStatusInfo("\nABORTING: The protection password of the " + "entry with alias: \"" + alias + "\" is " + "different from the keystore password and you haven't" + "provided it!");
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                chain = entry.getCertificateChain();
                if (entry.getType() != KeystoreEntryDefinition.SECRET_KEY_ENTRY) {
                    if (chain == null) {
                        JKSManagerGUI.addStatusInfo("\nABORTING: The certificate chain of alias \"" + alias + "\" cannot be null!");
                        JKSManagerGUI.allowStatusWindowToClose(true);
                        return;
                    }
                    cert = chain[0];
                    try {
                        JKSManagerGUI.addStatusInfo("Extracting the certificate for alias \"" + alias + "\" ... ");
                        str = new String("-----BEGIN CERTIFICATE-----\n");
                        str = str + format(Base64.encode(cert.getEncoded()));
                        str = str + "-----END CERTIFICATE-----\n";
                        certs.put(alias, str);
                        JKSManagerGUI.addStatusInfo("OK\n");
                    } catch (CertificateEncodingException cee) {
                        JKSManagerGUI.addStatusInfo("\nABORTING: There occurred an encoding error while trying to " + "retieve the content of the certificate with alias \"" + alias + "\".");
                        JKSManagerGUI.allowStatusWindowToClose(true);
                        return;
                    }
                }
                if (entry.getType() == KeystoreEntryDefinition.PRIVATE_KEY_ENTRY) {
                    JKSManagerGUI.addStatusInfo("Extracting the private key for alias \"" + alias + "\" ... ");
                    keypass = ((PasswordProtection) entry.getProtectionParams()).getPassword();
                    key = ((PrivateKeyEntry) entry.getEntry()).getPrivateKey();
                    str = new String("-----BEGIN ENCRYPTED PRIVATE KEY-----\n");
                    try {
                        keyStr = getPEMEncryptedPrivateKeyString(key, keypass);
                    } catch (Exception e) {
                        JKSManagerGUI.addStatusInfo("\nABORTING: The following Exception occurred while trying to " + "retrieve the pkcs12 information of the private key of the " + "entry with alias\"" + entry.getAlias() + "\":\n" + e.getClass().getName() + e.getMessage());
                        JKSManagerGUI.allowStatusWindowToClose(true);
                        return;
                    }
                    str = str + format(keyStr);
                    str = str + "-----END ENCRYPTED PRIVATE KEY-----\n";
                    keys.put(alias, str);
                    JKSManagerGUI.addStatusInfo("OK\n");
                }
            }
        } catch (Exception e) {
            JKSManagerGUI.addStatusInfo("ABORTING: The following exception occurred while trying " + "to collect the needed information for the .pem format:\n" + e.getClass().getName() + ": " + e.getMessage());
            JKSManagerGUI.allowStatusWindowToClose(true);
            return;
        }
        try {
            it = keys.keySet().iterator();
            JKSManagerGUI.addStatusInfo("Writing the collected info in " + pemLocation + " ... ");
            while (it.hasNext()) {
                alias = it.next().toString();
                fileWriter.write("### Alias => '" + alias + "'\n");
                fileWriter.write(keys.get(alias.toString()));
            }
            it = certs.keySet().iterator();
            while (it.hasNext()) {
                alias = it.next().toString();
                fileWriter.write("### Alias => '" + alias + "'\n");
                fileWriter.write(certs.get(alias.toString()));
            }
            fileWriter.close();
            JKSManagerGUI.addStatusInfo("OK\n");
            JKSManagerGUI.addStatusInfo("DONE\n");
            JKSManagerGUI.allowStatusWindowToClose(true);
        } catch (Exception e) {
            JKSManagerGUI.addStatusInfo("ABORTING: The following exception occurred while trying " + "to write the .pem file:\n" + e.getClass().getName() + ": " + e.getMessage());
            JKSManagerGUI.allowStatusWindowToClose(true);
            return;
        }
    }

    public static void jks2x509(HashMap<String, KeystoreEntryDefinition> content, String basename) {
        KeystoreEntryDefinition entry;
        X509Certificate x509;
        Certificate[] chain;
        DEREncoder enc;
        String alias;
        Iterator it;
        File f;
        it = content.keySet().iterator();
        while (it.hasNext()) {
            alias = it.next().toString();
            f = new File(basename + "_" + alias + ".crt");
            if (f.exists()) {
                JKSManagerGUI.addStatusInfo("\nABORTING: The file " + basename + "_" + alias + ".crt" + " already " + "exists. Please choose another basename!");
                JKSManagerGUI.allowStatusWindowToClose(true);
                return;
            }
        }
        try {
            it = content.values().iterator();
            while (it.hasNext()) {
                entry = (KeystoreEntryDefinition) it.next();
                JKSManagerGUI.addStatusInfo("Extracting certificate of alias \"" + entry.getNewAlias() + "\" ... ");
                chain = entry.getCertificateChain();
                if (chain == null) {
                    JKSManagerGUI.addStatusInfo("\nABORTING: The certificate chain of alias \"" + entry.getNewAlias() + "\" cannot be null!");
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                JKSManagerGUI.addStatusInfo("OK\n");
                try {
                    JKSManagerGUI.addStatusInfo("Converting it in x509 format ... ");
                    x509 = new X509Certificate(chain[0].getEncoded());
                    JKSManagerGUI.addStatusInfo("OK\n");
                } catch (CertificateEncodingException cee) {
                    JKSManagerGUI.addStatusInfo("\nABORTING: The exported certificate " + "of alias \"" + entry.getNewAlias() + "\" is not of type x509!");
                    JKSManagerGUI.allowStatusWindowToClose(true);
                    return;
                }
                JKSManagerGUI.addStatusInfo("Writing the file " + basename + "_" + entry.getNewAlias() + ".crt... ");
                enc = new DEREncoder(new FileOutputStream(basename + "_" + entry.getNewAlias() + ".crt"));
                x509.encode(enc);
                enc.close();
                JKSManagerGUI.addStatusInfo("OK\n");
            }
            JKSManagerGUI.addStatusInfo("DONE\n");
            JKSManagerGUI.allowStatusWindowToClose(true);
        } catch (Exception e) {
            JKSManagerGUI.addStatusInfo("\nABORTING: " + e.getClass().getName() + ": " + e.getMessage() + "!");
            JKSManagerGUI.allowStatusWindowToClose(true);
        }
    }

    public static void jks2certsJKS(HashMap<String, KeystoreEntryDefinition> content, String jksLocation, char[] jksPass, Provider jksProvider) {
        HashMap<String, KeystoreEntryDefinition> entries;
        KeystoreEntryDefinition entry;
        KeyStore.Entry certEntry;
        Certificate cert;
        Iterator it;
        String alias;
        entries = new HashMap<String, KeystoreEntryDefinition>();
        it = content.keySet().iterator();
        try {
            while (it.hasNext()) {
                alias = it.next().toString();
                entry = (KeystoreEntryDefinition) content.get(alias);
                JKSManagerGUI.addStatusInfo("Extracting certificate of alias \"" + alias + "\" ... ");
                cert = entry.getCertificateChain()[0];
                certEntry = new KeyStore.TrustedCertificateEntry(cert);
                entries.put(alias, new KeystoreEntryDefinition(alias, null, null, null, certEntry, null, null, KeystoreEntryDefinition.TRUSTED_CERTIFICATE_ENTRY, false));
                JKSManagerGUI.addStatusInfo("OK\n");
            }
            JKSManagerGUI.addStatusInfo("Saving new JKS file ... ");
            JKSManager.saveJKS(jksLocation, jksPass, jksProvider, entries);
            JKSManagerGUI.addStatusInfo("OK\n");
            JKSManagerGUI.addStatusInfo("DONE\n");
            JKSManagerGUI.allowStatusWindowToClose(true);
        } catch (Exception e) {
            JKSManagerGUI.addStatusInfo("\nABORTING: The following exception occurred while trying " + "to write the file " + jksLocation + ":\n" + e);
            JKSManagerGUI.allowStatusWindowToClose(true);
            return;
        }
    }

    private static void writeLDIFInfo(KeystoreEntryDefinition entry, String ldifLocation, String objectClass, boolean append, boolean firstKey) throws Exception {
        Certificate[] chain;
        Certificate cert;
        Name subjectDN;
        Name issuerDN;
        FileWriter fileWriter = null;
        X509Certificate x509 = null;
        String str = null;
        try {
            fileWriter = new FileWriter(ldifLocation, append);
        } catch (IOException fnfe) {
            throw new IllegalArgumentException("\nABORTING: The file " + ldifLocation + " cannot be opened!\n");
        }
        chain = entry.getCertificateChain();
        if (chain == null) {
            throw new IllegalArgumentException("\nABORTING: The certificate chain of alias \"" + entry.getNewAlias() + "\" cannot be null!");
        }
        cert = chain[0];
        try {
            JKSManagerGUI.addStatusInfo("Extracting the certificate of alias : " + entry.getAlias() + " ... ");
            x509 = new X509Certificate(cert.getEncoded());
            JKSManagerGUI.addStatusInfo("OK\n");
        } catch (CertificateEncodingException cee) {
            throw new IllegalArgumentException("\nABORTING: The first certificate of the certificate chain of alias \"" + entry.getNewAlias() + "\" cannot be casted to type X509 and " + "the requested information cannot be read!");
        }
        JKSManagerGUI.addStatusInfo("\nCreating LDIF information for alias \"" + entry.getAlias() + "\" ... \n");
        subjectDN = (Name) x509.getSubjectDN();
        if (subjectDN != null) {
            str = subjectDN.toString();
            str = str.replace("\"", "");
            JKSManagerGUI.addStatusInfo("subject dist.name: " + str + "\n");
        }
        if (str != null) {
            fileWriter.write("dname: " + str + "\n");
            JKSManagerGUI.addStatusInfo("dname: " + str + "\n");
        }
        issuerDN = (Name) x509.getIssuerDN();
        if (issuerDN != null) {
            str = issuerDN.toString();
            str = str.replace("\"", "");
            fileWriter.write("issuer: " + str + "\n");
            JKSManagerGUI.addStatusInfo("issuer dist.name: " + str + "\n");
        }
        JKSManagerGUI.addStatusInfo("serialNumber: " + x509.getSerialNumber() + "\n");
        fileWriter.write("serialNumber: " + x509.getSerialNumber() + "\n");
        fileWriter.write("userCertificate;binary::\n" + format(Base64.encode(x509.getEncoded())));
        fileWriter.close();
    }

    private static String format(String str) {
        if (str == null) {
            return null;
        }
        StringBuffer buf;
        buf = new StringBuffer();
        for (int i = 0; i < str.length(); i += 64) {
            if (i + 66 > str.length()) {
                buf.append(" " + str.substring(i));
            } else {
                buf.append(" " + str.substring(i, i + 64));
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    private static String getPEMEncryptedPrivateKeyString(PrivateKey key, char[] passwd) throws Exception {
        ByteArrayOutputStream baos;
        EncryptedPrivateKeyInfo encPKI = new EncryptedPrivateKeyInfo();
        encPKI.setPrivateKey(key, passwd);
        baos = new ByteArrayOutputStream();
        DEREncoder enc = new DEREncoder(baos);
        encPKI.encode(enc);
        enc.close();
        return Base64.encode(baos.toByteArray());
    }

    private static byte[] getFingerprint(X509Certificate cert) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(DIGEST);
            md.update(cert.getEncoded());
            return md.digest();
        } catch (Exception e) {
            return null;
        }
    }
}
