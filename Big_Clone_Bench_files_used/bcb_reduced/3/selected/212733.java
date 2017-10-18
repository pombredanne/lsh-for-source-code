package ba_leipzig_lending_and_service_control_system.conroller;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Controller-Class that handles the database connectivity
 *
 * @author Chris Hagen
 */
public class ctrlDatabase {

    /**
     * Establishes a connection to the server and sets the internal flags
     * for the username and the users rights.
     *
     * @param username the username
     * @param password the password
     * @param serverurl the server's URL
     * @param serverport the server's Port
     * @param sericename the server's Oracle-Service-Name on the server machine
     * @return the connection
     */
    public static Connection establishConnection(String username, String password, String serverurl, String serverport, String servicename) throws Exception {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Properties cp = new Properties();
        cp.put("user", "LASCS");
        cp.put("password", "LASCS");
        Connection c = DriverManager.getConnection("jdbc:oracle:thin:@" + serverurl + ":" + serverport + ":" + servicename, cp);
        String pwd = String.valueOf(password);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(pwd.getBytes());
        pwd = Base64.encode(digest);
        String strPWD = dlookup(c, "LAUPWD", "LAUSER", "LAUSTAT='A' AND LAUID='" + username + "'");
        if (strPWD.replaceAll("\n", "").replaceAll(" ", "").equals(pwd.replaceAll("\n", "").replaceAll(" ", ""))) {
            ctrlMain.setRight(Integer.parseInt(dlookup(c, "LAURIGHT", "LAUSER", "LAUID='" + username + "'")));
            ctrlMain.setUser(username);
            return c;
        } else return null;
    }

    /**
     * Counts the the number of recordsets of a sql select.
     *
     * @param con database connection
     * @param tableName table name
     * @param whereClause SQL-Where-Clause to filter the rows
     * @return record count
     */
    public static int dcount(Connection con, String tableName, String whereClause) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) AS WERT" + " FROM LASCS." + tableName.trim() + (whereClause.trim().length() > 0 ? " WHERE " + whereClause : ""));
            if (rs.next()) return rs.getInt(1); else return -1;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Gets a new number for a specific column
     *
     * @param con database connection
     * @param colName column name
     * @param tableName table name
     * @param whereClause SQL-Where-Clause to filter the rows
     * @return new number
     */
    public static int getNewNr(Connection con, String colName, String tableName, String whereClause) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        int res = 0;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SELECT MAX(" + colName + ") AS WERT" + " FROM LASCS." + tableName.trim() + " WHERE " + whereClause);
            if (rs.next()) res = rs.getInt(1);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
        return res + 1;
    }

    /**
     * Gets a new number for a specific column
     *
     * @param con database connection
     * @param colName column name
     * @param tableName table name
     * @return new number
     */
    public static int getNewNr(Connection con, String colName, String tableName) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        int res = 0;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SELECT MAX(" + colName + ") AS WERT" + " FROM LASCS." + tableName.trim());
            if (rs.next()) res = rs.getInt(1);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
        return res + 1;
    }

    /**
     * Gets the value of a database field.
     *
     * @param con database connection
     * @param columnName columnName of the field
     * @param tableName tableName of the specific column
     * @param whereClause SQL-Where-Clause to filter the field
     * @return value
     */
    public static String dlookup(Connection con, String columnName, String tableName, String whereClause) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SELECT " + columnName.trim() + " FROM LASCS." + tableName.trim() + " WHERE " + whereClause);
            if (rs.next()) return rs.getString(1); else return null;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Gets the values of a ResultSet-Object.
     *
     * @param con database connection
     * @param columnName columnName of the fields
     * @param tableName tableName of the specific columns
     * @param whereClause SQL-Where-Clause to filter the fields
     * @return ArrayList with all filtered values
     */
    public static ArrayList getResultSet(Connection con, String columnName, String tableName, String whereClause) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery("SELECT " + columnName.trim() + " FROM LASCS." + tableName.trim() + (whereClause.trim().length() > 0 ? " WHERE " + whereClause : ""));
            ArrayList rows = new ArrayList();
            while (rs.next()) {
                ArrayList row = new ArrayList();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.add(rs.getString(i));
                }
                rows.add(row);
            }
            return rows;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Executes a specific query on the database connection.
     *
     * @param con database connection
     * @param s query string
     */
    public static void executeQuery(Connection con, String s) throws Exception {
        Statement stat = null;
        try {
            stat = con.createStatement();
            stat.execute(s);
        } finally {
            if (stat != null) stat.close();
        }
    }
}
