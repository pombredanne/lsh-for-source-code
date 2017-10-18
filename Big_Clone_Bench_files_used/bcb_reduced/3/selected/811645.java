package util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author luctanbinh
 */
public class MD5Helper {

    public static String encryptPass(String pass) {
        String passEncrypt;
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
        }
        md5.update(pass.getBytes());
        BigInteger dis = new BigInteger(1, md5.digest());
        passEncrypt = dis.toString(16);
        return passEncrypt;
    }

    public static String encryptPass2(String pass) throws UnsupportedEncodingException {
        String passEncrypt;
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
        }
        md5.update(pass.getBytes());
        String dis = new String(md5.digest(), 10);
        passEncrypt = dis.toString();
        return passEncrypt;
    }
}
