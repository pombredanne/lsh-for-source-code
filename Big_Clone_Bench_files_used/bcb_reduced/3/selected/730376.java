package ua.org.nuos.realm;

import com.sun.appserv.security.AppservPasswordLoginModule;
import javax.security.auth.login.LoginException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: dio
 * Date: 02.10.11
 * Time: 20:55
 * To change this template use File | Settings | File Templates.
 */
public class SDLoginModule extends AppservPasswordLoginModule {

    private static final Logger l;

    private static final String REQUEST;

    private static List<String> ROLES;

    static {
        l = Logger.getLogger(SDLoginModule.class.getName());
        REQUEST = "select role from user_roles ur, user_ u where ur.user_id = u.id and u.email = ? and u.password = ?";
        ROLES = Arrays.asList(new String[] { "ADMIN", "CLIENT" });
    }

    private Connection connection;

    protected Configuration configuration;

    /**
     * for tests
     *
     * @param _username user login
     */
    public void setUsername(String _username) {
        this._username = _username;
    }

    /**
     * for tests
     *
     * @param _passwd
     */
    public void setPassword(char[] _passwd) {
        this._passwd = _passwd;
    }

    protected Configuration getConfiguration() {
        return new Configuration();
    }

    private void initConfiguration() {
        if (configuration == null) {
            configuration = getConfiguration();
        }
    }

    @Override
    protected void authenticateUser() throws LoginException {
        String userName = this.getUsername();
        String password = String.valueOf(this.getPasswordChar());
        connect();
        Collection<String> groupNamesList = getUserGroups(userName, password);
        commitUserAuthentication(groupNamesList.toArray(new String[] {}));
    }

    protected void connect() throws LoginException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            l.log(Level.SEVERE, "Cant find jdbs postgresql driver. Fail.", e);
            throw new LoginException();
        }
        try {
            initConfiguration();
            connection = DriverManager.getConnection(configuration.getJdbsUrl(), configuration.getJdbsLogin(), configuration.getJdbsPassword());
        } catch (SQLException e) {
            l.log(Level.SEVERE, "Init connection fail.", e);
            throw new LoginException();
        }
    }

    protected Collection<String> getUserGroups(String email, String password) throws LoginException {
        Collection<String> groupNamesList = new LinkedList<String>();
        try {
            initConfiguration();
            String hashPassword = hash(password);
            PreparedStatement statement = connection.prepareStatement(REQUEST);
            statement.setString(1, email);
            statement.setString(2, hashPassword);
            ResultSet usersRS = statement.executeQuery();
            while (usersRS.next()) {
                String role = usersRS.getString("role");
                if (ROLES.contains(role)) {
                    groupNamesList.add(role);
                }
            }
            if (groupNamesList.isEmpty()) {
                throw new LoginException();
            }
        } catch (SQLException e) {
            l.log(Level.SEVERE, "Cant create prepared statement.", e);
            throw new LoginException(email);
        } catch (NoSuchAlgorithmException e) {
            l.log(Level.SEVERE, "Cant hash password.", e);
            throw new LoginException(email);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                l.log(Level.SEVERE, "Cant close connection.", e);
                throw new LoginException(email);
            }
        }
        return groupNamesList;
    }

    private String hash(String text) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(text.getBytes());
        BigInteger hash = new BigInteger(1, md5.digest());
        return hash.toString(16);
    }
}
