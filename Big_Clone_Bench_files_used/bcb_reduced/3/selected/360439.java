package net.sf.opengroove.common.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.security.Certificate;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * This class contains utilities related to X.509 certificates.<br/><br/>
 * 
 * For methods that read and write cryptographic objects (such as a certificate
 * chain or a private key), the output is typically in PEM format. The read
 * methods will throw a ClassCastException if the PEM input is not of the
 * specified type. This would happen if, for example, a certificate PEM string
 * (IE one beginning with "-----BEGIN CERTIFICATE-----") was passed into a
 * method that reads private keys.
 * 
 * @author Alexander Boyd
 * 
 */
public class CertificateUtils {

    private static final char[] pass = "pass".toCharArray();

    /**
     * Reads a pair of a certificate chain and a private key
     * 
     * @param in
     * @return
     */
    public static CertPair readCertPair(String in) {
        try {
            PEMReader reader = new PEMReader(new StringReader(in));
            KeyPair pair = (KeyPair) reader.readObject();
            CertPair certpair = new CertPair();
            certpair.setKey(pair.getPrivate());
            ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
            X509Certificate cert;
            while ((cert = (X509Certificate) reader.readObject()) != null) {
                certs.add(cert);
            }
            certpair.setChain(certs.toArray(new X509Certificate[0]));
            return certpair;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static String writeCertPair(CertPair pair) {
        try {
            StringWriter sw = new StringWriter();
            PEMWriter writer = new PEMWriter(sw);
            writer.writeObject(pair.getKey());
            for (X509Certificate cert : pair.getChain()) {
                writer.writeObject(cert);
            }
            writer.flush();
            return sw.toString();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static X509Certificate readCert(String in) {
        try {
            PEMReader reader = new PEMReader(new StringReader(in));
            return (X509Certificate) reader.readObject();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static String writeCert(X509Certificate cert) {
        try {
            StringWriter sw = new StringWriter();
            PEMWriter writer = new PEMWriter(sw);
            writer.writeObject(cert);
            writer.flush();
            return sw.toString();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static X509Certificate[] readCertChain(String in) {
        try {
            PEMReader reader = new PEMReader(new StringReader(in));
            ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
            X509Certificate cert;
            while ((cert = (X509Certificate) reader.readObject()) != null) {
                certs.add(cert);
            }
            return certs.toArray(new X509Certificate[0]);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static String writeCertChain(X509Certificate[] chain) {
        try {
            StringWriter sw = new StringWriter();
            PEMWriter writer = new PEMWriter(sw);
            for (X509Certificate cert : chain) {
                writer.writeObject(cert);
            }
            writer.flush();
            return sw.toString();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static PrivateKey readPrivateKey(String in) {
        try {
            PEMReader reader = new PEMReader(new StringReader(in));
            return ((KeyPair) reader.readObject()).getPrivate();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static String writePrivateKey(PrivateKey key) {
        try {
            StringWriter sw = new StringWriter();
            PEMWriter writer = new PEMWriter(sw);
            writer.writeObject(key);
            writer.flush();
            return sw.toString();
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    /**
     * Same as
     * {@link #createCert(X500Principal, X500Principal, Date, Date, String, PublicKey, PrivateKey)},
     * but generates a certificate that is valid starting now and for the number
     * of days specified.
     * 
     * @param subject
     * @param issuer
     * @param days
     *            The number of days that this certificate is valid for
     * @param sigalg
     * @param subjectKey
     * @param issuerKey
     * @return
     */
    public static X509Certificate createCert(X500Principal subject, X500Principal issuer, int days, String sigalg, PublicKey subjectKey, PrivateKey issuerKey) {
        return createCert(subject, issuer, new Date(), new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS)), sigalg, subjectKey, issuerKey);
    }

    /**
     * Generates a new X.509 certificate, signed by the authority specified.
     * 
     * @param subject
     *            The subject that this certificate is being issued to
     * @param issuer
     *            The issuer of this certificate
     * @param start
     *            The start date at which the certificate is valid
     * @param end
     *            The end date after which the certificate is not valid
     * @param sigalg
     *            The signature algorithm, which should correspond with the
     *            issuer's key. If this is null, "SHA512withRSA" is used, which
     *            requires issuerKey to be an RSAPrivateKey.
     * @param subjectKey
     *            The public key of this certificate's subject
     * @param issuerKey
     *            The private key of this certificate's issuer
     * @return A new certificate for the subject, signed by the issuer
     */
    public static X509Certificate createCert(X500Principal subject, X500Principal issuer, Date start, Date end, String sigalg, PublicKey subjectKey, PrivateKey issuerKey) {
        try {
            X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
            gen.setIssuerDN(issuer);
            gen.setNotBefore(start);
            gen.setNotAfter(end);
            gen.setSerialNumber(new BigInteger("" + System.currentTimeMillis()));
            if (sigalg == null) sigalg = "SHA512withRSA";
            gen.setSignatureAlgorithm(sigalg);
            gen.setSubjectDN(subject);
            gen.setPublicKey(subjectKey);
            X509Certificate cert = gen.generate(issuerKey);
            return cert;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static KeyPair createKeyPair(String algorithm, int size) {
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance(algorithm);
            keygen.initialize(size);
            KeyPair keys = keygen.generateKeyPair();
            return keys;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    /**
     * Generates an RSA 3072-bit keypair.
     * 
     * @return
     */
    public static KeyPair createKeyPair() {
        return createKeyPair(3072);
    }

    /**
     * Creates an RSA keypair of the specified size.
     * 
     * @param size
     *            The size, in bits, of the keypair. The private, or decryption,
     *            exponent and the modulus will have this many bits.
     * @return
     */
    public static KeyPair createKeyPair(int size) {
        return createKeyPair("RSA", size);
    }

    public static String fingerprint(X509Certificate certificate) {
        return UserFingerprint.fingerprint(writeCert(certificate));
    }

    public static String fingerprint(X509Certificate[] chain) {
        return UserFingerprint.fingerprint(writeCertChain(chain));
    }

    /**
     * Checks that the signatures in this chain are valid. Specifically, for
     * each pair of certificates made up of chain[n] and chain[n+1], where n <
     * chain.length - 1 and n >= 0, chain[n].{@link X509Certificate#verify(PublicKey) verify}
     * (chain[n+1].{@link X509Certificate#getPublicKey() getPublicKey}()) is
     * called, and if it throws an exception, false is returned from this
     * method. If none of the invocations throw an exception, true is returned.
     * 
     * @param chain
     *            The chain to check
     * @return True if all signatures are valid, false otherwise
     */
    public static boolean checkSignatureChainValid(X509Certificate[] chain) {
        for (int i = 0; i < chain.length - 1; i++) {
            try {
                chain[i].verify(chain[i + 1].getPublicKey());
            } catch (Exception e) {
                if (!(e instanceof SignatureException)) e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean checkChainDateValid(X509Certificate[] chain) {
        for (X509Certificate cert : chain) {
            try {
                cert.checkValidity();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the two certificates are equal by encoding them using the
     * writeCert method and checking to see if the resulting encodings are
     * equals.
     * 
     * @param c1
     *            The first certificate to compare
     * @param c2
     *            The second certificate to compare
     * @return True if c1 is equal to c2, false otherwise.
     */
    public static boolean isEncodedEqual(X509Certificate c1, X509Certificate c2) {
        return writeCert(c1).equals(writeCert(c2));
    }

    /**
     * Generates a symmetric key with the specified algorithm and size.
     * 
     * @param algorithm
     * @param size
     * @return
     */
    public static Key generateSymmetricKey(String algorithm, int size) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(algorithm);
            generator.init(size);
            return generator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates an AES-256 key.
     * 
     * @return
     */
    public static Key generateSymmetricKey() {
        return generateSymmetricKey("AES", 256);
    }

    public byte[] encryptAES(byte[] bytes, byte[] key) {
        try {
            SecretKeySpec spec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(new byte[8]));
            byte[] data = cipher.doFinal(bytes);
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] decryptAES(byte[] bytes, byte[] key) {
        try {
            SecretKeySpec spec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(new byte[8]));
            byte[] data = cipher.doFinal(bytes);
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hash(InputStream stream) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] bytes = new byte[1024];
        int i;
        while ((i = stream.read(bytes)) != -1) {
            digest.update(bytes, 0, i);
        }
        stream.close();
        return digest.digest();
    }

    public static byte[] encryptRsa(byte[] bytes, BigInteger key, BigInteger modulus) {
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, key);
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA512AndMGF1Padding");
            cipher.init(cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePrivate(keySpec), new SecureRandom());
            byte[] result = cipher.doFinal(bytes);
            return result;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }

    public static byte[] decryptRsa(byte[] bytes, BigInteger key, BigInteger modulus) {
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, key);
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA512AndMGF1Padding");
            cipher.init(cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(keySpec), new SecureRandom());
            byte[] result = cipher.doFinal(bytes);
            return result;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Cryptographic exception", e);
        }
    }
}
