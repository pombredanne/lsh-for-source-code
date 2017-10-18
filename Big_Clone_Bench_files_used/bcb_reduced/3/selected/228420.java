package ch.squix.nataware.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import ch.squix.nataware.util.StreamUtil;
import ch.squix.nataware.util.Util;
import com.google.protobuf.ByteString;

public class Crypto {

    public static final String HASH_NAME = "SHA-256";

    public static final String CIPHER_ALGORITHM = "AES";

    public static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static final int CRYPTIO_KEY_SIZE = 128;

    private static MessageDigest createNewDigester() throws Exception {
        final MessageDigest newDigest = MessageDigest.getInstance(HASH_NAME);
        return newDigest;
    }

    public static byte[] createHash(final byte[]... dataToHash) throws Exception {
        final MessageDigest tmpDigester = createNewDigester();
        return tmpDigester.digest(Util.concatByteArrays(dataToHash));
    }

    public static ByteString createHashFromByteString(ByteString... dataToHash) throws Exception {
        final MessageDigest tmpDigester = createNewDigester();
        tmpDigester.update(Util.concatByteStrings(dataToHash).asReadOnlyByteBuffer());
        return ByteString.copyFrom(tmpDigester.digest());
    }

    private static byte[] encryptByteArray(byte[] blockToEncrypt, byte[] iv, byte[]... encryptionKey) throws Exception {
        return processCipher(blockToEncrypt, Cipher.ENCRYPT_MODE, iv, encryptionKey);
    }

    private static byte[] decryptByteArray(byte[] blockToEncrypt, byte[] iv, byte[]... encryptionKey) throws Exception {
        return processCipher(blockToEncrypt, Cipher.DECRYPT_MODE, iv, encryptionKey);
    }

    public static byte[] encryptByteString(ByteString blockToEncrypt, ByteString iv, ByteString... encryptionKey) throws Exception {
        return encryptByteArray(blockToEncrypt.toByteArray(), iv.toByteArray(), Util.concatByteStrings(encryptionKey).toByteArray());
    }

    public static byte[] decryptByteString(ByteString blockToDecrypt, ByteString iv, ByteString... encryptionKey) throws Exception {
        return decryptByteArray(blockToDecrypt.toByteArray(), iv.toByteArray(), Util.concatByteStrings(encryptionKey).toByteArray());
    }

    public void encrypt(InputStream in, OutputStream out, byte[] iv, byte[]... encryptionKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException {
        final Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, iv, encryptionKey);
        out = new CipherOutputStream(out, cipher);
        StreamUtil.iterateWholeStream(in, out);
    }

    public void decrypt(InputStream in, OutputStream out, byte[] iv, byte[]... encryptionKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException {
        final Cipher cipher = initCipher(Cipher.DECRYPT_MODE, iv, encryptionKey);
        in = new CipherInputStream(in, cipher);
        StreamUtil.iterateWholeStream(in, out);
        out.close();
    }

    /**
     * Converts a InputStream to a CipherInputStream, which enabl
     * 
     * @param streamToDecrypt
     * @param decryptionKey
     * @param saltPrefix
     * @param saltSuffix
     * @return
     * @throws Exception
     */
    public static CipherInputStream getCipherInputStream(InputStream streamToDecrypt, byte[] iv, byte[]... decryptionKey) throws Exception {
        final Cipher cipher = initCipher(Cipher.DECRYPT_MODE, iv, decryptionKey);
        return new CipherInputStream(streamToDecrypt, cipher);
    }

    /**
     * @param blockToEncrypt
     * @param encryptionKey
     * @param saltPrefix
     * @param saltSuffix
     * @param cryptionMode
     *            TODO
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private static byte[] processCipher(byte[] blockToEncrypt, int cryptionMode, byte[] iv, byte[]... encryptionKey) throws Exception {
        final Cipher cipher = initCipher(cryptionMode, iv, encryptionKey);
        return cipher.doFinal(blockToEncrypt);
    }

    /**
     * @param encryptionKey
     * @param saltPrefix
     * @param saltSuffix
     * @param cryptionMode
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchProviderException
     */
    private static Cipher initCipher(int cryptionMode, byte[] iv, byte[]... encryptionKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyGenerator keyGen;
        keyGen = KeyGenerator.getInstance(CIPHER_ALGORITHM);
        final SecureRandom randomSeed = new SecureRandom();
        randomSeed.setSeed(Util.concatByteArrays(encryptionKey));
        keyGen.init(CRYPTIO_KEY_SIZE, randomSeed);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKey secretKey = keyGen.generateKey();
        final SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), CIPHER_ALGORITHM);
        final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(cryptionMode, secretKeySpec, ivSpec);
        return cipher;
    }
}
