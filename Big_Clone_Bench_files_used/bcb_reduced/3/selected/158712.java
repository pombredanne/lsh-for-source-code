package us.tryclickon.utility;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UtilMisc {

    public static String getMd5(String message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(message.getBytes(), 0, message.length());
        return new BigInteger(1, digest.digest()).toString(16);
    }
}
