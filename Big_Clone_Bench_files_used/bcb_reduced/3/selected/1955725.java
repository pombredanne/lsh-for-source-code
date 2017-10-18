package glaceo.utils.data;

import glaceo.error.GException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides services for dealing with passwords.
 *
 * @version $Id: GPasswordService.java 83 2005-10-09 14:49:39Z jankejan $
 * @author jankejan
 */
public class GPasswordService {

    private static Log s_log = LogFactory.getLog(GPasswordService.class);

    private static GPasswordService s_instance;

    /** Private constructor (because of Singleton pattern). */
    private GPasswordService() {
    }

    /**
   * Returns an instance of the GPasswordService.
   */
    public static synchronized GPasswordService getInstance() {
        if (s_instance == null) s_instance = new GPasswordService();
        return s_instance;
    }

    /**
   * Computes an MD5 hash for the given plain text.
   *
   * @param plainText the text for which an MD5 hash shall be computed
   * @return the computed hashcode
   * @throws GException if the desired hash algorithm is not available
   */
    public byte[] computeMD5(String plainText) throws GException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new GException("The MD5 hash algorithm is not available.", ex);
        }
        try {
            md.update(plainText.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new GException("The UTF-8 encoding is not supported.", ex);
        }
        return md.digest();
    }
}
