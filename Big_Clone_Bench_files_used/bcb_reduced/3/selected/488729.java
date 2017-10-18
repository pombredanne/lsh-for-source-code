package org.ndx.jebliki.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * User specific informations. In this class, there are two password fields. A
 * classical one (used for edit purpose) and a hashed one (which is really
 * memorized).
 * 
 * @author Nicolas Delsaux
 * 
 */
public class User implements java.io.Serializable {

    /**
	 * Interface holding all roles names, for easier use in the rest of the application
	 * @author Nicolas Delsaux
	 */
    public static interface Roles {

        /**
		 * Role of users able to modify pages
		 */
        public static final String EDITOR = "editor";

        /**
		 * Role of users able to create new pages
		 */
        public static final String AUTHOR = "author";

        /**
		 * Default role of any logged-in user. It allows distinction between logged-in 
		 * and non logged-in users
		 */
        public static final String USER = "user";
    }

    private static final long serialVersionUID = 1L;

    /** User login */
    private String login;

    /**
	 * User password, given only for edit purpose. As one may see, this field is
	 * made transient Since it won't be saved. instead, the password
	 */
    private transient String password;

    /**
	 * This password hash will be stored and used for login. It will be
	 * generated from password using
	 */
    private String passwordHash;

    /**
	 * User's roles
	 */
    private Set<String> roles = new TreeSet<String>();

    public User() {
    }

    public User(String login) {
        this();
        setLogin(login);
    }

    /**
	 * Method borrowed from http://www.javafr.com/code.aspx?ID=18643
	 * @param key input key
	 * @return the MD5 hashed String
	 * @throws NoSuchAlgorithmException if no MD5 provider is found
	 */
    private static String getEncodedPassword(String key) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(key.getBytes());
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    /**
	 * Notice that, when setting the clear text password, the passwordHash is also built.
	 * @param password
	 * @throws NoSuchAlgorithmException 
	 * @see {@link #getEncodedPassword(String)}
	 */
    public void setPassword(String password) throws NoSuchAlgorithmException {
        this.password = password;
        passwordHash = getEncodedPassword(password);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /**
	 * Notice this method should only be used by persistence mechanism
	 * @param passwordHash
	 */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
	 * This method compares the input possiblePassword (not in MD5) with the hashed one.
	 * For that, possiblePassword gets hashed and compared to {@link #passwordHash}
	 * @param possiblePassword
	 * @return true if encrypted possiblePassword is stricly equals to {@link #passwordHash}
	 * @throws NoSuchAlgorithmException due to use of {@link #getEncodedPassword(String)}
	 */
    public boolean isGoodPassword(String possiblePassword) throws NoSuchAlgorithmException {
        return getEncodedPassword(possiblePassword).equals(passwordHash);
    }

    public String getRolesString() {
        return Page.getCollectionAsString(getRoles());
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void removeRole(String name) {
        roles.remove(name);
    }

    public void addRole(String name) {
        roles.add(name);
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
