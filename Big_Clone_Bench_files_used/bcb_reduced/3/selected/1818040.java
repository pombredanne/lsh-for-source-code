package Persistance;

import sun.misc.BASE64Encoder;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import org.apache.log4j.Logger;

public class PasswordService {

    private static Logger logger = Logger.getLogger("MovieSystem.Persistance.PasswordService");

    public static String encrypt(String plaintext) throws NoSuchAlgorithmException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            logger.error("unable to encrypt password" + e.getMessage());
            throw new NoSuchAlgorithmException(e.getMessage());
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("unable to encrypt password" + e.getMessage());
            throw new NoSuchAlgorithmException(e.getMessage());
        }
        byte raw[] = md.digest();
        return (new BASE64Encoder()).encode(raw);
    }

    public static boolean compare(String plaintext, String encrypedText) throws NoSuchAlgorithmException {
        return encrypedText.equals(encrypt(plaintext));
    }
}
