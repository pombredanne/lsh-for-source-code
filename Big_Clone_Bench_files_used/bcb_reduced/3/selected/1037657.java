package edu.gatech.oad.shared.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import edu.gatech.oad.client.net.DBUtils;
import edu.gatech.oad.client.net.Database;
import edu.gatech.oad.shared.user.User;

/**
 * Security class handles login,logout,password encryption, sanitization
 * Security is a Singleton datatype
 * 
 * @author Donovan Hatch
 * @version 1.0
 * 
 */
public class Security {

    /**
     * Currently logged in User
     */
    private User loggedInUser = null;

    /**
     * username used for logging in then discarded
     */
    private String username;

    /**
     * password used for logging in then discarded
     */
    private char[] password;

    /**
     * Instance of the security class
     */
    private static Security Instance = null;

    /**
     * logger used for logging security class
     */
    private static final Logger MYLOGGER = Logger.getLogger("edu.gatech.oad.shared.security.Security");

    /**
     * Singleton static method that returns the instance of the Class
     * 
     * @return instance Security Instance
     */
    public static Security getInstance() {
        if (Instance == null) {
            Instance = new Security();
        }
        return Instance;
    }

    /**
     * Returns Username
     * 
     * @return username username of the user logging in
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username
     * 
     * @param username
     *            username of the user logging in
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * returns the password of the user logging in
     * 
     * @return password password of the user logging in
     */
    public char[] getPassword() {
        return password;
    }

    /**
     * Sets the password of the user logging in
     * 
     * @param cs
     *            password of the user logging in
     */
    public void setPassword(char[] cs) {
        this.password = cs.clone();
    }

    /**
     * Sets the logged in User
     * 
     * @param loggedInUser
     *            User to set logged in
     */
    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    /**
     * gets the logged in user
     * 
     * @return loggedInUser logged in user
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Logs in the user and sets the loggedInUser
     * 
     * @return loginSuccess true if logged in, false if otherwise
     */
    public boolean isLoggedIn() {
        boolean loginSuccess = false;
        User tempUser = null;
        String type = null;
        String realPass = null;
        String inputPass = "";
        final int maxFailedLogins = 3;
        final String maxAttemptsExceeded = "You have " + "reached the maximum attempts of logging in incorrectly. " + "Please speak to the System Administrator " + "to removed this hold and reset your password.";
        int failedLogins = 0;
        int active = 0;
        final String accountDeleted = "This account has been deleted. " + "If your account should not be deleted, please contact the sys " + "admin";
        int suspended = 0;
        final String accountSuspended = "This account has been Suspended. " + "If your account should not be deleted, please contact the sys " + "admin";
        final String loginFailed = "Login failed";
        if (this.username.equals("")) {
            this.username = null;
            this.password = null;
            return false;
        } else if (this.password == null) {
            this.username = null;
            this.password = null;
            return false;
        }
        final String query = "SELECT  `ContactInfo`. * ,  `Address`. * ,  `Users`. *, " + "ead.* FROM ContactInfo " + "LEFT JOIN  `donovanh_syntaxerror`.`Address` " + "ON  `ContactInfo`.`addressId` =  `Address`.`id` " + "LEFT JOIN  `donovanh_syntaxerror`.`Users` " + "ON  `ContactInfo`.`id` =  `Users`.`contactInfoId` " + "LEFT JOIN  `donovanh_syntaxerror`.`Address` ead ON  " + "`ContactInfo`.`emergencyContactAddressId` =  ead.`id` " + " WHERE `Username` = '" + this.getUsername() + "'";
        final ResultSet result = Database.getInstance().querySelect(query);
        try {
            if (result.first()) {
                failedLogins = Integer.parseInt(result.getString("failedLogins"));
                active = Integer.parseInt(result.getString("active"));
                suspended = Integer.parseInt(result.getString("suspended"));
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
            this.username = null;
            this.password = null;
            return false;
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        if (failedLogins == maxFailedLogins) {
            JOptionPane.showMessageDialog(null, maxAttemptsExceeded);
            this.username = null;
            this.password = null;
            return false;
        } else if (active == 0) {
            JOptionPane.showMessageDialog(null, accountDeleted);
            this.username = null;
            this.password = null;
            return false;
        } else if (suspended == 1) {
            JOptionPane.showMessageDialog(null, accountSuspended);
            this.username = null;
            this.password = null;
            return false;
        }
        try {
            type = result.getString("type");
            realPass = result.getString("Password");
            result.beforeFirst();
            if (type.equals("patient")) {
                tempUser = DBUtils.populatePatientsFromRs(result).get(0);
            } else if (type.equals("nurse")) {
                tempUser = DBUtils.populateNurseFromRs(result).get(0);
            } else if (type.equals("doctor")) {
                tempUser = DBUtils.populateDoctorsFromRs(result).get(0);
            } else if (type.equals("sysadmin")) {
                tempUser = DBUtils.populateSysAdminFromRs(result).get(0);
            }
        } catch (SQLException e2) {
            e2.printStackTrace();
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        inputPass = new String(this.password);
        try {
            if (realPass.equals(encryptPassword(inputPass))) {
                this.setLoggedInUser(tempUser);
                loginSuccess = true;
                this.username = null;
                this.password = null;
            } else {
                failedLogins++;
                Database.getInstance().queryUpdate("UPDATE `Users` SET  `failedLogins` =  '" + failedLogins + "' WHERE  `Username` ='" + this.username + "'");
                this.username = null;
                this.password = null;
                if (!(loginSuccess)) {
                    JOptionPane.showMessageDialog(null, loginFailed);
                }
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return loginSuccess;
    }

    /**
     * logs out the logged in user
     */
    public void logout() {
        this.loggedInUser = null;
    }

    /**
     * Used to convert the hashed byte password to hex.
     * 
     * @param buf
     *            password that needs to be converted to hex from bytes
     * @return password encrypted password
     */
    public static String asHex(byte[] buf) {
        final Formatter formatter = new Formatter();
        for (byte b : buf) formatter.format("%02x", b);
        return formatter.toString();
    }

    /**
     * encrypts password using sha-256 with a salt
     * 
     * @param password
     * @return encryptedPassword encrypted password
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String encryptPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digester = MessageDigest.getInstance("sha-256");
        digester.reset();
        digester.update("Carmen Sandiago".getBytes());
        return asHex(digester.digest(password.getBytes("UTF-8")));
    }

    /**
     * Sanitizes the input from the GUI
     * 
     * @param input
     *            input to sanitize
     * @return returns sanitized input
     */
    public String cleanInput(String input) {
        return "";
    }

    /**
     * Creates a string representation of the object. In this case it just
     * returns if a user is logged in
     * 
     * @return String says if user is logged in or not
     */
    public String toString() {
        if (loggedInUser == null) {
            return "No user is currently logged in";
        } else {
            return "There is a user logged in";
        }
    }
}
