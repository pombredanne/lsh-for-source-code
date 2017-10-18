package cb_commonobjects.util.hash;

import cb_commonobjects.logging.GlobalLog;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author B1
 */
public class HashProvider {

    private static MessageDigest md;

    public static byte[] getHash(byte[] thisBinaryData) {
        try {
            if (md == null) md = MessageDigest.getInstance("SHA");
            md.update(thisBinaryData);
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            GlobalLog.logError(ex);
        }
        return null;
    }

    public static byte[] getHash_SHA(byte[] thisBinaryData) {
        return getHash(thisBinaryData);
    }
}
