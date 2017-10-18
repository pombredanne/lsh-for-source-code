package demo;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {

    private static MessageDigest digest;

    private static Digest instance;

    private Digest() {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static Digest getInstance() {
        if (instance == null) {
            instance = new Digest();
        }
        return instance;
    }

    public static final char[] hexDigits = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public String getDigest(String arg) {
        try {
            digest.reset();
            digest.update(arg.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest();
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            int currentByte = 0x000000FF & hash[i];
            result.append(hexDigits[currentByte / 16]);
            result.append(hexDigits[currentByte % 16]);
        }
        return result.toString();
    }
}
