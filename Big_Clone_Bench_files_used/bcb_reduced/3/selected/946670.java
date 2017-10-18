package org.kablink.teaming.module.file.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.kablink.teaming.module.file.FileEncryption;

public class CryptoFileEncryption implements FileEncryption {

    protected Cipher ecipher;

    protected Cipher dcipher;

    protected KeyGenerator kgen;

    protected SecretKey key;

    private String SALT = "The Secret Salt";

    private boolean initialized = false;

    public CryptoFileEncryption() {
        try {
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
        } catch (NoSuchAlgorithmException e) {
            return;
        } catch (InvalidParameterException e) {
            return;
        }
        key = kgen.generateKey();
    }

    public CryptoFileEncryption(byte[] raw) {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        initCipher(skeySpec);
    }

    public CryptoFileEncryption(String password) {
        byte[] key;
        try {
            key = (SALT + password).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
        } catch (UnsupportedEncodingException e) {
            return;
        } catch (NoSuchAlgorithmException e) {
            return;
        }
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        initCipher(skeySpec);
    }

    private void initCipher(SecretKeySpec skeySpec) {
        try {
            ecipher = Cipher.getInstance("AES");
            dcipher = Cipher.getInstance("AES");
            ecipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            dcipher.init(Cipher.DECRYPT_MODE, skeySpec);
            initialized = true;
        } catch (javax.crypto.NoSuchPaddingException e) {
        } catch (java.security.NoSuchAlgorithmException e) {
        } catch (java.security.InvalidKeyException e) {
        }
    }

    public SecretKey getSecretKey() {
        return key;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public InputStream getEncryptionInputEncryptedStream(InputStream in) {
        return new CipherInputStream(in, ecipher);
    }

    public InputStream getEncryptionInputDecryptedStream(InputStream in) {
        return new CipherInputStream(in, dcipher);
    }

    public OutputStream getEncryptionOutputEncryptedStream(OutputStream out) {
        return new CipherOutputStream(out, ecipher);
    }

    public OutputStream getEncryptionOutputDecryptedStream(OutputStream out) {
        return new CipherOutputStream(out, dcipher);
    }
}
