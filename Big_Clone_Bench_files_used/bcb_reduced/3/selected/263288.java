package br.eteg.curso.java.util.seguranca;

import java.security.MessageDigest;

public class SegurancaUtil {

    public static String gerarDigest(String mensagem) {
        String mensagemCriptografada = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            System.out.println("Mensagem original: " + mensagem);
            md.update(mensagem.getBytes());
            byte[] digest = md.digest();
            mensagemCriptografada = converterBytesEmHexa(digest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mensagemCriptografada;
    }

    public static String converterBytesEmHexa(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(Integer.toHexString(b));
        }
        return builder.toString();
    }
}
