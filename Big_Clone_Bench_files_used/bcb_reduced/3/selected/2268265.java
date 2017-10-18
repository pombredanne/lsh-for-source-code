package sf2.security.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TestSHA1 {

    public static void main(String[] args) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update("Test".getBytes());
            byte[] digest = sha1.digest();
            for (int i = 0; i < digest.length; i++) {
                System.err.print(Integer.toHexString(0xFF & digest[i]));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
