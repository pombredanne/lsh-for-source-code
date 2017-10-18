package org.remus.infomngmnt.efs.password;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Tom Seidel <tom.seidel@remus-software.org>
 */
public class AESCryptoManager {

    public static SecureRandom rand = new SecureRandom();

    private final byte[] pass;

    private static final String CIPHER_TYPE = "AES/CBC/PKCS5Padding";

    private static final String CIPHER_KEY_TYPE = "AES";

    private static final String HASH_ALG = "SHA-1";

    private static final int KEY_LENGTH = 16;

    private static final byte[] SALT = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    private static final byte[] IV = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    public AESCryptoManager(final byte[] pass) {
        this.pass = pass;
    }

    public OutputStream getEncryptedOutputStream(final File file) throws Exception {
        FileOutputStream fileOut = new FileOutputStream(file);
        Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
        IvParameterSpec ivParamSpec = new IvParameterSpec(IV);
        SecretKeySpec sKeySpec = new SecretKeySpec(deriveKey(), CIPHER_KEY_TYPE);
        cipher.init(Cipher.ENCRYPT_MODE, sKeySpec, ivParamSpec);
        return new CipherOutputStream(fileOut, cipher);
    }

    public InputStream getDecryptedInputStream(final File file) throws Exception {
        FileInputStream fileIn = new FileInputStream(file);
        IvParameterSpec ivParamSpec = new IvParameterSpec(IV);
        SecretKeySpec sKeySpec = new SecretKeySpec(deriveKey(), CIPHER_KEY_TYPE);
        Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec, ivParamSpec);
        return new CipherInputStream(fileIn, cipher);
    }

    private byte[] deriveKey() throws Exception {
        MessageDigest md = MessageDigest.getInstance(HASH_ALG);
        md.update(this.pass);
        md.update(SALT);
        byte[] key = new byte[KEY_LENGTH];
        System.arraycopy(md.digest(), 0, key, 0, KEY_LENGTH);
        return key;
    }
}
