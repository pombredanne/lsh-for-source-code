import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    static final MessageDigest getDigest(String digest) {
        try {
            return MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    static final MessageDigest MD5 = getDigest("MD5");

    public static String arrayToString(byte[] array) {
        int length = array.length * 2;
        StringBuilder builder = new StringBuilder(length);
        for (byte b : array) {
            builder.append(Integer.toString(b & 0xff, 16));
        }
        return builder.toString();
    }

    public static String md5String(long number) {
        return md5String(BigInteger.valueOf(number));
    }

    public static String md5String(BigInteger number) {
        return arrayToString(MD5.digest(number.toByteArray()).clone());
    }
}
