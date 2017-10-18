package com.dynamide.security;

import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import org.webmacro.datatable.DataTable;
import org.webmacro.datatable.SimpleTableRow;
import com.dynamide.Field;
import com.dynamide.IPropertyProvider;
import com.dynamide.Property;
import com.dynamide.Session;
import com.dynamide.datatypes.DatatypeException;
import com.dynamide.db.RDBDatabase;
import com.dynamide.db.RDBDatasource;
import com.dynamide.db.RDBTable;
import com.dynamide.db.SimpleDatasource;
import com.dynamide.db.RDBDatasource.ColMeta;
import com.dynamide.resource.ResourceManager;
import com.dynamide.util.Log;
import com.dynamide.util.StringList;
import com.dynamide.util.Tools;

public class Login extends SimpleDatasource {

    public Login(Session session, String dbContextName, String id) {
        super(session, session);
        Log.debug(Login.class, "Login constructor called, V.5. session: " + getSession() + " session: " + session);
        connect(dbContextName);
        setID(id);
        addColumn("surname");
        addColumn("familiarName");
        addColumn("otherName");
        addColumn("login");
        addColumn("loginID");
        addColumn("eMail");
        addColumn("password");
        addTempColumn("newPassword");
        addTempColumn("newPassword2");
        addTempColumn("referrer");
        addRow();
    }

    public void connect(String dbContextName) {
        ResourceManager rm;
        Session session = getSession();
        if (session == null) {
            rm = ResourceManager.getRootResourceManager();
        } else {
            rm = session.getResourceManager();
        }
        m_database = rm.openDatabase(dbContextName, getSession());
    }

    public String toString() {
        return "{com.dynamide.security.Login:" + m_login + '}';
    }

    public static final int OK = 0;

    public static final int NO_USER = 1;

    public static final int WRONG_PASSWORD = 2;

    public static final int NULL_PASSWORD = 3;

    public static final int USER_NOT_LOGGED_IN = 4;

    public static final int SYSTEM_ERROR = 5;

    public static final int ADDUSER_ALLOWED = 11;

    public static final int ADDUSER_DENIED = 12;

    public static final int ADDUSER_PENDING = 13;

    public static final int ADDUSER_ERROR = 14;

    public static final String ACTION_OK = "OK";

    public static final String ACTION_LOGIN = "Login";

    public static final String ACTION_NEW_USER = "New user";

    public static final String ACTION_SYSTEM_ERROR = "System error";

    public static final String ACTION_WRONG_PASSWORD = "Wrong password";

    public static final String ACTION_NULL_PASSWORD = "Empty password";

    public static final String ACTION_MISMATCH_PASSWORD = "Incorrect entry: passwords should match.";

    public static final String ACTION_NO_USER = "User not registered";

    public static final String ACTION_USER_NOT_LOGGED_IN = "User not logged in";

    public static final String ACTION_CHANGE_USER = "Profile changed";

    public static final String ACTION_CHANGE_PASSWORD = "Password changed";

    public static final String ACTION_FORBIDDEN = "Forbidden";

    public static final String ACTION_FORBIDDEN_ADMIN = "Forbidden: not Admin";

    public static final String ACTION_ADDUSER_PENDING = "addUSER: PENDING";

    public static final String ACTION_ADDUSER_ALLOWED = "addUSER: ALLOWED";

    public static final String ACTION_ADDUSER_DENIED = "addUSER: DENIED";

    public static final String errorCodeToString(int code) {
        switch(code) {
            case OK:
                return ACTION_OK;
            case NO_USER:
                return ACTION_NO_USER;
            case WRONG_PASSWORD:
                return ACTION_WRONG_PASSWORD;
            case NULL_PASSWORD:
                return ACTION_NULL_PASSWORD;
            case USER_NOT_LOGGED_IN:
                return ACTION_USER_NOT_LOGGED_IN;
            case SYSTEM_ERROR:
                return ACTION_SYSTEM_ERROR;
        }
        return "Unknown Code";
    }

    private String m_lastError = "";

    public String getLastError() {
        return m_lastError;
    }

    public void setLastError(int code) {
        setLastError(code, "");
    }

    public void setLastError(int code, String msg) {
        if (code == OK) {
            m_lastError = "";
        }
        m_lastError = errorCodeToString(code) + ((msg.length() > 0) ? ": " + msg : "");
    }

