package br.com.gerpro.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Criptografia {

    private static MessageDigest md = null;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Erro durante criptografia");
            ex.printStackTrace();
        }
    }

    private static char[] hexCodes(byte[] texto) {
        char[] saidaHex = new char[texto.length * 2];
        String stringHex;
        for (int i = 0; i < texto.length; i++) {
            stringHex = "00" + Integer.toHexString(texto[i]);
            stringHex.toUpperCase().getChars(stringHex.length() - 2, stringHex.length(), saidaHex, i * 2);
        }
        return saidaHex;
    }

    public static String criptografar(String senha) {
        if (md != null) {
            return new String(hexCodes(md.digest(senha.getBytes())));
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(criptografar("admin"));
    }
}
