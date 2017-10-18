package com.incendiaryblue.user;

import com.incendiaryblue.appframework.AppConfig;
import com.incendiaryblue.storage.BusinessObject;
import com.incendiaryblue.storage.StorageManager;
import com.incendiaryblue.storage.String2Key;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.security.*;
import java.net.URLEncoder;
import java.math.*;

/**
 *
 *	@author $Author: giles $ - h.soran@syzygy.net
 *	@version $Revision: 1.6.2.2 $ - $Date: 2002/12/06 10:58:28 $
 *  	This class to represent User. It has only essential attributes to create an user. The
 *		attributes are username, password, password prompt, type, state and status. As user
 *		properties changing from project to projects, property definitions are done separately.
 *		That approach will allow user to have customised and common properties. Common
 *		properties are defined in the com.incendiaryblue.user.UserConstants interface temporarily and
 *		those will be assigned to any user created by any of the
 * 		constructors defined for this class. <BR>
 *
 * 		PROGRAMMING LOGIC : <UL>
 *		<LI>GUEST user <BR>
 *			Username 	= null<BR>
 *			Password 	= null<BR>
 *			Prompt   	= null<BR>
 *			Type 		= GUEST 	(Update it when user becomes registered)<BR>
 *			State		= unknown 	(first time comers)<BR>
 *			State 		= known 	(recognised by cookie)<BR>
 *			State 		= loggedin 	(is NEVER the case)<BR>
 *			Properties	= common properties defined for the system <BR>
 *
 *		<LI>REGISTERED user<BR>
 *			Username 	= not null<BR>
 *			Password 	= not null<BR>
 *			Prompt   	= not null  (can be null)<BR>
 *			Type 		= REGISTERED<BR>
 *			State		= unknown 	(not recognised)<BR>
 *			State 		= known 	(recognised by cookie)<BR>
 *			State 		= loggedin 	<BR>
 *			Properties	= common properties defined for the system & customised ones<BR>
 *		<LI>STATUS of an user<BR>
 *			This is defined as constants in the UserConstants interface as active, inactive and
 *			removed. Metods i.e. disable and enable  can be used to update the status of
 *			a user. Generally : <BR>
 *			Disable - disables an active user<BR>
 *			Enable - enables a disabled user (if required but it is risky)<BR>
 */
public class User extends UserBusinessObject implements UserConstants, Comparable, Serializable {

    /** Strorage manager of this class */
    private static final StorageManager storageManager = (StorageManager) AppConfig.getComponent(StorageManager.class, "User");

    public static final String QUERY_ALL = "all_users";

    public static final String QUERY_BY_USERNAME = "user_by_username";

    public static final String QUERY_BY_USERNAME_START = "user_by_usernamestart";

    public static final String QUERY_BY_PROPERTY = "users_by_property";

    public static final String QUERY_BY_PROPERTY_EXACT = "users_by_property_exact";

    private String szUsername;

    private byte baPassword[];

    private String szPrompt;

    private byte nType;

    private byte nUserState;

    private static String[] alphaNumeric = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

    private void setCommonUserProperties() {
        String szPropName;
        UserProperty oUserProperty;
        DefaultUserProperty oDUP = null;
        List loDUP = DefaultUserProperty.getAllDUP();
        for (Iterator itp = loDUP.iterator(); itp.hasNext(); ) {
            oDUP = (DefaultUserProperty) itp.next();
            szPropName = oDUP.getName();
            oUserProperty = new UserProperty(this, szPropName, null);
            oUserProperty.store();
        }
    }

    /** @return required user by its id */
    public static User getUser(Object oPKey) {
        return (User) storageManager.getObject(oPKey);
    }

    /**
	 * @return Required User by username; null if param==null
	 * @param szUsername (unique) username of User object
	 */
    public static User getUser(String szUsername) {
        if (szUsername == null) {
            return null;
        }
        return (User) storageManager.getObject(QUERY_BY_USERNAME, szUsername);
    }

    /**
	 * Searches for users based on a given property and value, returning the set
	 * of users that have associated with them the named property with the
	 * supplied value.
	 *
	 * A LIKE search is performed, properties will match if their value
	 * contains as a subset the given value.
	 *
	 * @return A List populated with User objects with matching property/value,
	 *         or an empty List object if no matching users were found with the
	 *         specified property name and supplied value. Null is returned
	 *         if either the property name or value supplied is null.
	 */
    public static List getUsersByProperty(String propName, String propVal) {
        return getUsersByProperty(propName, propVal, false);
    }

