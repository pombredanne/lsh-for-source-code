package org.isurf.spmiddleware.security;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import org.apache.log4j.Logger;

/**
 * Simple utility used to perform one way and two way encryption of String
 * values.
 */
public class EncrypterImpl implements Encrypter {

    private static final Logger logger = Logger.getLogger(EncrypterImpl.class);

    private String digestAlgorithm;

    private String encryptionAlgorithm;

    private String cipherTransformation;

    private String keyString;

    /**
	 * Constructs an Encrypter.
	 *
	 * @param cipher
	 * @param digestAlgorithm
	 * @param encryptionAlgorithm
	 * @param keyString
	 */
    public EncrypterImpl(String digestAlgorithm, String encryptionAlgorithm, String cipherTransformation, String keyString) {
        this.cipherTransformation = cipherTransformation;
        this.digestAlgorithm = digestAlgorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.keyString = keyString;
    }

    public String digest(String clearText) {
        try {
            if (clearText == null) {
                return null;
            }
            MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
            byte[] digest = md.digest(clearText.getBytes());
            return new String(digest);
        } catch (NoSuchAlgorithmException e) {
            logger.error("digest: " + digestAlgorithm + " algorithm not found", e);
        }
        return null;
    }

    public String encrypt(String clearText) {
        try {
            if (clearText == null) {
                return null;
            }
            Key key = getKey();
            Cipher cipher = Cipher.getInstance(cipherTransformation);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(clearText.getBytes());
            return getString(encrypted);
        } catch (Exception e) {
            logger.error("encrypt: error occured encrypting...", e);
        }
        return null;
    }

    public String decrypt(String encrypted) {
        try {
            Key key = getKey();
            Cipher cipher = Cipher.getInstance(cipherTransformation);
            byte[] ciphertext = getBytes(encrypted);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = cipher.doFinal(ciphertext);
            return new String(cleartext);
        } catch (Exception e) {
            logger.error("decrypt: error occured decrypting...", e);
        }
        return null;
    }

    /**
	 * Gets the key for encryption / decryption.
	 *
	 * @return The key.
	 */
    private Key getKey() {
        try {
            byte[] bytes = getBytes(keyString);
            DESKeySpec pass = new DESKeySpec(bytes);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(encryptionAlgorithm);
            SecretKey s = skf.generateSecret(pass);
            return s;
        } catch (Exception e) {
            logger.error("getKey: error occured encrypting...", e);
        }
        return null;
    }

    /**
	 * Converts the byte array to a String.
	 */
    private String getString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            sb.append((int) (0x00FF & b));
            if (i + 1 < bytes.length) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    /**
	 * Splits the string and converts to a byte array.
	 */
    private byte[] getBytes(String string) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String[] array = string.split("-");
        for (String s : array) {
            int i = Integer.parseInt(s);
            bos.write((byte) i);
        }
        return bos.toByteArray();
    }
}
