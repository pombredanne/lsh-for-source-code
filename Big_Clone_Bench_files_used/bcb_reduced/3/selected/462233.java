package vavi.apps.ipm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Message Digester Class.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050802 nsano initial version <br>
 */
final class MessageDigester {

    /** */
    private static MessageDigest md;

    /** */
    private MessageDigester() {
    }

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    /** @throws IllegalStateException */
    public static String getMD5(String src) {
        byte[] buf = md.digest(src.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            sb.append(Integer.toString(new Byte(buf[i]).intValue(), Character.MAX_RADIX));
        }
        return sb.toString();
    }
}
