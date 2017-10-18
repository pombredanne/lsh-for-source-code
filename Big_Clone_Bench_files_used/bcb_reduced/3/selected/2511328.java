package org.griffante.session.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Digester � a classe respons�vel para criptogrfar uma senha.
 * Os algoritmos de criptografia disponibilizados aqui s�o os dois
 * algoritmos mais utilizados, o MD5 () e o SHA-1 (). 
 * 
 * @author Giuliano B Griffante
 * 
 * @since 0.9
 */
public class Digest {

    public static final String MD5 = "MD5";

    public static final String SHA = "SHA";

    private String defaultSeparator = "";

    private MessageDigest message;

    private Digest(String algorithm) throws NoSuchAlgorithmException {
        message = MessageDigest.getInstance(algorithm);
    }

    private Digest(String algorithm, byte[] input) throws NoSuchAlgorithmException {
        message = MessageDigest.getInstance(algorithm);
        message.update(input);
    }

    public static Digest getInstance(String algorithm) throws NoSuchAlgorithmException {
        return new Digest(algorithm);
    }

    public static Digest getInstance(String algorithm, byte[] input) throws NoSuchAlgorithmException {
        return new Digest(algorithm, input);
    }

    /**
     * M�todo para criptografar a entrada (input) passada por par�metro
     * no construtor ou ent�o pela utiliza��o do m�todo update.
     * @return String contendo o array de bytes criptografados.
     */
    public String digest() {
        byte[] hash = message.digest();
        String d = "";
        for (int i = 0; i < hash.length; i++) {
            int v = hash[i] & 0xff;
            if (v < 16) d += "0";
            d += Integer.toString(v, 16).toUpperCase() + defaultSeparator;
        }
        return d;
    }

    /**
     * Informa a entrada para ser criptografada.
     * @param input dado de entrada.
     */
    public void update(byte[] input) {
        message.update(input);
    }

    /**
     * 
     * @param sep
	 */
    public void setDefaultSeparator(String sep) {
        defaultSeparator = sep;
    }

    public String getDefaultSeparator() {
        return defaultSeparator;
    }
}
