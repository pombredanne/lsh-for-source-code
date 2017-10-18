package org.psw.manager.common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * @author Roberts Vartins
 */
public class SecurityUtils {

    public static String encrypt(String stringToEncrypt, String password) {
        try {
            byte[] key = getKey(password);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, Constants.ENCRYPTION_METHOD);
            Cipher cipher = Cipher.getInstance(Constants.ENCRYPTION_METHOD);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(stringToEncrypt.getBytes(Constants.ENCODING));
            return new String(Base64.encodeBase64(encrypted), Constants.ENCODING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String stringTodecrypt, String password) {
        try {
            byte[] key = getKey(password);
            SecretKeySpec skeySpec = new SecretKeySpec(key, Constants.ENCRYPTION_METHOD);
            Cipher cipher = Cipher.getInstance(Constants.ENCRYPTION_METHOD);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] original = cipher.doFinal(Base64.decodeBase64(stringTodecrypt.getBytes(Constants.ENCODING)));
            return new String(original, Constants.ENCODING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getKey(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(Constants.HASH_FUNCTION);
        messageDigest.update(password.getBytes(Constants.ENCODING));
        byte[] hashValue = messageDigest.digest();
        int keyLengthInbytes = Constants.ENCRYPTION_KEY_LENGTH / 8;
        byte[] result = new byte[keyLengthInbytes];
        System.arraycopy(hashValue, 0, result, 0, keyLengthInbytes);
        return result;
    }
}
