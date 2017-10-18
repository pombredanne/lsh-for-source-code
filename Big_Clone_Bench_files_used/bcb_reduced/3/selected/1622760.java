package org.apache.jetspeed.services.security.turbine;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.mail.internet.MimeUtility;
import javax.servlet.ServletConfig;
import org.apache.jetspeed.om.profile.Profile;
import org.apache.jetspeed.om.security.BaseJetspeedUser;
import org.apache.jetspeed.om.security.JetspeedUser;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.om.security.UserNamePrincipal;
import org.apache.jetspeed.om.security.turbine.TurbineUser;
import org.apache.jetspeed.om.security.turbine.TurbineUserPeer;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.Profiler;
import org.apache.jetspeed.services.PsmlManager;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.services.rundata.JetspeedRunDataService;
import org.apache.jetspeed.services.security.CredentialsManagement;
import org.apache.jetspeed.services.security.InsufficientPrivilegeException;
import org.apache.jetspeed.services.security.JetspeedSecurityException;
import org.apache.jetspeed.services.security.JetspeedSecurityService;
import org.apache.jetspeed.services.security.NotUniqueUserException;
import org.apache.jetspeed.services.security.UnknownUserException;
import org.apache.jetspeed.services.security.UserException;
import org.apache.jetspeed.services.security.UserManagement;
import org.apache.torque.om.NumberKey;
import org.apache.torque.util.Criteria;
import org.apache.turbine.services.InitializationException;
import org.apache.turbine.services.TurbineBaseService;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.localization.Localization;
import org.apache.turbine.services.resources.ResourceService;
import org.apache.turbine.services.rundata.RunDataService;
import org.apache.turbine.util.RunData;

/**
 * Default Jetspeed-Turbine User Management implementation
 *
 *
 * @author <a href="mailto:david@bluesunrise.com">David Sean Taylor</a>
 * @author <a href="mailto:morciuch@apache.org">Mark Orciuch</a>
 * @version $Id: TurbineUserManagement.java,v 1.13 2004/02/23 03:54:49 jford Exp
 *          $
 */
public class TurbineUserManagement extends TurbineBaseService implements UserManagement, CredentialsManagement {

    /**
   * Static initialization of the logger for this class
   */
    private static final JetspeedLogger logger = JetspeedLogFactoryService.getLogger(TurbineUserManagement.class.getName());

    private static final String CONFIG_SECURE_PASSWORDS_KEY = "secure.passwords";

    private static final String CONFIG_SECURE_PASSWORDS_ALGORITHM = "secure.passwords.algorithm";

    private static final String CONFIG_SYSTEM_USERS = "system.users";

    boolean securePasswords = false;

    String passwordsAlgorithm = "SHA";

    Vector systemUsers = null;

    private static final String CONFIG_NEWUSER_ROLES = "newuser.roles";

    private static final String[] DEFAULT_CONFIG_NEWUSER_ROLES = { "user" };

    String roles[] = null;

    /** The JetspeedRunData Service. */
    private JetspeedRunDataService runDataService = null;

