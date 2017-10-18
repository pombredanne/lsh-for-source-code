package net.naijatek.myalumni.util.encryption;

import java.security.MessageDigest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Encoder {

    private static Log logger = LogFactory.getLog(Encoder.class);

    private static MessageDigest digest = null;

    private static boolean isInited = false;

    private Encoder() {
    }

    public static synchronized String getMD5_Base64(final String input) {
        if (isInited == false) {
            isInited = true;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (Exception ex) {
                logger.error("Cannot get MessageDigest. Application may fail to run correctly.", ex);
            }
        }
        if (digest == null) {
            return input;
        }
        try {
            digest.update(input.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException ex) {
            logger.error("Assertion: This should never occur.");
        }
        byte[] rawData = digest.digest();
        byte[] encoded = Base64.encode(rawData);
        String retValue = new String(encoded);
        return retValue;
    }
}
