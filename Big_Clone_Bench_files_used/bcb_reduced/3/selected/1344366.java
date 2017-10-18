package de.fhbrs.litsearch;

import com.softtech.jdbc.SQLResults;
import java.lang.String;
import java.security.MessageDigest;

/**
 * Provides methods to initialize user after login and manage all users
 *
 * @version 	1.0 6 Jan 2006
 * @author 		Marco Werner, Christian Clever
 * 
 */
public class UserBean {

    private String loggedInUserFullName, loggedInUserRole, loggedInUserPassword;

    private String firstName, lastName, role, username, oldPassword, password, passRepeated;

    private int loggedInUserId, id;

    private String error, message;

    private boolean loggedIn, loginIncorrect;

    private LanguageBean languageObj;

    /** 
	 * Set all instance variables to default values
	 */
    public UserBean() {
        this.loggedInUserFullName = this.loggedInUserRole = this.loggedInUserPassword = "";
        this.firstName = this.lastName = this.role = "";
        this.username = this.oldPassword = this.password = this.passRepeated = "";
        this.loggedInUserId = this.id = 0;
        this.error = this.message = "";
        this.loggedIn = this.loginIncorrect = false;
        this.languageObj = null;
    }

    /**
	 * Filters all unwanted characters from a string
	 * @param stringToFilter String with unwanted characters
	 * @return String without the unwanted characters
	 */
    private String filter(String stringToFilter) {
        String filteredString;
        stringToFilter = stringToFilter.trim();
        filteredString = stringToFilter.replaceAll(" ", "");
        filteredString = filteredString.replaceAll(";", "");
        filteredString = filteredString.replaceAll("\\|", "");
        filteredString = filteredString.replaceAll("!", "");
        filteredString = filteredString.replaceAll("\"", "");
        filteredString = filteredString.replaceAll("\'", "");
        filteredString = filteredString.replaceAll("\\\\", "");
        return filteredString;
    }

    /**
	 * Checks for the existance of invalid characters
	 * @return True if at least one of the instance variables contains invalid characters, false otherwise
	 */
    private boolean containsInvalidChars(String stringToTest) {
        return stringToTest.matches(".*[\\|;!\"\'\\\\].*");
    }

    /**
	 * Gets user information from database and logs user in
	 * @param username Username as a string
	 * @param password Password as a string
	 */
    public void initialize(String username, String password) {
        if ((username == null) || (password == null)) {
            this.loginIncorrect = false;
        } else {
            username = this.filter(username);
            password = this.filter(password);
            if ((username.equals("")) || (password.equals(""))) this.loginIncorrect = true; else {
                JDBCBean dbCon = new JDBCBean();
                SQLResults res = dbCon.getResultSet("SELECT * FROM users WHERE username=\"" + username + "\" AND password =MD5(\"" + password + "\")");
                if (res.getRowCount() == 0) this.loginIncorrect = true; else {
                    this.loggedIn = true;
                    this.loggedInUserId = res.getInt(0, "id");
                    this.loggedInUserFullName = res.getString(0, "firstname") + " " + res.getString(0, "lastname");
                    this.loggedInUserRole = res.getString(0, "role");
                    this.loggedInUserPassword = res.getString(0, "password");
                }
            }
        }
    }

    /** 
	 * Logs user out
	 */
    public void logout() {
        this.loggedInUserId = 0;
        this.loggedInUserFullName = "";
        this.loggedInUserRole = "";
        this.loggedInUserPassword = "";
        this.loggedIn = false;
        this.loginIncorrect = false;
    }

    /** 
	 * Get full name of logged-in user
	 * @return String with logged-in users full name
	 */
    public String getLoggedInUserFullName() {
        return this.loggedInUserFullName;
    }

    /** 
	 * Get role of logged-in user
	 * @return String with logged-in users role
	 */
    public String getLoggedInUserRole() {
        return this.loggedInUserRole;
    }

    /** 
	 * Get id of new or edited user
	 * @return ID of new or edited user
	 */
    public int getId() {
        return this.id;
    }