    /**
   * Retrieves a <code>JetspeedUser</code> given the primary principle. The
   * principal can be any valid Jetspeed Security Principal:
   * <code>org.apache.jetspeed.om.security.UserNamePrincipal</code>
   * <code>org.apache.jetspeed.om.security.UserIdPrincipal</code>
   *
   * The security service may optionally check the current user context to
   * determine if the requestor has permission to perform this action.
   *
   * @param principal
   *          a principal identity to be retrieved.
   * @return a <code>JetspeedUser</code> associated to the principal identity.
   * @exception UserException
   *              when the security provider has a general failure retrieving a
   *              user.
   * @exception UnknownUserException
   *              when the security provider cannot match the principal identity
   *              to a user.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public JetspeedUser getUser(Principal principal) throws JetspeedSecurityException {
        Criteria criteria = new Criteria();
        if (principal instanceof UserNamePrincipal) {
            criteria.add(TurbineUserPeer.LOGIN_NAME, principal.getName());
        } else if (principal instanceof UserIdPrincipal) {
            criteria.add(TurbineUserPeer.USER_ID, principal.getName());
        } else {
            throw new UserException("Invalid Principal Type in getUser: " + principal.getClass().getName());
        }
        List users;
        try {
            users = TurbineUserPeer.doSelectUsers(criteria);
        } catch (Exception e) {
            String message = "Failed to retrieve user '" + principal.getName() + "'";
            logger.error(message, e);
            throw new UserException(message, e);
        }
        if (users.size() > 1) {
            throw new UserException("Multiple Users with same username '" + principal.getName() + "'");
        }
        if (users.size() == 1) {
            return (JetspeedUser) users.get(0);
        }
        throw new UnknownUserException("Unknown user '" + principal.getName() + "'");
    }

    public JetspeedUser getUser(RunData rundata, Principal principal) throws JetspeedSecurityException {
        return getUser(principal);
    }

    /**
   * Retrieves a collection of all <code>JetspeedUser</code>s. The security
   * service may optionally check the current user context to determine if the
   * requestor has permission to perform this action.
   *
   * @return a collection of <code>JetspeedUser</code> entities.
   * @exception UserException
   *              when the security provider has a general failure retrieving
   *              users.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public Iterator getUsers() throws JetspeedSecurityException {
        Criteria criteria = new Criteria();
        List users;
        try {
            users = TurbineUserPeer.doSelectUsers(criteria);
        } catch (Exception e) {
            logger.error("Failed to retrieve users ", e);
            throw new UserException("Failed to retrieve users ", e);
        }
        return users.iterator();
    }

    /**
   * Retrieves a collection of <code>JetspeedUser</code>s filtered by a security
   * provider-specific query string. For example SQL, OQL, JDOQL. The security
   * service may optionally check the current user context to determine if the
   * requestor has permission to perform this action.
   *
   * @return a collection of <code>JetspeedUser</code> entities.
   * @exception UserException
   *              when the security provider has a general failure retrieving
   *              users.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public Iterator getUsers(String filter) throws JetspeedSecurityException {
        Criteria criteria = new Criteria();
        List users;
        try {
            users = TurbineUserPeer.doSelectUsers(criteria);
        } catch (Exception e) {
            logger.error("Failed to retrieve users ", e);
            throw new UserException("Failed to retrieve users ", e);
        }
        return users.iterator();
    }

    /**
   * Saves a <code>JetspeedUser</code>'s attributes into permanent storage. The
   * user's account is required to exist in the storage. The security service
   * may optionally check the current user context to determine if the requestor
   * has permission to perform this action.
   *
   * @exception UserException
   *              when the security provider has a general failure retrieving
   *              users.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public void saveUser(JetspeedUser user) throws JetspeedSecurityException {
        if (!accountExists(user, true)) {
            throw new UnknownUserException("Cannot save user '" + user.getUserName() + "', User doesn't exist");
        }
        Criteria criteria = TurbineUserPeer.buildCriteria(user);
        try {
            TurbineUserPeer.doUpdate(criteria);
        } catch (Exception e) {
            logger.error("Failed to save user object ", e);
            throw new UserException("Failed to save user object ", e);
        }
    }

    /**
   * Adds a <code>JetspeedUser</code> into permanent storage. The security
   * service can throw a <code>NotUniqueUserException</code> when the public
   * credentials fail to meet the security provider-specific unique constraints.
   * The security service may optionally check the current user context to
   * determine if the requestor has permission to perform this action.
   *
   * @exception UserException
   *              when the security provider has a general failure retrieving
   *              users.
   * @exception NotUniqueUserException
   *              when the public credentials fail to meet the security
   *              provider-specific unique constraints.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public void addUser(JetspeedUser user) throws JetspeedSecurityException {
        if (accountExists(user)) {
            throw new NotUniqueUserException("The account '" + user.getUserName() + "' already exists");
        }
        String initialPassword = user.getPassword();
        String encrypted = JetspeedSecurity.encryptPassword(initialPassword);
        user.setPassword(encrypted);
        Criteria criteria = TurbineUserPeer.buildCriteria(user);
        try {
            NumberKey key = (NumberKey) TurbineUserPeer.doInsert(criteria);
            ((BaseJetspeedUser) user).setUserId(key.toString());
        } catch (Exception e) {
            String message = "Failed to create account '" + user.getUserName() + "'";
            logger.error(message, e);
            throw new UserException(message, e);
        }
        addDefaultPSML(user);
    }

    protected void addDefaultPSML(JetspeedUser user) throws JetspeedSecurityException {
        for (int ix = 0; ix < roles.length; ix++) {
            try {
                JetspeedSecurity.grantRole(user.getUserName(), JetspeedSecurity.getRole(roles[ix]).getName());
            } catch (Exception e) {
                logger.error("Could not grant role: " + roles[ix] + " to user " + user.getUserName(), e);
            }
        }
        try {
            JetspeedRunData rundata = getRunData();
            if (rundata != null && Profiler.useRoleProfileMerging() == false) {
                Profile profile = Profiler.createProfile();
                profile.setUser(user);
                profile.setMediaType("html");
                Profiler.createProfile(getRunData(), profile);
            }
        } catch (Exception e) {
            logger.error("Failed to create profile for new user ", e);
            removeUser(new UserNamePrincipal(user.getUserName()));
            throw new UserException("Failed to create profile for new user ", e);
        }
    }

    /**
   * Removes a <code>JetspeedUser</code> from the permanent store. The security
   * service may optionally check the current user context to determine if the
   * requestor has permission to perform this action.
   *
   * @param principal
   *          the principal identity to be retrieved.
   * @exception UserException
   *              when the security provider has a general failure retrieving a
   *              user.
   * @exception UnknownUserException
   *              when the security provider cannot match the principal identity
   *              to a user.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public void removeUser(Principal principal) throws JetspeedSecurityException {
        if (systemUsers.contains(principal.getName())) {
            throw new UserException("[" + principal.getName() + "] is a system user and cannot be removed");
        }
        JetspeedUser user = getUser(principal);
        Criteria criteria = new Criteria();
        if (principal instanceof UserNamePrincipal) {
            criteria.add(TurbineUserPeer.LOGIN_NAME, principal.getName());
        } else if (principal instanceof UserIdPrincipal) {
            criteria.add(TurbineUserPeer.USER_ID, principal.getName());
        } else {
            throw new UserException("Invalid Principal Type in removeUser: " + principal.getClass().getName());
        }
        try {
            TurbineUserPeer.doDelete(criteria);
            PsmlManager.removeUserDocuments(user);
        } catch (Exception e) {
            String message = "Failed to remove account '" + user.getUserName() + "'";
            logger.error(message, e);
            throw new UserException(message, e);
        }
    }

    /**
   * Allows for a user to change their own password.
   *
   * @param user
   *          the JetspeedUser to change password
   * @param oldPassword
   *          the current password supplied by the user.
   * @param newPassword
   *          the current password requested by the user.
   * @exception UserException
   *              when the security provider has a general failure retrieving a
   *              user.
   * @exception UnknownUserException
   *              when the security provider cannot match the principal identity
   *              to a user.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public void changePassword(JetspeedUser user, String oldPassword, String newPassword) throws JetspeedSecurityException {
        oldPassword = JetspeedSecurity.convertPassword(oldPassword);
        newPassword = JetspeedSecurity.convertPassword(newPassword);
        String encrypted = JetspeedSecurity.encryptPassword(oldPassword);
        if (!accountExists(user)) {
            throw new UnknownUserException(Localization.getString("UPDATEACCOUNT_NOUSER"));
        }
        if (!user.getPassword().equals(encrypted)) {
            throw new UserException(Localization.getString("UPDATEACCOUNT_BADOLDPASSWORD"));
        }
        user.setPassword(JetspeedSecurity.encryptPassword(newPassword));
        user.setPasswordChanged(new Date());
        saveUser(user);
    }

    /**
   * Forcibly sets new password for a User.
   *
   * Provides an administrator the ability to change the forgotten or
   * compromised passwords. Certain implementatations of this feature would
   * require administrative level access to the authenticating server / program.
   *
   * @param user
   *          the user to change the password for.
   * @param password
   *          the new password.
   * @exception UserException
   *              when the security provider has a general failure retrieving a
   *              user.
   * @exception UnknownUserException
   *              when the security provider cannot match the principal identity
   *              to a user.
   * @exception InsufficientPrivilegeException
   *              when the requestor is denied due to insufficient privilege
   */
    public void forcePassword(JetspeedUser user, String password) throws JetspeedSecurityException {
        if (!accountExists(user)) {
            throw new UnknownUserException("The account '" + user.getUserName() + "' does not exist");
        }
        user.setPassword(JetspeedSecurity.encryptPassword(password));
        saveUser(user);
    }

