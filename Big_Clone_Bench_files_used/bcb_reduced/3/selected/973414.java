package org.sempere.commons.encryption;

import org.apache.commons.lang.*;
import org.slf4j.*;
import sun.misc.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/**
 * Encrypter implementation based on the classes provided by the JDK.
 *
 * @author bsempere
 */
public class DefaultEncrypter implements Encrypter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEncrypter.class);

    protected static final int DEFAULT_RGS_LENGTH = 20;

    private PassPhraseProvider provider;

    public DefaultEncrypter() {
    }

    public DefaultEncrypter(PassPhraseProvider provider) {
        this.provider = provider;
    }

    public String encrypt(String clearText) {
        if (this.provider == null) {
            throw new IllegalStateException("PassPhraseProvider is not correctly set. Please initialize it by using the appropriate constructor or setter.");
        }
        return this.encrypt(clearText, this.provider.getPassPhrase());
    }

    public String decrypt(String cipherText) {
        if (this.provider == null) {
            throw new IllegalStateException("PassPhraseProvider is not correctly set. Please initialize it by using the appropriate constructor.");
        }
        return this.decrypt(cipherText, this.provider.getPassPhrase());
    }

    public String encrypt(String clearText, String passPhrase) {
        return this.encrypt(clearText, this.createKey(passPhrase));
    }

    public String decrypt(String cipherText, String passPhrase) {
        return this.decrypt(cipherText, this.createKey(passPhrase));
    }

    public String encrypt(String clearText, Key key) {
        if (StringUtils.isBlank(clearText)) {
            throw new IllegalArgumentException("Cannot encrypt empty text.");
        }
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null.");
        }
        byte[] cipherTextBytes = null;
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] clearTextBytes = clearText.getBytes();
            cipherTextBytes = cipher.doFinal(clearTextBytes);
        } catch (Exception e) {
            throw new CryptoException("Cannot encrypt text [" + clearText + "].", e);
        }
        return new BASE64Encoder().encode(cipherTextBytes);
    }

    public String decrypt(String cipherText, Key key) {
        if (StringUtils.isBlank(cipherText)) {
            throw new IllegalArgumentException("Cannot decrypt empty text data.");
        }
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null.");
        }
        byte[] clearTextBytes = null;
        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cipherTextBytes = new BASE64Decoder().decodeBuffer(cipherText);
            clearTextBytes = cipher.doFinal(cipherTextBytes);
        } catch (Exception e) {
            throw new CryptoException("Cannot decrypt text [" + cipherText + "].", e);
        }
        return new String(clearTextBytes);
    }

    public byte[] getRandomlyGeneratedSalt() {
        return getRandomlyGeneratedSalt(DEFAULT_RGS_LENGTH);
    }

    public byte[] getRandomlyGeneratedSalt(int length) {
        byte[] rgs = new byte[length];
        new Random().nextBytes(rgs);
        return rgs;
    }

    public byte[] getPasswordHash(byte[] password, byte[] rgs) {
        byte[] pwdrgs = ArrayUtils.addAll(password, rgs);
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-1");
            digester.update(pwdrgs);
            return digester.digest();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Algorithm could not be found.", e);
        }
        return null;
    }

    public boolean verifyPassword(byte[] password, byte[] pwh, byte[] rgs) {
        byte[] expectedPwh = getPasswordHash(password, rgs);
        return Arrays.equals(pwh, expectedPwh);
    }

    protected Key createKey(String passPhrase) {
        if (StringUtils.isBlank(passPhrase)) {
            throw new IllegalArgumentException("PassPhrase cannot be empty.");
        }
        Key key = null;
        try {
            KeySpec keySpec = new DESKeySpec(passPhrase.getBytes());
            key = SecretKeyFactory.getInstance("DES").generateSecret(keySpec);
        } catch (Exception e) {
            throw new CryptoException("Cannot create a key from the passPhrase [" + passPhrase + "].", e);
        }
        return key;
    }

    public void setProvider(PassPhraseProvider provider) {
        this.provider = provider;
    }
}
