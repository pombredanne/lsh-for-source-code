package siac.com.controller;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

public class Encriptacao {

    public static String md5(String senha) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        BigInteger hash = new BigInteger(1, md.digest(senha.getBytes()));
        return hash.toString(16);
    }

    public static void main(String[] args) {
        try {
            String senha = JOptionPane.showInputDialog("Digite uma senha:");
            String saida = "Entrada: " + senha + "\nSenha com MD5: " + md5(senha);
            JOptionPane.showConfirmDialog(null, saida, "Resultado", JOptionPane.CLOSED_OPTION);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
