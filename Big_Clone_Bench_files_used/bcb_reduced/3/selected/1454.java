package hash;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author F�bio Nogueira de Lucena
 * @version 0.1
 */
public class HashingImpl implements Hashing {

    public byte[] md5(byte[] entrada) {
        return getHash(entrada, "MD5");
    }

    private byte[] getHash(String nomeArquivo, String metodo) {
        MessageDigest md = null;
        FileInputStream fis = null;
        try {
            md = MessageDigest.getInstance(metodo);
            byte[] buffer = new byte[4096];
            fis = new FileInputStream(nomeArquivo);
            int bytesLidos = -1;
            while ((bytesLidos = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesLidos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
        }
        return md.digest();
    }

    /**
     * Obt�m o valor de hash para a entrada fornecida. A fun��o empregada
     * tamb�m � fornecida como argumento.
     * @param entrada Array de bytes cujo valor de hash � desejado.
     * @param hashFunction M�todo de hash a ser empregado.
     * @return Valor de hash.
     */
    private byte[] getHash(byte[] entrada, String hashFunction) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(hashFunction);
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            return null;
        }
        return md.digest(entrada);
    }

    public byte[] sha1(byte[] entrada) {
        return getHash(entrada, "SHA-1");
    }

    /**
     * Obt�m os 16 bytes (128 bits) do valor de hash empregando o 
     * algoritmo MD5 para o arquivo cujo nome � fornecido.
     * @param nomeArquivo Nome do arquivo cujo valor de hash � desejado.
     * @return Valor de hash MD5 do arquivo fornecido ou null, caso a opera��o
     * tenha sido realizada insatisfatoriamente.
     */
    public byte[] md5(String nomeArquivo) {
        return getHash(nomeArquivo, "MD5");
    }

    public byte[] sha1(String nomeArquivo) {
        return getHash(nomeArquivo, "SHA");
    }

    public String toHex(byte[] hash) {
        final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char strHash[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            strHash[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            strHash[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(strHash);
    }
}
