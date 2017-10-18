package com.marcosperon.textdb.io;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SimpleCrypto {

    private static final String hexDigits = "0123456789abcdef";

    /**
     * Construtor
     */
    private SimpleCrypto() {
    }

    /**
     * Realiza a encripta��o apartir de uma chave.
     * 
     * @param Key
     *            - chave de codifica��o.
     * @param message
     *            - menssagem a ser codificada.
     * @return String - O resultado da criptografia em hexadecimal.
     * @throws Exception
     *             - Caso o algoritmo fornecido n�o seja v�lido
     */
    public static String encrypt(String key, String message) throws Exception {
        byte[] hexByte = asByte(key);
        SecretKeySpec skeySpec = new SecretKeySpec(hexByte, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal((message.getBytes()));
        return asHex(encrypted);
    }

    /**
     * Realiza a dencripta��o apartir de uma chave.
     * 
     * @param Key
     *            - chave de codifica��o.
     * @param message
     *            - menssagem a ser decodificada.
     * @return String - Retorna a String original.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws Exception
     *             - Caso o algoritmo fornecido n�o seja v�lido
     */
    public static String decrypt(String key, String hex) {
        byte[] hexByte = asByte(key);
        SecretKeySpec skeySpec = new SecretKeySpec(hexByte, "AES");
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        byte[] encrypted = new BigInteger(hex, 16).toByteArray();
        try {
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        byte[] original = null;
        try {
            original = cipher.doFinal(encrypted);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        String originalString = new String(original);
        return originalString;
    }

    public static String generateKey() throws NoSuchAlgorithmException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return asHex(raw);
    }

    /**
     * Realiza um digest em um array de bytes atrav�s do algoritmo especificado
     * 
     * @param input
     *            - O array de bytes a ser criptografado
     * @param algoritmo
     *            - O algoritmo a ser utilizado
     * @return String - O resultado da criptografia
     * @throws NoSuchAlgorithmException
     *             - Caso o algoritmo fornecido n�o seja v�lido
     */
    public static String digest(byte[] input, String algoritmo) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algoritmo);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.reset();
        return asHex(md.digest(input));
    }

    /**
     * Converte o array de bytes em uma representa��o hexadecimal.
     * 
     * @param input
     *            - O array de bytes a ser convertido.
     * @return Uma String com a representa��o hexa do array
     */
    public static String asHex(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (byte element : b) {
            int j = (element) & 0xFF;
            buf.append(hexDigits.charAt(j / 16));
            buf.append(hexDigits.charAt(j % 16));
        }
        return buf.toString();
    }

    /**
     * Converte uma String hexa no array de bytes correspondente.
     * 
     * @param hexa
     *            - A String hexa
     * @return O vetor de bytes
     * @throws IllegalArgumentException
     *             - Caso a String n�o sej auma representa��o haxadecimal v�lida
     */
    public static byte[] asByte(String hexa) throws IllegalArgumentException {
        if (hexa.length() % 2 != 0) throw new IllegalArgumentException("String hexa inv�lida");
        byte[] b = new byte[hexa.length() / 2];
        for (int i = 0; i < hexa.length(); i += 2) {
            b[i / 2] = (byte) ((hexDigits.indexOf(hexa.charAt(i)) << 4) | (hexDigits.indexOf(hexa.charAt(i + 1))));
        }
        return b;
    }
}
