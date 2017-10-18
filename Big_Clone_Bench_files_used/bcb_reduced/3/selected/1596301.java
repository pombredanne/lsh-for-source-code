package Toepen.LoungeFacade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.*;
import java.math.BigInteger;

/**
 * @author Sjmeut
 * Deze klasse is voor het queryen naar de database
 */
public class DBUsers {

    private Connection connect = null;

    private PreparedStatement statement = null;

    private ResultSet resultSet = null;

    /**
     * Constructor voor de DBUsers klasse
     * @param connect Connection waarmee deze klasse connect
     */
    public DBUsers(Connection connect) {
        this.connect = connect;
    }

    /**
     * @param print defunct
     * @return aantal users
     */
    public int getNumUsers(boolean print) {
        int numUsers = 0;
        try {
            statement = connect.prepareStatement("SELECT id, username, first_name, last_name, email from toepen.users");
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if (print) {
                    for (int i = 1; i < 6; i++) {
                        if (i == 1) {
                            System.out.print(resultSet.getInt(i));
                        } else {
                            System.out.print(", " + resultSet.getString(i));
                        }
                    }
                    System.out.print("\n");
                }
                numUsers++;
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            close();
        }
        return numUsers;
    }

    public boolean validateLogin(String username, String password) {
        boolean user_exists = false;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            String password_hash = hash.toString(16);
            statement = connect.prepareStatement("SELECT id from toepen.users WHERE username = ? AND password = ?");
            statement.setString(1, username);
            statement.setString(2, password_hash);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                user_exists = true;
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            close();
            return user_exists;
        }
    }

    /**
     * Creer een nieuwe gebruiker in de database
     * @param username gewenste username
     * @param password gewenst password
     * @param name gewenste naam
     * @return true als dit gelukt is
     * @throws java.lang.Exception
     */
    public boolean createUser(String username, String password, String name) throws Exception {
        boolean user_created = false;
        try {
            statement = connect.prepareStatement("SELECT COUNT(*) from toepen.users WHERE username = ? LIMIT 1");
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) == 0) {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(password.getBytes());
                BigInteger hash = new BigInteger(1, md5.digest());
                String password_hash = hash.toString(16);
                long ctime = System.currentTimeMillis() / 1000;
                statement = connect.prepareStatement("INSERT INTO toepen.users " + "(username, password, name, ctime) " + "VALUES (?, ?, ?, ?)");
                statement.setString(1, username);
                statement.setString(2, password_hash);
                statement.setString(3, name);
                statement.setLong(4, ctime);
                if (statement.executeUpdate() > 0) {
                    user_created = true;
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            close();
            return user_created;
        }
    }

    /**
     * Sluit de verbinding
     */
    private void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
