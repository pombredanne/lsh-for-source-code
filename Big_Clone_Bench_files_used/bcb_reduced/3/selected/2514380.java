package com.lyrisoft.chat.server.remote.persistence.auth;

import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.lyrisoft.chat.Translator;
import com.lyrisoft.chat.server.remote.AccessDenied;
import com.lyrisoft.chat.server.remote.ChatClient;
import com.lyrisoft.chat.server.remote.ChatServer;
import com.lyrisoft.chat.server.remote.persistence.Jdbc;
import com.lyrisoft.util.properties.PropertyException;
import com.lyrisoft.util.properties.PropertyTool;

public class JdbcAuthenticator extends NullAuthenticator {

    /** No password encryption. */
    private static final int NONE = 0;

    /** Leave password encryption to the MySQL database. */
    private static final int MYSQL = 1;

    /** MD5 password encryption. */
    private static final int MD5 = 2;

    /** The encryption method to use. One of the static's below. Defaults to NONE. */
    private int _cryptMethod = NONE;

    /** The MessageDigest object that creates the digest (md5). */
    private MessageDigest _digest;

    /** The PreparedStatement to see whether a user is in the database. */
    private PreparedStatement _idStmt;

    /** The PreparedStatement that checks a user's password. */
    private PreparedStatement _selectStmt;

    /** The PreparedStatement that adds new users to the database. */
    private PreparedStatement _insertStmt;

    public JdbcAuthenticator(ChatServer server, boolean allowGuests, boolean storeGuests) {
        super(server, allowGuests, storeGuests);
        try {
            String table = PropertyTool.getString("jdbc.Table", Jdbc.p);
            String idField = PropertyTool.getString("jdbc.IdField", Jdbc.p);
            String passwordField = PropertyTool.getString("jdbc.PasswordField", Jdbc.p);
            String authField = PropertyTool.getString("jdbc.AuthField", Jdbc.p);
            try {
                if (PropertyTool.getBoolean("jdbc.CryptPassword", Jdbc.p)) _cryptMethod = MYSQL;
            } catch (PropertyException pe) {
            }
            try {
                String cType = PropertyTool.getString("jdbc.CryptMethod", Jdbc.p);
                if (cType.equalsIgnoreCase("md5")) {
                    _cryptMethod = MD5;
                    _digest = MessageDigest.getInstance("MD5");
                } else if (cType.equalsIgnoreCase("mySQL")) _cryptMethod = MYSQL;
            } catch (PropertyException pe) {
            }
            _idStmt = Jdbc.conn.prepareStatement("SELECT " + idField + " FROM " + table + " WHERE lower(" + idField + ") = lower(?)");
            String query = "SELECT " + authField + " FROM " + table + " WHERE " + idField + "=? ";
            if (_cryptMethod == MYSQL) query += "AND " + passwordField + "=password(?)"; else query += "AND " + passwordField + "=?";
            _selectStmt = Jdbc.conn.prepareStatement(query);
            query = "INSERT INTO " + table + " (" + idField + ", " + passwordField + ", " + authField + ")";
            if (_cryptMethod == MYSQL) query = query + " VALUES (?, password(?), ?)"; else query = query + " VALUES (?, ?, ?)";
            _insertStmt = Jdbc.conn.prepareStatement(query);
            if (_allowGuests) {
                if (_storeGuests) ChatServer.log("JDBC authentication initialized. Guest access allowed, adding guests to the database."); else ChatServer.log("JDBC authentication initialized. Guest access allowed, not adding guests to the database.");
            } else ChatServer.log("JDBC authentication initialized. No guest access allowed.");
        } catch (Exception e) {
            ChatServer.log(e);
            throw new RuntimeException(e.toString());
        }
    }

    /**
	 * Encrypt the given password with the currently selected encryption method.
	 * If the encryption method is NONE or MYSQL, just return the password itself.
	 */
    private String encrypt(String password) {
        if (password == null) password = "";
        if (_cryptMethod == MD5) return asHexString(_digest.digest(password.getBytes())); else return password;
    }

    /**
	 * Create a hexidecimal representation of the given byte array.
	 */
    private String asHexString(byte[] b) {
        StringBuffer buffer = new StringBuffer(b.length * 2);
        int i;
        for (i = 0; i < b.length; i++) {
            if (((int) b[i] & 0xff) < 0x10) buffer.append("0");
            buffer.append(Long.toString(((int) b[i]) & 0xff, 16));
        }
        return buffer.toString();
    }

    public String getUserId(String target) {
        String s = super.getUserId(target);
        if (s == null) {
            s = getStoredUser(target);
        }
        return s;
    }

    private final String getStoredUser(String target) {
        String s = null;
        try {
            synchronized (Jdbc.conn) {
                _idStmt.setString(1, target);
                ResultSet rs = _idStmt.executeQuery();
                if (rs.next()) {
                    s = rs.getString(1);
                }
                rs.close();
            }
        } catch (Exception e) {
            ChatServer.log(e);
        }
        return s;
    }

    /**
	 * If a user if found in the users table, his password is checked.
	 * If a user is not found, the access level IAuthenticator.USER
	 * is returned if guests are allowed.
	 */
    public final Auth authenticate(ChatClient client, String password) throws AccessDenied {
        String userId = client.getUserId();
        ResultSet rs = null;
        try {
            synchronized (Jdbc.conn) {
                _selectStmt.setString(1, userId);
                _selectStmt.setString(2, encrypt(password));
                rs = _selectStmt.executeQuery();
                if (rs.next()) {
                    return new Auth(userId, rs.getInt(1));
                } else {
                    if (getStoredUser(userId) != null) {
                        throw new AccessDenied(userId);
                    }
                    if (_allowGuests) {
                        Auth auth = super.authenticate(client, password);
                        if (_storeGuests) {
                            _insertStmt.setString(1, auth.getUserId());
                            _insertStmt.setString(2, encrypt(password));
                            _insertStmt.setInt(3, IAuthenticator.USER);
                            _insertStmt.executeUpdate();
                        }
                        return auth;
                    } else throw new AccessDenied(userId);
                }
            }
        } catch (SQLException sqle) {
            ChatServer.log(sqle);
            ChatServer.logError("  SQLException: " + sqle.getMessage());
            ChatServer.logError("  SQLState:     " + sqle.getSQLState());
            ChatServer.logError("  VendorError:  " + sqle.getErrorCode());
            throw new AccessDenied(Translator.getMessage("sql_error", sqle.toString()));
        } finally {
            try {
                _idStmt.clearParameters();
                _selectStmt.clearParameters();
            } catch (SQLException nevermind) {
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    ChatServer.log(e);
                }
            }
        }
    }
}