    private boolean m_isAdmin = false;

    private RDBDatabase m_database = null;

    public RDBDatabase getDatabase() {
        return m_database;
    }

    public void setDatabase(RDBDatabase new_value) {
        m_database = new_value;
    }

    public boolean getDebugSQL() {
        Property prop = getProperty("debugSQL");
        if (prop != null) {
            return Tools.isTrue(prop.getStringValue());
        }
        return false;
    }

    public void setDebugSQL(boolean new_value) {
        try {
            setProperty("debugSQL", "" + new_value);
        } catch (Exception e) {
            Log.error(Login.class, "couldn't set property in RDBDatabase: " + new_value + e);
        }
    }

    private boolean m_noCrypt = true;

    public boolean getNoCrypt() {
        return m_noCrypt;
    }

    public void setNoCrypt(boolean new_value) {
        m_noCrypt = new_value;
    }

    private String m_loginID = "";

    public String getLoginID() {
        return m_loginID;
    }

    private String m_login = "";

    public String getLogin() {
        return m_login;
    }

    private boolean m_loggedIn = false;

    private String m_passwordEncrypted;

    protected StringList m_propertiesTable = new StringList();

    private IPropertyProvider m_propertyProvider = null;

    public IPropertyProvider getPropertyProvider() {
        return m_propertyProvider;
    }

    public void setPropertyProvider(IPropertyProvider new_value) {
        m_propertyProvider = new_value;
    }

    public Property getProperty(String propertyName) {
        Property result;
        if (m_propertyProvider != null) {
            result = m_propertyProvider.getProperty(propertyName);
            if (result != null) {
                return result;
            }
        }
        return (Property) m_propertiesTable.get(propertyName);
    }

    public void setProperty(String name, String value) throws DatatypeException {
        if (m_propertyProvider != null) {
            m_propertyProvider.setProperty(name, value);
        } else {
            m_propertiesTable.remove(name);
            m_propertiesTable.addObject(name, new Property(null, name, value));
        }
    }

    public Field addField(String fieldName, Object value) throws DatatypeException {
        fieldName = fixCase(fieldName);
        Field aField = new Field(this, getSession(), fieldName, value);
        RDBDatasource.ColMeta metadata = new RDBDatasource.ColMeta();
        metadata.persistent = false;
        addColumn(fieldName, metadata);
        return aField;
    }

    public int addUser() {
        m_loggedIn = false;
        logDebug("addUser");
        String newPassword = getFieldStringValue("newPassword");
        String newPassword2 = getFieldStringValue("newPassword2");
        if (newPassword.length() == 0) {
            setFieldError("newPassword", "Required field.");
            return returnCode(NULL_PASSWORD, ACTION_NULL_PASSWORD, m_login);
        }
        if (!newPassword.equals(newPassword2)) {
            setFieldError("newPassword2", ACTION_MISMATCH_PASSWORD);
            return returnCode(WRONG_PASSWORD, ACTION_MISMATCH_PASSWORD, m_login, "entry error: newPassword != newPassword2");
        }
        String login;
        if (getSession() != null) {
            login = getSession().getFieldStringValue("USER");
        } else {
            login = getFieldStringValue("login");
        }
        if (!loginExists(login)) {
            String cryptPW = crypt(newPassword);
            String sql = "select * from DynamideUser.addUser('" + login + "', '" + cryptPW + "','" + getFieldStringValue("familiarName") + "','" + getFieldStringValue("surname") + "','" + getFieldStringValue("otherName") + "','" + getFieldStringValue("eMail") + "');";
            logDebug("sql:" + sql);
            String resultString = "";
            ResultSet rs = null;
            try {
                rs = m_database.executeQuery(sql, getID());
                if (rs == null) {
                    return ADDUSER_ERROR;
                }
                rs.next();
                resultString = rs.getString(1);
            } catch (SQLException e) {
                logError("addUser failed on sql: " + sql, e);
                return ADDUSER_ERROR;
            }
            logDebug("addUser resultString: " + resultString);
            if (resultString.equalsIgnoreCase("DENIED")) {
                return returnCode(ADDUSER_DENIED, ACTION_ADDUSER_DENIED, login, "Database denied addUser()");
            }
            int result = ADDUSER_ERROR;
            if (resultString.equalsIgnoreCase("PENDING")) {
                result = returnCode(ADDUSER_PENDING, ACTION_ADDUSER_PENDING, login, "User PENDING");
            }
            if (resultString.equalsIgnoreCase("ALLOWED")) {
                result = returnCode(ADDUSER_ALLOWED, ACTION_ADDUSER_ALLOWED, login, "User ALLOWED");
            }
            returnCode(OK, ACTION_NEW_USER, login);
            int code = login(login, newPassword, true);
            if (code == OK) {
                return result;
            }
            return ADDUSER_ERROR;
        } else {
            String msg = "ERROR: User '" + login + "' already exists?";
            return returnCode(SYSTEM_ERROR, ACTION_SYSTEM_ERROR, login, msg);
        }
    }

