package org.gridtrust.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CommonUtil {

    private static final Log log = LogFactory.getLog(CommonUtil.class);

    public static byte[] getMessageDigest(byte[] input) {
        byte[] digestValue = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA");
            messageDigest.update(input);
            digestValue = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            log.error("No message digest algorithm specified error " + e.getMessage());
        }
        return digestValue;
    }

    public static boolean compareDigestValues(byte[] digest1, byte[] digest2) {
        boolean isEqual = false;
        isEqual = MessageDigest.isEqual(digest1, digest2);
        return isEqual;
    }
}
