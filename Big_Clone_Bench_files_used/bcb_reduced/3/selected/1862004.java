package Security;

import java.io.UnsupportedEncodingException;
import java.rmi.server.UID;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SharedKey implements SecurityObject {

    static SharedKey sKey;

    MyKey mk = new MyKey();

    static final String xform = "PBEWithMD5AndDES/CBC/PKCS5Padding";

    private SharedKey() {
    }

    public static SharedKey getInstance() {
        if (sKey == null) {
            sKey = new SharedKey();
        }
        return sKey;
    }

    public String decrypt(byte[] data, MyKey mk) {
        if (data == null) {
            return null;
        }
        byte[] resultArray;
        String result = "";
        Cipher pbeCipher = null;
        try {
            pbeCipher = Cipher.getInstance(xform);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            pbeCipher.init(Cipher.DECRYPT_MODE, mk.skey, mk.pps);
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
        } catch (InvalidAlgorithmParameterException e1) {
            e1.printStackTrace();
        }
        try {
            resultArray = pbeCipher.doFinal(data);
            result = new String(resultArray);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public byte[] encrypt(String data, MyKey mk) {
        if (data == null) {
            return null;
        }
        Cipher pbeCipher = null;
        byte[] ciphertext = null;
        try {
            pbeCipher = Cipher.getInstance(xform);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            pbeCipher.init(Cipher.ENCRYPT_MODE, mk.skey, mk.pps);
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
        } catch (InvalidAlgorithmParameterException e1) {
            e1.printStackTrace();
        }
        byte[] cleartext;
        if (data == null) {
            cleartext = null;
        } else {
            cleartext = data.getBytes();
        }
        try {
            ciphertext = pbeCipher.doFinal(cleartext);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return ciphertext;
    }

    public byte[] sessionKeyEncrypt(String k_session, String data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String salt = "saltings";
        PBEParameterSpec paramspec = new PBEParameterSpec(salt.getBytes(), 20);
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        PBEKeySpec keyspec = new PBEKeySpec(k_session.toCharArray());
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = factory.generateSecret(keyspec);
        cipher.init(Cipher.ENCRYPT_MODE, key, paramspec);
        byte[] ciphertext = cipher.doFinal(data.getBytes());
        return ciphertext;
    }

    public String sessionKeyDecrypt(String k_session, byte[] cipherText) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        PBEKeySpec keyspec = new PBEKeySpec(k_session.toCharArray());
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = factory.generateSecret(keyspec);
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        String salt = "saltings";
        PBEParameterSpec paramspec = new PBEParameterSpec(salt.getBytes(), 20);
        cipher.init(Cipher.DECRYPT_MODE, key, paramspec);
        byte[] decryptedText = cipher.doFinal(cipherText);
        return new String(decryptedText);
    }

    public MyKey generateKeyWithPwd(String pwd) {
        SecretKeyFactory keyFac = null;
        SecretKey pbeKey = null;
        char[] passwordChar = pwd.toCharArray();
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passwordChar);
        try {
            keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            pbeKey = keyFac.generateSecret(pbeKeySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        mk.skey = pbeKey;
        byte[] salt = pwd.getBytes();
        byte[] s = new byte[8];
        for (int i = 0; i < s.length; i++) {
            s[i] = (byte) i;
        }
        mk.pps = new PBEParameterSpec(s, 8);
        return mk;
    }

    public SecretKey generateWithInt(int gene) {
        try {
            KeyGenerator generator;
            generator = KeyGenerator.getInstance("DES");
            generator.init(gene);
            SecretKey key = generator.generateKey();
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String decrypt(byte[] data, byte[] key) {
        return null;
    }

    @Override
    public byte[] encrypt(String data, byte[] key) {
        return null;
    }

    public static String getHash(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = str.getBytes("utf8");
            md.update(array);
            byte[] temp;
            temp = md.digest();
            String result = "";
            for (int i = 0; i < temp.length; i++) {
                result += Integer.toHexString((0x000000ff & temp[i]) | 0xffffff00).substring(6);
            }
            return result;
        } catch (Exception e) {
        }
        return null;
    }

    public static Boolean checkHash(String str, String hash) {
        if (hash.equals(getHash(str))) return true; else return false;
    }
}
