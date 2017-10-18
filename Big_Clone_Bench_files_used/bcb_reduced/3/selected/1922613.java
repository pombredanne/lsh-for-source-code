package net.sf.oneWayCrypto;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyProvider {

    private static final String ASYMMETRIC_ALG = "RSA";

    public static final int ASYMMETRIC_KEYSIZE = 2048;

    public static final String SYMMETRIC_ALG = "AES";

    public static final int SYMMETRIC_KEYSIZE = 256;

    public static void newKeyPair(String password) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(ASYMMETRIC_ALG);
            gen.initialize(ASYMMETRIC_KEYSIZE);
            KeyPair keyPair = gen.generateKeyPair();
            saveKeys(keyPair, password);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    public static PrivateKey getPrivateKey(String password) throws IOException, InvalidKeyException {
        File keyFile = new File(AppUtils.getContext().getFilesDir(), "private.key");
        FileInputStream fis = new FileInputStream(keyFile);
        byte[] cryptedPrivateKey = new byte[(int) keyFile.length()];
        fis.read(cryptedPrivateKey);
        fis.close();
        byte[] encodedPrivateKey = decryptKey(cryptedPrivateKey, password);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey;
        try {
            privateKey = KeyFactory.getInstance(ASYMMETRIC_ALG).generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException("Invalid unlock password for the private key");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
        return privateKey;
    }

    public static PublicKey getPublicKey() {
        File keyFile = new File(AppUtils.getContext().getFilesDir(), "public.key");
        try {
            FileInputStream fis = new FileInputStream(keyFile);
            byte[] encodedKey = new byte[(int) keyFile.length()];
            fis.read(encodedKey);
            fis.close();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
            PublicKey publicKey;
            try {
                publicKey = KeyFactory.getInstance(ASYMMETRIC_ALG).generatePublic(keySpec);
            } catch (InvalidKeySpecException e) {
                throw new Error(e);
            } catch (NoSuchAlgorithmException e) {
                throw new Error(e);
            }
            return publicKey;
        } catch (IOException e) {
            return null;
        }
    }

    private static void saveKeys(KeyPair keyPair, String password) {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
            FileOutputStream fos = AppUtils.getContext().openFileOutput("public.key", 0);
            fos.write(x509EncodedKeySpec.getEncoded());
            fos.close();
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
            fos = AppUtils.getContext().openFileOutput("private.key", 0);
            byte[] cryptedPrivate = encryptKey(pkcs8EncodedKeySpec.getEncoded(), password);
            fos.write(cryptedPrivate);
            fos.close();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static byte[] encryptKey(byte[] plainKey, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] passwordHash = md.digest(password.getBytes());
            SecretKey secret = new SecretKeySpec(passwordHash, SYMMETRIC_ALG);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            return cipher.doFinal(plainKey);
        } catch (IllegalBlockSizeException e) {
            throw new Error(e);
        } catch (BadPaddingException e) {
            throw new Error(e);
        } catch (InvalidKeyException e) {
            throw new Error(e);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (NoSuchPaddingException e) {
            throw new Error(e);
        }
    }

    /**
	 * Encrypt the file using simmetric encryption
	 */
    public static byte[] symmetricEncrypt(SecretKey encryptionKey, FileInputStream fis) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(fis.available());
            int bytes = fis.available();
            while (bytes > 0) {
                byte[] buffer = new byte[bytes];
                fis.read(buffer);
                baos.write(cipher.update(buffer));
                bytes = fis.available();
            }
            baos.write(cipher.doFinal());
            return baos.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (NoSuchPaddingException e) {
            throw new Error(e);
        } catch (InvalidKeyException e) {
            throw new Error(e);
        } catch (IllegalBlockSizeException e) {
            throw new Error(e);
        } catch (BadPaddingException e) {
            throw new Error(e);
        }
    }

    public static byte[] symmetricDecrypt(SecretKey key, FileInputStream cryptedStream) throws InvalidKeyException {
        try {
            Cipher cipher = Cipher.getInstance(KeyProvider.SYMMETRIC_ALG);
            cipher.init(Cipher.DECRYPT_MODE, key);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(cryptedStream.available());
            int bytes = cryptedStream.available();
            while (bytes != 0) {
                byte[] buffer = new byte[bytes];
                cryptedStream.read(buffer);
                baos.write(cipher.update(buffer));
                bytes = cryptedStream.available();
            }
            baos.write(cipher.doFinal());
            return baos.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (NoSuchPaddingException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        } catch (IllegalBlockSizeException e) {
            throw new Error(e);
        } catch (BadPaddingException e) {
            throw new Error(e);
        }
    }

    private static byte[] decryptKey(byte[] cryptedKey, String password) throws InvalidKeyException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] passwordHash = md.digest(password.getBytes());
            SecretKey secret = new SecretKeySpec(passwordHash, SYMMETRIC_ALG);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALG);
            cipher.init(Cipher.DECRYPT_MODE, secret);
            return cipher.doFinal(cryptedKey);
        } catch (IllegalBlockSizeException e) {
            throw new Error(e);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (NoSuchPaddingException e) {
            throw new InvalidKeyException(e);
        } catch (BadPaddingException e) {
            throw new InvalidKeyException(e);
        }
    }

    public static byte[] cryptSecretKey(SecretKey secretKey, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (InvalidKeyException e) {
            throw new Error(e);
        } catch (NoSuchPaddingException e) {
            throw new Error(e);
        } catch (IllegalBlockSizeException e) {
            throw new Error(e);
        } catch (BadPaddingException e) {
            throw new Error(e);
        }
    }

    public static SecretKey recoverSecretKey(PrivateKey privateKey, byte[] cryptedKey) throws IOException, InvalidKeyException {
        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_ALG);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plainSecret = cipher.doFinal(cryptedKey);
            return new SecretKeySpec(plainSecret, KeyProvider.SYMMETRIC_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (NoSuchPaddingException e) {
            throw new Error(e);
        } catch (IllegalBlockSizeException e) {
            throw new Error(e);
        } catch (BadPaddingException e) {
            throw new Error(e);
        }
    }

    /**
	 * Generate a random symmetric key to use with encryption
	 */
    public static SecretKey getRandomKey() {
        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance(SYMMETRIC_ALG);
            kgen.init(SYMMETRIC_KEYSIZE, new SecureRandom());
            return new SecretKeySpec(kgen.generateKey().getEncoded(), SYMMETRIC_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }
}