    public int allowUser(String USER, String newUSER) {
        if (USER.length() == 0) {
            return returnCode(NO_USER, ACTION_NO_USER, USER, "Empty USER name not allowed for administrator.");
        }
        if (!userLoggedIn(USER)) {
            return returnCode(USER_NOT_LOGGED_IN, ACTION_USER_NOT_LOGGED_IN, USER);
        }
        Map values = new HashMap();
        values.put("authorized", "1");
        RDBTable rdbtable = new RDBTable(m_database, "DynamideUser.users");
        if (getDebugSQL()) rdbtable.setDebugSQL(true);
        rdbtable.update(values, "login = '" + newUSER + "'");
        return returnCode(OK, ACTION_CHANGE_USER, USER);
    }

    public int changePassword() throws Exception {
        String password = getFieldStringValue("password");
        String newPassword = getFieldStringValue("newPassword");
        String newPassword2 = getFieldStringValue("newPassword2");
        if (!userLoggedIn(m_login)) {
            logError("changePassword user not logged in: " + m_login + " code: " + userLoggedIn(m_login));
            setFieldError("USER", ACTION_USER_NOT_LOGGED_IN);
            return returnCode(USER_NOT_LOGGED_IN, ACTION_USER_NOT_LOGGED_IN, m_login);
        }
        if (password.length() == 0) {
            setFieldError("password", ACTION_NULL_PASSWORD);
            return returnCode(NULL_PASSWORD, ACTION_NULL_PASSWORD, m_login);
        }
        if (newPassword.length() == 0) {
            setFieldError("newPassword", ACTION_NULL_PASSWORD);
            return returnCode(NULL_PASSWORD, ACTION_NULL_PASSWORD, m_login);
        }
        if (!newPassword.equals(newPassword2)) {
            setFieldError("newPassword2", ACTION_MISMATCH_PASSWORD);
            return returnCode(WRONG_PASSWORD, ACTION_MISMATCH_PASSWORD, m_login, "entry error: newPassword != newPassword2");
        }
        String cryptPW = crypt(password);
        if (!loginExists(m_login)) {
            if (getSession() != null) getSession().setFieldError("USER", ACTION_NO_USER);
            return returnCode(NO_USER, ACTION_NO_USER, m_login);
        }
        String currentCryptPW = lookupUserPW(m_login);
        if (!currentCryptPW.equals(cryptPW)) {
            setFieldError("password", ACTION_WRONG_PASSWORD);
            return returnCode(WRONG_PASSWORD, ACTION_WRONG_PASSWORD, m_login);
        }
        Map values = new HashMap();
        values.put("id", m_loginID);
        values.put("login", m_login);
        String cryptNewPW = crypt(newPassword);
        values.put("password", cryptNewPW);
        RDBTable rdbtable = new RDBTable(m_database, "DynamideUser.users");
        if (getDebugSQL()) rdbtable.setDebugSQL(true);
        rdbtable.update(values, "id = '" + m_loginID + "'");
        return returnCode(OK, ACTION_CHANGE_PASSWORD, m_login);
    }

    public int changeUserProfile() {
        try {
            return changeUserProfile(m_login, getFieldStringValue("familiarName"), getFieldStringValue("otherName"), getFieldStringValue("surname"), getFieldStringValue("eMail"));
        } catch (Throwable t) {
            System.out.println("Throwable caught in changeUserProfile: " + t);
            t.printStackTrace();
            return SYSTEM_ERROR;
        }
    }

