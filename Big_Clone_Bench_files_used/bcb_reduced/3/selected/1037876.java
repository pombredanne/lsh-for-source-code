package sjrd.util;

import java.io.*;
import java.security.*;

/**
 * Utilitaires de hash
 * @author sjrd
 */
public class HashUtils {

    /**
     * Calcule le hash MD5 d'une chaîne et le renvoie en chaîne hexa
     * @param string Chaîne à hasher
     * @return Hash MD5 de la chaîne <tt>string</tt>
     */
    public static String md5String(String string) {
        try {
            MessageDigest msgDigest = MessageDigest.getInstance("MD5");
            msgDigest.update(string.getBytes("UTF-8"));
            byte[] digest = msgDigest.digest();
            String result = "";
            for (int i = 0; i < digest.length; i++) {
                int value = digest[i];
                if (value < 0) value += 256;
                result += Integer.toHexString(value);
            }
            return result;
        } catch (UnsupportedEncodingException error) {
            throw new IllegalArgumentException(error);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalArgumentException(error);
        }
    }
}
