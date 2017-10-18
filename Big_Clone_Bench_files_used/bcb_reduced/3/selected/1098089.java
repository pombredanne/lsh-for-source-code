package ao.com.bna.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class EncriptacaoUtil {

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

    public static boolean validaSenha(String senhaNoBanco, String senhaIntroduzida) throws NoSuchAlgorithmException {
        byte[] b;
        b = EncriptacaoUtil.digest(senhaIntroduzida.getBytes(), "md5");
        String senhaCriptografada = EncriptacaoUtil.byteArrayToHexString(b);
        if (senhaNoBanco.equals(senhaCriptografada)) {
            return true;
        } else {
            return false;
        }
    }
}
