package org.yawlfoundation.yawl.engine.interfce;

import org.yawlfoundation.yawl.exceptions.YQueryException;
import sun.misc.BASE64Encoder;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 
 * @author Lachlan Aldred
 * Date: 20/05/2005
 * Time: 19:02:27
 */
public class DBConnector {

    private Connection _conn;

    public DBConnector() throws ClassNotFoundException, SQLException {
        _conn = getConnection();
    }

    public static synchronized String encrypt(String plaintext) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = null;
        md = MessageDigest.getInstance("SHA");
        md.update(plaintext.getBytes("UTF-8"));
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String dbName = "yawl";
        String dbUserName = "postgres";
        String dbPassword = "admin";
        String url = "jdbc:postgresql:" + dbName;
        Properties props = new Properties();
        props.setProperty("user", dbUserName);
        props.setProperty("password", dbPassword);
        return DriverManager.getConnection(url, props);
    }

    /**
     * Executes a query over the organisation model of the YAWL system.
     * @param query
     * @return
     */
    public List whichUsersForThisQuery(String query) throws SQLException, YQueryException {
        List users = new ArrayList();
        ResultSet rs = executeQuery(query);
        while (rs.next()) {
            String user = rs.getString("hresid");
            if (null == user) {
                throw new YQueryException("" + "Something Wrong with a query inside the YAWL Process Specification:\n" + "The worklist executed query [" + query + "] over the " + "organisational model and this yielded a improperly typed " + "query result.");
            }
            users.add(user);
        }
        return users;
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Statement stmnt = _conn.createStatement();
        ResultSet rs = stmnt.executeQuery(query);
        return rs;
    }

    public int executeUpdate(String sql) throws SQLException {
        Statement statement = _conn.createStatement();
        return statement.executeUpdate(sql);
    }
}
