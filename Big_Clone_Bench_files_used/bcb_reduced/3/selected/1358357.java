package regnumhelper.rmi;

import java.math.BigInteger;
import java.security.*;

/**
 * Contains the username and password for the mule.
 * @author Michael Speth
 * @version 1.0
 */
public class MuleUser implements java.io.Serializable {

    /**
     * The user's name.
     **/
    public String username;

    /**
     * The user's password.
     **/
    public String password;

    /**
     * The group this user belongs to.
     **/
    public String group;

    /**
     * Sets the user name and converts password into MD5 hash.
     * @param username the name of the user.
     * @param password the password to be hashed.
     * @param group the group this user belongs to.
     **/
    public MuleUser(String username, String password, String group) {
        this.username = username;
        this.password = getMD5(password);
        this.group = group;
    }

    /**
     * Gets the MD5 hash with a predefined salt.
     * @param password the password to hash.
     * @return the hashed password.
     **/
    public static String getMD5(String password) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String salt = "UseTheForce4";
            password = salt + password;
            md5.update(password.getBytes(), 0, password.length());
            password = new BigInteger(1, md5.digest()).toString(16);
        } catch (Exception e) {
        }
        return password;
    }
}
