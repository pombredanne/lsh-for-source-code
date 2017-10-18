package info.jmonit.web.ui;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Abstract class for building a secured controller.
 *
 * @author <a href="mailto:ndeloof@sourceforge.net">ndeloof</a>
 */
public abstract class AbstractSecuredController extends SimpleController {

    /**
     * Creates a digest of password
     *
     * @param password user passwrod
     * @param algorithm the digest algorithm to use
     * @return digest string
     * @throws NoSuchAlgorithmException unsuported algorithm
     */
    protected String digest(String password, String algorithm) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance(algorithm).digest(password.getBytes());
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]).toUpperCase();
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        password = hashString.toString();
        return password;
    }
}
