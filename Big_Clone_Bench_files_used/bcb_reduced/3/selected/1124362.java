package de.searchworkorange.lib.misc.hash;

import de.searchworkorange.lib.logger.LoggerCollection;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import org.apache.log4j.Level;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author Sascha Kriegesmann kriegesmann at vaxnet.de
 */
public class MD4Hash {

    private static final boolean CLASSDEBUG = false;

    public String md4Result;

    /**
     *
     * @param loggerCol
     * @param input
     */
    MD4Hash(LoggerCollection loggerCol, String input) {
        if (loggerCol == null || input == null) {
            throw new IllegalArgumentException();
        } else {
            md4Result = plainToMD(loggerCol, input);
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
        StringBuilder md4result = new StringBuilder();
        try {
            md = MessageDigest.getInstance("MD4", new BouncyCastleProvider());
            md.reset();
            md.update(input.getBytes("UnicodeLittleUnmarked"));
            byteHash = md.digest();
            for (int i = 0; i < byteHash.length; i++) {
                md4result.append(Integer.toHexString(0xFF & byteHash[i]));
            }
        } catch (UnsupportedEncodingException ex) {
            loggerCol.logException(CLASSDEBUG, "de.searchworkorange.lib.misc.hash.MD4Hash", Level.FATAL, ex);
        } catch (NoSuchAlgorithmException ex) {
            loggerCol.logException(CLASSDEBUG, "de.searchworkorange.lib.misc.hash.MD4Hash", Level.FATAL, ex);
        }
        return (md4result.toString());
    }
}
