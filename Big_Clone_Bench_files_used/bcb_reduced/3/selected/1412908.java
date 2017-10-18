package info.joseluismartin.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

/**
 * An AuthStrategy that use HashMD5
 * @author Jose Luis Martin - (jlm@joseluismartin.info)
 *
 */
public class AuthHashMD5 implements AuthStrategy {

    /** log */
    private static Log log = LogFactory.getLog(AuthHashMD5.class);

    /**
	 * Test if userPassword is a md5 hash of suppliedPassword
	 * 
	 * @param suppliedPassword password form user
	 * @param userPassword password from db
	 * @return true if passwords match
	 */
    public boolean validate(String suppliedPassword, String userPassword) {
        if (suppliedPassword == null || userPassword == null) {
            return false;
        }
        try {
            String encriptedPassword = crypt(suppliedPassword);
            return userPassword.equals(encriptedPassword);
        } catch (NoSuchAlgorithmException nsae) {
            log.error(nsae);
            return false;
        }
    }

    /**
	 * Encript password 
	 * @param suppliedPassword
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
    public String crypt(String suppliedPassword) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(suppliedPassword.getBytes());
        String encriptedPassword = null;
        try {
            encriptedPassword = new String(Base64.encode(md.digest()), "ASCII");
        } catch (UnsupportedEncodingException e) {
        }
        return encriptedPassword;
    }
}
