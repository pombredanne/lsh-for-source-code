package org.tranche.security;

import org.tranche.users.User;
import org.tranche.logs.LogUtil;
import org.tranche.hash.Base64;
import org.tranche.hash.Base16;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebugUtil;
import org.tranche.exceptions.PassphraseRequiredException;
import org.tranche.hash.BigHash;
import org.tranche.hash.BigHashMaker;
import org.tranche.users.UserCertificateUtil;
import org.tranche.util.IOUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>A helper class for handling security related tasks. You can also find helper methods here for looking up default X.509 certificates and RSA keys.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SecurityUtil {

    private static final byte[] ENCRYPTION_SALT = new byte[8];

    private static final int ENCRYPTION_ITERATIONS = 1000;

    private static String adminCertificateLocation = "/org/tranche/test/admin.public.certificate", writeCertificateLocation = "/org/tranche/test/write.public.certificate", userCertificateLocation = "/org/tranche/test/user.public.certificate", readCertificateLocation = "/org/tranche/test/read.public.certificate", autocertCertificateLocation = "/org/tranche/test/autocert.public.certificate", anonCertificateLocation = "/org/tranche/test/anonymous.public.certificate", anonPrivateKeyLocation = "/org/tranche/test/anonymous.private.key", emailCertificateLocation = "/org/tranche/test/email.public.certificate", emailPrivateKeyLocation = "/org/tranche/test/email.private.key";

    private static boolean uninitialized = true;

    private static X509Certificate defaultCertificate = null;

    private static PrivateKey defaultKey = null;

    /**
     * <p>Size of signature in bytes, used by buffer.</p>
     */
    public static final int SIGNATURE_BUFFER_SIZE = 10000;

    private static X509Certificate adminCert = null, userCert = null, readOnlyCert = null, writeOnlyCert = null, autoCert = null, anonCert = null, emailCert = null;

    private static PrivateKey anonKey = null, emailKey = null;

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param f
     * @param algorithm
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(File f, String algorithm, byte[] buf) throws IOException, GeneralSecurityException {
        FileInputStream fis = new FileInputStream(f);
        byte[] hash = null;
        try {
            hash = hash(fis, algorithm, buf);
        } finally {
            fis.close();
        }
        return hash;
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param f
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(File f, String algorithm) throws IOException, GeneralSecurityException {
        return hash(f, algorithm, new byte[SIGNATURE_BUFFER_SIZE]);
    }

    /**
     * <p>Helper method to make MD5 hashes digestable and file-name friendly. This encoding represents every 4 bits as 0-f.</p>
     * @param bytes
     * @return
     */
    public static String encodeBytes(byte[] bytes) {
        StringWriter name = new StringWriter();
        for (int i = 0; i < bytes.length; i++) {
            int a = 0xf & bytes[i];
            name.write(encodeByte((byte) a));
            int b = 0xf & (bytes[i] >> 4);
            name.write(encodeByte((byte) b));
        }
        return name.toString();
    }

    /**
     * <p>Encodes a byte as a character. Only encodes byte if 0-15, inclusive. Throws a RuntimeException if not in aforementioned range.</p>
     * @param b
     * @return
     */
    public static char encodeByte(byte b) {
        switch(b) {
            case (byte) 0:
                return '0';
            case (byte) 1:
                return '1';
            case (byte) 2:
                return '2';
            case (byte) 3:
                return '3';
            case (byte) 4:
                return '4';
            case (byte) 5:
                return '5';
            case (byte) 6:
                return '6';
            case (byte) 7:
                return '7';
            case (byte) 8:
                return '8';
            case (byte) 9:
                return '9';
            case (byte) 10:
                return 'a';
            case (byte) 11:
                return 'b';
            case (byte) 12:
                return 'c';
            case (byte) 13:
                return 'd';
            case (byte) 14:
                return 'e';
            case (byte) 15:
                return 'f';
            default:
                throw new RuntimeException("Can't have a byte outside the range 0-15.");
        }
    }

    /**
     * <p>Generate a random base64 password of specified size.</p>
     * @param size
     * @return
     */
    public static String generateBase64Password(int size) {
        byte[] bytes = new byte[size];
        RandomUtil.getBytes(bytes);
        String encoded = Base64.encodeBytes(bytes);
        return encoded.substring(0, size);
    }

    /**
     * <p>Generate a random base64 password 20 characters long.</p>
     * @return
     */
    public static String generateBase64Password() {
        return generateBase64Password(20);
    }

    /**
     * <p>Helper method that uses the bouncycastle.org's X509 certificate generator to make a certificate for the given public/private key pair.</p>
     * @param name
     * @param pub
     * @param priv
     * @return
     * @throws java.security.GeneralSecurityException
     */
    public static Certificate createCertificate(String name, PublicKey pub, PrivateKey priv) throws GeneralSecurityException {
        lazyLoad();
        X509V1CertificateGenerator gen = new X509V1CertificateGenerator();
        Hashtable attrs = new Hashtable();
        attrs.put(X509Principal.CN, name);
        attrs.put(X509Principal.OU, "Default DFS Website");
        attrs.put(X509Principal.O, "Certificate Auto-Generator");
        attrs.put(X509Principal.L, "Ann Arbor");
        attrs.put(X509Principal.ST, "Michigan");
        attrs.put(X509Principal.C, "US");
        Date firstDate = new Date();
        firstDate.setTime(firstDate.getTime() - 10 * 60 * 1000);
        Date lastDate = new Date();
        lastDate.setTime(lastDate.getTime() + (60 * (24 * 60 * 60 * 1000)));
        byte[] serno = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed((new Date().getTime()));
        random.nextBytes(serno);
        BigInteger sn = new java.math.BigInteger(serno).abs();
        X509Principal principal = new X509Principal(attrs);
        gen.setSerialNumber(sn);
        gen.setIssuerDN(principal);
        gen.setNotBefore(firstDate);
        gen.setNotAfter(lastDate);
        gen.setSubjectDN(principal);
        gen.setSignatureAlgorithm("SHA1WITHRSA");
        gen.setPublicKey(pub);
        return gen.generateX509Certificate(priv);
    }

    /**
     * <p>Sign a file using a private key. Uses key's signature algorithm to sign.</p>
     * @param f
     * @param key
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key) throws IOException, GeneralSecurityException {
        return sign(f, key, SecurityUtil.getSignatureAlgorithm(key));
    }

    /**
     * <p>Sign a file using a private key. Uses key's signature algorithm to sign.</p>
     * @param f
     * @param key
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key, byte[] buf) throws IOException, GeneralSecurityException {
        return sign(f, key, SecurityUtil.getSignatureAlgorithm(key), buf);
    }

    /**
     * <p>Sign a file using a private key and a specified algorithm.</p>
     * @param f
     * @param key
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key, String algorithm) throws IOException, GeneralSecurityException {
        byte[] buffer = new byte[1000];
        return sign(f, key, algorithm, buffer);
    }

    /**
     * <p>Sign a file using a private key and a specified algorithm.</p>
     * @param f
     * @param key
     * @param algorithm
     * @param buffer
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key, String algorithm, byte[] buffer) throws IOException, GeneralSecurityException {
        FileInputStream fis = null;
        byte[] bytes = null;
        Exception exception = null;
        try {
            fis = new FileInputStream(f);
            bytes = sign(fis, key, algorithm, buffer);
        } catch (Exception e) {
            exception = e;
        } finally {
            try {
                fis.close();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException) exception;
            }
            if (exception instanceof GeneralSecurityException) {
                throw (GeneralSecurityException) exception;
            }
        }
        return bytes;
    }

    /**
     * <p>Sign data from an InputStream using a private key. Uses key's signature algorithm.</p>
     * @param is
     * @param key
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(InputStream is, PrivateKey key) throws IOException, GeneralSecurityException {
        return sign(is, key, SecurityUtil.getSignatureAlgorithm(key));
    }

    /**
     * <p>Sign data from an InputStream using a private key and a specific algorithm.</p>
     * @param is
     * @param key
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(InputStream is, PrivateKey key, String algorithm) throws IOException, GeneralSecurityException {
        byte[] buf = new byte[SIGNATURE_BUFFER_SIZE];
        return sign(is, key, algorithm, buf);
    }

    /**
     * <p>Sign data from an InputStream using a private key and a specific algorithm.</p>
     * <p>After profiling this was hot-spot that dominated the AddFileTool's time. The only speed improvement to make is allowing a reusable buffer of bytes.</p>
     * @param is
     * @param key
     * @param algorithm
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final byte[] sign(InputStream is, PrivateKey key, String algorithm, byte[] buf) throws IOException, GeneralSecurityException {
        java.security.Signature sig = java.security.Signature.getInstance(algorithm);
        sig.initSign(key);
        for (int bytesRead = is.read(buf); bytesRead > 0; bytesRead = is.read(buf)) {
            sig.update(buf, 0, bytesRead);
        }
        return sig.sign();
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param bytes
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(byte[] bytes, String algorithm) throws IOException, GeneralSecurityException {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            return hash(bais, algorithm);
        } finally {
            IOUtil.safeClose(bais);
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param is
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(InputStream is, String algorithm) throws IOException, GeneralSecurityException {
        return hash(is, algorithm, new byte[SIGNATURE_BUFFER_SIZE]);
    }

    /**
     * <p>Lazy load resources used by utility methods.</p>
     */
    public static void lazyLoad() {
        if (uninitialized) {
            Security.addProvider(new BouncyCastleProvider());
            uninitialized = false;
        }
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param is
     * @param algorithm
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(InputStream is, String algorithm, byte[] buf) throws IOException, GeneralSecurityException {
        lazyLoad();
        MessageDigest md = MessageDigest.getInstance(algorithm, "BC");
        for (int bytesRead = is.read(buf); bytesRead > 0; bytesRead = is.read(buf)) {
            md.update(buf, 0, bytesRead);
        }
        return md.digest();
    }

    /**
     * <p>Retrieve the PrivateKey (used to sign bytes) from the system keystore.</p>
     * @param keystore
     * @param keystorePassword
     * @param alias
     * @param aliasPassword
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKeyFromKeyStore(String keystore, String keystorePassword, String alias, String aliasPassword) throws IOException, GeneralSecurityException {
        FileInputStream fis = new FileInputStream(keystore);
        PrivateKey key = null;
        try {
            key = getPrivateKeyFromKeyStore(fis, keystorePassword, alias, aliasPassword);
        } catch (Exception e) {
            LogUtil.logError(e);
        }
        fis.close();
        return key;
    }

    /**
     * <p>Retrieve the PrivateKey (used to sign bytes) from the system keystore.</p>
     * @param keystore
     * @param keystorePassword
     * @param alias
     * @param aliasPassword
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKeyFromKeyStore(InputStream keystore, String keystorePassword, String alias, String aliasPassword) throws IOException, GeneralSecurityException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(keystore, new String(keystorePassword).toCharArray());
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, new String(aliasPassword).toCharArray());
        return privateKey;
    }

    /**
     * <p>Returns certificate with priveldges: read only</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getAnonymousCertificate() throws IOException, GeneralSecurityException {
        if (anonCert == null) {
            anonCert = getCertificate(ConfigureTranche.openStreamToFile(anonCertificateLocation));
        }
        return anonCert;
    }

    /**
     * <p>Returns key with priveldges: read only</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final PrivateKey getAnonymousKey() throws IOException, GeneralSecurityException {
        if (anonKey == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = ConfigureTranche.openStreamToFile(anonPrivateKeyLocation);
            IOUtil.getBytes(is, baos);
            is.close();
            anonKey = getPrivateKey(baos.toByteArray());
        }
        return anonKey;
    }

    /**
     * <p>Returns certificate used for signing email to be sent from server.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getEmailCertificate() throws IOException, GeneralSecurityException {
        if (emailCert == null) {
            emailCert = getCertificate(ConfigureTranche.openStreamToFile(emailCertificateLocation));
        }
        return emailCert;
    }

    /**
     * <p>Returns key used for signing email to be sent from server.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final PrivateKey getEmailKey() throws IOException, GeneralSecurityException {
        if (emailKey == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = ConfigureTranche.openStreamToFile(emailPrivateKeyLocation);
            IOUtil.getBytes(is, baos);
            is.close();
            emailKey = getPrivateKey(baos.toByteArray());
        }
        return emailKey;
    }

    /**
     * <p>Returns certificate with priveldges: all</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getAdminCertificate() throws IOException, GeneralSecurityException {
        if (adminCert == null) {
            adminCert = getCertificate(ConfigureTranche.openStreamToFile(adminCertificateLocation));
        }
        return adminCert;
    }

    /**
     * <p>Returns certificate with priveldges: read, write, delete (not set configuration)</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getUserCertificate() throws IOException, GeneralSecurityException {
        if (userCert == null) {
            userCert = getCertificate(ConfigureTranche.openStreamToFile(userCertificateLocation));
        }
        return userCert;
    }

    /**
     * <p>Returns certificate with priveldges: read only</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getReadOnlyCertificate() throws IOException, GeneralSecurityException {
        if (readOnlyCert == null) {
            readOnlyCert = getCertificate(ConfigureTranche.openStreamToFile(readCertificateLocation));
        }
        return readOnlyCert;
    }

    /**
     * <p>Returns certificate with priveldges: write</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getWriteOnlyCertificate() throws IOException, GeneralSecurityException {
        if (writeOnlyCert == null) {
            writeOnlyCert = getCertificate(ConfigureTranche.openStreamToFile(writeCertificateLocation));
        }
        return writeOnlyCert;
    }

    /**
     * <p>Returns certificate with priveldges: read, write (no delete, no write configuration)</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getAutoCertCertificate() throws IOException, GeneralSecurityException {
        if (autoCert == null) {
            autoCert = getCertificate(ConfigureTranche.openStreamToFile(autocertCertificateLocation));
        }
        return autoCert;
    }

    /**
     * <p>Get User object representing adminstrative privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getAdmin() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getAdminCertificate());
        user.setFlags(User.ALL_PRIVILEGES);
        return user;
    }

    /**
     * <p>Get User object representing user privileges (read, write, delete). (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getUser() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getUserCertificate());
        user.setFlags(getUserFlags());
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: (super) user.</p>
     * @return
     */
    public static final int getUserFlags() {
        return User.CAN_DELETE_DATA | User.CAN_DELETE_META_DATA | User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing read-only privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getReadOnly() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getReadOnlyCertificate());
        user.setFlags(getReadOnlyFlags());
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: read-only.</p>
     * @return
     */
    public static final int getReadOnlyFlags() {
        return User.CAN_GET_CONFIGURATION | User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing anonymous, read-only privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getAnonymous() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getAnonymousCertificate());
        user.setFlags(getAnonymousFlags());
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: anonymous.</p>
     * @return
     */
    public static final int getAnonymousFlags() {
        return User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing write-only privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getWriteOnly() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getWriteOnlyCertificate());
        user.setFlags(getWriteOnlyFlags());
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: write-only.</p>
     * @return
     */
    public static final int getWriteOnlyFlags() {
        return User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing auto-certificate privileges (read, write). (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getAutoCert() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getAutoCertCertificate());
        user.setFlags(getAutoCertFlags());
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: auto-cert.</p>
     * @return
     */
    public static final int getAutoCertFlags() {
        return User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.VERSION_ONE;
    }

    /**
     * <p>Returns X509Certificate object serialized into bytes.</p>
     * @param bytes
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getCertificate(byte[] bytes) throws IOException, GeneralSecurityException {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            return getCertificate(bais);
        } finally {
            IOUtil.safeClose(bais);
        }
    }

    /**
     * <p>Helper method to load an X509 certificate from an input stream.</p>
     * @param in
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getCertificate(InputStream in) throws IOException, GeneralSecurityException {
        lazyLoad();
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        if (in == null) {
            throw new IOException("Certificate stream is null!");
        }
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        return cert;
    }

    /**
     * <p>Helper method to load an X509 certificate from a file.</p>
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static X509Certificate getCertificate(File file) throws IOException, GeneralSecurityException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return getCertificate(fis);
        } finally {
            IOUtil.safeClose(fis);
        }
    }

    /**
     * <p>Verify that the contents of an InputStream's bytes were signed by a certificate using a particular algorithm.</p>
     * @param is
     * @param digitalSignature
     * @param algorithm
     * @param cert
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static boolean verify(InputStream is, byte[] digitalSignature, String algorithm, Certificate cert) throws IOException, GeneralSecurityException {
        return verify(is, digitalSignature, algorithm, cert.getPublicKey());
    }

    /**
     * <p>Verify that the contents of an InputStream's bytes were signed by a public key using a particular algorithm.</p>
     * @param is
     * @param digitalSignature
     * @param algorithm
     * @param publicKey
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static boolean verify(InputStream is, byte[] digitalSignature, String algorithm, PublicKey publicKey) throws IOException, GeneralSecurityException {
        Signature verify = Signature.getInstance(algorithm);
        verify.initVerify(publicKey);
        byte[] buf = new byte[512];
        for (int bytesRead = is.read(buf); bytesRead > 0; bytesRead = is.read(buf)) {
            verify.update(buf, 0, bytesRead);
        }
        return verify.verify(digitalSignature);
    }

    /**
     * <p>Helper method to convert certificate's into unique names.</p>
     * @param cert
     * @return
     */
    public static String getMD5Name(X509Certificate cert) {
        try {
            byte[] md5 = SecurityUtil.hash(cert.getEncoded(), "md5");
            return Base16.encode(md5);
        } catch (Exception e) {
        }
        throw new RuntimeException("Can't create MD5 name for certificate.");
    }

    /**
     * <p>Extract the signature algorithm used with specified PublicKey.</p>
     * @param key
     * @return
     */
    public static String getSignatureAlgorithm(PublicKey key) {
        String signatureAlgorithm = "SHA1withRSA";
        if (key instanceof DSAPublicKey) {
            signatureAlgorithm = "SHA1withDSA";
        }
        return signatureAlgorithm;
    }

    /**
     * <p>Extract the signature algorithm used with specified PrivateKey.</p>
     * @param key
     * @return
     */
    public static String getSignatureAlgorithm(PrivateKey key) {
        String signatureAlgorithm = "SHA1withRSA";
        if (key instanceof DSAPrivateKey) {
            signatureAlgorithm = "SHA1withDSA";
        }
        return signatureAlgorithm;
    }

    /**
     * <p>Load the PrivateKey serialized to a file.</p>
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKey(File file) throws IOException, GeneralSecurityException {
        SecurityUtil.lazyLoad();
        byte[] keyBytes = IOUtil.getBytes(file);
        return getPrivateKey(keyBytes);
    }

    /**
     * <p>Load the PrivateKey serialized to a byte array.</p>
     * @param keyBytes
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKey(byte[] keyBytes) throws IOException, GeneralSecurityException {
        SecurityUtil.lazyLoad();
        PKCS8EncodedKeySpec newkspec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
        return kf.generatePrivate(newkspec);
    }

    /**
     * <p>Retrieve the default X.509 certificate used by tool. Unless configured otherwise, this is the anonymous certificate.</p>
     * @return
     */
    public static X509Certificate getDefaultCertificate() {
        if (defaultCertificate == null) {
            try {
                defaultCertificate = getAnonymousCertificate();
            } catch (Exception e) {
            }
        }
        return defaultCertificate;
    }

    /**
     * <p>Set the default X.509 certificate used by the tool. If not specified, uses the anonymous certificate.</p>
     * @param aDefaultCertificate
     */
    public static void setDefaultCertificate(X509Certificate aDefaultCertificate) {
        defaultCertificate = aDefaultCertificate;
    }

    /**
     * <p>Retrieve the default private key used by the tool. If not specified, uses the anonymous key.</p>
     * @return
     */
    public static PrivateKey getDefaultKey() {
        if (defaultKey == null) {
            try {
                defaultKey = getAnonymousKey();
            } catch (Exception e) {
            }
        }
        return defaultKey;
    }

    /**
     * <p>Set the default private key used by the tool. If not specified, uses the anonymous key.</p>
     * @param aDefaultKey
     */
    public static void setDefaultKey(PrivateKey aDefaultKey) {
        defaultKey = aDefaultKey;
    }

    /**
     * <p>Encrypts a file using AES and a passphrase.</p>
     * @param passphrase
     * @param file
     * @return
     * @throws java.io.IOException
     */
    public static File encryptDiskBacked(String passphrase, File file) throws IOException {
        AESFastEngine encrypt = new AESFastEngine();
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), ENCRYPTION_SALT, ENCRYPTION_ITERATIONS);
        CipherParameters params = pg.generateDerivedParameters(256);
        encrypt.init(true, params);
        int blockSize = encrypt.getBlockSize();
        File encryptedFile = TempFileUtil.createTemporaryFile();
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        java.io.BufferedOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            fos = new FileOutputStream(encryptedFile);
            bos = new java.io.BufferedOutputStream(fos);
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            int bytesRead = 0;
            for (bytesRead = bis.read(data); bytesRead == blockSize; bytesRead = bis.read(data)) {
                encrypt.processBlock(data, 0, encrypted, 0);
                bos.write(encrypted);
            }
            if (bytesRead == -1) {
                bytesRead = 0;
            }
            int paddingLength = data.length - bytesRead;
            for (int i = bytesRead; i < data.length; i++) {
                data[i] = (byte) (0xff & paddingLength);
            }
            encrypt.processBlock(data, 0, encrypted, 0);
            bos.write(encrypted);
            return encryptedFile;
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(bos);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>In-memory version of encryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param dataBytes
     * @return
     * @throws java.io.IOException
     */
    public static byte[] encryptInMemory(String passphrase, byte[] dataBytes) throws IOException {
        AESFastEngine encrypt = new AESFastEngine();
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), ENCRYPTION_SALT, ENCRYPTION_ITERATIONS);
        CipherParameters params = pg.generateDerivedParameters(256);
        encrypt.init(true, params);
        int blockSize = encrypt.getBlockSize();
        ByteArrayInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream fos = null;
        try {
            fis = new ByteArrayInputStream(dataBytes);
            bis = new BufferedInputStream(fis);
            fos = new ByteArrayOutputStream();
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            int bytesRead = 0;
            for (bytesRead = bis.read(data); bytesRead == blockSize; bytesRead = bis.read(data)) {
                encrypt.processBlock(data, 0, encrypted, 0);
                fos.write(encrypted);
            }
            if (bytesRead == -1) {
                bytesRead = 0;
            }
            int paddingLength = data.length - bytesRead;
            for (int i = bytesRead; i < data.length; i++) {
                data[i] = (byte) (0xff & paddingLength);
            }
            encrypt.processBlock(data, 0, encrypted, 0);
            fos.write(encrypted);
            return fos.toByteArray();
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>Decrypt an AES-encrypted file using a specified passphrase.</p>
     * @param passphrase
     * @param file
     * @return
     * @throws WrongPassphraseException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static File decryptDiskBacked(String passphrase, File file) throws WrongPassphraseException, IOException, GeneralSecurityException {
        return decryptDiskBacked(passphrase, file, null);
    }

    /**
     * <p>Decrypt an AES-encrypted file using a specified passphrase.</p>
     * @param passphrase
     * @param file
     * @param expectedHash
     * @return
     * @throws WrongPassphraseException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static File decryptDiskBacked(String passphrase, File file, BigHash expectedHash) throws WrongPassphraseException, IOException, GeneralSecurityException {
        if (passphrase == null) {
            throw new PassphraseRequiredException("Can't decrypt file. No passphrase specified.");
        }
        DebugUtil.debugOut(SecurityUtil.class, "Decrypting " + file.getAbsolutePath() + " using passphrase " + passphrase);
        AESFastEngine encrypt = new AESFastEngine();
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), ENCRYPTION_SALT, ENCRYPTION_ITERATIONS);
        CipherParameters params = pg.generateDerivedParameters(256);
        encrypt.init(false, params);
        int blockSize = encrypt.getBlockSize();
        File encryptedFile = TempFileUtil.createTemporaryFile();
        BigHashMaker bhm = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            if (expectedHash != null) {
                bhm = new BigHashMaker();
            }
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            fos = new FileOutputStream(encryptedFile);
            bos = new BufferedOutputStream(fos);
            int round = 0, bufferBlocks = 10;
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            byte[] encryptedBuffer = new byte[blockSize * bufferBlocks];
            int offset = 0;
            for (int bytesRead = bis.read(data, offset, data.length - offset); bytesRead != -1; bytesRead = bis.read(data, offset, data.length - offset)) {
                if (bytesRead + offset != data.length) {
                    offset += bytesRead;
                    continue;
                }
                offset = 0;
                encrypt.processBlock(data, 0, encrypted, 0);
                if (round >= bufferBlocks) {
                    bos.write(encryptedBuffer, 0, blockSize);
                    if (bhm != null) {
                        bhm.update(encryptedBuffer, 0, blockSize);
                    }
                    for (int i = 1; i < bufferBlocks - 1; i++) {
                        System.arraycopy(encryptedBuffer, blockSize * i, encryptedBuffer, blockSize * (i - 1), blockSize);
                    }
                    System.arraycopy(encryptedBuffer, blockSize * (bufferBlocks - 1), encryptedBuffer, blockSize * (bufferBlocks - 2), encryptedBuffer.length - (blockSize * (bufferBlocks - 1)));
                    System.arraycopy(encrypted, 0, encryptedBuffer, blockSize * (bufferBlocks - 1), encrypted.length);
                } else {
                    System.arraycopy(encrypted, 0, encryptedBuffer, blockSize * round, encrypted.length);
                }
                round++;
            }
            int paddingLength = (int) (0xff & encryptedBuffer[encryptedBuffer.length - 1]);
            if (paddingLength < 0) {
                DebugUtil.debugOut(SecurityUtil.class, "Expected Padding length: " + paddingLength);
                DebugUtil.debugOut(SecurityUtil.class, "Buffer length: " + encryptedBuffer.length);
                throw new WrongPassphraseException();
            } else if (paddingLength > encryptedBuffer.length) {
                paddingLength = encryptedBuffer.length;
            }
            bos.write(encryptedBuffer, 0, encryptedBuffer.length - paddingLength);
            bos.flush();
            if (bhm != null) {
                bhm.update(encryptedBuffer, 0, encryptedBuffer.length - paddingLength);
                BigHash actualHash = BigHash.createFromBytes(bhm.finish());
                if (!actualHash.equals(expectedHash)) {
                    DebugUtil.debugOut(SecurityUtil.class, "Expected " + expectedHash + " (" + expectedHash.getLength() + ") but actually " + actualHash + " (" + actualHash.getLength() + ")");
                    throw new WrongPassphraseException();
                }
            }
            return encryptedFile;
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(bos);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>In-memory version of decryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param dataBytes
     * @return
     * @throws WrongPassphraseException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static byte[] decryptInMemory(String passphrase, byte[] dataBytes) throws WrongPassphraseException, IOException, GeneralSecurityException {
        return decryptInMemory(passphrase, dataBytes, null);
    }

    /**
     * <p>In-memory version of decryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param dataBytes
     * @param expectedHash
     * @return
     * @throws WrongPassphraseException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static byte[] decryptInMemory(String passphrase, byte[] dataBytes, BigHash expectedHash) throws WrongPassphraseException, IOException, GeneralSecurityException {
        if (passphrase == null) {
            throw new PassphraseRequiredException("Can't decrypt file. No passphrase specified.");
        }
        DebugUtil.debugOut(SecurityUtil.class, "Decrypting file in memory using passphrase " + passphrase);
        AESFastEngine encrypt = new AESFastEngine();
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), ENCRYPTION_SALT, ENCRYPTION_ITERATIONS);
        CipherParameters params = pg.generateDerivedParameters(256);
        encrypt.init(false, params);
        int blockSize = encrypt.getBlockSize();
        BigHashMaker bhm = null;
        ByteArrayInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream bos = null;
        try {
            if (expectedHash != null) {
                bhm = new BigHashMaker();
            }
            fis = new ByteArrayInputStream(dataBytes);
            bis = new BufferedInputStream(fis);
            bos = new ByteArrayOutputStream();
            int round = 0, bufferBlocks = 10;
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            byte[] encryptedBuffer = new byte[blockSize * bufferBlocks];
            int offset = 0;
            for (int bytesRead = bis.read(data, offset, data.length - offset); bytesRead != -1; bytesRead = bis.read(data, offset, data.length - offset)) {
                if (bytesRead + offset != data.length) {
                    offset += bytesRead;
                    continue;
                }
                offset = 0;
                encrypt.processBlock(data, 0, encrypted, 0);
                if (round >= bufferBlocks) {
                    bos.write(encryptedBuffer, 0, blockSize);
                    if (bhm != null) {
                        bhm.update(encryptedBuffer, 0, blockSize);
                    }
                    for (int i = 1; i < bufferBlocks - 1; i++) {
                        System.arraycopy(encryptedBuffer, blockSize * i, encryptedBuffer, blockSize * (i - 1), blockSize);
                    }
                    System.arraycopy(encryptedBuffer, blockSize * (bufferBlocks - 1), encryptedBuffer, blockSize * (bufferBlocks - 2), encryptedBuffer.length - (blockSize * (bufferBlocks - 1)));
                    System.arraycopy(encrypted, 0, encryptedBuffer, blockSize * (bufferBlocks - 1), encrypted.length);
                } else {
                    System.arraycopy(encrypted, 0, encryptedBuffer, blockSize * round, encrypted.length);
                }
                round++;
            }
            int paddingLength = (int) (0xff & encryptedBuffer[encryptedBuffer.length - 1]);
            if (paddingLength < 0) {
                DebugUtil.debugOut(SecurityUtil.class, "Expected Padding length: " + paddingLength);
                DebugUtil.debugOut(SecurityUtil.class, "Buffer length: " + encryptedBuffer.length);
                throw new WrongPassphraseException();
            } else if (paddingLength > encryptedBuffer.length) {
                paddingLength = encryptedBuffer.length;
            }
            bos.write(encryptedBuffer, 0, encryptedBuffer.length - paddingLength);
            bos.flush();
            if (bhm != null) {
                bhm.update(encryptedBuffer, 0, encryptedBuffer.length - paddingLength);
                BigHash actualHash = BigHash.createFromBytes(bhm.finish());
                if (!actualHash.equals(expectedHash)) {
                    DebugUtil.debugOut(SecurityUtil.class, "Expected " + expectedHash + " (" + expectedHash.getLength() + ") but actually " + actualHash + " (" + actualHash.getLength() + ")");
                    throw new WrongPassphraseException();
                }
            }
            return bos.toByteArray();
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(bos);
        }
    }

    /**
     * <p>Checks to see whether certificate is signed by the loaded certificates.</p>
     * <p>Primary use case is to help decide on client's side whether to perform certain network-specific actions. The content will still be validated server-side.</p>
     * @param cert The certificate to check
     * @return True if signed by the default certs, false otherwise
     * @throws java.lang.Exception
     */
    public static boolean isCertificateSignedByDefaultCerts(X509Certificate cert) throws Exception {
        Set<X509Certificate> pcCerts = new HashSet();
        pcCerts.add(SecurityUtil.getAdminCertificate());
        pcCerts.add(SecurityUtil.getAutoCertCertificate());
        pcCerts.add(SecurityUtil.getReadOnlyCertificate());
        pcCerts.add(SecurityUtil.getUserCertificate());
        pcCerts.add(SecurityUtil.getWriteOnlyCertificate());
        for (X509Certificate pcCert : pcCerts) {
            if (certificateNamesMatch(UserCertificateUtil.readUserName(pcCert), UserCertificateUtil.readIssuerName(cert))) {
                try {
                    cert.verify(pcCert.getPublicKey());
                    return true;
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        return false;
    }

    /**
     * <p>A utility method to check whether two certificate names match.</p>
     * <p>Not sufficient for security, but a fast way to check whether found a matching cert name before more expensive security checks.</p>
     * @param a
     * @param b
     * @return
     */
    public static boolean certificateNamesMatch(String a, String b) {
        String[] partsA = a.split(", *");
        Arrays.sort(partsA);
        String[] partsB = b.split(", *");
        Arrays.sort(partsB);
        if (partsA.length != partsB.length) {
            return false;
        }
        for (int i = 0; i < partsA.length; i++) {
            if (!partsA[i].equals(partsB[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Set the administrator X.509 certificate.</p>
     * @param cert
     */
    public static void setAdminCert(X509Certificate cert) {
        adminCert = cert;
    }

    /**
     * <p>Set the file path to the administrator X.509 certificate.</p>
     * @param adminCertificateLocation
     */
    public static void setAdminCertLocation(String adminCertificateLocation) {
        if (SecurityUtil.adminCertificateLocation == null || !SecurityUtil.adminCertificateLocation.equals(adminCertificateLocation)) {
            adminCert = null;
            SecurityUtil.adminCertificateLocation = adminCertificateLocation;
            DebugUtil.debugOut(SecurityUtil.class, "admin: " + adminCertificateLocation);
        }
    }

    /**
     * <p>Set the user (read, write, delete) X.509 certificate.</p>
     * @param cert
     */
    public static void setUserCert(X509Certificate cert) {
        userCert = cert;
    }

    /**
     * <p>Set the file path to the user (read, write, delete) X.509 certificate.</p>
     * @param userCertificateLocation
     */
    public static void setUserCertLocation(String userCertificateLocation) {
        if (SecurityUtil.userCertificateLocation == null || !SecurityUtil.userCertificateLocation.equals(userCertificateLocation)) {
            userCert = null;
            SecurityUtil.userCertificateLocation = userCertificateLocation;
            DebugUtil.debugOut(SecurityUtil.class, "user: " + userCertificateLocation);
        }
    }

    /**
     * <p>Set the write-only X.509 certificate.</p>
     * @param cert
     */
    public static void setWriteCert(X509Certificate cert) {
        writeOnlyCert = cert;
    }

    /**
     * <p>Set the file path to the write-only X.509 certificate.</p>
     * @param writeCertificateLocation
     */
    public static void setWriteCertLocation(String writeCertificateLocation) {
        if (SecurityUtil.writeCertificateLocation == null || !SecurityUtil.writeCertificateLocation.equals(writeCertificateLocation)) {
            writeOnlyCert = null;
            SecurityUtil.writeCertificateLocation = writeCertificateLocation;
            DebugUtil.debugOut(SecurityUtil.class, "write: " + writeCertificateLocation);
        }
    }

    /**
     * <p>Set the read-only X.509 certificate.</p>
     * @param cert
     */
    public static void setReadCert(X509Certificate cert) {
        readOnlyCert = cert;
    }

    /**
     * <p>Set the file path to the read-only X.509 certificate.</p>
     * @param readCertificateLocation
     */
    public static void setReadCertLocation(String readCertificateLocation) {
        if (SecurityUtil.readCertificateLocation == null || !SecurityUtil.readCertificateLocation.equals(readCertificateLocation)) {
            readOnlyCert = null;
            SecurityUtil.readCertificateLocation = readCertificateLocation;
            DebugUtil.debugOut(SecurityUtil.class, "read: " + readCertificateLocation);
        }
    }

    /**
     * <p>Set the auto-certificate (read, write) X.509 certificate.</p>
     * @param cert
     */
    public static void setAutoCertCert(X509Certificate cert) {
        autoCert = cert;
    }

    /**
     * <p>Set the file path to the read-only X.509 certificate.</p>
     * @param autocertCertificateLocation
     */
    public static void setAutocertCertLocation(String autocertCertificateLocation) {
        if (SecurityUtil.autocertCertificateLocation == null || !SecurityUtil.autocertCertificateLocation.equals(autocertCertificateLocation)) {
            SecurityUtil.autoCert = null;
            SecurityUtil.autocertCertificateLocation = autocertCertificateLocation;
            DebugUtil.debugOut(SecurityUtil.class, "auto: " + autocertCertificateLocation);
        }
    }

    /**
     * <p>Set the anonymous, read-only X.509 certificate.</p>
     * @param cert
     */
    public static void setAnonCert(X509Certificate cert) {
        anonCert = cert;
    }

    /**
     * <p>Set the anonymous, read-only private key.</p>
     * @param key
     */
    public static void setAnonKey(PrivateKey key) {
        anonKey = key;
    }

    /**
     * <p>Set the file path to the anonymous, read-only X.509 certificate.</p>
     * @param anonCertificateLocation
     */
    public static void setAnonCertLocation(String anonCertificateLocation) {
        if (SecurityUtil.anonCertificateLocation == null || !SecurityUtil.anonCertificateLocation.equals(anonCertificateLocation)) {
            anonCert = null;
            SecurityUtil.anonCertificateLocation = anonCertificateLocation;
            DebugUtil.debugOut(SecurityUtil.class, "anon (cert): " + anonCertificateLocation);
        }
    }

    /**
     * <p>Set the file path to the anonymous, read-only private key.</p>
     * @param anonPrivateKeyLocation
     */
    public static void setAnonKeyLocation(String anonPrivateKeyLocation) {
        if (SecurityUtil.anonPrivateKeyLocation == null || !SecurityUtil.anonPrivateKeyLocation.equals(anonPrivateKeyLocation)) {
            anonKey = null;
            SecurityUtil.anonPrivateKeyLocation = anonPrivateKeyLocation;
            DebugUtil.debugOut(SecurityUtil.class, "anon (key): " + anonCertificateLocation);
        }
    }

    /**
     * <p>Set the X.509 certificate used to sign email data to be sent by server.</p>
     * @param cert
     */
    public static void setEmailCert(X509Certificate cert) {
        emailCert = cert;
    }

    /**
     * <p>Set the private key used to sign email data to be sent by server.</p>
     * @param key
     */
    public static void setEmailKey(PrivateKey key) {
        emailKey = key;
    }

    /**
     * <p>Set the file path to the certificate used for signing email to be sent by server.</p>
     * @param emailCertificateLocation
     */
    public static void setEmailCertLocation(String emailCertificateLocation) {
        if (SecurityUtil.emailCertificateLocation == null || !SecurityUtil.emailCertificateLocation.equals(emailCertificateLocation)) {
            emailCert = null;
            SecurityUtil.emailCertificateLocation = emailCertificateLocation;
        }
    }

    /**
     * <p>Set the file path to the private key used for signing email to be sent by server.</p>
     * @param emailPrivateKeyLocation
     */
    public static void setEmailKeyLocation(String emailPrivateKeyLocation) {
        if (SecurityUtil.emailPrivateKeyLocation == null || !SecurityUtil.emailPrivateKeyLocation.equals(emailPrivateKeyLocation)) {
            emailKey = null;
            SecurityUtil.emailPrivateKeyLocation = emailPrivateKeyLocation;
        }
    }
}
