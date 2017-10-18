package org.mitre.rt.client.xml;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.impl.util.Base64;
import org.mitre.rt.client.core.DataManager;
import org.mitre.rt.client.core.IDManager;
import org.mitre.rt.client.exceptions.DataManagerException;
import org.mitre.rt.client.exceptions.RTClientException;
import org.mitre.rt.client.util.GlobalUITools;
import org.mitre.rt.rtclient.ChangeTypeEnum;
import org.mitre.rt.rtclient.RTDocument.RT;
import org.mitre.rt.rtclient.UserType;

/**
 * This class has several helper methods used for accessing commonly accessed 
 * user information.
 * 
 * @author BAKERJ
 */
public class UserHelper extends AbsHelper<UserType, RT> {

    private static final Logger logger = Logger.getLogger(UserHelper.class.getPackage().getName());

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final String EMAIL_REGEX = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*$";

    private Map<String, String> getUserNameMap() {
        Map<String, String> userMap = new LinkedHashMap<String, String>();
        try {
            RT rt = DataManager.instance().getRTDocument().getRT();
            List<UserType> users = rt.getUsers().getUserList();
            for (UserType user : users) {
                String id = user.getId();
                String name = user.getLastName() + ", " + user.getLastName();
                userMap.put(id, name);
            }
        } catch (Exception ex) {
            logger.warn(ex);
        }
        return userMap;
    }

    /**
     * Returns a user with the given username
     * @param userName
     * @return
     */
    public UserType getItem(String userName) {
        UserType user = null;
        try {
            RT rt = DataManager.instance().getRTDocument().getRT();
            List<UserType> users = rt.getUsers().getUserList();
            for (UserType tmpUser : users) {
                if (tmpUser.getUserName().equals(userName)) {
                    user = tmpUser;
                    break;
                }
            }
        } catch (Exception ex) {
            logger.warn(ex);
        }
        return user;
    }

    /**
     * For a given user id return the formatted last name, first name string.
     * 
     * @param userId
     * @return formatted last name, firrt name string
     * @throws org.mitre.rt.exceptions.RTClientException thrown if user not found
     * @throws org.mitre.rt.exceptions.DataManagerException thrown if a problem occures with the DataManger
     */
    public String getLastNameFirstNameForUser(String userId) {
        UserType user = null;
        String retVal = "";
        try {
            RT rt = DataManager.instance().getRTDocument().getRT();
            List<UserType> users = rt.getUsers().getUserList();
            user = this.getItem(users, userId);
        } catch (Exception ex) {
            logger.warn(ex);
        }
        if (user != null) retVal = user.getLastName() + ", " + user.getFirstName();
        return retVal;
    }

    /**
     * For a given user return the formatted last name, first name string.
     * 
     * @param userId
     * @return formatted last name, firrt name string
     * @throws org.mitre.rt.exceptions.RTClientException thrown if user not found
     * @throws org.mitre.rt.exceptions.DataManagerException thrown if a problem occures with the DataManger
     */
    public String getLastNameFirstNameForUser(UserType user) {
        return user.getLastName() + ", " + user.getFirstName();
    }

    /**
     * For a given user id return the username
     * 
     * @param userId
     * @return user name
     * @throws org.mitre.rt.exceptions.RTClientException thrown if the user is not found
     * @throws org.mitre.rt.exceptions.DataManagerException thrown if a problem occures with the DataManger
     */
    public String getUsernameForUser(String userId) throws RTClientException, DataManagerException {
        String retVal = "";
        try {
            RT rt = DataManager.instance().getRTDocument().getRT();
            List<UserType> users = rt.getUsers().getUserList();
            UserType user = this.getItem(users, userId);
            retVal = user.getUserName();
        } catch (Exception ex) {
            logger.warn(ex);
        }
        return retVal;
    }

