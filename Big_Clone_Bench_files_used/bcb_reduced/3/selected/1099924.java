package gnu.javax.crypto.sasl;

import gnu.java.security.util.Util;
import java.security.MessageDigest;

/**
 * Utility methods for SASL-related classes.
 */
public class SaslUtil {

    private SaslUtil() {
        super();
    }

    public static final boolean validEmailAddress(String address) {
        return (address.indexOf("@") != -1);
    }

    /** Returns the context of the designated hash as a string. */
    public static final String dump(MessageDigest md) {
        String result;
        try {
            result = Util.dumpString(((MessageDigest) md.clone()).digest());
        } catch (Exception ignored) {
            result = "...";
        }
        return result;
    }
}
