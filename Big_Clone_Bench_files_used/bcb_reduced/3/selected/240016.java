package de.renier.jkeepass.encryption;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import de.renier.jkeepass.util.Utils;

/**
 * Cryptor
 *
 * @author <a href="mailto:software@renier.de">Renier Roth</a>
 */
public class Cryptor {

    private static final String CHARACTOR_CODE = "UTF-8";

    private static byte[] pass = null;

    /**
   * Constructor
   */
    private Cryptor() {
    }

    /**
   * initialize
   *
   * @param key
   * @throws CryptionException
   */
    public static void initialize(String key) throws CryptionException {
        if (!Utils.isEmpty(key)) {
            try {
                pass = buildValid128BitKey(key);
            } catch (Exception e) {
                throw new CryptionException(e.getMessage());
            }
        } else {
            throw new CryptionException("Initialize with empty key is not allowed");
        }
    }

    /**
   * encryptText
   *
   * @param key
   * @param text
   * @return encrypted text as byte[]
   * @throws CryptionException
   */
    public static byte[] encryptText(String text) throws CryptionException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(pass, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(text.getBytes(CHARACTOR_CODE));
        } catch (Exception e) {
            throw new CryptionException(e.getMessage());
        }
    }

    /**
   * encryptText
   *
   * @param out
   * @param text
   * @throws CryptionException
   */
    public static void encryptText(OutputStream out, String text) throws CryptionException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(pass, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            GZIPOutputStream zipOut = new GZIPOutputStream(new CipherOutputStream(out, cipher));
            zipOut.write(text.getBytes(CHARACTOR_CODE));
            zipOut.flush();
            zipOut.close();
        } catch (Exception e) {
            throw new CryptionException(e.getMessage());
        }
    }

    /**
   * decryptText
   *
   * @param key
   * @param text
   * @return decrypted text as byte[]
   * @throws CryptionException
   */
    public static String decryptText(InputStream in) throws CryptionException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(pass, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            GZIPInputStream zipIn = new GZIPInputStream(new CipherInputStream(in, cipher));
            int read = 0;
            byte[] data = new byte[1024];
            StringBuffer buffer = new StringBuffer();
            while ((read = zipIn.read(data, 0, 1024)) != -1) {
                buffer.append(new String(data, 0, read, CHARACTOR_CODE));
            }
            zipIn.close();
            return buffer.toString();
        } catch (Exception e) {
            throw new CryptionException(e.getMessage());
        }
    }

    /**
   * decryptText
   *
   * @param decryptedText
   * @return
   * @throws CryptionException
   */
    public static String decryptText(byte[] decryptedText) throws CryptionException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(pass, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return new String(cipher.doFinal(decryptedText), CHARACTOR_CODE);
        } catch (Exception e) {
            throw new CryptionException(e.getMessage());
        }
    }

    /**
   * buildValid128BitKey
   *
   * @param key
   * @return
   * @throws UnsupportedEncodingException 
   * @throws NoSuchAlgorithmException 
   */
    private static byte[] buildValid128BitKey(String key) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5").digest(key.getBytes(CHARACTOR_CODE));
    }

    /**
   * CryptionException
   * 
   * @author <a href="mailto:roth@hlp.de">Renier Roth</a>
   */
    public static class CryptionException extends Exception {

        private static final long serialVersionUID = -4516742168434373663L;

        public CryptionException() {
            super();
        }

        public CryptionException(String message) {
            super(message);
        }
    }

    /**
   * isInitialized
   *
   * @return
   */
    public static boolean isInitialized() {
        return pass != null && pass.length > 0;
    }
}
