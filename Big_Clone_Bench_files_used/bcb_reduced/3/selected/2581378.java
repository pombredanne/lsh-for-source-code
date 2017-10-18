package BeanManager;

import Beans.User;
import Database.DBConnection;
import Database.PoolDatabaseManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Raed Shomali
 */
public class UserManager {

    public static boolean insertUser(User user) {
        boolean isSuccessful = false;
        DBConnection connection = (DBConnection) PoolDatabaseManager.getConnection();
        if (connection != null) {
            try {
                String sql = "insert into User (PK_EMAIL_ADDRESS, FIRST_NAME, LAST_NAME, PASSWORD, PRIVILEGE_TYPE, IS_CONFIRMED, WEBSITE, PHONE, REGISTERED_DATE) " + "values( ? , ? , ? , ? , ? , ? , ? , ? , STR_TO_DATE(?, '%m-%d-%Y') );";
                PreparedStatement preparedStatement = (PreparedStatement) connection.prepareStatement(sql);
                SimpleDateFormat dateFmt = new SimpleDateFormat("MM-dd-yyyy");
                preparedStatement.setString(1, user.getEmailAddress());
                preparedStatement.setString(2, user.getFirstName());
                preparedStatement.setString(3, user.getLastName());
                preparedStatement.setString(4, user.getPassword());
                preparedStatement.setString(5, user.getPrivilegeType());
                preparedStatement.setBoolean(6, user.isConfirmed());
                preparedStatement.setString(7, user.getWebsiteURL());
                preparedStatement.setLong(8, user.getPhoneNumber());
                preparedStatement.setString(9, dateFmt.format(user.getRegisteredDate()));
                preparedStatement.executeUpdate();
                preparedStatement.close();
                isSuccessful = true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            } finally {
                PoolDatabaseManager.disconnect(connection);
            }
        }
        return isSuccessful;
    }

    public static String updateUser(String emailAddress, String column_name, String value) {
        DBConnection connection = (DBConnection) PoolDatabaseManager.getConnection();
        if (connection != null) {
            try {
                String sql = "update User set " + column_name + "= ? where PK_EMAIL_ADDRESS = ?";
                PreparedStatement preparedStatement = (PreparedStatement) connection.prepareStatement(sql);
                preparedStatement.setString(1, value);
                preparedStatement.setString(2, emailAddress);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                PoolDatabaseManager.disconnect(connection);
            }
        }
        return "Your " + column_name + " has been updated.<br />";
    }

    public static String deleteUser(String emailAddress) {
        DBConnection connection = (DBConnection) PoolDatabaseManager.getConnection();
        if (connection != null) {
            try {
                String sql = "delete from User where PK_EMAIL_ADDRESS = ?";
                PreparedStatement preparedStatement = (PreparedStatement) connection.prepareStatement(sql);
                preparedStatement.setString(1, emailAddress);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                PoolDatabaseManager.disconnect(connection);
            }
        }
        return "You have been deleted from the JUG Database";
    }

    public static User getUser(String emailAddress) {
        DBConnection connection = (DBConnection) PoolDatabaseManager.getConnection();
        User user = null;
        if (connection != null) {
            try {
                String sql = "select FIRST_NAME , LAST_NAME, PRIVILEGE_TYPE , IS_CONFIRMED , REGISTERED_DATE , WEBSITE , PASSWORD , PHONE from User where PK_EMAIL_ADDRESS = ?";
                PreparedStatement preparedStatement = (PreparedStatement) connection.prepareStatement(sql);
                preparedStatement.setString(1, emailAddress);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs != null && rs.next()) {
                    String firstName = rs.getString(1);
                    String lastName = rs.getString(2);
                    String privilegeType = rs.getString(3);
                    boolean isConfirmed = rs.getBoolean(4);
                    Date registeredDate = rs.getDate(5);
                    String website = rs.getString(6);
                    String password = rs.getString(7);
                    long phone = rs.getLong(8);
                    user = new User();
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setIsConfirmed(isConfirmed);
                    user.setPassword(password);
                    user.setPhoneNumber(phone);
                    user.setPrivilegeType(privilegeType);
                    user.setRegisteredDate(registeredDate);
                    user.setWebsiteURL(website);
                    user.setEmailAddress(emailAddress);
                }
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                PoolDatabaseManager.disconnect(connection);
            }
        }
        return user;
    }

    public static String encrypt(String text) {
        final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        String result = "";
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes());
            byte[] hash = digest.digest();
            char buffer[] = new char[hash.length * 2];
            for (int i = 0, x = 0; i < hash.length; i++) {
                buffer[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
                buffer[x++] = HEX_CHARS[hash[i] & 0xf];
            }
            result = new String(buffer);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
}
