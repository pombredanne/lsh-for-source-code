package org.piuframework.samples.usermgmt.flavors.pojo;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.piuframework.context.ApplicationContext;
import org.piuframework.context.PersistenceContext;
import org.piuframework.persistence.dao.DAOFactory;
import org.piuframework.samples.usermgmt.Role;
import org.piuframework.samples.usermgmt.User;
import org.piuframework.samples.usermgmt.UserManagementService;
import org.piuframework.samples.usermgmt.UserManagementServiceException;
import org.piuframework.samples.usermgmt.persistence.UserDAO;
import org.piuframework.samples.usermgmt.persistence.codegen.RoleDAO;
import org.piuframework.service.ApplicationContextAware;
import org.piuframework.service.ServiceException;

/**
 * POJO implementation of UserService component
 * 
 * @author Dirk Mascher
 */
public class UserManagementServiceImpl implements UserManagementService, ApplicationContextAware, Serializable {

    private static final long serialVersionUID = -1153128159931283958L;

    public static final int MIN_PASSWORD_LENGTH = 5;

    private static final Log log = LogFactory.getLog(UserManagementServiceImpl.class);

    private UserDAO userDAO;

    private RoleDAO roleDAO;

    public UserManagementServiceImpl() {
    }

    public void setApplicationContext(ApplicationContext context) {
        PersistenceContext persistenceContext = (PersistenceContext) context.getSubContext(PersistenceContext.NAME);
        DAOFactory daoFactory = persistenceContext.getDAOFactory();
        userDAO = (UserDAO) daoFactory.createDAO(UserDAO.class);
        roleDAO = (RoleDAO) daoFactory.createDAO(RoleDAO.class);
    }

    public Role addRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        roleDAO.insert(role);
        return role;
    }

    public User addUser(String login, String password, String roleName) throws UserManagementServiceException {
        checkPassword(password);
        User user = new User();
        user.setLogin(login);
        user.setPassword(getPasswordHash(password));
        Role role = roleDAO.findByName(roleName);
        userDAO.addUserToRole(user, role);
        userDAO.insert(user);
        return user;
    }

    public void addUserToRole(String login, String roleName) {
        User user = userDAO.findByLogin(login);
        Role role = roleDAO.findByName(roleName);
        userDAO.addUserToRole(user, role);
    }

    public User getUser(String login) {
        User user = userDAO.findByLogin(login);
        userDAO.initialize(user.getRoles());
        return user;
    }

    public Role getRole(String roleName) {
        Role role = roleDAO.findByName(roleName);
        roleDAO.initialize(role.getUsers());
        return role;
    }

    public Collection listAllUsers() {
        return userDAO.findAll();
    }

    public Collection listAllRoles() {
        return roleDAO.findAll();
    }

    public void lockUser(String login) {
        User user = userDAO.findByLogin(login);
        user.setLocked(true);
    }

    public void unlockUser(String login) {
        User user = userDAO.findByLogin(login);
        user.setLocked(false);
    }

    public void setPassword(String login, String password) throws UserManagementServiceException {
        checkPassword(password);
        User user = userDAO.findByLogin(login);
        user.setPassword(getPasswordHash(password));
    }

    public boolean isPasswordValid(String login, String password) throws UserManagementServiceException {
        User user = userDAO.findByLogin(login);
        if (user.isLocked()) {
            throw new UserManagementServiceException(UserManagementServiceException.ERRCODE_USER_LOCKED);
        }
        String hash = getPasswordHash(password);
        return user.getPassword().equals(hash);
    }

    public boolean isUserInRole(String login, String roleName) {
        User user = userDAO.findByLogin(login);
        Role role = roleDAO.findByName(roleName);
        return userDAO.isInRole(user, role);
    }

    public void removeUser(String login) {
        User user = userDAO.findByLogin(login);
        userDAO.delete(user);
    }

    public void removeRole(String roleName) {
        Role role = roleDAO.findByName(roleName);
        roleDAO.delete(role);
    }

    public void removeUserFromRole(String login, String roleName) {
        User user = userDAO.findByLogin(login);
        Role role = roleDAO.findByName(roleName);
        userDAO.removeUserFromRole(user, role);
    }

    protected void checkPassword(String password) throws UserManagementServiceException {
        if (password == null) {
            throw new UserManagementServiceException(UserManagementServiceException.ERRCODE_PASSWORD_NOT_SET);
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new UserManagementServiceException(UserManagementServiceException.ERRCODE_PASSWORD_LENGTH_TOO_SHORT);
        }
    }

    protected String getPasswordHash(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found", e);
            throw new ServiceException(e);
        }
        md.update(password.getBytes());
        byte[] hash = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            buf.append(Integer.toHexString(hash[i] & 0xff));
        }
        return buf.toString();
    }
}
