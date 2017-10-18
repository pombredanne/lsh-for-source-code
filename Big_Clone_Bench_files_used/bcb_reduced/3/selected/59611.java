package com.projectmanagement.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.projectmanagement.PMApplication;
import com.projectmanagement.model.dao.UserDAO;
import com.projectmanagement.model.dao.hibernate.UserDAOImpl;
import com.projectmanagement.model.data.User;

public class Auth {

    /**
	 * Hashes the password found in the use object. If user is
	 * 
	 * @param user
	 * @return
	 */
    public static User login(String username, String password) {
        UserDAO userDAO = new UserDAOImpl();
        User loggedUser = userDAO.login(username, password);
        if (loggedUser == null) {
            return null;
        }
        return loggedUser;
    }

    /**
	 * It creates a hash of the string parameter using the user data<br/>
	 * This way all hashed data will be user specific.
	 * 
	 * @param user
	 * @param string
	 * @return
	 */
    public static String hash(User user, String string) {
        return hash(user.getEmail() + user.getPassword() + string);
    }

    /**
	 * Creates MD5 hash of a string.
	 * 
	 * @param string
	 * @return
	 */
    public static String hash(String string) {
        StringBuffer sb = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            byte[] byteData = md.digest(string.getBytes());
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void logout(PMApplication app) {
        app.setUser(new User());
    }
}
