package net.pyxzl.profilepal.database;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class DBoperations {

    private static DerbyDB db = DerbyDB.getSingleton();

    private static HashMap<String, String> options = new HashMap<String, String>();

    /**
	 * Log the user into the system and return his internal ID
	 * 
	 * @param username name of the user to login
	 * @param password password of the user to login
	 * @return ID of the user or 0 if the login fails
	 */
    public static int login(final String username, final String password) {
        int res = 0;
        try {
            final Statement s = DBoperations.db.connection.createStatement();
            final ResultSet rs = s.executeQuery("SELECT userID FROM " + DerbyDB.schemaName + ".Users" + " WHERE username='" + username + "' AND password='" + DBoperations.encrypt(password) + "'");
            if (rs.next()) res = rs.getInt("userID");
            DBoperations.db.connection.commit();
            s.close();
        } catch (final SQLException e) {
            System.out.println("Couldn't read ReslutSet during login");
        }
        return res;
    }

    /**
	 * Check if a user with the given username allready exists in the Database
	 * @param username
	 * @return if user exists
	 */
    public static boolean userExists(final String username) {
        boolean ret = false;
        try {
            final Statement s = DBoperations.db.connection.createStatement();
            final ResultSet rs = s.executeQuery("SELECT userID FROM " + DerbyDB.schemaName + ".Users" + " WHERE username='" + username + "'");
            ret = rs.next();
            DBoperations.db.connection.commit();
            s.close();
        } catch (final SQLException e) {
            System.out.println("Couldn't read ReslutSet during login");
        }
        return ret;
    }

    /**
	 * get an option for the system stored in the database
	 * 
	 * @param key Name of the option to get
	 * @return Value of the requested option
	 */
    public static String getOption(final String key) {
        if (DBoperations.options.size() == 0) {
            try {
                final Statement s = DBoperations.db.connection.createStatement();
                final ResultSet rs = s.executeQuery("SELECT value, name FROM " + DerbyDB.schemaName + ".Options");
                while (rs.next()) DBoperations.options.put(rs.getString("name"), rs.getString("value"));
                DBoperations.db.connection.commit();
                s.close();
            } catch (final SQLException e) {
                System.out.println("Couldn't get Option");
            }
        }
        if (DBoperations.options.containsKey(key)) return DBoperations.options.get(key); else return null;
    }

    /**
	 * store an option for the system in the database
	 * 
	 * @param key name of the option
	 * @param value value of the option
	 */
    public static void setOption(final String key, final String value) {
        if (!DBoperations.options.containsKey(key) || DBoperations.options.get(key) != value) {
            try {
                final Statement s = DBoperations.db.connection.createStatement();
                s.executeUpdate("INSERT INTO " + DerbyDB.schemaName + ".Options (name, value)" + " VALUES ('" + key + "','" + value + "')");
                DBoperations.db.connection.commit();
                s.close();
            } catch (final SQLException e) {
                try {
                    Statement s = DBoperations.db.connection.createStatement();
                    s.executeUpdate("UPDATE " + DerbyDB.schemaName + ".Options SET value='" + value + "' WHERE name='" + key + "'");
                    DBoperations.db.connection.commit();
                    s.close();
                } catch (SQLException e1) {
                    System.out.println("Couldn't save Option");
                }
            }
        }
    }

    /**
	 * set the path where the database files are supposed to be stored. This is
	 * especially important for WARs
	 * 
	 * @param path Path where the DB is stored
	 */
    public static void setDbPath(String path) {
        if (!path.endsWith(File.separator)) path += File.separator;
        DerbyDB.dbPath = path;
    }

    /**
	 * Converts a standard string into an encrypted String to be used with the
	 * password in the database
	 * 
	 * @param s String to be converted
	 * @return Resulting MD5 string
	 */
    public static String encrypt(final String s) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] messageDigest = md.digest(s.getBytes());
            final BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (final NoSuchAlgorithmException e) {
            System.out.println("Couldn't find MD5 algorithm");
        }
        return null;
    }

    /**
	 * use this to clean all unlinked relations and do delete all historic
	 * profile entries
	 */
    public static void cleanUp() {
        try {
            final Statement s = DBoperations.db.connection.createStatement();
            s.executeUpdate("DELETE FROM " + DerbyDB.schemaName + ".User2Profile" + " WHERE userID=NULL OR profileID=NULL");
            s.executeUpdate("DELETE FROM " + DerbyDB.schemaName + ".Profiles" + " WHERE profileID NOT IN (SELECT profileID from " + DerbyDB.schemaName + ".User2Profiles)");
            DBoperations.db.connection.commit();
            s.close();
        } catch (final SQLException e) {
            System.out.println("Database clean up failed");
        }
    }
}
