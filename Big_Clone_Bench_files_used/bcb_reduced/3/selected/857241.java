package org.jefb.util;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

public class CryptographyUtil {

    /**
	 * Decrypt AES key and create {@code Key} instance.
	 * @param encryptedKey encrypted AES key.
	 * @param privKey private RSA key the AES key should be decrypted with
	 * @return decrypted AES key
	 */
    public static SecretKey decryptAESKey(byte[] encryptedKey, PrivateKey privKey) {
        byte[] aesKey = decryptRSA(privKey, encryptedKey);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        return (SecretKey) keySpec;
    }

    /**
	 * Decrypt AES-encrypted data.
	 * @param key secret AES key.
	 * @param data encrypted data.
	 * @return decrypted data.
	 */
    public static byte[] decryptAES(Key key, byte[] data) {
        try {
            Cipher aesDecryptor = Cipher.getInstance("AES");
            aesDecryptor.init(Cipher.DECRYPT_MODE, key);
            return aesDecryptor.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt AES-ecrypted data:", e);
        }
    }

    /**
	 * Generate a new AES key.
	 * @return
	 */
    public static SecretKey generateAESKey() {
        try {
            return createAESKeyGenerator().generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generatre AES key:", e);
        }
    }

    /**
	 * Decrypt RSA-ecncrypted data. 
	 * @param privKey
	 * @param encryptedData
	 * @return decrypted data
	 */
    public static byte[] decryptRSA(PrivateKey privKey, byte[] encryptedData) {
        try {
            Cipher decryptor = Cipher.getInstance("RSA");
            decryptor.init(Cipher.DECRYPT_MODE, privKey);
            return decryptor.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt RSA encrypted data:" + e.getMessage());
        }
    }

    /**
	 * Encrypt given data using public key.
	 * @param publicKey public key for encryption.
	 * @param plainData data to be ebcrypted.
	 * @return RSA-encrypted data.
	 */
    public static byte[] encryptRSA(PublicKey publicKey, byte[] plainData) {
        try {
            Cipher encryptor = Cipher.getInstance("RSA");
            encryptor.init(Cipher.ENCRYPT_MODE, publicKey, new SecureRandom());
            return encryptor.doFinal(plainData);
        } catch (Exception e) {
            throw new RuntimeException("Unable to encrypt data:", e);
        }
    }

    /**
	 * Create key pair generator for RSA algorithm
	 * 
	 * @return created key generator
	 */
    public static KeyPairGenerator createRSAKeyPairGenerator() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024, new SecureRandom());
            return generator;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create RSA key genarator.", e);
        }
    }

    /**
	 * Create digest from input stream.
	 * 
	 * @param is
	 *            InputStream the digest should be created for
	 * @return created digest
	 * @throws Exception
	 */
    public static byte[] createDigest(InputStream is) throws Exception {
        try {
            MessageDigest shaDigester = createSHADigester();
            DigestInputStream dis = new DigestInputStream(is, shaDigester);
            while (dis.read() >= 0) {
            }
            return dis.getMessageDigest().digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create digest.", e);
        } catch (Exception e) {
            throw new Exception("Unable to create digest.", e);
        }
    }

    /**
	 * Creates MD5 digest for given data
	 * 
	 * @param data
	 *            the digest should be created for
	 * @return MD5 digest
	 */
    public static byte[] createSHADigest(byte[] data) {
        try {
            MessageDigest md = createSHADigester();
            return md.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create SHA-256 digest.", e);
        }
    }

    /**
	 * Create SHA-256 digester.
	 * 
	 * @return created digester
	 * @throws NoSuchAlgorithmException
	 */
    public static MessageDigest createSHADigester() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    /**
	 * Create and initialize AES key generator.
	 * 
	 * @param keySize
	 *            size for keys should be created by created generator
	 * @return created key generator
	 * @throws NoSuchAlgorithmException
	 *             in AES algorithm can't be found
	 */
    public static KeyGenerator createAESKeyGenerator() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(new SecureRandom());
        return generator;
    }

    public static byte[] encryptAES(Key aesKey, byte[] data) {
        try {
            Cipher encryptor = Cipher.getInstance("AES");
            encryptor.init(Cipher.ENCRYPT_MODE, aesKey, new SecureRandom());
            return encryptor.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Unable to encrypt data:", e);
        }
    }

    /**
	 * Encodes password to SHA-256 hash
	 * @param password to encoded
	 * @return encoded password
	 */
    public static String encodeSHA256(String password) {
        ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);
        return encoder.encodePassword(password, null);
    }
}
