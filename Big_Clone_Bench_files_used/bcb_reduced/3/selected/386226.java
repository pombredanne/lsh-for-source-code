package jhomenet.commons.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 * This class provides authentication utility methods.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class AuthUtil {

    private AuthUtil() {
    }

    /**
	 * Encrypt the plaintext password using the SHA hash method.
	 * 
	 * @param plaintextPassword The plain text password
	 * @return The password encyrpted using the SHA hashing method
	 */
    public static synchronized String encrypt(String plaintextPassword) throws Exception {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new Exception(e);
        }
        try {
            md.update(plaintextPassword.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new Exception(e);
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }
}
