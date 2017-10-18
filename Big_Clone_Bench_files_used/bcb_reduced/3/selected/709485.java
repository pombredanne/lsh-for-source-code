package org.streets.commons.util;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import com.sun.crypto.provider.SunJCE;

public class Encryptor {

    public static final String DES_ALGORITHM = "DES";

    public static final String SHA1_ALGORITHM = "SHA-1";

    public static final Key PUB_KEY = Encryptor.generateKey();

    /**
	 * 
	 * @return
	 */
    public static Key generateKey() {
        try {
            Security.addProvider(new SunJCE());
            KeyGenerator generator = KeyGenerator.getInstance("DES");
            generator.init(56, new SecureRandom());
            Key key = generator.generateKey();
            return key;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * 
	 * @param key
	 * @param encryptedString
	 * @return
	 */
    public static String decrypt(Key key, String encryptedString) {
        try {
            Security.addProvider(new SunJCE());
            Cipher cipher = Cipher.getInstance(DES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedBytes = Base64.decode(encryptedString);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedString = new String(decryptedBytes, "UTF8");
            return decryptedString;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
	 * 
	 * @param key
	 * @param plainText
	 * @return
	 */
    public static String encrypt(Key key, String plainText) {
        try {
            Security.addProvider(new SunJCE());
            Cipher cipher = Cipher.getInstance(DES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] decryptedBytes = plainText.getBytes("UTF8");
            byte[] encryptedBytes = cipher.doFinal(decryptedBytes);
            String encryptedString = Base64.encodeToString(encryptedBytes, false);
            return encryptedString;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /** 
	 * Description:用MD5或SHA-1对密码进行加密的类�??
	 */
    public byte[] digest(byte[] rawInfo) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA1_ALGORITHM);
            md.update(rawInfo);
            byte[] cipher = md.digest();
            return cipher;
        } catch (NoSuchAlgorithmException nsae) {
            return null;
        }
    }
}
