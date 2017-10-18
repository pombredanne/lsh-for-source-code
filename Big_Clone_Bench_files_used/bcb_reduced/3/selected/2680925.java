package unibg.overencrypt.utility;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * Implements the MD5 algorithm.
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class AeSimpleMD5 {

    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(AeSimpleMD5.class);

    /**
	 * Encrypt the string passed as argument with MD5 algorithm.
	 *
	 * @param string the string to encrypt MD5 algorithm
	 * @return the string encrypted with MD5 algorithm
	 */
    public static String md5(String string) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exception) {
            LOGGER.warn(exception.getMessage());
        }
        byte[] md5hash = new byte[32];
        try {
            md.update(string.getBytes("iso-8859-1"), 0, string.length());
        } catch (UnsupportedEncodingException exception) {
            LOGGER.warn(exception.getMessage());
        }
        md5hash = md.digest();
        return convertToHex(md5hash);
    }

    /**
	 * Convert to hex.
	 */
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