    /** 
	 * Get first name of new or edited user
	 * @return String with first name of new or edited user
	 */
    public String getFirstName() {
        return this.firstName;
    }

    /** 
	 * Get last name of new or edited user
	 * @return String with last name of new or edited user
	 */
    public String getLastName() {
        return this.lastName;
    }

    /** 
	 * Get role of new or edited user as HTML select
	 * @param role Role as a string
	 * @return String with HTML select of new or edited users role
	 */
    public String getRoleAsHTMLSelect(String role) {
        String myRole = "";
        if ((role != null) && (role.length() > 0)) myRole = role; else myRole = this.role;
        String roleSelect = "<select name=\"role\" size=\"1\">";
        roleSelect += "<option ";
        if (myRole.equals("Erfasser")) {
            roleSelect += "selected=\"selected\" ";
        }
        roleSelect += "value=\"Erfasser\">Erfasser</option>";
        roleSelect += "<option ";
        if (myRole.equals("Bibliothekar")) {
            roleSelect += "selected=\"selected\" ";
        }
        roleSelect += "value=\"Bibliothekar\">Bibliothekar</option>";
        roleSelect += "<option ";
        if (myRole.equals("Administrator")) {
            roleSelect += "selected=\"selected\" ";
        }
        roleSelect += "value=\"Administrator\">Administrator</option>";
        roleSelect += "</select>";
        return roleSelect;
    }

    /** 
	 * Get username of new or edited user
	 * @return String with username of new or edited user
	 */
    public String getUsername() {
        return this.username;
    }

    /** 
	 * Get error from last operation
	 * @return String with error message
	 */
    public String getError() {
        return this.error;
    }

    /** 
	 * Get message from last operation
	 * @return String with message
	 */
    public String getMessage() {
        return this.message;
    }

    /** 
	 * Get information on login status of active user
	 * @return true if active user is logged in, false otherwise
	 */
    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    /** 
	 * Get information on login success
	 * @return true if login was successful, false otherwise
	 */
    public boolean loginIsIncorrect() {
        return this.loginIncorrect;
    }

    /** 
	 * Checks if the active user is an administrator
	 * @return true if active user is administrator, false otherwise
	 */
    public boolean isAdmin() {
        return this.isLoggedIn() && this.getLoggedInUserRole().equals("Administrator");
    }

    /** 
	 * Checks if the active user is a publisher
	 * @return true if active user is administrator or publisher, false otherwise
	 */
    public boolean isPublisher() {
        return this.isLoggedIn() && (this.getLoggedInUserRole().equals("Bibliothekar") || this.isAdmin());
    }

    /** 
	 * Checks if the active user is an editor
	 * @return true if active user is administrator, publisher or editor, false otherwise
	 */
    public boolean isEditor() {
        return this.isLoggedIn() && (this.getLoggedInUserRole().equals("Erfasser") || this.isPublisher());
    }

    /** 
	 * Set language object to active language object from jsp-file
	 * @param languageObj Language object from jsp-file
	 */
    public void setLanguage(LanguageBean languageObj) {
        this.languageObj = languageObj;
    }

    /** 
	 * Set ID of new or edited user
	 * @param id ID of new or edited user
	 */
    public void setId(int id) {
        this.id = id;
    }

    /** 
	 * Set first name of new or edited user
	 * @param firstName First name of new or edited user
	 */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** 
	 * Set last name of new or edited user
	 * @param lastName Last name of new or edited user
	 */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /** 
	 * Set username of new or edited user
	 * @param username Username of new or edited user
	 */
    public void setUsername(String username) {
        this.username = username;
    }

    /** 
	 * Set role of new or edited user
	 * @param role Role of new or edited user
	 */
    public void setRole(String role) {
        this.role = role;
    }

    /** 
	 * Set old password of current user
	 * @param oldPassword Old password of current user
	 */
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    /** 
	 * Set password of new or edited user
	 * @param password Password of new or edited user
	 */
    public void setPassword(String password) {
        this.password = password;
    }

    /** 
	 * Set repeated password of new or edited user
	 * @param passRepeated Repeated password of new or edited user
	 */
    public void setPassRepeated(String passRepeated) {
        this.passRepeated = passRepeated;
    }

