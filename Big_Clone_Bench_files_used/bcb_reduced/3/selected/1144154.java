package symore.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

/**
 * HashDBState implements a method to create a 
 * hashvalue out of the current state of the 
 * database. This value can then be used to 
 * compare different databases.
 * (see "Compare-by-Hash" by Val Henson)
 * 
 * @author Sebastian Martens, Manuel Scholz
 */
public class HashDBState {

    private static Logger m_oLogger = Logger.getLogger("symore.util.HashDBState");

    private static String[] m_aTableType = { "TABLE" };

    /**
	 * Creates a string using the SHA1 hash algorithm from the 
	 * passed symore data source.
	 * 
	 * @param dbConnection	Connection object to access the database for creating a hashvalue 
	 * @return	string containing hash value
	 * @throws Exception thrown if something went wrong during creation of hash value
	 */
    public static String CreateHashValue(Connection dbConnection) throws Exception {
        String sResult = null;
        if (dbConnection == null) throw new Exception("Connection object is null...");
        HashDBState.m_oLogger.debug("Start creating hash from database object.");
        ArrayList aStatementList = new ArrayList();
        StringBuffer aResults = new StringBuffer();
        StringBuffer aHashValue = new StringBuffer();
        HashDBState.fillStatementList(aStatementList, dbConnection);
        if ((aStatementList != null) && (aStatementList.size() > 0)) {
            Iterator it = aStatementList.iterator();
            while (it.hasNext()) {
                String sCurStatement = (String) it.next();
                HashDBState.addTableContent(aResults, sCurStatement, dbConnection);
            }
        } else {
            throw new Exception("No statements generated... aborting hashing");
        }
        try {
            dbConnection.commit();
        } catch (SQLException e) {
            m_oLogger.error("Error while committing hash statements: " + e.getMessage(), e);
        }
        MessageDigest oSHA1 = MessageDigest.getInstance("SHA-1");
        oSHA1.reset();
        oSHA1.update(aResults.toString().getBytes());
        byte[] aMessageBytes = oSHA1.digest();
        if ((aMessageBytes != null) && (aMessageBytes.length > 0)) {
            for (int iCurByte = 0; iCurByte < aMessageBytes.length; iCurByte++) {
                aHashValue.append(HashDBState.toHexString(aMessageBytes[iCurByte]));
            }
        }
        sResult = aHashValue.toString();
        HashDBState.m_oLogger.debug("Hashvalue of database is: " + sResult);
        return sResult;
    }

    /**
	 * Uses the passed statement to obtain the table values and add
	 * these to the passed StringBuffer.
	 * 
	 * @param oBuffer		Buffer to add the result to
	 * @param sStatement	Statement to execute for receiving table content
	 * @param dbConnection	Connection to the database
	 */
    private static void addTableContent(StringBuffer oBuffer, String sStatement, Connection dbConnection) throws Exception {
        if ((oBuffer == null) || (sStatement == null) || (dbConnection == null)) throw new Exception("Passed arguments are not correct...");
        ResultSet dbResult = null;
        try {
            Statement dbStatement = dbConnection.createStatement();
            dbResult = dbStatement.executeQuery(sStatement);
            if (dbResult != null) {
                while (dbResult.next()) {
                    int iColumns = dbResult.getMetaData().getColumnCount();
                    for (int iCurColumn = 1; iCurColumn <= iColumns; iCurColumn++) {
                        Object oCurObject = dbResult.getObject(iCurColumn);
                        if (oCurObject == null) {
                            oBuffer.append(" null ");
                        } else {
                            oBuffer.append(" ");
                            oBuffer.append(oCurObject.toString());
                            oBuffer.append(" ");
                        }
                    }
                    oBuffer.append("\r\n");
                }
            }
        } catch (SQLException e) {
            HashDBState.m_oLogger.error("Error while executing statement: " + e.getMessage());
        } finally {
            if (dbResult != null) dbResult.close();
        }
    }

    /**
	 * This method will fill the passed list with statements,
	 * that will be used later to obtain values from the tables.
	 * 
	 * @param dbConnection		- connection that will be used to obtain schema information
	 * @param aStatementList	- list to which the statements should be added
	 */
    private static void fillStatementList(ArrayList aStatementList, Connection dbConnection) throws Exception {
        ResultSet dbTables = dbConnection.getMetaData().getTables(null, "APP", "%", HashDBState.m_aTableType);
        if (dbTables != null) {
            while (dbTables.next()) {
                String sCurTable = dbTables.getString(3);
                HashDBState.m_oLogger.debug("Table found: " + sCurTable);
                StringBuffer oCurStatement = new StringBuffer();
                boolean bFirst = true;
                ResultSet dbColumns = dbConnection.getMetaData().getColumns(null, "APP", sCurTable, "%");
                if (dbColumns != null) {
                    oCurStatement.append("SELECT ");
                    while (dbColumns.next()) {
                        if (!bFirst) {
                            oCurStatement.append(", ");
                        }
                        oCurStatement.append(dbColumns.getString(4));
                        bFirst = false;
                    }
                    oCurStatement.append(" FROM APP.");
                    oCurStatement.append(sCurTable);
                    oCurStatement.append(" ORDER BY ROWID");
                }
                if (oCurStatement.length() > 0) {
                    aStatementList.add(oCurStatement.toString());
                    HashDBState.m_oLogger.debug("New statement: " + oCurStatement.toString());
                }
            }
        }
    }

    private static String toHexString(byte b) {
        int value = (b & 0x7F) + (b < 0 ? 128 : 0);
        String ret = (value < 16 ? "0" : "");
        ret += Integer.toHexString(value).toUpperCase();
        return ret;
    }
}
