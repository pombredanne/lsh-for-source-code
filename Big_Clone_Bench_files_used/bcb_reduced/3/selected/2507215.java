package it.aton.proj.dem.commons.sec;

import it.aton.proj.dem.commons.util.armors.Base64;
import it.aton.proj.dem.commons.util.armors.HexDump;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Various security-related utilities
 */
public class SecUtils {

    private static MessageDigest SHA256;

    private static Random RND;

    private static Cipher AES;

    static {
        try {
            long seed = System.currentTimeMillis();
            SHA256 = MessageDigest.getInstance("SHA-256");
            AES = Cipher.getInstance("AES");
            RND = SecureRandom.getInstance("SHA1PRNG");
            RND.setSeed(seed + System.currentTimeMillis());
        } catch (NoSuchAlgorithmException e) {
        } catch (NoSuchPaddingException e) {
        }
    }

    public static final byte[] computeSHA256(byte[] arg) {
        byte[] ret = new byte[32];
        synchronized (SHA256) {
            ret = SHA256.digest(arg);
        }
        return ret;
    }

    public static final byte[] computeDoubleSHA256(byte[] arg) {
        return computeSHA256(computeSHA256(arg));
    }

    public static final byte[] reduceLength(byte[] arg, int reduceTo) {
        byte[] ret = new byte[reduceTo];
        for (int i = 0; i < arg.length; i++) ret[i % reduceTo] = (byte) ((ret[i % reduceTo] ^ arg[i]) & 0xff);
        return ret;
    }

    /**
	 * Converts an arbitrary array of bytes to encoded string form
	 */
    public static final String toEncodedString(byte[] byteArray, Encoding enc) {
        if (enc == Encoding.BASE_64) return Base64.encode(byteArray);
        if (enc == Encoding.HEX_STRING) return HexDump.encode(byteArray);
        return null;
    }

    /**
	 * Convert an encoded string into its byte representation.
	 */
    public static byte[] toByteArray(String str, Encoding enc) {
        if (enc == Encoding.BASE_64) return Base64.decode(str);
        if (enc == Encoding.HEX_STRING) return HexDump.decode(str);
        return null;
    }

    private static final int PWD_LEN = 23;

    private static final char[] PWD_CHARS = { 'a', 'b', 'c', 'd', 'e', 'g', 'h', 'j', 'k', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

    /**
	 * Returns a 23 char password, with 48 chars chosen in the range a-z, A-Z to
	 * provide 128 bits of randomness (23 * log2(48) >= 128). The discarded
	 * chars are chosen to avoid graphical ambiguities.
	 */
    public static String getSecurePassword() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < PWD_LEN; i++) synchronized (RND) {
            sb.append(PWD_CHARS[RND.nextInt(PWD_CHARS.length)]);
        }
        return sb.toString();
    }

    private static SecretKeySpec getSecKey(byte[] key) {
        byte[] raw = SecUtils.reduceLength(SecUtils.computeDoubleSHA256(key), 16);
        return new SecretKeySpec(raw, "AES");
    }

    public static byte[] AESEncrypt(byte[] text, byte[] key) {
        try {
            synchronized (AES) {
                AES.init(Cipher.ENCRYPT_MODE, getSecKey(key));
                return AES.doFinal(text);
            }
        } catch (InvalidKeyException e) {
            return null;
        } catch (IllegalBlockSizeException e) {
            return null;
        } catch (BadPaddingException e) {
            return null;
        }
    }

    public static byte[] AESDecrypt(byte[] text, byte[] key) {
        try {
            synchronized (AES) {
                AES.init(Cipher.DECRYPT_MODE, getSecKey(key));
                return AES.doFinal(text);
            }
        } catch (InvalidKeyException e) {
            return null;
        } catch (IllegalBlockSizeException e) {
            return null;
        } catch (BadPaddingException e) {
            return null;
        }
    }
}
