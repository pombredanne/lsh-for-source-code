package bookez.model.service.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import bookez.model.businessobject.User;
import bookez.model.dao.UserDao;
import bookez.model.exception.BusinessLogicLayerException;
import bookez.model.exception.DataAccessLayerException;
import bookez.model.exception.PasswordIncorrectException;
import bookez.model.exception.UserNotExistException;
import bookez.model.service.GenericService;
import bookez.model.service.UserService;

public class UserServiceImpl extends GenericService<UserDao> implements UserService {

    @Override
    public User login(String username, String password) throws BusinessLogicLayerException {
        try {
            getLogger().info("Begin getting User by using UserDao.");
            User user = getDao().findByUsername(username);
            if (user == null) {
                throw new UserNotExistException(username);
            } else {
                String encryptedPwd = encryptPassword(password);
                if (!user.getPassword().equals(encryptedPwd)) {
                    throw new PasswordIncorrectException();
                }
            }
            return user;
        } catch (DataAccessLayerException dalEx) {
            dalEx.printStackTrace();
            throw new BusinessLogicLayerException("Could not retrieve data from database.", dalEx);
        }
    }

    /**
	 * Encrypt password with MD5 algorithm.
	 * Compatible with md5() function of MySql
	 * @param password
	 * @return
	 */
    private String encryptPassword(String password) {
        String result = password;
        if (password != null) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.reset();
                md5.update(password.getBytes());
                BigInteger hash = new BigInteger(1, md5.digest());
                result = hash.toString(16);
                if ((result.length() % 2) != 0) {
                    result = "0" + result;
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                getLogger().error("Cannot generate MD5", e);
            }
        }
        return result;
    }
}
