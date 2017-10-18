package com.explosion.utilities;

import java.security.MessageDigest;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import xjava.security.Cipher;
import xjava.security.SecretKey;
import cryptix.provider.key.DESKeyGenerator;
import cryptix.provider.key.RawSecretKey;
import cryptix.util.core.Hex;

/**
 * @author Stephen Cowx
 * @version
 */
public class CryptoUtils {

    private static Logger log = LogManager.getLogger(CryptoUtils.class);

    private static final byte[] salt = { 5, 19, 8, 24, 3, 10, 8, 21, 17, 5, 1 };

    /**
     * This method returns a String which is a BCD representation of the text
     * which has been hashed using an SHA encryption algorithm. This is a
     * non-reversible operation
     */
    public static String getSHAEncoded(String text) throws Exception {
        if (text == null || text.length() < 1) return "";
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] intermediateHash = md.digest(text.getBytes());
        md.update(intermediateHash);
        return new String(ByteUtils.getCharsFromBCD(md.digest(salt), false, false));
    }

    /**
     * This method encrypts the string given to it.
     */
    public static String encrypt(String valueToEncrypt, byte[] privateKeyBytes) throws Exception {
        java.security.Security.addProvider(new cryptix.provider.Cryptix());
        RawSecretKey key = new RawSecretKey("DES", privateKeyBytes);
        Cipher encrypt = Cipher.getInstance("DES/CBC/PKCS5Padding", "Cryptix");
        encrypt.initEncrypt(key);
        return Hex.toString(encrypt.doFinal(valueToEncrypt.getBytes()));
    }

    /**
     * This method decrypts a string given to it.
     */
    public static String decrypt(String valueToDecrypt, byte[] privateKeyBytes) throws Exception {
        java.security.Security.addProvider(new cryptix.provider.Cryptix());
        RawSecretKey key = new RawSecretKey("DES", privateKeyBytes);
        Cipher decrypt = Cipher.getInstance("DES/CBC/PKCS5Padding", "Cryptix");
        decrypt.initDecrypt(key);
        String decryptedText = new String(decrypt.doFinal(Hex.fromString(valueToDecrypt)));
        return decryptedText;
    }

    /**
     * This method changes a Hex String private key into an array of bytes.
     */
    public static byte[] resolveToKey(String keyHexVersion) throws Exception {
        return Hex.fromString(keyHexVersion);
    }

    /**
     * This method constructs a PrivateKey and returns it as a hex string
     */
    public static String generatePrivateKey() throws Exception {
        byte[] bytes = generatePrivateKeyBytes();
        return Hex.toString(bytes);
    }

    /**
     * This method constructs a PrivateKey
     */
    private static byte[] generatePrivateKeyBytes() throws Exception {
        DESKeyGenerator generator = new DESKeyGenerator();
        SecretKey key = generator.generateKey();
        return key.getEncoded();
    }

    public static void main(String[] args) {
        try {
            String txt = "Hi I went to my grandmothers one day.";
            String key = generatePrivateKey();
            byte[] keyBytes = CryptoUtils.resolveToKey(key);
            String encrypted = CryptoUtils.encrypt(txt, keyBytes);
            String decrypted = CryptoUtils.decrypt(encrypted, keyBytes);
            log.debug("txt       :" + txt);
            log.debug("key       :" + key);
            log.debug("encrypted :" + encrypted);
            log.debug("decrypted :" + decrypted);
            log.debug(generatePrivateKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
