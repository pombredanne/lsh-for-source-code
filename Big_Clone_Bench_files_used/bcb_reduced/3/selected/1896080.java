package br.org.reconcavotecnologia.update19.registro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe para gerar as chaves de arquivo. Através desta chave é possível saber se um arquivo teve alguma modificação em seu conteúdo.
 * 
 * Fonte: http://www.java-tips.org/java-se-tips/java.io/reading-a-file-into-a-byte-
 * array.html
 * 
 * @author Cássio Oliveira
 */
public class MD5 {

    /** Motor para "digerir" a mensagem */
    private MessageDigest md = null;

    /** Símbolos da codificação Hexadecimal para auxiliar na criação do HASH em formato de String */
    private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** Constructor is private so you must use the getInstance method */
    public MD5() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
    }

    /** Realiza o HASH de um array de bytes */
    public String hashData(byte[] dataToHash) {
        return hexStringFromBytes((calculateHash(dataToHash)));
    }

    /** Realiza o HASH de um objeto FILE */
    public String hashData(File file) throws IOException {
        return hashData(getBytesFromFile(file));
    }

    /** Calcula o HASH em bytes a partir de um array bytes */
    private byte[] calculateHash(byte[] dataToHash) {
        md.update(dataToHash, 0, dataToHash.length);
        return (md.digest());
    }

    /** Converte um array de bytes em uma String hexadecimal*/
    private String hexStringFromBytes(byte[] b) {
        String hex = "";
        int msb;
        int lsb = 0;
        int i;
        for (i = 0; i < b.length; i++) {
            msb = ((int) b[i] & 0x000000FF) / 16;
            lsb = ((int) b[i] & 0x000000FF) % 16;
            hex = hex + hexChars[msb] + hexChars[lsb];
        }
        return (hex);
    }

    /** Retorna o array de butes de um determinado arquivo */
    private byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    /** Método de teste da classe HASH */
    public static void main(String[] args) {
        try {
            MD5 md = new MD5();
            System.out.println(md.hashData("hello".getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.out);
        }
    }
}
