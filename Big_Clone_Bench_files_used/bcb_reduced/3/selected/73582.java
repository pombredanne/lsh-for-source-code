package backuper;

import java.io.*;
import java.security.*;
import java.sql.*;
import java.util.Properties;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import java.sql.Timestamp;
import java.util.ArrayList;
import javax.sql.rowset.CachedRowSet;

/**
 *
 * @author Maciek
 */
public class CServerDataBaseConnector {

    private static Connection DBconnection;

    private static Statement DBstatement;

    private static CServerDataBaseConnector instance;

    private static void saveFile(CFileInfo fileInfo, String serverFileName) {
        try {
            File outputFile = new File(fileInfo.getUserName() + "/" + serverFileName);
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(fileInfo.getFile());
            fos.close();
            System.out.println("file: " + fileInfo.getFileName() + " saved as: " + serverFileName);
        } catch (IOException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static File getFile(String filename, String filepath, String login, int version) {
        String dbFilename = getDBFileName(filename, filepath, login, version);
        File fileToGet = new File(login + "/" + dbFilename);
        System.out.println("file: " + dbFilename + " ready to send");
        return fileToGet;
    }

    private CServerDataBaseConnector() {
        try {
            DBconnection = getConnection();
            DBstatement = DBconnection.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
  *
  * @return
  */
    public static CServerDataBaseConnector getInstance() {
        if (instance == null) {
            instance = new CServerDataBaseConnector();
        }
        return instance;
    }

    /**
    *
    * @Override clone nadpisanie
    */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
 * tymczasowa metoda do testowania połączania z bazą
 *
  * @param args
  * @throws NoSuchAlgorithmException
  */
    public static void main(String[] args) throws NoSuchAlgorithmException {
        @SuppressWarnings("static-access") CServerDataBaseConnector csdbc = CServerDataBaseConnector.getInstance();
        long fs = new Long(10202);
        long md = new Long(28942);
    }

    /**
 * @return zwraca zestawione polaczenie do bazy danych.
 *
 * Zaczytuje informacje o sterownikow z pliku database.properties
 * @throws SQLException
 * @throws IOException
 */
    public static Connection getConnection() throws SQLException, IOException {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream("database.properties");
        props.load(in);
        System.out.println("wczytano plik database.properties");
        in.close();
        String drivers = props.getProperty("jdbc.drivers");
        if (drivers != null) System.setProperty("jdbc.drivers", drivers);
        String url = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");
        return DriverManager.getConnection(url, username, password);
    }

    /**
 *
 * @return zwraca aktualny timestamp
 */
    public static Timestamp getDate() {
        Date now = new Date();
        Timestamp ts = new Timestamp(now.getTime());
        return ts;
    }

    /**
 *
 * @TODO: zrobić okienkowe wyświetlanie rekordów z users albo files
 */
    public static void ShowResultSet(Statement stat) throws SQLException {
        ResultSet result = stat.getResultSet();
        ResultSetMetaData metaData = result.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) System.out.print(", ");
            System.out.print(metaData.getColumnLabel(i));
        }
        System.out.println();
        while (result.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(result.getString(i) + ", ");
            }
            System.out.println();
        }
        result.close();
    }

    /**
 * @param clientFileName
 * @param clientFilePath
 * @param un
 * @return zwraca przerobiona nazwe pliku z ktora zapiszemy plik na dysku
 */
    public static String createServerFileName(String clientFileName, String clientFilePath, String un) {
        String fname;
        int ver = 0;
        if (getFileVersion(clientFileName, clientFilePath, un) == 0) ver = 1; else ver = getFileVersion(clientFileName, clientFilePath, un) + 1;
        try {
            fname = clientFileName + hash(clientFilePath) + "v" + ver;
            return fname;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    /**
 *
 * @param clientFileName
 * @param cilentFilePath
 * @param userName
 * @return zwraca wersje pliku ktora znajduje sie w bazie, default =0
 */
    public static int getFileVersion(String clientFileName, String cilentFilePath, String userName) {
        int ver = 0;
        try {
            int userID = getUserID(userName);
            String query = "SELECT max(file_version) FROM files WHERE  client_file_name ='" + clientFileName + "' and client_file_path='" + cilentFilePath + "' and user_id=" + userID;
            DBstatement.execute(query);
            ResultSet result = DBstatement.getResultSet();
            while (result.next()) {
                ver = result.getInt(1);
            }
            return ver;
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ver;
    }

    /**
 *
 * @param clientFileName
 * @param cilentFilePath
 * @param un
 * @return zwraca nazwe pliku ktora jest zapisana w bazie
 */
    public static String getDBFileName(String clientFileName, String cilentFilePath, String un, int version) {
        try {
            String fname = "DEFAULT_FILENAME";
            String query = "Select server_file_name FROM files WHERE client_file_name ='" + clientFileName + "' and client_file_path='" + cilentFilePath + "' and file_version=" + version + "";
            DBstatement.execute(query);
            ResultSet result = DBstatement.getResultSet();
            while (result.next()) {
                fname = result.getString("server_file_name");
            }
            return fname;
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "ERROR in getDBFileName !!!! ";
    }

    /**
 *
 * @param un
 * @return zwraca liste wszystkich plików w bazie w postaci tablicy typu CFileInfo
 * @throws SQLException
 */
    public static ArrayList getAllFilesInfo(String un) {
        ArrayList lst = new ArrayList();
        try {
            int userID = getUserID(un);
            String query = "SELECT * FROM files WHERE user_id=" + getUserID(un);
            ResultSet result = DBstatement.executeQuery(query);
            CachedRowSet crs = new com.sun.rowset.CachedRowSetImpl();
            crs.populate(result);
            while (crs.next()) {
                String fileName = crs.getString("client_file_name");
                String filePath = crs.getString("client_file_path");
                long modificationDate = (crs.getTimestamp("file_modification_date")).getTime();
                String fileHash = crs.getString("file_hash");
                long fileSize = crs.getLong("file_size");
                int fileVersion = crs.getInt("FILE_VERSION");
                String userName = getUserName(crs.getInt("user_id"));
                String userPass = null;
                CFileInfo cfi = new CFileInfo(fileName, filePath, fileHash, userName, userPass, modificationDate, fileSize, fileVersion);
                lst.add(cfi);
            }
            System.out.println("METODA getAllFilesInfo(" + un + ") ---- wyeksportowano pliki do tablicy");
            return lst;
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lst;
    }

    /**
 *
 * @param username
 * @param pass
 * @return zwraca true jezeli wstawienie do tabeli sie powiedzie
 */
    public static boolean insertUser(String username, String pass) {
        String query = "INSERT INTO users ( user_name, user_password, user_creation_date) VALUES ('" + username + "','" + pass + "','" + getDate() + "')";
        try {
            DBstatement.execute(query);
            System.out.println("user: " + username + " wstawiony");
            new File(username).mkdir();
            return true;
        } catch (Exception ex) {
            System.out.println("nie wstawiono usera " + username + " ----ERROR-----  " + ex.getMessage());
            return false;
        }
    }

    /**
 *
 * @param cfi
 * @return
 */
    public static boolean insertFile(CFileInfo cfi) {
        try {
            String sFileName = createServerFileName(cfi.getFileName(), cfi.getFilePath(), cfi.getUserName());
            String sFilePath = cfi.getFilePath();
            String cFileName = cfi.getFileName();
            String cFilePath = cfi.getFilePath();
            String userName = cfi.getUserName();
            long fileSize = cfi.getFileSize();
            String fileHash = cfi.getFileHash();
            Timestamp fileModificationDate = cfi.getModificationDate();
            Timestamp fileUploadDate = getDate();
            int userID = getUserID(userName);
            int ver = 0;
            if (getFileVersion(cFileName, cFilePath, userName) == 0) {
                ver = 1;
            } else {
                ver = getFileVersion(cFileName, cFilePath, userName) + 1;
            }
            String query = "INSERT INTO files (server_file_name, server_file_path, client_file_name, client_file_path, file_size, file_hash, file_modification_date, file_upload_date, user_id, file_version) " + "                VALUES ('" + sFileName + "','" + sFilePath + "','" + cFileName + "','" + cFilePath + "'," + fileSize + ",'" + fileHash + "','" + fileModificationDate + "','" + fileUploadDate + "'," + userID + "," + ver + ")";
            try {
                DBstatement.execute(query);
                System.out.println("file: " + cFileName + " wstawiony");
                saveFile(cfi, sFileName);
                return true;
            } catch (Exception ex) {
                System.out.println("nie wstawiono  pliku " + cFileName + " ----ERROR-----  " + ex.getMessage());
                ex.printStackTrace();
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
 *
 * @param clientFileName
 * @param cilentFilePath
 * @param un
 * @return sprawdza czy istnieje dany plik w bazie
 */
    public static boolean checkExistence(String clientFileName, String cilentFilePath, String un) {
        try {
            int userID = getUserID(un);
            String query = "Select count(*) from Files where client_file_name ='" + clientFileName + "' and client_file_path='" + cilentFilePath + "' and user_id = " + userID;
            DBstatement.execute(query);
            ResultSet result = DBstatement.getResultSet();
            while (result.next()) {
                if (result.getInt(1) > 0) {
                    System.out.println("user " + un + " exists");
                    return true;
                } else {
                    System.out.println("user " + un + " does not exist");
                    return false;
                }
            }
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
 * metoda do ustawienie ID usera tak aby w zbierac odpowiednie rekordy z bazy
 * @param un
 * @throws SQLException
 */
    private static int getUserID(String un) throws SQLException {
        String query = "Select user_id from users where user_name ='" + un + "'";
        System.out.println(un);
        DBstatement.execute(query);
        ResultSet result = DBstatement.getResultSet();
        int userID = 0;
        while (result.next()) {
            userID = result.getInt("user_id");
            System.out.println("userID -1- = " + userID);
        }
        System.out.println("userID= " + userID);
        return userID;
    }

    /**
 *
 * @param uid
 * @return zwraca nazwe usera na podstawie jego id
 * @throws SQLException
 */
    public static String getUserName(int uid) throws SQLException {
        String query = "Select user_name from users where user_id =" + uid + "";
        String un = "DEFAULT USER NAME";
        DBstatement.execute(query);
        ResultSet result = DBstatement.getResultSet();
        while (result.next()) {
            un = result.getString("USER_NAME");
        }
        return un;
    }

    /**
 *
 * @param un
 * @param pass
 * @return zwraca true jezeli user istnieje w bazie/ potrzebne przy logowaniu sie usera
 */
    public static boolean checkUser(String un, String pass) {
        String query = "Select count(*) from users where user_name='" + un + "' and user_password='" + pass + "'";
        boolean status = false;
        try {
            DBstatement.execute(query);
            ResultSet result = DBstatement.getResultSet();
            while (result.next()) {
                if (result.getInt(1) == 1) {
                    status = true;
                } else {
                    status = false;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(CServerDataBaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return status;
    }

    public static void removeFile(String fileName, String filePath, int fileVersion, String user) throws SQLException {
        int userID = getUserID(user);
        String dbFilename = getDBFileName(fileName, filePath, user, fileVersion);
        File fileToRemove = new File(user + "/" + dbFilename);
        fileToRemove.delete();
        System.out.println("file: " + dbFilename + " removed");
        String query = "DELETE FROM files where client_file_name='" + fileName + "' and client_file_path='" + filePath + "' and file_version=" + fileVersion + " and user_id=" + userID;
        boolean status = false;
        DBstatement.execute(query);
        ResultSet result = DBstatement.getResultSet();
    }

    /**
 * @param arg 
 * @return zwraca skrót stringa
 * @throws NoSuchAlgorithmException
 */
    public static String hash(String arg) throws NoSuchAlgorithmException {
        String input = arg;
        String output;
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(input.getBytes());
        output = Hex.encodeHexString(md.digest());
        return output;
    }
}
