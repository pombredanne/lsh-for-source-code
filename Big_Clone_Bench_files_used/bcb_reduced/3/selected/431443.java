package blasar.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Jesus Navalon i Pastor <jnavalon at redhermes dot net>
 */
public final class Encryptation {

    private static String getHash(char[] passwd, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest alg = MessageDigest.getInstance(algorithm);
        alg.reset();
        alg.update(new String(passwd).getBytes());
        byte[] digest = alg.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xff & digest[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String getMD5(char[] passwd) throws NoSuchAlgorithmException {
        return getHash(passwd, "MD5");
    }

    public static String getSHA512(char[] passwd) throws NoSuchAlgorithmException {
        return getHash(passwd, "SHA-512");
    }
}
