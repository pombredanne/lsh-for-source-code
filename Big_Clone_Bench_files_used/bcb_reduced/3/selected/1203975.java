package br.org.eteg.curso.javaoo.capitulo11.seguranca;

import java.security.DigestException;
import java.security.MessageDigest;

public class ExemploMessageDigest {

    /**
	 * @param args
	 * @throws DigestException
	 */
    public static void main(String[] args) {
        try {
            String mensagem = "The book is on the table";
            String mensagemCriptografada = gerarDigest(mensagem);
            System.out.println("Mensagem criptografada: " + mensagemCriptografada);
            String mensagem2 = "The book is on the table";
            String mensagemCriptografada2 = gerarDigest(mensagem2);
            System.out.println("Mensagem criptografada: " + mensagemCriptografada);
            System.out.println("Comparacao: " + mensagemCriptografada.equals(mensagemCriptografada2));
            String mensagem3 = "The book is on the table2";
            String mensagemCriptografada3 = gerarDigest(mensagem3);
            System.out.println("Mensagem criptografada: " + mensagemCriptografada3);
            System.out.println("Comparacao: " + mensagemCriptografada.equals(mensagemCriptografada3));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
