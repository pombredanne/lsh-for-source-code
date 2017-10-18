package at.zweinull.druidsafe.core.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import at.zweinull.druidsafe.core.exceptions.DruidSafeRuntimeException;
import at.zweinull.druidsafe.core.storage.PasswordItem;

/**
 * @version $Revision: $
 * @author Hannes Halenka <hannes@zweinull.at>
 * @since 0.1
 */
public final class SecurityHelper {

    private SecurityHelper() {
    }

    /**
     * @param password
     *            Clear text password
     * @param salt
     *            Salt
     * @return Hashed string
     * @throws NullPointerException
     *             If the password parameter is null
     * @throws DruidSafeRuntimeException
     */
    public static String hashClientPassword(String algorithm, String password, String salt) throws IllegalArgumentException, DruidSafeRuntimeException {
        if (algorithm == null) {
            throw new IllegalArgumentException("THE ALGORITHM MUST NOT BE NULL");
        }
        if (password == null) {
            throw new IllegalArgumentException("THE PASSWORD MUST NOT BE NULL");
        }
        if (salt == null) {
            salt = "";
        }
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(password.getBytes());
            md.update(salt.getBytes());
            result = SecurityHelper.byteArrayToHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new DruidSafeRuntimeException(e);
        }
        return result;
    }

    /**
     * Generates a secure salt
     * 
     * @return The salt
     */
    public static String generateSecureSalt() {
        return (new BigInteger(130, new SecureRandom())).toString(32);
    }

    /**
     * Generates a secure salt with specific length
     * 
     * @param length
     * @return The salt
     * @throws IllegalArgumentException
     *             if length < 1
     */
    public static String generateSecureSalt(int length) throws IllegalArgumentException {
        if (length < 1) {
            throw new IllegalArgumentException("THE LENGTH MUST NOT BE SMALLER THAN 1");
        }
        int baseLength = 26;
        if (length > baseLength) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < (length / 26) + 1; i++) {
                sb.append(generateSecureSalt());
            }
            return sb.substring(0, length);
        }
        return generateSecureSalt().substring(0, length);
    }

    /**
     * @param input
     * @return The hex string of a byte array
     */
    public static String byteArrayToHexString(byte[] input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException();
        }
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            output.append(Integer.toString((input[i] & 0xff) + 0x100, 16).substring(1));
        }
        return output.toString();
    }

    /**
     * @param key
     *            The secret key
     * @return The hex string
     */
    public static String secretKeyToHexString(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("THE KEY MUST NOT BE NULL");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(key);
            out.close();
        } catch (IOException e) {
            throw new DruidSafeRuntimeException(e);
        }
        return byteArrayToHexString(baos.toByteArray());
    }

    public static byte[] hexStringToByteArray(String str) throws NullPointerException, IllegalArgumentException {
        if (str == null) {
            throw new NullPointerException("THE INPUT STRING MUST NOT BE NULL");
        }
        int len = str.length();
        if (len < 1) {
            throw new IllegalArgumentException("THE STRING MUST NOT BE LEFT EMTPY");
        }
        if (len % 2 != 0) {
            throw new NumberFormatException("INVALID STRING LENGTH");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Generates a SecretKey instance by using a password/passphrase
     * 
     * @param password
     * @param algorithm
     * @return The SecretKey instance
     * @throws IllegalArgumentException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static SecretKey getSecretKeyFromPassword(String password, String algorithm) throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (password == null) {
            throw new IllegalArgumentException("THE PASSWORD MUST NOT BE NULL");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("THE ALGORITHM MUST NOT BE NULL");
        }
        SecretKey key = null;
        SecretKeyFactory keyFact = SecretKeyFactory.getInstance(algorithm);
        key = keyFact.generateSecret(new PBEKeySpec(password.toCharArray()));
        return key;
    }

    /**
     * Generates a secret key
     * 
     * @param algorithm
     * @return The secret key
     * @throws NullPointerException
     *             If the algorithm is NULL
     * @throws NoSuchAlgorithmException
     */
    public static SecretKey generateSecretKey(String algorithm) throws NullPointerException, NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new IllegalArgumentException("THE ALGORITHM MUST NOT BE NULL");
        }
        return KeyGenerator.getInstance(algorithm).generateKey();
    }

    public static byte[] wrapSecretKey(SecretKey wrappingKey, SecretKey keyToBeWrapped, String salt) throws IllegalArgumentException, GeneralSecurityException {
        int iterationCount = 20;
        if (wrappingKey == null) {
            throw new IllegalArgumentException("THE WRAPPING KEY MUST NOT BE NULL");
        }
        if (keyToBeWrapped == null) {
            throw new IllegalArgumentException("THE KEY TO BE WRAPPED MUST NOT BE NULL");
        }
        if (salt == null) {
            throw new IllegalArgumentException("THE SALT MUST NOT BE NULL");
        }
        if (salt.getBytes().length != 8) {
            throw new IllegalArgumentException("INVALID SALT LENGTH");
        }
        PBEParameterSpec paramSpec = new PBEParameterSpec(salt.getBytes(), iterationCount);
        Cipher c = Cipher.getInstance(wrappingKey.getAlgorithm());
        c.init(Cipher.WRAP_MODE, wrappingKey, paramSpec);
        return c.wrap(keyToBeWrapped);
    }

    public static SecretKey unwrapSecretKey(SecretKey wrappingKey, byte[] wrappedKey, String algorithm, String salt) throws GeneralSecurityException {
        int iterationCount = 20;
        if (wrappedKey == null) {
            throw new IllegalArgumentException("THE WRAPPING KEY MUST NOT BE NULL");
        }
        if (wrappedKey == null) {
            throw new IllegalArgumentException("THE WRAPPED KEY MUST NOT BE NULL");
        }
        if (wrappedKey.length < 1) {
            throw new IllegalArgumentException("THE WRAPPED KEY MUST NOT BE LEFT BLANK");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("THE ALGORITHM MUST NOT BE NULL");
        }
        if (salt == null) {
            throw new IllegalArgumentException("THE SALT MUST NOT BE NULL");
        }
        if (salt.getBytes().length != 8) {
            throw new IllegalArgumentException("INVALID SALT LENGTH");
        }
        Cipher c = Cipher.getInstance(wrappingKey.getAlgorithm());
        c.init(Cipher.UNWRAP_MODE, wrappingKey, new PBEParameterSpec(salt.getBytes(), iterationCount));
        return (SecretKey) c.unwrap(wrappedKey, algorithm, Cipher.SECRET_KEY);
    }

    public static void encryptPasswordItem(final PasswordItem item, final SecretKey key) throws GeneralSecurityException {
        if (item == null) {
            throw new IllegalArgumentException("THE ITEM MUST NOT BE NULL");
        }
        if (key == null) {
            throw new IllegalArgumentException("THE KEY MUST NOT BE NULL");
        }
        Cipher c = Cipher.getInstance(key.getAlgorithm());
        c.init(Cipher.ENCRYPT_MODE, key);
        if (item.getUsername() != null) {
            item.setUsername(SecurityHelper.byteArrayToHexString(c.doFinal(item.getUsername().getBytes())));
        }
        if (item.getUrl() != null) {
            item.setUrl(SecurityHelper.byteArrayToHexString(c.doFinal(item.getUrl().getBytes())));
        }
        if (item.getPassword() != null) {
            item.setPassword(SecurityHelper.byteArrayToHexString(c.doFinal(item.getPassword().getBytes())));
        }
        if (item.getComment() != null) {
            item.setComment(SecurityHelper.byteArrayToHexString(c.doFinal(item.getComment().getBytes())));
        }
        if (item.getExpireDate() != null) {
            item.setExpireDate(SecurityHelper.byteArrayToHexString(c.doFinal(item.getExpireDate().getBytes())));
        }
    }

    public static void decryptPasswordItem(final PasswordItem item, final SecretKey key) throws GeneralSecurityException {
        if (item == null) {
            throw new IllegalArgumentException("THE ITEM MUST NOT BE NULL");
        }
        if (key == null) {
            throw new IllegalArgumentException("THE KEY MUST NOT BE NULL");
        }
        Cipher c = Cipher.getInstance(key.getAlgorithm());
        c.init(Cipher.DECRYPT_MODE, key);
        if (item.getUsername() != null) {
            item.setUsername(new String(c.doFinal(SecurityHelper.hexStringToByteArray(item.getUsername()))));
        }
        if (item.getUrl() != null) {
            item.setUrl(new String(c.doFinal(SecurityHelper.hexStringToByteArray(item.getUrl()))));
        }
        if (item.getPassword() != null) {
            item.setPassword(new String(c.doFinal(SecurityHelper.hexStringToByteArray(item.getPassword()))));
        }
        if (item.getComment() != null) {
            item.setComment(new String(c.doFinal(SecurityHelper.hexStringToByteArray(item.getComment()))));
        }
        if (item.getExpireDate() != null) {
            item.setExpireDate(new String(c.doFinal(SecurityHelper.hexStringToByteArray(item.getExpireDate()))));
        }
    }
}
