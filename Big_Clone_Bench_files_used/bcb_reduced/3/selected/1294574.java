package net.vicms.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chquyet
 */
public class Util {

    private static Logger logger = Logger.getLogger(Util.class.getName());

    /**
     * Generate Id
     * @return String
     */
    public static String generateUUID() {
        UUID uudi = UUID.randomUUID();
        return String.valueOf(uudi);
    }

    /**
     * Generate md5 string
     * @param str
     * @return String
     */
    public static String generateMD5(String str) {
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
            logger.log(Level.SEVERE, null, nsae);
        }
        return hashword;
    }
}