    /**
     * Checks to see if the clear-text password provided is the user's password.
     * Passwords are hashed and salted.
     * @param nameMatch The user to check
     * @param password The user's cleartext password
     * @return True if the password matches, false otherwise
     */
    public boolean validatePassword(UserType nameMatch, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.reset();
            md.update(nameMatch.getSalt().getBytes("UTF-8"));
            md.update(password.getBytes("UTF-8"));
            String encodedString = new String(Base64.encode(md.digest()));
            return encodedString.equals(nameMatch.getPassword());
        } catch (UnsupportedEncodingException ex) {
            logger.fatal("Your computer does not have UTF-8 support for Java installed.", ex);
            logger.fatal("Shutting down...");
            GlobalUITools.displayFatalExceptionMessage(null, "UTF-8 for Java not installed", ex, true);
            assert false : "This should never happen";
            return false;
        } catch (NoSuchAlgorithmException ex) {
            String errorMessage = "Could not use algorithm " + HASH_ALGORITHM;
            logger.fatal(ex.getMessage());
            logger.fatal(errorMessage);
            GlobalUITools.displayFatalExceptionMessage(null, "Could not use algorithm " + HASH_ALGORITHM, ex, true);
            assert false : "This could should never be reached";
            return false;
        }
    }

    /**
     * Sets a user's password to some new value.  The cleartext password 
     * provided is salted and hashed.
     * @param user A user to modify
     * @param clearPassword <code>user's</code> new password - in cleartext
     */
    public void setPassword(UserType user, String clearPassword) {
        try {
            Random r = new Random();
            String newSalt = Long.toString(Math.abs(r.nextLong()));
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.reset();
            md.update(newSalt.getBytes("UTF-8"));
            md.update(clearPassword.getBytes("UTF-8"));
            String encodedString = new String(Base64.encode(md.digest()));
            user.setPassword(encodedString);
            user.setSalt(newSalt);
            this.markModified(user);
        } catch (UnsupportedEncodingException ex) {
            logger.fatal("Your computer does not have UTF-8 support for Java installed.", ex);
            GlobalUITools.displayFatalExceptionMessage(null, "UTF-8 for Java not installed", ex, true);
        } catch (NoSuchAlgorithmException ex) {
            String errorMessage = "Could not use algorithm " + HASH_ALGORITHM;
            logger.fatal(errorMessage, ex);
            GlobalUITools.displayFatalExceptionMessage(null, errorMessage, ex, true);
        }
    }

    /**
     * Creates a new user; does not automatically save the user to the XML file.
     * Changes are only applied upon a save.
     * @param userName The desired username
     * @param firstName The user's first (given) name
     * @param lastName The user's last (family) name
     * @param clearPassword The user's desired password (in cleartext)
     * @param email The user's email address
     * @param isAdmin Whether or not the user should be an administrator
     * @return The new user object
     * @throws org.mitre.rt.exceptions.DataManagerException When the <code>DataManager</code> has not been initialized
     * @throws org.mitre.rt.exceptions.RTClientException If an error occurs accessing the XML
     */
    public UserType add(String userName, String firstName, String lastName, String clearPassword, String email, boolean isAdmin) throws DataManagerException, RTClientException {
        try {
            if (this.getItem(userName) != null) {
                throw new IllegalArgumentException("Cannot have two users " + "with the same username.");
            }
            RT rt = DataManager.instance().getRTDocument().getRT();
            UserType newUser = this.getNewItem(rt);
            newUser.setAdmin(isAdmin);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setUserName(userName);
            newUser.setEmail(email);
            newUser.setNotes("Created on " + new Date());
            setPassword(newUser, clearPassword);
            logger.debug("Created new user " + userName + " with ID " + newUser.getId());
            return newUser;
        } catch (DataManagerException ex) {
            logger.error("The DataManager must be initialized before " + "you try to use it." + ex.getMessage());
            throw ex;
        }
    }

    public void add(UserType user) throws Exception {
        RT rt = DataManager.instance().getRTDocument().getRT();
        UserType newUser = rt.getUsers().addNewUser();
        newUser.set(user);
    }

    @Override
    public boolean isDeleted(UserType user) {
        return user.getDeleted();
    }

    public boolean isUserNameUsed(String username) {
        return (this.getItem(username) != null);
    }

    /** isValidEmailAddress
     * Uses a basic regular expression match to insure that
     * the given email address "looks" like it should be valid.
     * TODO: find a better regex this one allows for any type string of letters and numbers to end the address. I think
     * it should check that it ends in .xx or .xxx
     * @param email
     * @return true if valid, else false
     */
    public boolean isEmailValid(String email) {
        if (email.isEmpty()) {
            return false;
        }
        return email.matches(EMAIL_REGEX);
    }

    @Override
    public void markDeleted(RT rt, UserType user) {
        user.setDeleted(true);
        this.markModified(user);
    }

    /**
     * An acitve user is a user that is not soft deleted or hard deleted.
     * 
     * @param user
     * @return
     */
    public boolean isActiveUser(UserType user) {
        boolean active = true;
        if (user == null) {
            return false;
        }
        if (user.getChangeType() == ChangeTypeEnum.DELETED) {
            active = false;
        }
        if (user.getDeleted()) {
            active = false;
        }
        return active;
    }

    public List<UserType> sortUserIdList(List<UserType> users) {
        List<String> userNames = new ArrayList<String>(users.size()), goldNames = new ArrayList<String>(users.size());
        List<UserType> goldList = new ArrayList<UserType>(users.size()), rtnList = new ArrayList<UserType>(users.size());
        goldList.addAll(users);
        if (users.size() > 0) {
            try {
                for (UserType user : users) {
                    String userName = (user.getLastName() + user.getFirstName()).toLowerCase();
                    userNames.add(userName);
                    goldNames.add(userName);
                }
                Collections.sort(userNames);
                for (String name : userNames) {
                    int oldIndex = goldNames.indexOf(name);
                    rtnList.add(goldList.get(oldIndex));
                }
            } catch (Exception ex) {
                logger.debug(ex);
            }
        }
        return rtnList;
    }

    @Override
    public String getNewId(RT rt) {
        return IDManager.getNextUserId(rt);
    }

    @Override
    protected UserType getInstance() {
        return UserType.Factory.newInstance();
    }
}