    /**
   * This method provides client-side encryption of passwords.
   *
   * If <code>secure.passwords</code> are enabled in JetspeedSecurity
   * properties, the password will be encrypted, if not, it will be returned
   * unchanged. The <code>secure.passwords.algorithm</code> property can be used
   * to chose which digest algorithm should be used for performing the
   * encryption. <code>SHA</code> is used by default.
   *
   * @param password
   *          the password to process
   * @return processed password
   */
    public String encryptPassword(String password) throws JetspeedSecurityException {
        if (securePasswords == false) {
            return password;
        }
        if (password == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(passwordsAlgorithm);
            byte[] digest = md.digest(password.getBytes("UTF-8"));
            ByteArrayOutputStream bas = new ByteArrayOutputStream(digest.length + digest.length / 3 + 1);
            OutputStream encodedStream = MimeUtility.encode(bas, "base64");
            encodedStream.write(digest);
            encodedStream.flush();
            encodedStream.close();
            return bas.toString();
        } catch (Exception e) {
            logger.error("Unable to encrypt password." + e.getMessage(), e);
            return null;
        }
    }

    /**
   * This is the early initialization method called by the Turbine
   * <code>Service</code> framework
   *
   * @param conf
   *          The <code>ServletConfig</code>
   * @exception throws a <code>InitializationException</code> if the service
   *            fails to initialize
   */
    public synchronized void init(ServletConfig conf) throws InitializationException {
        if (getInit()) return;
        super.init(conf);
        ResourceService serviceConf = ((TurbineServices) TurbineServices.getInstance()).getResources(JetspeedSecurityService.SERVICE_NAME);
        securePasswords = serviceConf.getBoolean(CONFIG_SECURE_PASSWORDS_KEY, securePasswords);
        passwordsAlgorithm = serviceConf.getString(CONFIG_SECURE_PASSWORDS_ALGORITHM, passwordsAlgorithm);
        systemUsers = serviceConf.getVector(CONFIG_SYSTEM_USERS, new Vector());
        try {
            roles = serviceConf.getStringArray(CONFIG_NEWUSER_ROLES);
        } catch (Exception e) {
        }
        if (null == roles || roles.length == 0) {
            roles = DEFAULT_CONFIG_NEWUSER_ROLES;
        }
        this.runDataService = (JetspeedRunDataService) TurbineServices.getInstance().getService(RunDataService.SERVICE_NAME);
        setInit(true);
    }