    /**
	 * Searches for users based on a list of supplied properties and values,
	 * returning the set of users that have associated with them all the given
	 * properties and corresponding values.
	 *
	 * A LIKE search is performed, properties will match if their value
	 * contains as a subset the given value. (The operation is case-insensitive.)
	 *
	 * @parm propMap HashMap object containing a list of keyname-value mappings.
	 * @return A List populated with User objects that have associated with them
	 *         all the given properties and corresponding values, or an empty
	 *         List object if no matching ones were found.
	 *         <br>If an empty HashMap is given, <i>all</i> Users are returned.
	 */
    public static List getUsersByProperties(HashMap propMap) {
        List userList = null;
        Iterator propIterator = propMap.keySet().iterator();
        if (propIterator.hasNext()) {
            String propName = (String) propIterator.next();
            String propVal = (String) propMap.get(propName);
            System.out.println("\nGot list of users based on: " + propName);
            System.out.println("\nwith val: " + propVal);
            userList = getUsersByProperty(propName, propVal);
        } else {
            return getAllUsers();
        }
        Iterator userIterator = userList.iterator();
        List retList = new ArrayList();
        while (userIterator.hasNext()) {
            User u = (User) userIterator.next();
            propIterator = propMap.keySet().iterator();
            boolean match = true;
            while (propIterator.hasNext()) {
                String propName = (String) propIterator.next();
                System.out.println("\nChecking property: " + propName);
                String propVal = ((String) propMap.get(propName)).toLowerCase();
                String storedVal = u.getPropertyValue(propName);
                if (storedVal == null || storedVal.toLowerCase().indexOf(propVal) == -1) {
                    match = false;
                    break;
                }
            }
            if (match) retList.add(u);
        }
        return retList;
    }

    /**
	 * Return a List of User objects with usernames that start with the specified
	 * pattern.
	 * @param usernameStart The pattern for the start of the username of Users to
	 *                      be returned
	 * @return A List of User objects whose username starts with usernameStart
	 */
    public static List getUsersByUsername(String usernameStart) {
        return storageManager.getObjectList(QUERY_BY_USERNAME_START, usernameStart);
    }

    public static List getUsersByProperty(String propName, String propValue, boolean exact) {
        if (propName == null || propValue == null) {
            return null;
        }
        String cacheKey = exact ? QUERY_BY_PROPERTY_EXACT : QUERY_BY_PROPERTY;
        return storageManager.getObjectList(cacheKey, new String2Key(propName, propValue));
    }

    /** @return the user property object of the required property name for this user */
    public UserProperty getProperty(String szPropName) {
        return UserProperty.getUserProperty(this, szPropName);
    }

    /** @return list of user objects in the system */
    public static List getAllUsers() {
        return storageManager.getObjectList(QUERY_ALL, null);
    }

    /** Get a list of the groups this user belongs to. */
    public List getGroups() {
        return UserGroupRel.getGroups(this);
    }

    /** Test if this user is in the specified group. */
    public boolean isInGroup(Group g) {
        return UserGroupRel.userIsInGroup(this, g);
    }

    /** Add this user to the specified group. */
    public boolean addToGroup(Group g) {
        return UserGroupRel.addUserToGroup(this, g);
    }

    /** Remove the user from the specifed group. */
    public boolean removeFromGroup(Group g) {
        return UserGroupRel.removeUserFromGroup(this, g);
    }

    /** @return list of user property objects of this user */
    public List getProperties() {
        return UserProperty.getProperties(this);
    }

    /** Delete the user. */
    public void delete() {
        StorageManager liveContentStorageManager = (StorageManager) AppConfig.getComponent(StorageManager.class, "livecontent");
        liveContentStorageManager.delete("UPDATE_MODIFIED_BY", this.getPrimaryKey());
        StorageManager workingContentStorageManager = (StorageManager) AppConfig.getComponent(StorageManager.class, "workingcontent");
        workingContentStorageManager.delete("UPDATE_MODIFIED_BY", this.getPrimaryKey());
        UserGroupRel.removeAllGroups(this);
        UserProperty.deleteByUser(this);
        storageManager.delete(this);
    }

    /** Store the user. */
    public void store() {
        storageManager.store(this);
    }

    /** Instance of the user factory to use. */
    private static UserFactory userFactory = null;

    /**
	 * Set the UserFactory to use.
	 *
	 * <p>Called from UserManagement.</p>
	 */
    static void setUserFactory(UserFactory uf) {
        userFactory = uf;
    }

