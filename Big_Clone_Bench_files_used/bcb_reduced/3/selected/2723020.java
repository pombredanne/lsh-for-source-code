package converters;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author pedro
 */
public class Criptografia {

    public static String md5(String senha) throws UnsupportedEncodingException {
        String sen;
        StringBuilder sb = new StringBuilder(32);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(senha.getBytes("UTF-8")));
        sen = hash.toString(16);
        int tam = sen.length();
        while (tam < 32) {
            sb.append('0');
            tam++;
        }
        sb.append(sen);
        return sb.toString();
    }

    public static String randomMd5() {
        int i = 0;
        char[] senha = new char[8];
        char[] charPermitidos = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'z', 'w', 'y', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'X', 'Z', 'W', 'Y' };
        while (i < 8) {
            int rand = (int) (1 + Math.random() * 62);
            senha[i] = charPermitidos[rand - 1];
            i++;
        }
        String str = String.valueOf(senha);
        return str;
    }
}
