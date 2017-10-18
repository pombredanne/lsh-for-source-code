package ms.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class UID {

    byte[] id;

    static SecureRandom prng = null;

    private UID(byte[] value) {
        id = value;
    }

    public static void initialize() throws NoSuchAlgorithmException {
        prng = SecureRandom.getInstance("SHA1PRNG");
    }

    public static UID generate() {
        try {
            if (prng == null) initialize();
            String randomNum = new Integer(prng.nextInt()).toString();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            return new UID(sha.digest(randomNum.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        int half = id.length / 2;
        for (int idx = 0; idx < half; ++idx) appendEncode(result, (id[idx] ^ id[idx + half]));
        return result.toString();
    }

    private final String digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxy_";

    private void appendEncode(StringBuilder b, int v) {
        if (v < 0) v = Math.abs(v);
        int radix = digits.length();
        do {
            b.append(digits.charAt(v % radix));
            v /= radix;
        } while (v > 0);
    }

    /**
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
    public static void main(String[] args) throws NoSuchAlgorithmException {
        UID.initialize();
        UID id = UID.generate();
        System.out.println(id.toString());
    }
}