    /**
	 * Get the configured UserFactory object.
	 *
	 * <p>If one hasn't been set, it creates an instance of DefaultUserFactory
	 * and returns that.</p>
	 */
    private static UserFactory getUserFactory() {
        if (userFactory == null) {
            userFactory = new DefaultUserFactory();
        }
        return userFactory;
    }

    /**
	 * Create a new User object for user data retrieved from persistent
	 * storage.
	 *
	 * <p>Implemented by passing the request to the configured UserFactory.</p>
	 */
    public static User createUserFromStorage(Integer primaryKey, String username, byte password[], String passwordPrompt, byte type, byte userState, boolean active, Date changed) {
        return getUserFactory().createUserFromStorage(primaryKey, username, password, passwordPrompt, type, userState, active, changed);
    }

    /**
	 * Create a new user and save it to persistent storage.
	 *
	 * <p>Implemented by passing the request to the configured UserFactory.</p>
	 */
    public static User createNewUser(String username, String password, String passwordPrompt, byte type, byte userState, boolean active) {
        return getUserFactory().createNewUser(username, password, passwordPrompt, type, userState, active);
    }

    /**
	 * Creates, stores and sets common properties of this user together with specified status.
	 *
	 * <p>This constructor is designed to be called from DefaultUserFactory, or from
	 * a custom subclass of User.</p>
	 */
    protected User(String szUsername, String szPassword, String szPrompt, byte nType, byte nUserState, boolean active) {
        super(active, new java.util.Date());
        setPassword(szPassword);
        this.szPrompt = szPrompt;
        this.nType = nType;
        this.nUserState = nUserState;
        setUsername(szUsername);
        this.store();
        setCommonUserProperties();
    }

    /**
	 * Create a user object from a database record.
	 *
	 * <p>This constructor is designed to be called from DefaultUserFactory, or from
	 * a custom subclass of User.</p>
	 */
    protected User(Integer iPKey, String szUsername, byte baPassword[], String szPrompt, byte nType, byte nUserState, boolean active, Date dChanged) {
        super(active, dChanged);
        this.szUsername = szUsername;
        this.baPassword = baPassword;
        this.szPrompt = szPrompt;
        this.nType = nType;
        setPrimaryKey(iPKey);
    }

    /** @return Username of the user */
    public String getUsername() {
        return this.szUsername;
    }

    /** Get the encrypted password - for internal use only. */
    public byte[] getPasswordDigest() {
        return this.baPassword;
    }

    /** @return Password prompt to remind the user about their password */
    public String getPrompt() {
        return this.szPrompt;
    }

    /** @return Type of the user i.e. GUEST or REGISTERED */
    public byte getType() {
        return this.nType;
    }

    /** @return Current state of the user i.e. ACTIVE, INACTIVE */
    public byte getUserState() {
        return this.nUserState;
    }

    /** @return required property value */
    public String getPropertyValue(String szPropName) {
        UserProperty oUserProperty = getProperty(szPropName);
        return oUserProperty != null ? oUserProperty.getValue() : null;
    }

    /** Sets the username atomically. */
    public void setUsername(String s) {
        if (s == null || s.equals("")) {
            throw new IllegalArgumentException("New username is null or empty.");
        }
        if (getState() == BusinessObject.STATE_STORED && szUsername.equals(s)) {
            return;
        }
        User existingUser = getUser(s);
        if (existingUser != null) {
            throw new UserExistsException(s, (Integer) existingUser.getPrimaryKey());
        }
        szUsername = s;
        setChanged();
    }

    /**
	 * returns a random alphanumeric password 8 chars in length.
	 */
    public String newPassword() {
        StringBuffer password = new StringBuffer();
        while (password.length() < 8) {
            int i = (int) Math.round(Math.random() * alphaNumeric.length);
            if (i >= 0 && i < alphaNumeric.length) {
                password.append(alphaNumeric[i]);
            }
        }
        return password.toString();
    }

    /** Sets the password */
    public void setPassword(String szPassword) {
        this.baPassword = encrypt(szPassword);
        setChanged();
    }

    /** Check whether the user's password is the same as the parameter. */
    public boolean checkPassword(String szTestPassword) {
        byte baTestPassword[] = encrypt(szTestPassword);
        return Arrays.equals(baPassword, baTestPassword);
    }

