package de.iritgo.aktera.authentication;

import de.iritgo.aktera.authentication.defaultauth.entity.AkteraUser;
import de.iritgo.aktera.authentication.defaultauth.entity.UserDAO;
import de.iritgo.simplelife.string.StringTools;

/**
 *
 */
public class AuthenticatorImpl implements Authenticator {

    private UserDAO userDAO;

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
	 * @see de.iritgo.aktera.authentication.Authenticator#authenticate(java.lang.String,
	 *      java.lang.String)
	 */
    public boolean authenticate(String userName, String loginPassword) {
        AkteraUser user = userDAO.findUserByName(userName);
        if (user == null || !StringTools.digest(loginPassword).equals(user.getPassword())) {
            return false;
        }
        return true;
    }
}
