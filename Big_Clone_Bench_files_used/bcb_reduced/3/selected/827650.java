package br.com.sms.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cripto {

    private static MessageDigest md = null;

    public Cripto() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * : Método de criptografia.
	 * @param text
	 * @return
	 */
    private char[] hexCodes(byte[] text) {
        char[] hexOutput = new char[text.length * 2];
        String hexString;
        for (int i = 0; i < text.length; i++) {
            hexString = "$isP@m" + Integer.toHexString(text[i]);
            hexString.toUpperCase().getChars(hexString.length() - 2, hexString.length(), hexOutput, i * 2);
        }
        return hexOutput;
    }

    /**
	 * : Recebe a senha para ser criptografada.
	 * @param senha
	 * @return
	 */
    public String criptografar(String senha) {
        if (md != null) {
            return new String(hexCodes(md.digest(senha.getBytes())));
        }
        return null;
    }
}
