package easyaccept.util.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class CriptoUtils {

    private static SecretKey skey;

    private static KeySpec ks;

    private static PBEParameterSpec ps;

    private static final String pbeAlgorithm = "PBEWithMD5AndDES";

    private static final String encryptlgorithm = "md5";

    private static BASE64Encoder enc = new BASE64Encoder();

    private static BASE64Decoder dec = new BASE64Decoder();

    static {
        try {
            final byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };
            SecretKeyFactory skf = SecretKeyFactory.getInstance(pbeAlgorithm);
            ps = new PBEParameterSpec(salt, 20);
            ks = new PBEKeySpec("Xbt7.fd*38Ab+B4-*/d]".toCharArray());
            skey = skf.generateSecret(ks);
        } catch (java.security.NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (java.security.spec.InvalidKeySpecException ex) {
            ex.printStackTrace();
        }
    }

    private static final String hexDigits = "0123456789abcdef";

    /**
	 * Digests an array of bytes
	 * 
	 * @param input
	 * @param algorithm 
	 * @return byte[] - result
	 * @throws NoSuchAlgorithmException 
	 */
    public static byte[] digest(byte[] input, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.reset();
        return md.digest(input);
    }

    /**
	 * @param input
	 * @return hexa string
	 */
    public static String byteArrayToHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int j = ((int) b[i]) & 0xFF;
            buf.append(hexDigits.charAt(j / 16));
            buf.append(hexDigits.charAt(j % 16));
        }
        return buf.toString();
    }

    /**
	 * Converts a String to hexa byte array.
	 * 
	 * @param hexa -
	 *            the hexa string
	 * @return result
	 * @throws IllegalArgumentException -
	 *             If the String is not a hexa valid string
	 */
    public static byte[] hexStringToByteArray(String hexa) throws IllegalArgumentException {
        if (hexa.length() % 2 != 0) {
            throw new IllegalArgumentException("Hexa String invalid");
        }
        byte[] b = new byte[hexa.length() / 2];
        for (int i = 0; i < hexa.length(); i += 2) {
            b[i / 2] = (byte) ((hexDigits.indexOf(hexa.charAt(i)) << 4) | (hexDigits.indexOf(hexa.charAt(i + 1))));
        }
        return b;
    }

    /**
	 * Digest a string with no decrypt disponible
	 * @param value
	 * @return string
	 */
    public static String digest(String value) {
        byte[] b;
        try {
            b = CriptoUtils.digest(value.getBytes(), encryptlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return CriptoUtils.byteArrayToHexString(b);
    }

    /**
	 * Encrypts password 
	 * @param text
	 * @return encrypted string
	 * @throws BadPaddingException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 */
    public static final String pwEncrypt(final String text) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        final Cipher cipher = Cipher.getInstance(pbeAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, skey, ps);
        return enc.encode(cipher.doFinal(text.getBytes()));
    }

    /**
	 * Decrypts password 
	 * @param text
	 * @return
	 * @throws BadPaddingException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 */
    public static final String pwDecrypt(final String text) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        final Cipher cipher = Cipher.getInstance(pbeAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, skey, ps);
        String ret = null;
        try {
            ret = new String(cipher.doFinal(dec.decodeBuffer(text)));
        } catch (Exception ex) {
        }
        return ret;
    }
}
