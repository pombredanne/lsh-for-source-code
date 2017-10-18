package cn.vlabs.simpleAuth;

import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

public class Digester {

    public static final String ENCODING = "UTF-8";

    public static final String DIGEST_ALGORITHM = "SHA";

    public static String digest(String text) {
        return digest(DIGEST_ALGORITHM, text);
    }

    public static String digest(String algorithm, String text) {
        MessageDigest mDigest = null;
        try {
            mDigest = MessageDigest.getInstance(algorithm);
            mDigest.update(text.getBytes(ENCODING));
        } catch (Exception e) {
            e.printStackTrace();
            mDigest = null;
        }
        if (mDigest == null) return null;
        byte[] raw = mDigest.digest();
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(raw);
    }
}
