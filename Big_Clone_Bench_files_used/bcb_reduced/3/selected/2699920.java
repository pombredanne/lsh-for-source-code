package com.codebitches.spruce.module.bb.domain.logic;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.security.auth.login.LoginException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Encoder;
import com.codebitches.spruce.module.bb.dao.IPostDAO;
import com.codebitches.spruce.module.bb.dao.IUserDAO;
import com.codebitches.spruce.module.bb.domain.hibernate.SprucebbUser;

/**
 * @author Stuart Eccles
 */
public class BBUserServiceImpl implements IBBUserService {

    private static Log log = LogFactory.getLog(BBUserServiceImpl.class);

    private IUserDAO userDAO;

    private IPostDAO postDAO;

    /**
	 * @param postDAO The postDAO to set.
	 */
    public void setPostDAO(IPostDAO postDAO) {
        this.postDAO = postDAO;
    }

    public SprucebbUser registerNewBBUser(SprucebbUser registerDetails) {
        registerDetails.setUserRegdate(new Date());
        registerDetails.setUserLastvisit(new Date());
        registerDetails.setUserPosts(0);
        registerDetails.setUserActive(true);
        registerDetails.setUserLevel(new Integer(0));
        registerDetails.setUserTimezone(new BigDecimal(0));
        registerDetails.setUserDateformat("ddMMyyy");
        registerDetails.setUserPassword(hashPassword(registerDetails.getUserPassword()));
        return createEditUser(registerDetails);
    }

    /**
	 * @param userDAO The userDAO to set.
	 */
    public void setUserDAO(IUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public SprucebbUser validateAuthenticationRequest(String username, String password) throws LoginException {
        SprucebbUser user = userDAO.findUserByUsername(username);
        if (user != null) {
            if (user.getUserPassword().equals(hashPassword(password))) {
                Date lastVisit = (Date) user.getUserLastvisit().clone();
                user.setUserLastvisit(new Date());
                userDAO.createOrUpdateUser(user);
                user.setPreviousLastvisit(lastVisit);
                return user;
            } else {
                throw new LoginException();
            }
        } else {
            throw new LoginException();
        }
    }

    public SprucebbUser viewProfile(long userId) {
        SprucebbUser user = userDAO.getUserById(userId);
        int totalPosts = postDAO.findNoTotalPosts();
        BigDecimal bd = new BigDecimal((user.getUserPosts() / totalPosts) * 100);
        bd.setScale(2, BigDecimal.ROUND_DOWN);
        user.setPercentagePosts(bd.doubleValue());
        return user;
    }

    private String hashPassword(String plainTextPassword) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(plainTextPassword.getBytes());
            BASE64Encoder enc = new BASE64Encoder();
            return enc.encode(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    public boolean isUsernameAvailableForRegistration(String username) {
        SprucebbUser user = userDAO.findUserByUsername(username);
        if (user == null) {
            return true;
        } else {
            return false;
        }
    }

    public SprucebbUser createEditUser(SprucebbUser user) {
        return userDAO.createOrUpdateUser(user);
    }

    public SprucebbUser validateAuthenticationRequest(long userId, String autoLoginString) throws LoginException {
        SprucebbUser user = userDAO.getUserById(userId);
        if (user != null) {
            if (user.getUserPassword().equals(autoLoginString)) {
                Date lastVisit = (Date) user.getUserLastvisit().clone();
                user.setUserLastvisit(new Date());
                userDAO.createOrUpdateUser(user);
                user.setPreviousLastvisit(lastVisit);
                return user;
            } else {
                throw new LoginException();
            }
        } else {
            throw new LoginException();
        }
    }
}
