package au.edu.uq.itee.eresearch.dimer.core.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static final char[] hexTable = "0123456789abcdef".toCharArray();

    /**
     * Checks plain text by comparing to an expected digest.
     * The digest should be hex-encoded, eg "0beec7b5ea3f0f".
     */
    public static boolean check(String plain, String digest) {
        return digest(plain).equals(digest);
    }

    public static String digest(String plain) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException exception) {
        }
        byte[] digest = null;
        try {
            digest = md.digest(plain.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException exception) {
        }
        StringBuffer hex = new StringBuffer(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            hex.append(hexTable[(b >> 4) & 15]);
            hex.append(hexTable[b & 15]);
        }
        return hex.toString();
    }
}