    /** 
	 * Get all existing users from database as HTML table
	 * @return String with HTML table of all existing users
	 */
    public String getExistingUsers() {
        String usersInHTMLTable = "";
        String color = "";
        JDBCBean dbCon = new JDBCBean();
        SQLResults res = dbCon.getResultSet("SELECT * FROM users");
        if (res.getRowCount() == 0) this.error = this.languageObj.getTextFor("users.noExistingUsers"); else {
            for (int i = 0; i < res.getRowCount(); i++) {
                if (i % 2 == 1) color = "class=\"darkgrey\""; else color = "class=\"lightgrey\"";
                if (this.loggedInUserId == res.getInt(i, "id")) {
                    usersInHTMLTable += "<tr " + color + ">";
                    usersInHTMLTable += "<td class=\"textright\">" + res.getInt(i, "id") + "</td>";
                    usersInHTMLTable += "<td>" + res.getString(i, "username") + "</td>";
                    usersInHTMLTable += "<td>" + res.getString(i, "firstname") + "</td>";
                    usersInHTMLTable += "<td>" + res.getString(i, "lastname") + "</td>";
                    usersInHTMLTable += "<td>" + res.getString(i, "role") + "</td>";
                    usersInHTMLTable += "<td colspan=\"2\">" + languageObj.getTextFor("users.userNotEditable") + "</td>";
                    usersInHTMLTable += "</tr>\n";
                } else {
                    usersInHTMLTable += "<form name=\"user" + i + "\" method=\"post\" action=\"index.jsp?content=admin#users\">";
                    usersInHTMLTable += "<tr " + color + ">";
                    usersInHTMLTable += "<td class=\"textright\"><input type=\"hidden\" name=\"id\" value=\"" + res.getInt(i, "id") + "\" />" + res.getInt(i, "id") + "</td>";
                    usersInHTMLTable += "<td><input type=\"text\" size=\"15\" maxlength=\"20\" name=\"username\" alt=\"Username\" value=\"" + res.getString(i, "username") + "\" /></td>";
                    usersInHTMLTable += "<td><input type=\"text\" size=\"15\" maxlength=\"30\" name=\"firstName\" alt=\"Firstname\" value=\"" + res.getString(i, "firstname") + "\" /></td>";
                    usersInHTMLTable += "<td><input type=\"text\" size=\"15\" maxlength=\"30\" name=\"lastName\" alt=\"Lastname\" value=\"" + res.getString(i, "lastname") + "\" /></td>";
                    usersInHTMLTable += "<td>" + this.getRoleAsHTMLSelect(res.getString(i, "role")) + "</td>";
                    usersInHTMLTable += "<td><input type=\"submit\" name=\"updUser\" value=\"" + languageObj.getTextFor("users.updateButton") + "\" alt=\"" + languageObj.getTextFor("users.updateButton") + "\"></input></td>";
                    usersInHTMLTable += "<td><input type=\"submit\" name=\"delUser\" value=\"" + languageObj.getTextFor("users.deleteButton") + "\" alt=\"" + languageObj.getTextFor("users.deleteButton") + "\"></input></td>";
                    usersInHTMLTable += "</tr>";
                    usersInHTMLTable += "</form>\n";
                }
            }
        }
        return usersInHTMLTable;
    }

    /** 
	 * Set all instance variables associated with new or edited user to default values
	 */
    public void reset() {
        this.error = this.message = "";
        this.id = 0;
        this.firstName = this.lastName = this.role = "";
        this.username = this.oldPassword = this.password = this.passRepeated = "";
    }

