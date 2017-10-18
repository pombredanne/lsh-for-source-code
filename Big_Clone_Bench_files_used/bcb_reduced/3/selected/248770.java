package stegosaur.crypto;

import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;

/**
 * <p>Utility class to provide crypto primitives.</p>
 *
 * @author gndn.k5
 * @since Stegosaur 1.0
 */
public final class Crypto {

    /** This class cannot be instantiated. **/
    private Crypto() {
    }

    private static String cryptoProvider = null;

    private static MessageDigest sha256 = null;

    private static MessageDigest sha384 = null;

    /** Static initializer to check for certain requirements. **/
    static {
        try {
            cryptoProvider = System.getProperty("stegosaur.provider");
            if (cryptoProvider != null) {
                sha256 = MessageDigest.getInstance("SHA-256", cryptoProvider);
                sha384 = MessageDigest.getInstance("SHA-384", cryptoProvider);
                Cipher cipher = Cipher.getInstance("AES/OFB/NoPadding", cryptoProvider);
            } else {
                sha256 = MessageDigest.getInstance("SHA-256");
                sha384 = MessageDigest.getInstance("SHA-384");
                Cipher cipher = Cipher.getInstance("AES/OFB/NoPadding");
            }
            if (Cipher.getMaxAllowedKeyLength("AES/OFB/NoPadding") != Integer.MAX_VALUE) throw new AssertionError("Fatal: Unlimited strength jurisdiction policy files must be installed.");
        } catch (NoSuchAlgorithmException nsa) {
            throw new AssertionError("Fatal: SHA-256 and/or SHA-384 and/or AES/OFB not supported by crypto provider.");
        } catch (NoSuchProviderException nsp) {
            throw new AssertionError("Fatal: Specified crypto provider not found.");
        } catch (NoSuchPaddingException nspad) {
            throw new AssertionError("Fatal: AES/OFB/NoPadding not supported by crypto provider.");
        }
    }

    /** Returns SHA-256 hash of specified data **/
    public static byte[] hash(byte[] input) {
        sha256.reset();
        return sha256.digest(input);
    }

    /** Internal utility method to create and initialize a Cipher object. **/
    private static Cipher createCipher(String passphrase, int encMode) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        byte[] hashedPass = null;
        hashedPass = sha384.digest(passphrase.getBytes("US-ASCII"));
        SecretKeySpec key = new SecretKeySpec(hashedPass, 0, 32, "AES");
        IvParameterSpec Iv = new IvParameterSpec(hashedPass, 32, 16);
        Cipher cipher = null;
        if (cryptoProvider != null) cipher = Cipher.getInstance("AES/OFB/NoPadding", cryptoProvider); else cipher = Cipher.getInstance("AES/OFB/NoPadding");
        cipher.init(encMode, key, Iv);
        return cipher;
    }

    /** 
   * Utility method to decrypt the given byte array and store the results in the given
   * output array.  The Cipher which is used to perform the decryption is returned,
   * and can be passed back in if multiple calls are required.
   *
   * @param passphrase The passphrase to use as decryption key
   * @param input The array of encrypted bytes
   * @param output The array which will receive the decrypted bytes
   * @param isFinal If true, doFinal is invoked on the cipher rather than update
   * @returns The Cipher object used for decryption
   */
    public static Cipher decrypt(String passphrase, byte[] input, byte[] output, boolean isFinal) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        Cipher cipher = createCipher(passphrase, Cipher.DECRYPT_MODE);
        return decrypt(passphrase, input, output, isFinal, cipher);
    }

    /**
   * Utility method to decrypt the given byte array and store the results in the given
   * output array, using the given Cipher object.
   *
   * @param passphrase The passphrase to use as decryption key
   * @param input The array of encrypted bytes
   * @param output The array which will receive the decrypted bytes
   * @param isFinal If true, doFinal is invoked on the cipher rather than update
   * @param cipher The cipher object to use for decryption
   * @returns The Cipher object used for decryption
   */
    public static Cipher decrypt(String passphrase, byte[] input, byte[] output, boolean isFinal, Cipher cipher) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        byte[] decrypted = null;
        if (!isFinal) decrypted = cipher.update(input); else decrypted = cipher.doFinal(input);
        System.arraycopy(decrypted, 0, output, 0, input.length);
        return cipher;
    }

    /**
   * Utility method to encrypt the given byte array and store the results in the given
   * output array.  The Cipher which is used to perform the encryption is returned,
   * and can be passed back in if multiple calls are required.
   *
   * @param passphrase The passphrase to use as encryption key
   * @param input The array of unencrypted bytes
   * @param output The array which will receive the encrypted bytes
   * @param isFinal If true, doFinal is invoked on the cipher rather than update
   * @returns The Cipher object used for encryption  
   */
    public static Cipher encrypt(String passphrase, byte[] input, byte[] output, boolean isFinal) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        Cipher cipher = createCipher(passphrase, Cipher.ENCRYPT_MODE);
        return encrypt(passphrase, input, output, isFinal, cipher);
    }

    /**
   * Utility method to encrypt the given byte array and store the results in the given
   * output array, using the given Cipher object.
   *
   * @param passphrase The passphrase to use as encryption key
   * @param input The array of unencrypted bytes
   * @param output The array which will receive the encrypted bytes
   * @param isFinal If true, doFinal is invoked on the cipher rather than update
   * @param cipher The cipher object to use for encryption
   * @returns The Cipher object used for encryption
   */
    public static Cipher encrypt(String passphrase, byte[] input, byte[] output, boolean isFinal, Cipher cipher) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        byte[] encrypted = null;
        if (!isFinal) encrypted = cipher.update(input); else encrypted = cipher.doFinal(input);
        System.arraycopy(encrypted, 0, output, 0, input.length);
        return cipher;
    }

    /** TODO remove me **/
    public static void main(String[] args) {
        try {
            String test = "blahblah";
            String pass = "asdf";
            byte[] unencrypted = test.getBytes("US-ASCII");
            byte[] encrypted = new byte[test.length()];
            encrypt(pass, unencrypted, encrypted, true);
            byte[] decrypted = new byte[encrypted.length];
            decrypt(pass, encrypted, decrypted, true);
            String decryptedStr = new String(decrypted);
            System.out.println("Encrypted \"" + test + "\" with pass \"" + pass + "\" and decrypted back to \"" + decryptedStr + "\"");
        } catch (Exception e) {
            System.out.println("Whoops!   " + e.getMessage());
            e.printStackTrace();
        }
    }
}
