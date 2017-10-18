package cornell.herbivore.util;

import java.security.MessageDigest;

public class HerbivoreSHA1 {

    private static MessageDigest md = null;

    public static synchronized void init() {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("SHA1");
            } catch (Exception e) {
                Log.exception(e);
            }
        }
    }

    public static byte[] digest() {
        if (md == null) init();
        return digest(null);
    }

    public static byte[] digest(byte[] d) {
        if (md == null) init();
        return digest(d, 0);
    }

    public static byte[] digest(byte[] d, int offset) {
        if (md == null) init();
        md.update(d, offset, d.length - offset);
        return md.digest();
    }
}
