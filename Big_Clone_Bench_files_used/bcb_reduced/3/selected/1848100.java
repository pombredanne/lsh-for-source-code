package org.guildkit.clients.sum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author bieniekr
 *
 */
public class UsersTableHelper {

    private static final Log log = LogFactory.getLog(UsersTableHelper.class);

    public class UsersTableModel extends AbstractTableModel {

        /**
		 * 
		 */
        private static final long serialVersionUID = 125221038391959040L;

        private List<UsersEntry> users;

        private UsersTableModel(List<UsersEntry> users) {
            this.users = users;
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            return this.users.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UsersEntry entry = this.users.get(rowIndex);
            Object result = null;
            switch(columnIndex) {
                case 0:
                    result = entry.getId();
                    break;
                case 1:
                    result = entry.getLogin();
                    break;
                case 2:
                    result = entry.getPasswordHash();
                    break;
                case 3:
                    result = entry.getUuidValue();
                    break;
                case 4:
                    result = entry.getLastLogin();
                    break;
            }
            return result;
        }

        @Override
        public String getColumnName(int column) {
            String name = "";
            switch(column) {
                case 0:
                    name = "ID";
                    break;
                case 1:
                    name = "login name";
                    break;
                case 2:
                    name = "password hash";
                    break;
                case 3:
                    name = "uuid";
                    break;
                case 4:
                    name = "last login";
                    break;
            }
            return name;
        }
    }

    private Connection connection;

    public UsersTableHelper(Connection connection) {
        this.connection = connection;
    }

    public List<UsersEntry> loadAllUsers() throws SQLException {
        List<UsersEntry> users = new LinkedList<UsersEntry>();
        PreparedStatement ps = connection.prepareStatement("select ID, LOGIN_NAME, PASSWORD_HASH, LAST_LOGIN, UUID, SITE_ADMIN from USERS order by ID");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            UsersEntry entry = new UsersEntry();
            entry.setId(rs.getLong("ID"));
            entry.setLogin(rs.getString("LOGIN_NAME"));
            entry.setPasswordHash(rs.getString("PASSWORD_HASH"));
            entry.setLastLogin(rs.getDate("LAST_LOGIN"));
            entry.setUuidValue(rs.getString("UUID"));
            entry.setSiteAdmin(rs.getBoolean("SITE_ADMIN"));
            users.add(entry);
        }
        rs.close();
        ps.close();
        connection.commit();
        return users;
    }

    public TableModel loadTableModel() throws SQLException {
        try {
            return new UsersTableModel(loadAllUsers());
        } catch (SQLException e) {
            log.debug("failed to load user entris", e);
            throw e;
        }
    }

    public boolean userExists(String login) throws SQLException {
        boolean result = false;
        PreparedStatement ps = connection.prepareStatement("select ID from USERS where LOGIN_NAME=?");
        ResultSet rs;
        ps.setString(1, login);
        rs = ps.executeQuery();
        result = rs.next();
        rs.close();
        ps.close();
        connection.commit();
        return result;
    }

    public void addUser(String loginName, String password) throws SQLException, NoSuchAlgorithmException {
        PreparedStatement ps = connection.prepareStatement("insert into USERS (ID, LOGIN_NAME, PASSWORD_HASH, UUID, SITE_ADMIN) VALUES (?,?,?,?,?)");
        ps.setLong(1, newId());
        ps.setString(2, loginName);
        ps.setString(3, hashPassword(password));
        ps.setString(4, UUID.randomUUID().toString());
        ps.setBoolean(5, false);
        ps.execute();
        connection.commit();
        ps.close();
    }

    private long newId() throws SQLException {
        long id = 0;
        PreparedStatement ps = connection.prepareStatement("select max(ID) from USERS");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) id = rs.getLong(1) + 1;
        rs.close();
        ps.close();
        return id;
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        String hash = null;
        MessageDigest md = MessageDigest.getInstance("SHA");
        log.debug("secure hash on password " + password);
        md.update(password.getBytes());
        hash = new String(Base64.encodeBase64(md.digest()));
        log.debug("returning hash " + hash);
        return hash;
    }

    public void deleteUser(long id) throws SQLException {
        PreparedStatement ups = connection.prepareStatement("delete from USERS where ID=?");
        PreparedStatement urps = connection.prepareStatement("delete from USER_ROLE where USERS_ID=?");
        urps.setLong(1, id);
        urps.execute();
        ups.setLong(1, id);
        ups.execute();
        connection.commit();
        urps.close();
        ups.close();
    }

    public void updatePassword(long id, String password) throws SQLException, NoSuchAlgorithmException {
        PreparedStatement ps = connection.prepareStatement("update USERS set PASSWORD_HASH=? where ID=?");
        ps.setString(1, hashPassword(password));
        ps.setLong(2, id);
        ps.execute();
        connection.commit();
        ps.close();
    }

    public UsersEntry getEntry(long id) throws SQLException {
        UsersEntry entry = new UsersEntry();
        PreparedStatement ps = connection.prepareStatement("select ID, LOGIN_NAME, PASSWORD_HASH, LAST_LOGIN, UUID from USERS WHERE ID=?");
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            entry.setId(rs.getLong("ID"));
            entry.setLogin(rs.getString("LOGIN_NAME"));
            entry.setPasswordHash(rs.getString("PASSWORD_HASH"));
            entry.setLastLogin(rs.getDate("LAST_LOGIN"));
            entry.setUuidValue(rs.getString("UUID"));
        }
        rs.close();
        ps.close();
        connection.commit();
        return entry;
    }
}
