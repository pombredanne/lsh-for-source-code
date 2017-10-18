package de.searchworkorange.lib.misc.hash;

import de.searchworkorange.lib.logger.LoggerCollection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Level;

/**
 * 
 * @author Sascha Kriegesmann kriegesmann at vaxnet.de
 */
public class MD5Hash {

    private static final boolean CLASSDEBUG = false;

    public String md5Result;

    /**
     * 
     * @param loggerCol
     * @param input
     */
    MD5Hash(LoggerCollection loggerCol, String input) {
        if (loggerCol == null || input == null) {
            throw new IllegalArgumentException();
        } else {
            md5Result = plainToMD(loggerCol, input);
        }
    }

    /**
     * 
     * @param loggerCol
     * @param input
     * @return String
     */
    public static String plainToMD(LoggerCollection loggerCol, String input) {
        byte[] byteHash = null;
        MessageDigest md = null;
        StringBuilder md5result = new StringBuilder();
        try {
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(input.getBytes());
            byteHash = md.digest();
            for (int i = 0; i < byteHash.length; i++) {
                md5result.append(Integer.toHexString(0xFF & byteHash[i]));
            }
        } catch (NoSuchAlgorithmException ex) {
            loggerCol.logException(CLASSDEBUG, "de.searchworkorange.lib.misc.hash.MD5Hash", Level.FATAL, ex);
        }
        return (md5result.toString());
    }
}
