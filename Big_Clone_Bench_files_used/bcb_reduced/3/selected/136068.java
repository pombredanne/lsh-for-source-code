package kz.simplex.photobox.util;

import java.security.MessageDigest;
import org.jboss.seam.util.Hex;

public class HashUtils {

    private static String digestAlgorithm = "SHA-1";

    private static String charset = "UTF-8";

    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
            digest.update(password.getBytes(charset));
            byte[] rawHash = digest.digest();
            return new String(Hex.encodeHex(rawHash));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final String tempPassword() {
        java.util.Random rand = new java.util.Random();
        int[] aNums = new int[8];
        for (int n = 0; n < aNums.length; n++) aNums[n] = rand.nextInt(9) + 1;
        char[] ach1 = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        char[] ach2 = new char[] { 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U' };
        char[] ach3 = new char[] { 'v', 'w', 'x', 'y', 'z', 'V', 'W', 'X', 'Y', 'Z' };
        char[] ach4 = new char[] { 'k', '$', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u' };
        char[] ach5 = new char[] { '$', '%', '!', '#', '$', '%', '!', '#', '$', '%' };
        return (ach4[aNums[7]] + String.valueOf(aNums[2]) + ach1[aNums[3]] + String.valueOf(aNums[0]) + ach3[aNums[5]] + ach2[aNums[4]] + ach4[aNums[6]] + ach5[aNums[1]]);
    }
}
