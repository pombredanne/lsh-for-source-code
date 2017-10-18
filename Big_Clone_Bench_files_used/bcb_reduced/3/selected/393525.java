package g1105.ps.utils;

import java.security.*;

public class Encrypter {

    public static String md5(String s) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            sun.misc.BASE64Encoder base64Encoder = new sun.misc.BASE64Encoder();
            return base64Encoder.encode(messageDigest.digest(s.getBytes("utf8")));
        } catch (Exception e) {
            return s;
        }
    }
}
