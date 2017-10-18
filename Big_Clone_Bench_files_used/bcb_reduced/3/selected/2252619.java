package br.com.danielnegri.nfe.negocio.util;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public final class CriptoUtils {

    private static final String hexDigits = "0123456789abcdef";

    /**
	   * Realiza um digest em um array de bytes atrav�s do algoritmo especificado
	   * @param input - O array de bytes a ser criptografado
	   * @param algoritmo - O algoritmo a ser utilizado
	   * @return byte[] - O resultado da criptografia
	   * @throws NoSuchAlgorithmException - Caso o algoritmo fornecido n�o seja
	   * v�lido
	   */
    public static byte[] digest(byte[] input, String algoritmo) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algoritmo);
        md.reset();
        return md.digest(input);
    }

    /**
	   * Converte o array de bytes em uma representa��o hexadecimal.
	   * @param input - O array de bytes a ser convertido.
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
	     * Converte uma String hexa no array de bytes correspondente.
	     * @param hexa - A String hexa
	     * @return O vetor de bytes
	     * @throws IllegalArgumentException - Caso a String n�o seja uma
	     * representa��o haxadecimal v�lida
	     */
    public static byte[] hexStringToByteArray(String hexa) throws IllegalArgumentException {
        if (hexa.length() % 2 != 0) {
            throw new IllegalArgumentException("String hexa inv�lida");
        }
        byte[] b = new byte[hexa.length() / 2];
        for (int i = 0; i < hexa.length(); i += 2) {
            b[i / 2] = (byte) ((hexDigits.indexOf(hexa.charAt(i)) << 4) | (hexDigits.indexOf(hexa.charAt(i + 1))));
        }
        return b;
    }

    public static String criptografar(String mensagem, String senha) {
        try {
            DESKeySpec keySpec = new DESKeySpec(senha.getBytes());
            SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, factory.generateSecret(keySpec));
            byte[] hashMensagem = cipher.doFinal(mensagem.getBytes());
            return byteArrayToHexString(hashMensagem);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String descriptografar(String mensagem, String senha) {
        try {
            DESKeySpec keySpec = new DESKeySpec(senha.getBytes());
            SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, factory.generateSecret(keySpec));
            byte[] hashMensagemHex = hexStringToByteArray(mensagem);
            byte[] hashMensagem = cipher.doFinal(hashMensagemHex);
            return new String(hashMensagem);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
