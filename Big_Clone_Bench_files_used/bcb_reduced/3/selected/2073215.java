package cornell.herbivore.util;

import java.security.MessageDigest;

public class HerbivoreMD5 {

    private static MessageDigest md = null;

    private static byte[] zero = { 0x00 };

    public static synchronized void init() {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("MD5");
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
        if (d == null) return md.digest(zero); else {
            md.update(d, offset, d.length - offset);
            return md.digest();
        }
    }
}
