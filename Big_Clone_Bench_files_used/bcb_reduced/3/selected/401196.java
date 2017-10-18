package net.virtualhockey.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vh.error.VHException;

/**
 * Provides services for dealing with passwords.
 * 
 * @version $Id: PasswordService.java 83 2005-10-09 14:49:39Z jankejan $
 * @author jankejan
 */
public class PasswordService {

    public static final String CLASS_REVISION = "$Id: PasswordService.java 83 2005-10-09 14:49:39Z jankejan $";

    private static Log s_log = LogFactory.getLog(PasswordService.class);

    private static PasswordService s_instance;

    /** Private constructor (because of Singleton pattern). */
    private PasswordService() {
    }

    /**
   * Returns an instance of the PasswordService.
   */
    public static synchronized PasswordService getInstance() {
        if (s_instance == null) s_instance = new PasswordService();
        return s_instance;
    }

    /**
   * Computes an MD5 hash for the given plain text.
   * 
   * @param plainText the text for which an MD5 hash shall be computed
   * @return the computed hashcode
   * @throws VHException if the desired hash algorithm is not available
   */
    public byte[] computeMD5(String plainText) throws VHException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new VHException("The MD5 hash algorithm is not available.", ex);
        }
        try {
            md.update(plainText.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new VHException("The UTF-8 encoding is not supported.", ex);
        }
        return md.digest();
    }
}
