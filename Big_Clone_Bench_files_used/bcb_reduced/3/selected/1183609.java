package criticker.lib;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static String HashMD5(String data) {
        try {
            MessageDigest msgDig = MessageDigest.getInstance("MD5");
            byte[] messageDigest = msgDig.digest(data.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hash = number.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
            return hash;
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static int GetColourForRating(float rating) {
        if (rating < 0) return 0xFFD7D7D7;
        if (rating < 2) return 0xFFFF0000;
        if (rating < 5) return 0xFFFBFB13;
        if (rating < 7) return 0xFF6CDF00;
        if (rating < 10) return 0xFF228A00;
        return 0xFFD7D7D7;
    }

    public static String GetQuipForRating(float rating) {
        if (rating < 0) return "Unknown";
        if (rating < 2) return "Terrible";
        if (rating < 3) return "Bad";
        if (rating < 4) return "Not Good";
        if (rating < 5) return "Not That Hot";
        if (rating < 7) return "Alright";
        if (rating < 9) return "Great";
        if (rating <= 10) return "Awesome";
        return "Unknown";
    }
}
