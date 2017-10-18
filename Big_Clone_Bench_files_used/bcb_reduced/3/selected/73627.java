package com.maicuole.user.login;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import com.maicuole.user.UserBean;
import com.maicuole.user.model.User;
import com.maicuole.user.persistence.UserDAO;

/**
 * @author Leopard Liu
 *
 */
public class UserLoginServiceImpl implements UserLoginService {

    private UserDAO userDao;

    @Override
    public UserBean doLogin(UserLoginDTO dto) {
        User user = userDao.queryUserByEmail(dto.getLoginId());
        UserBean userBean = null;
        if (user != null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                Base64 b64 = new Base64();
                String password = new String(b64.encode(digest.digest(user.getPassword().getBytes())));
                if (password.equals(dto.getPassword())) {
                    userBean = new UserBean(user.getId(), user.getName());
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return userBean;
    }

    /**
	 * @param userDao the userDao to set
	 */
    public void setUserDao(UserDAO userDao) {
        this.userDao = userDao;
    }
}
