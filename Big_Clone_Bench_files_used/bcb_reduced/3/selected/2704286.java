package es.ehrflex.core.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import es.ehrflex.core.db.DBUser;
import es.ehrflex.core.db.DBUserImpl;
import es.ehrflex.core.jdo.EHRflexException;
import es.ehrflex.core.jdo.EHRflexExceptionType;
import es.ehrflex.core.jdo.User;

/**
 * Implements {@link UserService}.
 * 
 */
public class UserServiceImpl implements UserService {

    private static Logger logger = LoggerFactory.getLogger(UserService.class);

    private DBUser mDBUser;

    private XMLService xmlService;

    private static volatile UserService mSingleton;

    /**
	 * connects needed services and db-objects
	 */
    private UserServiceImpl() {
        try {
            mDBUser = DBUserImpl.getInstance();
            xmlService = XMLServiceImpl.getInstance();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    ;

    /**
	 * Singleton pattern
	 * 
	 * @return instance of the only object
	 */
    public static UserService getInstance() {
        if (mSingleton == null) {
            synchronized (UserServiceImpl.class) {
                if (mSingleton == null) {
                    mSingleton = new UserServiceImpl();
                }
            }
        }
        return mSingleton;
    }

    /**
	 * Encrypts a given string into a MD5 hash
	 * 
	 * @param plainText
	 *            string to encrypt
	 * 
	 * @return md5 hash
	 * 
	 * @throws NoSuchAlgorithmException
	 *             possible exception
	 */
    private String getMD5Password(String plainText) throws NoSuchAlgorithmException {
        MessageDigest mdAlgorithm;
        StringBuffer hexString = new StringBuffer();
        String md5Password = "";
        mdAlgorithm = MessageDigest.getInstance("MD5");
        mdAlgorithm.update(plainText.getBytes());
        byte[] digest = mdAlgorithm.digest();
        for (int i = 0; i < digest.length; i++) {
            plainText = Integer.toHexString(0xFF & digest[i]);
            if (plainText.length() < 2) {
                plainText = "0" + plainText;
            }
            hexString.append(plainText);
        }
        md5Password = hexString.toString();
        return md5Password;
    }

    /**
	 * @see UserService#loginUser(String, String)
	 */
    public User loginUser(String username, String password) throws EHRflexException {
        try {
            return xmlService.getUserFromXML((String) mDBUser.login(username, this.getMD5Password(password)).getContent());
        } catch (Exception ex) {
            logger.error("", ex);
            throw EHRflexExceptionFactory.createEHRflexException(ex);
        }
    }

    /**
	 * @see UserService#createUser(User)
	 */
    public User createUser(User user) throws EHRflexException {
        User result = null;
        try {
            if (mDBUser.getUser(user.getLogin()) == null) {
                user.setPassword(this.getMD5Password(user.getPassword()));
                mDBUser.saveUser(xmlService.transformUserToXML(user), user.getLogin());
            } else {
                throw new EHRflexException(EHRflexExceptionType.LOGIN_ALREADY_EXISTS);
            }
        } catch (Exception e) {
            logger.error(e + "");
            throw EHRflexExceptionFactory.createEHRflexException(e);
        }
        return result;
    }

    /**
	 * @see UserService#updateUser(User)
	 */
    public User updateUser(User user) throws EHRflexException {
        User result = null;
        try {
            user.setPassword(this.getMD5Password(user.getPassword()));
            mDBUser.saveUser(xmlService.transformUserToXML(user), user.getLogin());
        } catch (Exception e) {
            logger.error(e + "");
            throw EHRflexExceptionFactory.createEHRflexException(e);
        }
        return result;
    }
}
