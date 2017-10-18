package org.infoeng.icws;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.infoeng.icws.ICWSServerImpl;
import org.infoeng.icws.database.DataStore;
import org.infoeng.icws.exception.PermissionException;
import org.infoeng.icws.utils.HexDump;

public class UserManager {

    private DataStore dataStore;

    public UserManager(Properties props) {
        dataStore = new DataStore(props.getProperty("databaseDriver"), props.getProperty("databaseURL"), props.getProperty("databaseUsername"), props.getProperty("databasePassword"));
    }

    public int getUserID(final String username) throws SQLException {
        Connection conn = dataStore.getConnection();
        int userID = -1;
        PreparedStatement userSelect = conn.prepareStatement("select * from users where userDN=?");
        SecureRandom sr = new SecureRandom();
        if (username != null) {
            userSelect.setString(1, username);
            ResultSet rs = userSelect.executeQuery();
            if ((rs != null)) {
                if (rs.next()) {
                    userID = rs.getInt("icwsUserID");
                }
            }
        }
        return userID;
    }

    public void allowOperation(final int userID, final String samlAssertion, final int operation) throws PermissionException, SQLException {
        switch(operation) {
            case ICWSServerImpl.CERTIFY_INFORMATION:
                break;
            case ICWSServerImpl.SECONDARY_ISSUANCE:
                break;
            case ICWSServerImpl.STORE_IC:
                break;
        }
    }

    public int createAccount(final String username, final String password) throws SQLException {
        int userID = -1;
        try {
            Connection conn = dataStore.getConnection();
            SecureRandom sr = new SecureRandom();
            Connection connTwo = dataStore.getConnection();
            Statement st = connTwo.createStatement();
            PreparedStatement userIns = conn.prepareStatement("insert into users values (?,?,?)");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] passBytes = sha.digest(password.getBytes());
            String passwordDigest = HexDump.byteArrayToHexString(passBytes).toLowerCase().replaceAll(" ", "");
            while (true) {
                userID = sr.nextInt(Integer.MAX_VALUE);
                userIns.setInt(1, userID);
                userIns.setString(2, username);
                userIns.setString(3, passwordDigest);
                int updated = userIns.executeUpdate();
                if (updated == 1) {
                    break;
                }
            }
            dataStore.releaseConnection(conn);
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return userID;
    }

    public void authorize(final String username, final String password, final int operation) throws PermissionException, SQLException {
        try {
            Connection c = dataStore.getConnection();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] passBytes = sha.digest(password.getBytes());
            String passwordDigest = HexDump.byteArrayToHexString(passBytes).toLowerCase().replaceAll(" ", "");
            PreparedStatement ps = c.prepareStatement("select passwordDigest from users where userDN=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    String passD = rs.getString(1);
                    if (passwordDigest.equals(passD)) return;
                }
            }
            throw new PermissionException("authorize failed for username " + username);
        } catch (java.security.NoSuchAlgorithmException e) {
        }
    }
}
