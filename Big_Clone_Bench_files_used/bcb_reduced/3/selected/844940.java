package adc.app.spec.util;

import java.security.MessageDigest;

public final class AppUtil {

    private AppUtil() {
        throw new IllegalStateException("This class is not meant to be instantiated, ever");
    }

    public static byte[] hash(String plainTextValue) {
        MessageDigest msgDigest;
        try {
            msgDigest = MessageDigest.getInstance("MD5");
            msgDigest.update(plainTextValue.getBytes("UTF-8"));
            byte[] digest = msgDigest.digest();
            return digest;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T anyCast(Object o) {
        return (T) o;
    }
}
