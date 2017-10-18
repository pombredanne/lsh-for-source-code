package org.brainypdm.security.auth;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.brainypdm.constants.ErrorCodes;
import org.brainypdm.exceptions.BaseException;

/** 
 * 
 * the password
 * 
 * @author <a href="mailto:nico@brainypdm.org">Nico Bagari</a>
 * 
 */
public class BrainyPassword implements Serializable {

    private static final long serialVersionUID = 6941878790061158691L;

    /**
	 * the password
	 */
    private String password;

    /**
	 * void constructor
	 *
	 */
    public BrainyPassword(String pwd) throws BaseException, NoSuchAlgorithmException {
        password = crypt(pwd);
    }

    /**
	 * crypt the password
	 * @param s
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
    private String crypt(String s) throws BaseException, NoSuchAlgorithmException {
        if (s != null && s.length() > 0) {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(s.getBytes());
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();
        } else {
            throw new BaseException(ErrorCodes.CODE_2100);
        }
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((password == null) ? 0 : password.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final BrainyPassword other = (BrainyPassword) obj;
        if (password == null) {
            if (other.password != null) return false;
        } else if (!password.equals(other.password)) return false;
        return true;
    }
}
