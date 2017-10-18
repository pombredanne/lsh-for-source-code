package com.google.code.sapien.util;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import com.google.code.sapien.security.BCrypt;

/**
 * Security utilities.
 * @author Adam
 * @version $Id: SecurityUtils.java 18 2009-04-20 05:19:32Z a.ruggles $
 * 
 * Created on Feb 7, 2009 at 5:06:46 PM 
 */
public final class SecurityUtils {

    /**
	 * Implements a cipher for encrypting and decrypting messages.
	 * @param mode The cipher mode, usually Cipher.DECRYPT_MODE or Cipher.ENCRYPT_MODE.
	 * @param message A byte array containing message data.
	 * @param key A byte array containing the cipher key.
	 * @return A string containing the ciphered message.
	 * @throws GeneralSecurityException If an error occurs in the cipher APIs.
	 */
    private static byte[] cipher(final int mode, final byte[] message, final byte[] key) throws GeneralSecurityException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, secretKeySpec);
        return cipher.doFinal(message);
    }

    /**
	 * Decrypts a message using a secret key.
	 * @param message The message to decrypt.
	 * @param secret The secret key to decrypt the message.
	 * @return The decrypted message.
	 * @throws GeneralSecurityException If an error occurs decrypting the data.
	 * @throws UnsupportedEncodingException If an error occurs processing the secret string.
	 * @throws DecoderException 
	 */
    public static String decrypt(final String message, final String secret) throws GeneralSecurityException, UnsupportedEncodingException, DecoderException {
        byte[] decrypted = cipher(Cipher.DECRYPT_MODE, Hex.decodeHex(message.toCharArray()), getKey(secret));
        return new String(decrypted);
    }

    /**
	 * Encrypts a message using a secret key.
	 * @param message The message to encrypt.
	 * @param secret The secret key to encrypt the message.
	 * @return The encrypted message.
	 * @throws GeneralSecurityException If an error occurs encrypting the data.
	 * @throws UnsupportedEncodingException If an error occurs processing the secret string.
	 */
    public static String encrypt(final String message, final String secret) throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] encrypted = cipher(Cipher.ENCRYPT_MODE, message.getBytes(), getKey(secret));
        return new String(Hex.encodeHex(encrypted));
    }

    /**
	 * Evaluates the plain text password and a hashed value.
	 * @param plain The plain text password.
	 * @param hash The hashed password to compare.
	 * @return True if the hashed password matches the plain text password.
	 */
    public static boolean evaluate(final String plain, final String hash) {
        if (plain == null || hash == null || plain.length() == 0 || hash.length() == 0) {
            return false;
        }
        return BCrypt.checkpw(plain, hash);
    }

    /**
	 * Converts the secret into a key.
	 * @param secret The secret key.
	 * @return A byte array representing the encryption/decryption key.
	 * @throws NoSuchAlgorithmException If an unsupported algorithm has been chosen.
	 * @throws UnsupportedEncodingException If the encoding is unsupported.
	 */
    private static byte[] getKey(final String secret) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return MessageDigest.getInstance("MD5").digest(secret.getBytes());
    }

    /**
	 * Encrypts a plain text password.
	 * @param password The plain text password to encrypt.
	 * @return The encrypted value of the password.
	 */
    public static String hash(final String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
	 * Private constructor.
	 */
    private SecurityUtils() {
    }
}
