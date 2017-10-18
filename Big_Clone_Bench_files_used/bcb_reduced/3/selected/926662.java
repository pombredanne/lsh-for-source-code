package com.dokumentarchiv.service.impl;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.dokumentarchiv.bean.UserBean;
import com.dokumentarchiv.service.IUserService;
import com.dokumentarchiv.service.ServiceException;
import com.dokumentarchiv.service.User;
import com.dokumentarchiv.util.FacesUtils;

/**
 * Simple user service that authenticates against a properties file
 * 
 * @author Carsten Burghardt
 * @version $Id: UserService.java 129 2005-09-05 20:55:06Z carsten $
 */
public class UserService implements IUserService {

    protected static final Log log = LogFactory.getLog(UserService.class);

    private static final String CONFIGFILE = "/com/dokumentarchiv/users.properties";

    private PropertiesConfiguration config = null;

    /**
     * Constructor
     */
    public UserService() {
        URL configUrl = getClass().getResource(CONFIGFILE);
        if (configUrl != null) {
            try {
                config = new PropertiesConfiguration(configUrl);
                config.setAutoSave(true);
            } catch (ConfigurationException e) {
                log.error("Error in config", e);
            }
        } else {
            log.error("configfile not found:" + CONFIGFILE);
        }
    }

    /**
     * Check user credentials
     * @see com.dokumentarchiv.service.IUserService#login(java.lang.String, java.lang.String)
     */
    public User login(String username, String password) throws ServiceException {
        if (config == null) {
            throw new ServiceException("login_invalid_configuration", "No configuration found");
        }
        String compare = config.getString(username);
        if (compare == null) {
            throw new ServiceException("login_unknown_user", "Unknown username");
        }
        if (!compare.equals(generateDigest(password))) {
            throw new ServiceException("login_invalid_password", "Invalid password");
        }
        User user = new User(username, password);
        UserBean bean = new UserBean();
        bean.setUser(user);
        FacesUtils.getSession().put(USERKEY, bean);
        log.info("User " + username + " logged in");
        return user;
    }

    /**
     * Save the encoded password of the given user
     * @param user
     * @throws ServiceException
     */
    public void savePassword(User user) throws ServiceException {
        if (user == null) {
            log.warn("No user given");
            return;
        }
        config.setProperty(user.getUsername(), generateDigest(user.getPassword()));
        log.info("Password changed for user " + user.getUsername());
    }

    /**
     * Generate a digest for the given string
     * @param password
     * @return digest string
     * @throws ServiceException
     */
    private String generateDigest(String password) throws ServiceException {
        byte[] digest = null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            digest = sha.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException(e.toString());
        }
        return hexEncode(digest);
    }

    /**
     * hex encode the string to make a nice string representation
     * @param aInput
     * @return hex encoded string
     */
    private static String hexEncode(byte[] aInput) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

    public void addUser(User user) throws ServiceException {
        if (!config.containsKey(user.getUsername())) {
            config.setProperty(user.getUsername(), generateDigest(user.getPassword()));
            log.info("User " + user.getUsername() + " added to config");
        } else {
            log.info("User " + user.getUsername() + " already present in config");
        }
    }

    public void logout() throws ServiceException {
        UserBean user = (UserBean) FacesUtils.getSession().get(USERKEY);
        if (user != null) {
            user.setUser(null);
        }
        FacesUtils.getSession().remove(user);
    }

    public List getUsers() throws ServiceException {
        List users = new ArrayList();
        Iterator it = config.getKeys();
        while (it.hasNext()) {
            String key = (String) it.next();
            User user = new User();
            user.setUsername(key);
            user.setPassword(config.getString(key));
            users.add(user);
        }
        return users;
    }

    public void deleteUser(String username) throws ServiceException {
        config.clearProperty(username);
    }

    public User getUser(String username) throws ServiceException {
        if (config.containsKey(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(config.getString(username));
            return user;
        }
        return null;
    }

    public User getCurrentUser() throws ServiceException {
        UserBean user = (UserBean) FacesUtils.getSession().get(USERKEY);
        if (user != null) {
            return user.getUser();
        }
        return null;
    }
}
