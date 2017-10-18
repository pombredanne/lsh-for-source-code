package org.thirdway.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.thirdway.exceptions.GeneralException;

/**
 * PBE with MD5 cipher algorithm and DES based encryption class.
 * 
 */
public class PassPhraseCrypto {

    /**
     * Static method to encrypt a password with salt, passPhrase and iteration
     * count.
     * 
     * @param passPhrase -
     *            the login name used to update digest.
     * @param password -
     *            the password to encrypt.
     * 
     * @return encypted password.
     */
    public static String encrypt(String passPhrase, String password) {
        String algorithm = "PBEWithMD5AndDES";
        byte[] salt = new byte[8];
        int iterations = 20;
        byte[] output = new byte[128];
        if (passPhrase == null || "".equals(passPhrase) || password == null || "".equals(password)) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "Required parameter missing");
        }
        try {
            Security.addProvider(new com.sun.crypto.provider.SunJCE());
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray());
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(passPhrase.getBytes());
            byte[] input = new byte[password.length()];
            input = password.getBytes();
            messageDigest.update(input);
            byte[] digest = messageDigest.digest();
            System.arraycopy(digest, 0, salt, 0, 8);
            AlgorithmParameterSpec algorithmParameterSpec = new PBEParameterSpec(salt, iterations);
            Cipher cipher = Cipher.getInstance(algorithm);
            int mode = Cipher.ENCRYPT_MODE;
            cipher.init(mode, secretKey, algorithmParameterSpec);
            output = cipher.doFinal(input);
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "Algorithm not found", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "nvalidAlgorithmParameter", e);
        } catch (InvalidKeySpecException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "InvalidKeySpec", e);
        } catch (InvalidKeyException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "InvalidKey", e);
        } catch (NoSuchPaddingException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "NoSuchPadding", e);
        } catch (BadPaddingException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "BadPadding", e);
        } catch (IllegalBlockSizeException e) {
            throw new GeneralException(PassPhraseCrypto.class, "encrypt", "IllegalBlockSize", e);
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < output.length; i++) {
            result.append(Byte.toString(output[i]));
        }
        return result.toString();
    }
}
