package siac.com.controller;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.JOptionPane;

public class Encriptacao2 {

    public static boolean validaSenha(String senha) throws NoSuchAlgorithmException {
        String senhaNoBanco = "";
        byte[] b;
        try {
            b = CriptoUtils.digest(senha.getBytes(), "md5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        String senhaCriptografada = CriptoUtils.byteArrayToHexString(b);
        if (senhaNoBanco.equalsIgnoreCase(senhaCriptografada)) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            String senha = JOptionPane.showInputDialog("Digite uma senha:");
            BigInteger hash = new BigInteger(1, CriptoUtils.digest(senha.getBytes(), "MD5"));
            String saida = "Entrada: " + senha + "\nSenha com MD5: " + hash.toString(16);
            JOptionPane.showConfirmDialog(null, saida + "\nTamanho: " + senha.length() + "\ninteiro: " + hash.toString(), "Resultado", JOptionPane.CLOSED_OPTION);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}

final class CriptoUtils {

    private static final String digitoHexadecimal = "0123456789abcdef";

    /**
	 * Realiza um digest em um array de bytes através do algoritmo especificado
	 * 
	 * @param input
	 *            - O array de bytes a ser criptografado
	 * @param algoritmo
	 *            - O algoritmo a ser utilizado
	 * @return byte[] - O resultado da criptografia
	 * @throws NoSuchAlgorithmException
	 *             - Caso o algoritmo fornecido não seja válido
	 */
    public static byte[] digest(byte[] input, String algoritmo) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algoritmo);
        md.reset();
        return md.digest(input);
    }

    /**
	 * Converte o array de bytes em uma representação hexadecimal.
	 * 
	 * @param input
	 *            - O array de bytes a ser convertido.
	 * @return Uma String com a representação hexa do array
	 */
    public static String byteArrayToHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int j = ((int) b[i]) & 0xFF;
            buf.append(digitoHexadecimal.charAt(j / 16));
            buf.append(digitoHexadecimal.charAt(j % 16));
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
	 *             - Caso a String não sej auma representação haxadecimal válida
	 */
    public static byte[] hexStringToByteArray(String hexa) throws IllegalArgumentException {
        if (hexa.length() % 2 != 0) {
            throw new IllegalArgumentException("String hexa inválida");
        }
        byte[] b = new byte[hexa.length() / 2];
        for (int i = 0; i < hexa.length(); i += 2) {
            b[i / 2] = (byte) ((digitoHexadecimal.indexOf(hexa.charAt(i)) << 4) | (digitoHexadecimal.indexOf(hexa.charAt(i + 1))));
        }
        return b;
    }
}
