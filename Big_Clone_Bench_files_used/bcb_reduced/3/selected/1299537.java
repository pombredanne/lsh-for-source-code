package de.intarsys.pdf.crypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An {@link ICryptHandler}implementing the AES algorithm.
 * 
 */
public class AESCryptHandler extends StandardCryptHandler {

    public static final String KEY_ALGORITHM = "AES";

    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    public static final String DIGEST_ALGORITHM = "MD5";

    private int blockSize;

    @Override
    protected synchronized byte[] basicDecrypt(byte[] data, byte[] encryptionKey, int objectNum, int genNum) throws COSSecurityException {
        try {
            updateHash(encryptionKey, objectNum, genNum);
            byte[] keyBase = md.digest();
            IvParameterSpec ivSpec = new IvParameterSpec(data, 0, blockSize);
            SecretKey skeySpec = new SecretKeySpec(keyBase, 0, length, KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(data, blockSize, data.length - blockSize);
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    protected synchronized byte[] basicEncrypt(byte[] data, byte[] encryptionKey, int objectNum, int genNum) throws COSSecurityException {
        try {
            updateHash(encryptionKey, objectNum, genNum);
            byte[] keyBase = md.digest();
            byte[] initVector = cipher.getIV();
            if (initVector == null) {
                initVector = new byte[16];
            }
            IvParameterSpec ivSpec = new IvParameterSpec(initVector, 0, initVector.length);
            SecretKey skeySpec = new SecretKeySpec(keyBase, 0, length, KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data, 0, data.length);
            byte[] result = new byte[initVector.length + encrypted.length];
            System.arraycopy(initVector, 0, result, 0, initVector.length);
            System.arraycopy(encrypted, 0, result, initVector.length, encrypted.length);
            return result;
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
            blockSize = cipher.getBlockSize();
        } catch (NoSuchAlgorithmException e) {
            throw new COSSecurityException(e);
        } catch (NoSuchPaddingException e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    protected void updateHash(byte[] encryptionKey, int objectNum, int genNum) {
        super.updateHash(encryptionKey, objectNum, genNum);
        md.update(new byte[] { 0x73, 0x41, 0x6c, 0x54 });
    }
}
