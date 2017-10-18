package onepoint.project.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import sun.security.rsa.RSAPublicKeyImpl;
import sun.security.util.DerValue;

/**
 * This OpHashProvider class provides the functionality of a message digest algorithm, such as MD5 or SHA. 
 * Message digests are secure one-way hash functions that take arbitrary-sized data and output a fixed-length hash 
 * value.
 * 
 * @author dfreis
 */
public class OpHashProvider {

    /**
    * the Op internally used encoding algorithm (SHA-1)
    */
    public static final String INTERNAL = "SHA-1";

    /**
    * used for Plain text authentication. This should never be used, but some modules (like ldap) do support this.
    */
    public static final String PLAIN = "PLAIN";

    private static final XLog logger = XLogFactory.getLogger(OpHashProvider.class);

    /**
    * Default Constructor
    */
    public OpHashProvider() {
    }

    /**
    * Calculates an SHA-1 hash for the given data.
    * @param data the data to calculate the hash for
    * @return the created hash.
    */
    public static String calculateHash(String data) {
        return calculateHash(data, INTERNAL);
    }

    /**
    * Calculates a hash for the given data. Known algorithms are: MD2, MD5, SHA-1, SHA-256, SHA-384, and SHA-51.
    * @param data the data to calculate the hash for
    * @param algorithm the algorithm to use, one of MD2, MD5, SHA-1, SHA-256, SHA-384, and SHA-51.
    * @return the calculated hash, or <code>null</code> if the given algorithm is unknown.
    */
    public static String calculateHash(String data, String algorithm) {
        if (data == null) {
            return null;
        }
        algorithm = (algorithm == null ? INTERNAL : algorithm.toUpperCase());
        if (algorithm.equals(PLAIN)) {
            return data;
        }
        if (algorithm.startsWith("{RSA}")) {
            return encode(data, algorithm.substring(5), "RSA");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(data.getBytes("UTF-8"));
            return getHashString(md.digest());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException nsae) {
            logger.error(nsae.getMessage());
            return null;
        }
    }

    private static String encode(String dataString, String key, String algorithm) {
        try {
            byte[] data = dataString.getBytes("UTF-8");
            byte[] keyData = fromHashString(key);
            DerValue der = new DerValue(keyData);
            PublicKey publicKey = RSAPublicKeyImpl.parse(der);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherData = cipher.doFinal(data);
            return getHashString(cipherData);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
        } catch (NoSuchPaddingException e) {
        } catch (IOException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (BadPaddingException e) {
        }
        return null;
    }

    /**
    * Print out the digest as a hex string
    */
    public static String getHashString(byte[] data) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            ret.append(Integer.toHexString(0xF0 & data[i]).charAt(0));
            ret.append(Integer.toHexString(0x0F & data[i]));
        }
        return ret.toString().toUpperCase();
    }

    /**
    * Print out the digest as a hex string
    */
    public static byte[] fromHashString(String data) {
        byte[] ret = new byte[data.length() / 2];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) Integer.parseInt(data.substring(2 * i, (2 * i) + 2), 16);
        }
        return ret;
    }
}
