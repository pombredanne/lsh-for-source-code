package support.db;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

public class Crypto {

    /**
	 * Generate encoded password
	 * @param _originalPassword
	 * @return
	 */
    public static String encodePassword(String _originalPassword) {
        MessageDigest md = null;
        String encodedPassword = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.update(_originalPassword.getBytes("UTF-8"));
            encodedPassword = (new BASE64Encoder()).encode(md.digest());
        } catch (NoSuchAlgorithmException _e) {
            _e.printStackTrace();
        } catch (UnsupportedEncodingException _e) {
            _e.printStackTrace();
        }
        return encodedPassword;
    }
}
