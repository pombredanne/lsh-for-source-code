package br.com.wepa.webapps.security;

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

    private static SecurityProperties properties = SecurityProperties.getInstance();

    private static SecretKey skey;

    private static KeySpec ks;

    private static PBEParameterSpec ps;

    private static final String pbeAlgorithm = properties.getPasswordEncryptAlgorithm();

    private static final String encryptlgorithm = properties.getEncryptAlgorithm();

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
	 * Realiza um digest em um array de bytes atrav�s do algoritmo especificado
	 * 
	 * @param input -
	 *            O array de bytes a ser criptografado
	 * @param algoritmo -
	 *            O algoritmo a ser utilizado
	 * @return byte[] - O resultado da criptografia
	 * @throws NoSuchAlgorithmException -
	 *             Caso o algoritmo fornecido n�o seja v�lido
	 */
    public static byte[] digest(byte[] input, String algoritmo) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algoritmo);
        md.reset();
        return md.digest(input);
    }

    /**
	 * @param input -
	 *            O array de bytes a ser convertido.
	 * @return Uma String com a representa��o hexa do array
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
	 * Converts a String top hexa byte array.
	 * 
	 * @param hexa - the hexa string 
	 * @return O vetor de bytes
	 * @throws IllegalArgumentException -
	 *             If the String is not a hexa valid string
	 */
    public static byte[] hexStringToByteArray(String hexa) throws IllegalArgumentException {
        if (hexa.length() % 2 != 0) {
            throw new IllegalArgumentException("String hexa invalida");
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
	 * @return
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
	 * @return
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

    public static void main(String[] args) throws Exception {
        String password = "hm";
        String encoded = CriptoUtils.pwEncrypt(password);
        System.out.println(encoded);
        System.out.println(CriptoUtils.pwDecrypt(encoded).equals(password));
        char[] enc = encoded.toCharArray();
        enc[2] = (char) (enc[2] + 1);
        encoded = new String(enc);
        System.out.println(encoded);
        System.out.println(password.equals(CriptoUtils.pwDecrypt(encoded)));
        System.out.println(CriptoUtils.digest("admin"));
    }
}