    public int changeUserProfile(String USER, String familiarName, String otherName, String surname, String eMail) {
        if (USER.length() == 0) {
            return returnCode(NO_USER, ACTION_NO_USER, USER, "Empty USER name not allowed");
        }
        if (!userLoggedIn(USER)) {
            return returnCode(USER_NOT_LOGGED_IN, ACTION_USER_NOT_LOGGED_IN, USER);
        }
        Map values = new HashMap();
        values.put("id", m_loginID);
        values.put("login", USER);
        values.put("familiarName", familiarName);
        values.put("surname", surname);
        values.put("otherName", otherName);
        values.put("eMail", eMail);
        RDBTable rdbtable = new RDBTable(m_database, "DynamideUser.users");
        if (getDebugSQL()) rdbtable.setDebugSQL(true);
        rdbtable.update(values, "id = '" + m_loginID + "'");
        return returnCode(OK, ACTION_CHANGE_USER, USER);
    }

    public String crypt(String passwd) {
        if (m_noCrypt) {
            return passwd;
        }
        return crypt(passwd, true);
    }

    public static String crypt(String passwd, boolean pad) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.update(passwd.getBytes());
            String c = new String(sha.digest());
            return toNumeric(c, pad, true);
        } catch (java.security.NoSuchAlgorithmException e) {
            Log.error(Login.class, "couldn't crypt()", e);
            return "";
        }
    }

    /** @see #setUserCookie(String,String,String)
    */
    public String getUserCookie(String USER, String cookieName) throws SQLException {
        String sql = "SELECT * from DynamideUser.getUserCookie('" + USER + "', '" + cookieName + "');";
        return getDatabase().executeQueryToString(sql, "getusercookie", getID());
    }

    /** @see #getUserCookie(String,String)
    */
    public void setUserCookie(String USER, String cookieName, String value) throws SQLException {
        String sql = "SELECT * from DynamideUser.setUserCookie('" + USER + "', '" + cookieName + "', '" + value + "');";
        getDatabase().executeQuery(sql, getID());
    }

    public Enumeration getUserLogins() throws Exception {
        String sql = "SELECT u.id, u.login from DynamideUser.users u ORDER BY u.login;";
        DataTable rsdt = m_database.readTable(sql, getID());
        Vector v = new Vector();
        v.add("");
        Iterator it = rsdt.iterator();
        while (it.hasNext()) {
            SimpleTableRow row = (SimpleTableRow) it.next();
            Object o;
            o = row.get("LOGIN");
            String login = (o != null) ? o.toString().trim() : "";
            v.add(login);
        }
        return v.elements();
    }

    public Enumeration getPendingUserLogins() throws Exception {
        String sql = "select LOGIN from DynamideUser.view_pending;";
        DataTable rsdt = m_database.readTable(sql, getID());
        Vector v = new Vector();
        v.add("");
        Iterator it = rsdt.iterator();
        while (it.hasNext()) {
            SimpleTableRow row = (SimpleTableRow) it.next();
            Object o;
            o = row.get("LOGIN");
            String login = (o != null) ? o.toString().trim() : "";
            v.add(login);
        }
        return v.elements();
    }

    public Enumeration getAuthorizedUserLogins() throws Exception {
        String sql = "select LOGIN from DynamideUser.view_authorized;";
        DataTable rsdt = m_database.readTable(sql, getID());
        Vector v = new Vector();
        v.add("");
        Iterator it = rsdt.iterator();
        while (it.hasNext()) {
            SimpleTableRow row = (SimpleTableRow) it.next();
            Object o;
            o = row.get("LOGIN");
            String login = (o != null) ? o.toString().trim() : "";
            v.add(login);
        }
        return v.elements();
    }

    public Map getUsers() throws Exception {
        String sql = "SELECT u.id, u.login from DynamideUser.users u;";
        DataTable rsdt = m_database.readTable(sql, getID());
        Map map = new TreeMap();
        Iterator it = rsdt.iterator();
        while (it.hasNext()) {
            SimpleTableRow row = (SimpleTableRow) it.next();
            Object o;
            o = row.get("ID");
            String id = (o != null) ? o.toString().trim() : "";
            o = row.get("LOGIN");
            String login = (o != null) ? o.toString().trim() : "";
            map.put(id, login);
        }
        return map;
    }

    public boolean isAdmin(String userID) {
        if (m_isAdmin) {
            return true;
        }
        try {
            String sql = "select count(permission_id) as c from DynamideUser.user_permissions where user_id = '" + userID + "' and permission_code = 'ADMIN';";
            DataTable rsdt = m_database.readTable(sql, getID());
            String c = "";
            Iterator it = rsdt.iterator();
            if (it.hasNext()) {
                SimpleTableRow row = (SimpleTableRow) it.next();
                Object o;
                o = row.get("C");
                c = (o != null) ? o.toString().trim() : "";
            }
            if (c.length() == 0 || Tools.stringToInt(c) == 0) {
                return false;
            } else {
                m_isAdmin = true;
                return true;
            }
        } catch (Exception e) {
            logError("in isAdmin", e);
            return false;
        }
    }

    public void logAction(String action, String login) {
        Log.info(Login.class, "logAction: " + action + " login: " + login);
    }

    public int login(String USER, String clearTextPassword) {
        return login(USER, clearTextPassword, false);
    }

    public boolean loginExists(String login) {
        try {
            String sql = "select count(id) as c from DynamideUser.users where login = '" + login + "';";
            DataTable rsdt = m_database.readTable(sql, getID());
            String c = "";
            Iterator it = rsdt.iterator();
            if (it.hasNext()) {
                SimpleTableRow row = (SimpleTableRow) it.next();
                Object o;
                o = row.get("C");
                c = (o != null) ? o.toString().trim() : "";
            }
            if (c.length() == 0 || Tools.stringToInt(c) == 0) {
                Log.debug(Login.class, "loginExists: (false) for " + login);
                return false;
            } else {
                Log.debug(Login.class, "loginExists: (true) for " + login);
                return true;
            }
        } catch (Exception e) {
            logError("in loginExists(\"" + login + "\")", e);
            return true;
        }
    }

    public String lookupUserID(String login) throws Exception {
        String sql = "SELECT u.id from DynamideUser.users u where u.login='" + login + "';";
        DataTable rsdt = m_database.readTable(sql, getID());
        String id = "";
        Iterator it = rsdt.iterator();
        if (it.hasNext()) {
            SimpleTableRow row = (SimpleTableRow) it.next();
            Object o;
            o = row.get("ID");
            id = (o != null) ? o.toString().trim() : "";
        }
        return id;
    }

    public int resetOtherUserPassword(String otherUSER, String newPassword) {
        if (!isAdmin(m_loginID)) {
            String err = "user " + m_login + " attempted admin function resetOtherUserPassword";
            logError(err);
            return returnCode(SYSTEM_ERROR, ACTION_FORBIDDEN_ADMIN, m_login, err);
        }
        String cryptNewPW = crypt(newPassword);
        String otherUSERID = "";
        try {
            otherUSERID = lookupUserID(otherUSER);
        } catch (Exception e) {
            return returnCode(SYSTEM_ERROR, ACTION_CHANGE_PASSWORD, m_login, "Error changing password for other user: " + otherUSER + " error:" + e);
        }
        Map values = new HashMap();
        values.put("id", otherUSERID);
        values.put("login", otherUSER);
        values.put("password", cryptNewPW);
        RDBTable rdbtable = new RDBTable(m_database, "users");
        if (getDebugSQL()) rdbtable.setDebugSQL(true);
        rdbtable.update(values, "id = '" + otherUSERID + "'");
        return returnCode(OK, ACTION_CHANGE_PASSWORD, otherUSER, "by admin: " + m_loginID);
    }

    public void setFieldError(String fieldName, String msg) {
        Log.debug(Login.class, "Field Error for '" + fieldName + "' msg: " + msg);
        super.setFieldError(fieldName, msg);
    }

    public boolean userLoggedIn() {
        return m_login.length() > 0 && m_loggedIn;
    }

    public boolean userLoggedIn(String USER) {
        return m_login.length() > 0 && m_login.equals(USER);
    }

    private void addColumn(String fieldName) {
        super.addColumn(fieldName, new ColMeta(fieldName, true));
    }

    private void addTempColumn(String fieldName) {
        super.addColumn(fieldName, new ColMeta(fieldName, false));
    }

    private String getFieldStringValue(String fieldName) {
        Field f = getField(fieldName);
        if (f != null) {
            return f.getStringValue();
        }
        return "";
    }

    /** private, for internal use of logging in and logging in after newUser case is handled.
     */
    private int login(String USER, String clearTextPassword, boolean skipPasswordCheck) {
        try {
            m_loggedIn = false;
            if (clearTextPassword.length() == 0) {
                setFieldError("password", "Required field.");
                return returnCode(NULL_PASSWORD, ACTION_NULL_PASSWORD, m_login);
            }
            String sql = "SELECT * from DynamideUser.users u where u.login='" + USER + "';";
            DataTable rsdt = m_database.readTable(sql, getID());
            String id = "";
            Iterator it = rsdt.iterator();
            if (it.hasNext()) {
                SimpleTableRow row = (SimpleTableRow) it.next();
                id = safeGet(row.get("ID"));
                String loginFromDB = safeGet(row.get("LOGIN"));
                if (!USER.equalsIgnoreCase(loginFromDB)) {
                    logError("Error: logins don't match. login: " + USER + " loginFromDB: " + loginFromDB);
                    return returnCode(SYSTEM_ERROR, ACTION_SYSTEM_ERROR, USER);
                }
                boolean pwok = true;
                if (!skipPasswordCheck) {
                    String pw = crypt(clearTextPassword);
                    String pwFromDB = safeGet(row.get("PASSWORD"));
                    if (pw.equals(pwFromDB)) {
                        pwok = true;
                    } else {
                        logError("crypt'd passwords don't match: user: " + pw + " from-db: " + pwFromDB);
                        pwok = false;
                    }
                }
                if (pwok) {
                    m_loginID = id;
                    m_login = USER;
                    setFieldValue("loginID", m_loginID);
                    setFieldValue("login", m_login);
                    setFieldValue("surname", safeGet(row, "SURNAME"));
                    setFieldValue("familiarName", safeGet(row, "familiarName"));
                    setFieldValue("otherName", safeGet(row, "otherName"));
                    setFieldValue("eMail", safeGet(row, "eMail"));
                    Log.debug(Login.class, "login OK for " + USER);
                    m_loggedIn = true;
                    return returnCode(OK, ACTION_LOGIN, USER);
                } else {
                    setFieldError("password", "Wrong password for " + USER);
                    return returnCode(WRONG_PASSWORD, ACTION_WRONG_PASSWORD, USER);
                }
            }
            logError("User not found: " + USER + " row: " + rsdt);
            getSession().setFieldError("USER", "User '" + USER + "' not found");
            return returnCode(NO_USER, ACTION_NO_USER, USER);
        } catch (Exception e) {
            logError("Couldn't login()", e);
            getSession().setFieldError("USER", "System error (logged).");
            return returnCode(SYSTEM_ERROR, ACTION_SYSTEM_ERROR, USER);
        }
    }

    private String lookupUserPW(String login) throws Exception {
        String sql = "SELECT u.password from DynamideUser.users u where u.login='" + login + "';";
        DataTable rsdt = m_database.readTable(sql, getID());
        String pwc = "";
        Iterator it = rsdt.iterator();
        if (it.hasNext()) {
            SimpleTableRow row = (SimpleTableRow) it.next();
            Object o;
            o = row.get("PASSWORD");
            pwc = (o != null) ? o.toString().trim() : "";
        }
        return pwc;
    }

    private int returnCode(int code, String action, String USER) {
        setLastError(code);
        logAction(action, USER);
        return code;
    }

    private int returnCode(int code, String action, String USER, String extraMessage) {
        setLastError(code, extraMessage);
        logAction(action, USER);
        return code;
    }

    private String safeGet(SimpleTableRow row, String column) {
        Object o = row.get(column);
        return (o != null) ? o.toString().trim() : "";
    }

    private String safeGet(Object o) {
        return (o != null) ? o.toString().trim() : "";
    }

    private static char[] chars = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };

    private static int charslen = chars.length;

    private static String toNumeric(String passwd, boolean pad, boolean tryAlpha) {
        String sequence = "";
        for (int a = 0; a < passwd.length(); a++) {
            int num = passwd.charAt(a);
            String sNum;
            int modnum = num % charslen;
            if (tryAlpha) {
                sNum = "" + chars[modnum];
            } else {
                sNum = "" + num;
            }
            if (pad) {
                sequence += sNum + "_";
            } else {
                sequence += sNum;
            }
        }
        return sequence;
    }
}
