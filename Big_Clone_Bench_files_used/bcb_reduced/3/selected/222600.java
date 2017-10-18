package com.incendiaryblue.cmslite.transfer;

import com.incendiaryblue.database.SQLDialect;
import java.sql.*;
import java.util.*;
import java.security.*;

/**
 * Provides functionality used by both XMLImport and XMLExport
 */
class TransferUtils {

    /**
	 * Create a database connection, reading the parameters out of a properties object.
	 */
    public static Connection createConnection(TransferProperties properties) throws TransferException {
        String prefix = "database.";
        String url = properties.getRequiredProperty(prefix + "url");
        String driver = properties.getRequiredProperty(prefix + "driver");
        String username = properties.getRequiredProperty(prefix + "user");
        String password = properties.getProperty(prefix + "password");
        try {
            Class driverClass = Class.forName(driver);
            driverClass.newInstance();
        } catch (Exception e) {
            throw new TransferException("Couldn't initialise database driver: " + driver + "\n(" + e + ")");
        }
        Properties dbprop = new Properties();
        dbprop.setProperty("user", username);
        if (password != null) dbprop.setProperty("password", password);
        try {
            return DriverManager.getConnection(url, dbprop);
        } catch (SQLException e) {
            throw new TransferException("Couldn't connect to database on: " + url + "\n(" + e + ")");
        }
    }

    /**
	 * Creates a hash of a user's password, for comparison with the one stored in the database.
	 *
	 * <p>This method is copied from the com.incendiaryblue.user.User class.  The original method can't be called from here
	 * without setting up an AppConfig object.</p>
	 */
    public static byte[] encryptUserPassword(String sz) {
        MessageDigest oMessageDigest;
        try {
            oMessageDigest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Can't instantiate SHA message digest algorithm");
            e.printStackTrace();
            throw new RuntimeException("Can't instantiate SHA message digest algorithm");
        }
        oMessageDigest.reset();
        return oMessageDigest.digest(sz.getBytes());
    }

    public static SQLDialect getSQLDialect(String className) throws TransferException {
        try {
            Class c = Class.forName(className);
            Object o = c.newInstance();
            return (SQLDialect) o;
        } catch (Exception e) {
            throw new TransferException("Error instantiating SQLDialect class: " + className);
        }
    }
}
