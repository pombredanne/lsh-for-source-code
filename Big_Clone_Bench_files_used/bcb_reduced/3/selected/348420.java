package axt.crypt;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import axt.config.Configuration;

public class DESCrypt {

    private static Log log = LogFactory.getLog("axt.crypt");

    private Configuration config = new Configuration();

    private String DESKEYFILE = config.getProperty("DESKeyFile");

    private String VALIDATIONDIGEST = config.getProperty("fileValidationDigest");

    private Cipher ecipher;

    private Cipher dcipher;

    private SecretKey key;

    public DESCrypt() throws Exception {
        log.debug("Starting DESCrypt");
        try {
            ecipher = Cipher.getInstance("DESede");
            dcipher = Cipher.getInstance("DESede");
            if (DESKEYFILE == null) {
                throw new Exception("No DES KeyFile Defined in the properties.");
            }
            try {
                key = readKey(new File(DESKEYFILE));
            } catch (InvalidKeySpecException specError) {
                log.error("Could not load DES Key File - ERROR: " + specError);
                throw new Exception("Could not load DES Key File - ERROR: " + specError);
            } catch (IOException DESIOError) {
                log.error("Could not load DES Key File - ERROR: " + DESIOError);
                throw new Exception("Could not load DES Key File - ERROR: " + DESIOError);
            }
            ecipher.init(Cipher.ENCRYPT_MODE, key);
            dcipher.init(Cipher.DECRYPT_MODE, key);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            log.error("Could Not Load Cipher - ERROR: " + e);
            throw new Exception("Could Not Load Cipher - ERROR: " + e);
        } catch (javax.crypto.NoSuchPaddingException e) {
            log.error("Could Not Load Cipher - ERROR: " + e);
            throw new Exception("Could Not Load Cipher - ERROR: " + e);
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("Could Not Load Cipher - ERROR: " + e);
            throw new Exception("Could Not Load Cipher - ERROR: " + e);
        } catch (java.security.InvalidKeyException e) {
            log.error("Could Not Load Cipher - ERROR: " + e);
            throw new Exception("Could Not Load Cipher - ERROR: " + e);
        }
    }

    public Object[] encrypt(InputStream in, OutputStream out, long fs) throws Exception {
        log.debug("Building Message Digest");
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance(VALIDATIONDIGEST);
        } catch (NoSuchAlgorithmException algException) {
            throw new Exception("Failed to get the defined file validation digest \"" + VALIDATIONDIGEST + "\" - ERROR: " + algException);
        }
        algorithm.reset();
        log.debug("Building Cipher Stream");
        CipherOutputStream cos = new CipherOutputStream(out, ecipher);
        byte[] buffer = new byte[2048];
        int bytesRead;
        log.debug("Starting file read");
        long fileSize = 0L;
        while (true) {
            if (fs >= 0L) {
                int foo;
                if (buffer.length < fs) {
                    foo = buffer.length;
                } else {
                    foo = (int) fs;
                }
                bytesRead = in.read(buffer, 0, foo);
                fs -= foo;
            } else {
                bytesRead = in.read(buffer);
            }
            if (bytesRead == -1) {
                break;
            }
            cos.write(buffer, 0, bytesRead);
            algorithm.update(buffer, 0, bytesRead);
            fileSize = fileSize + bytesRead;
            if (fs == 0L) {
                break;
            }
        }
        cos.close();
        log.debug("File Size is: " + fileSize);
        java.util.Arrays.fill(buffer, (byte) 0);
        byte messageDigest[] = algorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String MD5String = hexString.toString().toLowerCase();
        log.debug("File MD5 Hash: " + MD5String);
        Object[] returnValues = { MD5String, fileSize };
        return returnValues;
    }

    public Object[] encrypt(InputStream in, OutputStream out) throws Exception {
        return this.encrypt(in, out, -1L);
    }

    public void decrypt(InputStream in, OutputStream out) throws NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException {
        log.debug("Decrypting Data");
        byte[] buffer = new byte[2048];
        int bytesRead;
        long totalBytes = 0;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(dcipher.update(buffer, 0, bytesRead));
            out.flush();
            totalBytes += bytesRead;
        }
        log.debug("Total Data Transfered: " + totalBytes);
        log.debug("Writing final bit of data");
        out.write(dcipher.doFinal());
        log.debug("Flushing output stream");
        out.flush();
    }

    /** Read a TripleDES secret key from the specified file */
    public static SecretKey readKey(File f) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        byte[] rawkey = new byte[(int) f.length()];
        in.readFully(rawkey);
        in.close();
        DESedeKeySpec keyspec = new DESedeKeySpec(rawkey);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("DESede");
        SecretKey key = keyfactory.generateSecret(keyspec);
        return key;
    }
}
