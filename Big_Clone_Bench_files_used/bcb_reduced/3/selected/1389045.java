package pl.kwiecienm.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;
import org.jboss.seam.util.Base64;

/**
 * Responsible for encryption of strings.
 * 
 * @author kwiecienm
 * @since 2008-03-28
 */
public class Enc {

    /** default password, when encryption fails */
    private static final String DEFAULT_PASSWD = "JogurtNaturalny13";

    @Logger
    private static Log log;

    /**
	 * Encodes string using SHA algorithm and returns base 64 representation.
	 * 
	 * @param string
	 * @return
	 */
    public static final String enc(String string) {
        String ret = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] digest = md.digest(string.getBytes("UTF-8"));
            ret = new String(Base64.encodeBytes(digest));
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA not available, setting default password", e);
            ret = DEFAULT_PASSWD;
        } catch (UnsupportedEncodingException e) {
            log.error("UTF-8 not supported, setting default password", e);
            ret = DEFAULT_PASSWD;
        }
        return ret;
    }
}
