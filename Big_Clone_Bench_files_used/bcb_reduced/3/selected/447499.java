package soht.server.java;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Represents a user of the system.  Provides helper methods
 * to handle validation of passwords and mapping to the
 * properties file.
 *
 * @author Eric Daugherty
 */
public class User {

    private String userName;

    private String password;

    /** Handles the logging of messages */
    private static Logger log = Logger.getLogger(User.class);

    /**
     * Creates a new user.  All parameters are validated.
     * <ul>
     *   <li>The username must not be null or an empty string.</li>
     *   <li>The passwords must not be null or empty strings.</li>
     *   <li>The passwords must be equal</li>
     * </ul>
     *
     * @param userName the requested username.
     * @param password plain text password.
     * @param password2 plain text password.
     * @throws UIException contains a user friendly error message if validation fails.
     */
    public User(String userName, String password, String password2) throws UIException {
        if (userName == null || userName.length() < 1) {
            throw new UIException("Please specify a UserName!");
        }
        this.userName = userName;
        setPassword(password, password2);
    }

    /**
     * Creates a new User using the specified username and encrypted password.
     *
     * @param userName username
     * @param password encrypted password.
     */
    private User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * Returns the username.
     *
     * @return username.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns the encrypted password for this user.
     *
     * @return encrypted password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Validates that the plain text passwords match and are at
     * least 6 characters long.
     *
     * @param password plain text password
     * @param password2 plain text password
     * @throws UIException contains a user friendly error message if validation fails.
     */
    public void setPassword(String password, String password2) throws UIException {
        validatePasswords(password, password2);
        password = encryptPassword(password);
        if (password == null) {
            throw new UIException("Error encrypting password.");
        }
        this.password = password;
    }

    /**
     * Checks to see if the specified password matches this user's password.
     *
     * @param password plain text password
     * @return true if the password matches the user's password.
     */
    public boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        String encryptedPassword = encryptPassword(password);
        if (encryptedPassword == null) {
            log.error("Error encrypting password for user: " + userName);
            return false;
        }
        return encryptedPassword.equals(this.password);
    }

    /**
     * Returns a List of User instances loaded from the
     * specified properties file.
     *
     * @param properties
     * @return List of User instances.
     */
    public static List loadUsers(Properties properties) {
        List users = new ArrayList();
        Enumeration propertyNames = properties.propertyNames();
        String propertyName;
        String userName;
        String password;
        while (propertyNames.hasMoreElements()) {
            propertyName = (String) propertyNames.nextElement();
            if (propertyName.startsWith("user.")) {
                userName = propertyName.substring(5);
                password = properties.getProperty(propertyName);
                users.add(new User(userName, password));
            }
        }
        return users;
    }

    /**
     * Sets the user into the specified properties.
     *
     * @param user user to store
     * @param properties properties to modifiy
     */
    public static void setUser(User user, Properties properties) {
        properties.setProperty("user." + user.getUserName(), user.getPassword());
    }

    /**
     * Returns the user for the specified username.  Returns null
     * if the user does not exist.
     *
     * @param userName the username of the user to load.
     * @param properties the properties file to load the user from.
     * @return the loaded User, or null if the userName does not exist.
     */
    public static User getUser(String userName, Properties properties) {
        String password = properties.getProperty("user." + userName);
        if (password != null) {
            return new User(userName, password);
        }
        return null;
    }

    /**
     * Validates that the plain text passwords match and are at
     * least 6 characters long.
     *
     * @param password plain text password
     * @param password2 plain text password
     * @throws UIException contains a user friendly error message if validation fails.
     */
    public static void validatePasswords(String password, String password2) throws UIException {
        if (password == null || password2 == null) {
            throw new UIException("Invalid (null) parameter.");
        }
        if (!password.equals(password2)) {
            throw new UIException("Passwords do not match!");
        }
        if (password.length() < 6) {
            throw new UIException("Password must be at least 6 charecters");
        }
    }

    /**
     * Creates a one-way has of the specified password.  This allows passwords to be
     * safely stored without an easy way to retrieve the original value.
     *
     * @param password the string to encrypt.
     *
     * @return the encrypted password, or null if encryption failed.
     */
    public static String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes());
            byte[] hash = md.digest();
            StringBuffer hashStringBuf = new StringBuffer();
            String byteString;
            int byteLength;
            for (int index = 0; index < hash.length; index++) {
                byteString = String.valueOf(hash[index] + 128);
                byteLength = byteString.length();
                switch(byteLength) {
                    case 1:
                        byteString = "00" + byteString;
                        break;
                    case 2:
                        byteString = "0" + byteString;
                        break;
                }
                hashStringBuf.append(byteString);
            }
            return hashStringBuf.toString();
        } catch (NoSuchAlgorithmException nsae) {
            log.error("Error getting password hash - " + nsae.getMessage());
            return null;
        }
    }
}