    /**
   * Check whether a specified user's account exists.
   *
   * The login name is used for looking up the account.
   *
   * @param user
   *          the user to be checked.
   * @param checkUniqueId
   *          make sure that we aren't overwriting another user with different
   *          id
   * @return true if the specified account exists
   * @throws UserException
   *           if there was a general db access error
   *
   */
    protected boolean accountExists(JetspeedUser user) throws UserException {
        return accountExists(user, false);
    }

    protected boolean accountExists(JetspeedUser user, boolean checkUniqueId) throws UserException {
        String id = user.getUserId();
        Criteria criteria = new Criteria();
        criteria.add(TurbineUserPeer.LOGIN_NAME, user.getUserName());
        List users;
        try {
            users = TurbineUserPeer.doSelect(criteria);
        } catch (Exception e) {
            logger.error("Failed to check account's presence", e);
            throw new UserException("Failed to check account's presence", e);
        }
        if (users.size() < 1) {
            return false;
        }
        TurbineUser retrieved = (TurbineUser) users.get(0);
        int key = retrieved.getUserId();
        String keyId = String.valueOf(key);
        if (checkUniqueId && !keyId.equals(id)) {
            throw new UserException("User exists but under a different unique ID");
        }
        return true;
    }

    protected JetspeedRunData getRunData() {
        JetspeedRunData rundata = null;
        if (this.runDataService != null) {
            rundata = this.runDataService.getCurrentRunData();
        }
        return rundata;
    }
}