    /** 
	 * Create user in database with values stored in instance variables
	 */
    public void createUser() throws Exception {
        this.error = this.message = "";
        if (this.containsInvalidChars(this.firstName) || this.containsInvalidChars(this.lastName) || this.containsInvalidChars(this.username) || this.containsInvalidChars(this.password) || this.containsInvalidChars(this.passRepeated)) {
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.invalidChars") + "</p>";
        } else if ((this.username.length() == 0) || (this.firstName.length() == 0) || (this.lastName.length() == 0) || (this.role.length() == 0) || (this.password.length() == 0) || (this.passRepeated.length() == 0)) {
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.allFields") + "</p>";
        } else if (this.password.length() < 5) {
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.passwordTooShort") + "</p>";
        } else if (!this.password.equals(this.passRepeated)) {
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.passwordsDoNotMatch") + "</p>";
        } else {
            JDBCBean dbCon = new JDBCBean();
            SQLResults res = dbCon.getResultSet("SELECT * FROM users WHERE username=\'" + this.username + "\'");
            if (res.getRowCount() > 0) {
                this.username = "";
                this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.userAlreadyExists") + "</p>";
            } else {
                dbCon.executeQuery("INSERT INTO users (username,password,firstname,lastname,role) VALUES ('" + this.username + "','" + this.hashWithMD5(this.password) + "','" + this.firstName + "','" + this.lastName + "','" + this.role + "')");
                this.reset();
                this.message = "<p class=\"green\">" + this.languageObj.getTextFor("users.createdSuccessfully") + "</p>";
            }
        }
    }

    /** 
	 * Delete user from database whose id is stored in the instance variable
	 */
    public void deleteUser() {
        JDBCBean dbCon = new JDBCBean();
        dbCon.executeQuery("DELETE FROM users WHERE id=\'" + this.id + "\'");
        if (dbCon.getRecordsUpdated() == 1) {
            this.reset();
            this.message = "<p class=\"green\">" + this.languageObj.getTextFor("users.deletedSuccessfully") + "</p>";
        } else {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.deletionError") + "</p>";
        }
    }

    /** 
	 * Update user in database with values stored in instance variables
	 */
    public void updateUser() {
        if (this.containsInvalidChars(this.firstName) || this.containsInvalidChars(this.lastName) || this.containsInvalidChars(this.username)) {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.invalidChars") + "</p>";
        } else if ((this.username.length() == 0) || (this.firstName.length() == 0) || (this.lastName.length() == 0) || (this.role.length() == 0)) {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.allFields") + "</p>";
        } else {
            JDBCBean dbCon = new JDBCBean();
            dbCon.executeQuery("UPDATE users SET username='" + this.username + "', firstname='" + this.firstName + "', lastname='" + this.lastName + "', role='" + this.role + "' WHERE id=" + this.id);
            this.reset();
            this.message = "<p class=\"green\">" + this.languageObj.getTextFor("users.updatedSuccessfully") + "</p>";
        }
    }

    /** 
	 * Update password in database with value stored in instance variable
	 */
    public void updatePassword() throws Exception {
        if (this.containsInvalidChars(this.oldPassword) || this.containsInvalidChars(this.password) || this.containsInvalidChars(this.passRepeated)) {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.invalidChars") + "</p>";
        } else if ((this.oldPassword.length() == 0) || (this.password.length() == 0) || (this.passRepeated.length() == 0)) {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.allFields") + "</p>";
        } else if (this.password.length() < 5) {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.passwordTooShort") + "</p>";
        } else if (!this.loggedInUserPassword.equals(this.hashWithMD5(this.oldPassword))) {
            this.reset();
            this.error = "<p class=\"red\">" + this.languageObj.getTextFor("users.oldPasswordNotCorrect") + "</p>";
        } else {
            JDBCBean dbCon = new JDBCBean();
            dbCon.executeQuery("UPDATE users SET password='" + this.hashWithMD5(this.password) + "' WHERE id=" + this.loggedInUserId);
            this.reset();
            this.message = "<p class=\"green\">" + this.languageObj.getTextFor("users.passUpdatedSuccessfully") + "</p>";
        }
    }

    /** 
	 * Hashes the given string with MD5 algorithm
	 * @param stringToEncode String to be hashed
	 * @return Encoded string
	 */
    private String hashWithMD5(String stringToEncode) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest;
        String encodedString = "";
        digest = md.digest(stringToEncode.getBytes());
        for (int i = 0; i < digest.length; i++) {
            String s = Integer.toHexString(digest[i] & 0xFF);
            encodedString += (s.length() == 1) ? "0" + s : s;
        }
        return encodedString;
    }
}