    /**
	 * This routine encrypts a string and returns an array of bytes.
	 *
	 * <p>This doesn't set the password.  It is used this to encrypt a password
	 * before it is stored in the object.</p>
	 */
    public static byte[] encrypt(String sz) {
        MessageDigest oMessageDigest;
        try {
            oMessageDigest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Can't instantiate SHA message digest algorithm");
            e.printStackTrace();
            throw new RuntimeException("Can't instantiate SHA message digest algorithm");
        }
        oMessageDigest.reset();
        return oMessageDigest.digest(sz.getBytes());
    }

    /** Sets the password prompt */
    public void setPrompt(String szPrompt) {
        this.szPrompt = szPrompt;
        setChanged();
    }

    /** Sets the type */
    public void setType(byte nType) {
        this.nType = nType;
        setChanged();
    }

    /** Sets the state */
    public void setUserState(byte nUserState) {
        this.nUserState = nUserState;
        setChanged();
    }

    public void setPropertyValue(String szPropName, String szValue) {
        UserProperty oUserProperty;
        oUserProperty = UserProperty.getUserProperty(this, szPropName);
        if (oUserProperty != null) {
            oUserProperty.setValue(szValue);
            oUserProperty.store();
        } else {
            UserProperty newProp = new UserProperty(this, szPropName, szValue);
            newProp.store();
        }
    }

    /** @return a string version of this user object together with its properties */
    public String toString() {
        String szRetStr = "";
        List loUserProp = UserProperty.getProperties(this);
        UserProperty oUserProperty;
        szRetStr = "ID :" + getPrimaryKey().toString() + "|";
        szRetStr += "Uname: " + getUsername() + "|";
        szRetStr += "Prompt: " + getPrompt() + "|";
        szRetStr += "Type: " + getType() + "|";
        szRetStr += "State: " + getUserState() + "|";
        szRetStr += "Stus: " + (isActive() ? "ACTIVE" : "INACTIVE") + "|";
        szRetStr += "Date: " + getChanged() + "|<br>";
        szRetStr += "Properties: <p>";
        for (Iterator it = loUserProp.iterator(); it.hasNext(); ) {
            oUserProperty = (UserProperty) it.next();
            szRetStr += oUserProperty.toString();
        }
        return szRetStr + "<p>\n\n";
    }

    /**
	 * Does the user have admin (ie user management) privilege?
	 *
	 * <p>This implememtation checks wehter the user is in the "adms"
	 * (ie administrators) group.</p>
	 */
    public boolean isAdminUser() {
        Group adminGroup = Group.getGroup(Group.ADMIN_GROUP_ABBR);
        return UserGroupRel.userIsInGroup(this, adminGroup);
    }

    /**
	 * Hold the system's current user.
	 *
	 * <p>Stored as a storage key so that if the thread is idle it
	 * doesn't keep the object hanging around in memeory.</p>
	 */
    public static ThreadLocal currentUserKey = new ThreadLocal();

    /**
	 * Make this user the current user in the system.
	 *
	 * <p>This is called by UserHelper.currentUser(), which is called
	 * every time the user makes a page request.</p>
	 *
	 * <p>The default implementation sets a thread local variable so that
	 * the system then has a concept of who the current user is.  This can
	 * be overriden by subtypes to provide additional functionality.</p>
	 */
    public static void setCurrentUser(User u) {
        currentUserKey.set(u != null ? u.getPrimaryKey() : null);
        if (u != null) u.startUserRequestHook();
    }

    /**
	 * Get the current user.
	 *
	 * @return The last user object passed to setCurrentUser() executing in this thread.
	 */
    public static User getCurrentUser() {
        Object key = currentUserKey.get();
        return key != null ? getUser(key) : null;
    }

    /**
	 * Called when a request comes in that is identified as being from this user.
	 */
    protected void startUserRequestHook() {
    }

    /**
	 *
	 * @param o1    o1 an instance of User; this is the user to be compared
	 * @return      a negative integer, zero, or a positive integer if this users name is
	 *                      lexically less than, equal to, or greater than the passed users name
	 */
    public int compareTo(Object o1) {
        if (o1 == null) {
            throw new IllegalArgumentException("The passed user was null");
        }
        if (!(o1 instanceof User)) {
            throw new IllegalArgumentException("The passed object was not a User");
        }
        User u1 = (User) o1;
        if ((u1.getUsername() == null) || (u1.getUsername().length() == 0)) {
            throw new IllegalArgumentException("The passed User does not have a Username");
        }
        return String.CASE_INSENSITIVE_ORDER.compare(this.getUsername(), u1.getUsername());
    }
}
