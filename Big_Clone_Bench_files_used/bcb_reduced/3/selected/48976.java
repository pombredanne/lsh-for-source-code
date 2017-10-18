package de.saly.javacommonslib.base.crypto;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CryptoHelper {

    private static final String CRYPT_ALGO = "Blowfish";

    private static final String CIPHER_ALGO = CRYPT_ALGO + "/ECB/PKCS5Padding";

    public static Key storeNewKey(String path) throws IOException {
        Key key = generateSecreteKey();
        storeKey(key, path);
        return key;
    }

    public static void storeKey(Key key, String path) throws IOException {
        FileOutputStream fo = new FileOutputStream(path);
        ObjectOutputStream oo = new ObjectOutputStream(fo);
        oo.writeObject(key);
        oo.close();
    }

    public static Key loadKey(String path) throws IOException {
        try {
            FileInputStream fi = new FileInputStream(path);
            ObjectInputStream oi = new ObjectInputStream(fi);
            Key key = (Key) oi.readObject();
            oi.close();
            return key;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static SecretKey generateSecreteKey() {
        try {
            return KeyGenerator.getInstance(CRYPT_ALGO).generateKey();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static Cipher getCipher() {
        try {
            return Cipher.getInstance(CIPHER_ALGO);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (NoSuchPaddingException e) {
            return null;
        }
    }

    public static byte[] encryptFromString(String cleartext, Key key) {
        return encrypt(cleartext.getBytes(), key);
    }

    public static String decryptToString(byte[] ciphertext, Key key) {
        byte[] retVal = decrypt(ciphertext, key);
        if (retVal == null) return null;
        return new String(retVal);
    }

    public static byte[] encrypt(byte[] cleartext, Key key) {
        try {
            Cipher cipher = getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] ciphertext = cipher.doFinal(cleartext);
            return ciphertext;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] ciphertext, Key key) {
        try {
            Cipher cipher = getCipher();
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = cipher.doFinal(ciphertext);
            return cleartext;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String md5encrypt(String toEncrypt) {
        if (toEncrypt == null) {
            throw new IllegalArgumentException("null is not a valid password to encrypt");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toEncrypt.getBytes());
            byte[] hash = md.digest();
            return new String(dumpBytes(hash));
        } catch (NoSuchAlgorithmException nsae) {
            return toEncrypt;
        }
    }

    public static String dumpBytes(byte[] bytes) {
        int i;
        StringBuffer sb = new StringBuffer();
        for (i = 0; i < bytes.length; i++) {
            if (i % 32 == 0 && i != 0) {
                sb.append("\n");
            }
            String s = Integer.toHexString(bytes[i]);
            if (s.length() < 2) {
                s = "0" + s;
            }
            if (s.length() > 2) {
                s = s.substring(s.length() - 2);
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
