package org.xmldap.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.sourceforge.lightcrypto.Crypt;
import net.sourceforge.lightcrypto.SafeObject;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.util.Base64;

/**
 * CryptUtils provides a number of general purpose crypto utilities.  Digests, encrypt, decrupt, etc
 *
 * @author charliemortimore at gmail.com
 */
public class CryptoUtils {

    public static String convertSigningAlgorithm(String signatureAlgorithm) throws SerializationException {
        if (signatureAlgorithm.equalsIgnoreCase("SHA1withRSA")) {
            return "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        }
        if (signatureAlgorithm.equalsIgnoreCase("SHA256withRSA")) {
            return "http://www.w3.org/2000/09/xmldsig#rsa-sha256";
        }
        throw new SerializationException("unsupported signature algorithm");
    }

    public static String convertMessageDigestAlgorithm(String signatureAlgorithm) throws SerializationException {
        if (signatureAlgorithm.equalsIgnoreCase("SHA")) {
            return "http://www.w3.org/2000/09/xmldsig#sha1";
        }
        if (signatureAlgorithm.equalsIgnoreCase("SHA1")) {
            return "http://www.w3.org/2000/09/xmldsig#sha1";
        }
        if (signatureAlgorithm.equalsIgnoreCase("SHA-1")) {
            return "http://www.w3.org/2000/09/xmldsig#sha1";
        }
        if (signatureAlgorithm.equalsIgnoreCase("SHA256")) {
            return "http://www.w3.org/2000/09/xmldsig#sha256";
        }
        if (signatureAlgorithm.equalsIgnoreCase("SHA-256")) {
            return "http://www.w3.org/2000/09/xmldsig#sha256";
        }
        throw new SerializationException("unsupported signature algorithm");
    }

    public static String convertMessageDigestAlgorithmUrl(String signatureAlgorithmUrl) throws CryptoException {
        if (signatureAlgorithmUrl.equals("http://www.w3.org/2000/09/xmldsig#sha1")) {
            return "SHA";
        }
        if (signatureAlgorithmUrl.equals("http://www.w3.org/2001/04/xmlenc#sha256")) {
            return "SHA256";
        }
        if (signatureAlgorithmUrl.equals("http://www.w3.org/2000/09/xmldsig#sha256")) {
            return "SHA256";
        }
        throw new CryptoException("unsupported signature algorithm");
    }

    public static byte[] byteDigest(byte[] data, String messageDigestAlgorithm) throws CryptoException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(messageDigestAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        md.reset();
        md.update(data);
        return md.digest();
    }

    /**
     * Creates a Base64 encoded Digest of a byte[]
     *
     * @param data the data to digest
     * @return Base64 encoded digest of the data
     * @throws CryptoException
     */
    public static String digest(byte[] data, String messageDigestAlgorithm) throws CryptoException {
        return Base64.encodeBytesNoBreaks(byteDigest(data, messageDigestAlgorithm));
    }

    /**
   * @param keys
   * @param iv
   * @param dataBytes
   * @return
   * @throws CryptoException
   */
    public static byte[] aesCbcEncrypt(byte[] encryptionKey, byte[] iv, ByteArrayOutputStream dataBytes) throws CryptoException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            AESLightEngine aes = new AESLightEngine();
            CBCBlockCipher cbc = new CBCBlockCipher(aes);
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(cbc);
            KeyParameter key = new KeyParameter(encryptionKey);
            ParametersWithIV paramWithIV = new ParametersWithIV(key, iv);
            byte inputBuffer[] = new byte[16];
            byte outputBuffer[] = new byte[16];
            int bytesProcessed = 0;
            cipher.init(true, paramWithIV);
            int bytesRead = 0;
            ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes.toByteArray());
            while ((bytesRead = inputStream.read(inputBuffer)) > 0) {
                bytesProcessed = cipher.processBytes(inputBuffer, 0, bytesRead, outputBuffer, 0);
                if (bytesProcessed > 0) outputStream.write(outputBuffer, 0, bytesProcessed);
            }
            bytesProcessed = cipher.doFinal(outputBuffer, 0);
            if (bytesProcessed > 0) outputStream.write(outputBuffer, 0, bytesProcessed);
        } catch (Exception e) {
            throw new CryptoException("Error encrypting data", e);
        }
        byte[] cipherText = outputStream.toByteArray();
        return cipherText;
    }

    /**
     * Encrypts data using AES with a Chained Block Cipber.   Got rid of my own impl, and wrapped lightcrypto
     *
     * @param text     The data to encrypt
     * @param keybytes the key
     * @return the cipherText in a Stringbuffer
     * @throws CryptoException
     */
    public static StringBuffer encryptAESCBC(StringBuffer text, SafeObject keybytes) throws CryptoException {
        StringBuffer cipherText;
        try {
            cipherText = Crypt.encrypt(text, keybytes);
        } catch (net.sourceforge.lightcrypto.CryptoException e) {
            throw new CryptoException(e);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
        return cipherText;
    }

    /**
     * Decryptes AES with a Chained Block Cipher - wraps lightcrypto
     *
     * @param text     The cipher text
     * @param keybytes the decryptiong key
     * @return clearText in a StringBuffer
     * @throws CryptoException
     */
    public static StringBuffer decryptAESCBC(StringBuffer text, SafeObject keybytes) throws CryptoException {
        StringBuffer clearText;
        try {
            clearText = Crypt.decrypt(text, keybytes);
        } catch (net.sourceforge.lightcrypto.CryptoException e) {
            throw new CryptoException(e);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
        return clearText;
    }

    /**
     * Encrypts using RSA with OAEP
     *
     * @param input the clear text to encrypt
     * @param cert  the certificate to use for encryption
     * @return the base64 encoded cipher text
     * @throws CryptoException
     */
    public static byte[] rsaoaepEncryptBytes(byte[] input, RSAPublicKey rsaPublicKey) throws CryptoException {
        AsymmetricBlockCipher engine = new RSAEngine();
        OAEPEncoding cipher = new OAEPEncoding(engine);
        BigInteger mod = rsaPublicKey.getModulus();
        BigInteger exp = rsaPublicKey.getPublicExponent();
        RSAKeyParameters keyParams = new RSAKeyParameters(false, mod, exp);
        cipher.init(true, keyParams);
        int inputBlockSize = cipher.getInputBlockSize();
        int outputBlockSize = cipher.getOutputBlockSize();
        byte[] cipherText;
        try {
            cipherText = cipher.processBlock(input, 0, input.length);
        } catch (InvalidCipherTextException e) {
            throw new CryptoException(e);
        }
        return cipherText;
    }

    public static String rsaoaepEncrypt(byte[] input, RSAPublicKey rsaPublicKey) throws CryptoException {
        byte[] cipherText = rsaoaepEncryptBytes(input, rsaPublicKey);
        return Base64.encodeBytesNoBreaks(cipherText);
    }

    /**
     * Decrypts base 64 encoded data using RSA OAEP and the provided Key
     *
     * @param b64EncodedData the base 64 encoded ciphertext
     * @param inputKey            the private key to use for decryption
     * @return a byte[] of clear text
     * @throws CryptoException
     */
    public static byte[] decryptRSAOAEP(byte[] cipherText, PrivateKey inputKey) throws CryptoException {
        RSAPrivateKey key = (RSAPrivateKey) inputKey;
        RSAEngine engine = new RSAEngine();
        OAEPEncoding cipher = new OAEPEncoding(engine);
        BigInteger mod = key.getModulus();
        BigInteger exp = key.getPrivateExponent();
        RSAKeyParameters keyParams = new RSAKeyParameters(true, mod, exp);
        cipher.init(false, keyParams);
        byte[] clearText;
        try {
            clearText = cipher.processBlock(cipherText, 0, cipherText.length);
        } catch (InvalidCipherTextException e) {
            throw new CryptoException(e);
        }
        return clearText;
    }

    public static byte[] decryptRSAOAEP(String b64EncodedData, PrivateKey inputKey) throws CryptoException {
        byte[] cipherText = Base64.decode(b64EncodedData);
        return decryptRSAOAEP(cipherText, inputKey);
    }

    /**
     * Generates an AES SecretKey of a specified bit length
     *
     * @param bitSize length of key
     * @return the encoded key as a byte[]
     * @throws CryptoException
     */
    public static byte[] genKey(int bitSize) throws CryptoException {
        KeyGenerator keygen;
        try {
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        keygen.init(bitSize);
        SecretKey key = keygen.generateKey();
        return key.getEncoded();
    }

    /**
     * Generates an AES SecretKey of a specified bit length
     *
     * @param bitSize length of key
     * @return the key as a byte[]
     * @throws CryptoException
     */
    public static SecretKey genAesKey(int bitSize) throws CryptoException {
        KeyGenerator keygen;
        try {
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        keygen.init(bitSize);
        return keygen.generateKey();
    }

    /**
     * Verifies a public signature
     *
     * @param data      the signed data
     * @param signature the signature
     * @param mod       modulus
     * @param exp       exponent
     * @return valid or invalid
     * @throws CryptoException
     */
    public static boolean verifyRSA(byte[] data, byte[] signature, BigInteger mod, BigInteger exp, String messageDigestAlgorithm) throws CryptoException {
        boolean verified = false;
        try {
            RSAPublicKeySpec rsaKeySpec = new RSAPublicKeySpec(mod, exp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(rsaKeySpec);
            String signatureAlgorithm = null;
            if ("SHA".equals(messageDigestAlgorithm)) {
                signatureAlgorithm = "SHA1withRSA";
            } else if ("SHA256".equals(messageDigestAlgorithm)) {
                signatureAlgorithm = "SHA256withRSA";
            }
            if (signatureAlgorithm == null) {
                throw new CryptoException("Unsupported digest algorithm " + messageDigestAlgorithm);
            }
            Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initVerify(pubKey);
            sig.update(data);
            verified = sig.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (SignatureException e) {
            throw new CryptoException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
        return verified;
    }

    /**
     * Signs data
     *
     * @param data the data to sign
     * @param key  the private key to use for signing
     * @return sgined data
     * @throws CryptoException
     */
    public static String sign(byte[] data, PrivateKey key, String algorithm) throws CryptoException {
        String signedData;
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(key);
            signature.update(data);
            signedData = Base64.encodeBytesNoBreaks(signature.sign());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (SignatureException e) {
            throw new CryptoException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        }
        return signedData;
    }

    public static X509Certificate X509fromB64(String b64EncodedX509Certificate) throws CryptoException {
        StringBuffer sb = new StringBuffer("-----BEGIN CERTIFICATE-----\n");
        sb.append(b64EncodedX509Certificate);
        sb.append("\n-----END CERTIFICATE-----\n");
        ByteArrayInputStream bis = new ByteArrayInputStream(sb.toString().getBytes());
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            throw new CryptoException("Error creating X509 CertificateFactory", e);
        }
        try {
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(bis);
            return certificate;
        } catch (CertificateException e) {
            byte[] decodedBase64 = Base64.decode(b64EncodedX509Certificate);
            String b64 = Base64.encodeBytes(decodedBase64);
            X509Certificate certificate;
            try {
                sb = new StringBuffer("-----BEGIN CERTIFICATE-----\n");
                sb.append(b64);
                sb.append("\n-----END CERTIFICATE-----\n");
                bis = new ByteArrayInputStream(sb.toString().getBytes());
                certificate = (X509Certificate) cf.generateCertificate(bis);
            } catch (CertificateException e1) {
                throw new CryptoException("Error creating X509Certificate from base64-encoded String", e1);
            }
            return certificate;
        }
    }
}
