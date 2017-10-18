package de.intarsys.pdf.crypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * An {@link ICryptHandler} implementing the RC4 algorithm.
 * 
 */
public class ArcFourCryptHandler extends StandardCryptHandler {

    public static final String CIPHER_ALGORITHM = "RC4";

    public static final String KEY_ALGORITHM = "RC4";

    public static final String DIGEST_ALGORITHM = "MD5";

    @Override
    protected synchronized byte[] basicDecrypt(byte[] data, byte[] encryptionKey, int objectNum, int genNum) throws COSSecurityException {
        try {
            updateHash(encryptionKey, objectNum, genNum);
            byte[] keyBase = md.digest();
            SecretKey skeySpec = new SecretKeySpec(keyBase, 0, length, KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    protected synchronized byte[] basicEncrypt(byte[] data, byte[] encryptionKey, int objectNum, int genNum) throws COSSecurityException {
        try {
            updateHash(encryptionKey, objectNum, genNum);
            byte[] keyBase = md.digest();
            SecretKey skeySpec = new SecretKeySpec(keyBase, 0, length, CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    public void initialize(byte[] pCryptKey) throws COSSecurityException {
        super.initialize(pCryptKey);
        try {
            md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new COSSecurityException(e);
        } catch (NoSuchPaddingException e) {
            throw new COSSecurityException(e);
        }
    }
}
