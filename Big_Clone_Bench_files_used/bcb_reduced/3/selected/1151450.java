package net.sf.balm.security.acegi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.acegisecurity.providers.encoding.BaseDigestPasswordEncoder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;

/**
 * @author dz
 */
public abstract class AbstractPasswordEncoder extends BaseDigestPasswordEncoder {

    protected Log logger = LogFactory.getLog(getClass());

    /**
     * 
     */
    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        if (logger.isDebugEnabled()) {
            logger.debug("password before encode: " + rawPass);
        }
        if (rawPass.length() == 32) {
            return guessPassword(rawPass);
        }
        try {
            String saltedPass = mergePasswordAndSalt(rawPass, salt, false);
            MessageDigest messageDigest = getMessageDigest();
            byte[] digest = messageDigest.digest(saltedPass.getBytes());
            if (getEncodeHashAsBase64()) {
                return new String(Base64.encodeBase64(digest));
            } else {
                return new String(Hex.encodeHex(digest));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     */
    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws DataAccessException {
        String pass1 = "" + encPass;
        String pass2 = encodePassword(rawPass, salt);
        return pass1.equals(pass2);
    }

    /**
     * 猜密码
     * 
     * @param rawPass
     * @return
     */
    protected String guessPassword(String rawPass) {
        logger.debug("The length of raw password is 32 , we guess it has been encrypted by MD5 ");
        return rawPass;
    }

    /**
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    protected abstract MessageDigest getMessageDigest() throws NoSuchAlgorithmException;
}
