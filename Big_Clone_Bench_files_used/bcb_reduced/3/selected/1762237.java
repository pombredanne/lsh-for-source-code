package br.com.petrobras.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {

    private static final String SALT = "escambar";

    private static final String TRFM_BLOWFISH = "Blowfish/ECB/PKCS5Padding";

    private static final SecretKeySpec KEY = new SecretKeySpec(new byte[] { 100, 101, 115, 116, 97, 113, 117, 101 }, "Blowfish");

    private static final String HEXITS = "0123456789ABCDEF";

    public static String decryptCipher(String codedText) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] cipherText = MyBase64.decode(codedText);
        Cipher blowfish = Cipher.getInstance(TRFM_BLOWFISH);
        blowfish.init(Cipher.DECRYPT_MODE, KEY);
        byte[] raw = blowfish.doFinal(cipherText);
        String saltedText = new String(raw);
        return saltedText.substring(0, saltedText.length() - SALT.length());
    }

    public static String encryptCipher(String clearText) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String saltedText = clearText + SALT;
        Cipher blowfish = Cipher.getInstance(TRFM_BLOWFISH);
        blowfish.init(Cipher.ENCRYPT_MODE, KEY);
        byte[] raw = blowfish.doFinal(saltedText.getBytes("UTF-8"));
        String cipherText = MyBase64.encode(raw);
        return cipherText;
    }

    public static String encryptSimple(String in) {
        in = in.trim().toUpperCase();
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            int pad = ((i + 1) % 3 == 0) ? 1 : i % 2;
            int crypt = in.charAt(i);
            out.append((char) (pad ^ crypt));
        }
        return out.toString();
    }

    public static String decryptSimple(String in) {
        return encryptSimple(in);
    }

    public static String encryptHash(String in) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        in = in.trim().toUpperCase();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] raw = digest.digest(in.getBytes("UTF-8"));
        return toHex(raw);
    }

    private static String toHex(byte[] block) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < block.length; i++) {
            buf.append(HEXITS.charAt((block[i] >>> 4) & 0xf));
            buf.append(HEXITS.charAt(block[i] & 0xf));
        }
        return buf.toString();
    }
}
