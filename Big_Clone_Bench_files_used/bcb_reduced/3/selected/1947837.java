package net.sf.codechanges.transform;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import net.sf.codechanges.parse.Parser;
import org.apache.commons.codec.binary.Hex;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public abstract class DBTransform extends Transform {

    private static final String PROP_DB_URL = "db.url";

    private static final String PROP_DB_USER = "db.username";

    private static final String PROP_DB_PASSWORD = "db.password";

    private DataSource dataSource;

    private MessageDigest messageDigestSHA1;

    private Hex hex;

    public DBTransform(Properties props) throws Exception {
        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setURL(props.getProperty(PROP_DB_URL));
        dataSource.setUser(props.getProperty(PROP_DB_USER));
        dataSource.setPassword(props.getProperty(PROP_DB_PASSWORD));
        this.dataSource = dataSource;
        messageDigestSHA1 = MessageDigest.getInstance("SHA-1");
        hex = new Hex();
    }

    public void init() throws Exception {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = getConnection();
            stmt = connection.createStatement();
            stmt.execute("create table if not exists links (hash varchar(40) not null," + " repoid varchar(255) not null," + " issue varchar(255) not null," + " primary key (hash, issue)," + " index (hash))");
            stmt.execute("create table if not exists changes (hash varchar(40) not null, " + " repoid varchar(255) not null," + " timestamp timestamp not null," + " author varchar(255) not null," + " description text not null," + " branch varchar(255) not null," + " primary key (hash, repoid)," + " index (hash))");
            stmt.execute("create table if not exists repos (id varchar(255) not null, change_diff_url text not null, file_diff_url text not null," + " primary key (id)," + " index (id))");
            stmt.execute("create table if not exists files (filehash varchar(40) not null, path text not null," + " primary key (filehash)," + " index (filehash))");
            stmt.execute("create table if not exists changedfiles (hash varchar(40) not null," + " filehash varchar(40) not null," + " repoid varchar(255) not null," + " primary key (hash, filehash, repoid)," + " index (hash))");
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
        initDB();
    }

    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected void addChangeset(String hash, String repoID, long timestamp, String author, String comment, String branch) throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("insert ignore into changes (hash, repoid, timestamp, author, description, branch) values (?, ?, ?, ?, ?, ?)");
            stmt.setString(1, hash);
            stmt.setString(2, repoID);
            stmt.setTimestamp(3, new Timestamp(timestamp));
            stmt.setString(4, author);
            stmt.setString(5, comment);
            stmt.setString(6, branch);
            stmt.executeUpdate();
        } finally {
            if (conn != null) {
                conn.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    protected void addExternalIDs(String hash, String repoID, List<String> externalIDs) throws Exception {
        if (externalIDs == null) {
            return;
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("insert ignore into links (hash, repoid, issue) values (?, ?, ?)");
            for (String externalID : externalIDs) {
                stmt.setString(1, hash);
                stmt.setString(2, repoID);
                stmt.setString(3, externalID);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            if (conn != null) {
                conn.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    protected void addChangedFiles(String hash, String repoID, List<String> changedFiles) throws Exception {
        if (changedFiles == null) {
            return;
        }
        Connection conn = null;
        PreparedStatement filesStmt = null;
        PreparedStatement changedFilesStmt = null;
        try {
            conn = getConnection();
            filesStmt = conn.prepareStatement("insert ignore into files (filehash, path) values (?, ?)");
            changedFilesStmt = conn.prepareStatement("insert ignore into changedfiles (hash, filehash, repoid) values (?, ?, ?)");
            for (String changedFile : changedFiles) {
                String fileHash = new String(hex.encode(messageDigestSHA1.digest(changedFile.getBytes())));
                filesStmt.setString(1, fileHash);
                filesStmt.setString(2, changedFile);
                filesStmt.addBatch();
                changedFilesStmt.setString(1, hash);
                changedFilesStmt.setString(2, fileHash);
                changedFilesStmt.setString(3, repoID);
                changedFilesStmt.addBatch();
            }
            filesStmt.executeBatch();
            changedFilesStmt.executeBatch();
        } finally {
            if (conn != null) {
                conn.close();
            }
            if (filesStmt != null) {
                filesStmt.close();
            }
            if (changedFilesStmt != null) {
                changedFilesStmt.close();
            }
        }
    }

    protected abstract void initDB() throws Exception;

    public abstract void processData(Parser parser) throws Exception;
}
