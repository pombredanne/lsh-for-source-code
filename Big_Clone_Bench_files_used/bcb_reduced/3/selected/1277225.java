package jreceiver.server.db;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jreceiver.common.rec.Rec;
import jreceiver.common.rec.RecException;
import jreceiver.common.rec.security.User;
import jreceiver.common.rec.security.UserRec;
import jreceiver.server.util.db.ConnectionPool;
import jreceiver.server.util.db.DatabaseException;
import jreceiver.server.util.db.HelperDB;

/**
 * user table routines for JReceiver database
 *
 * @author Reed Esau
 * @version $Revision: 1.4 $ $Date: 2003/04/27 23:14:30 $
 */
public class UserDB extends UniqueKeyDB {

    private static final String DEFAULT_PASSWORD_ENCODING = "ISO-8859-1";

    private static final String DEFAULT_PASSWORD_DIGEST = "SHA";

    protected static final String BASE_TABLE = "user";

    public String getBaseTableName() {
        return BASE_TABLE;
    }

    /**
    * this class is implemented as a singleton
    */
    private static UserDB singleton;

    /**
     * obtain an instance of this singleton
     * <p>
     * Note that this uses the questionable DCL pattern (search on
     * DoubleCheckedLockingIsBroken for more info)
     * <p>
     * @return the singleton instance for this JVM
     */
    public static UserDB getInstance() {
        if (singleton == null) {
            synchronized (UserDB.class) {
                if (singleton == null) singleton = new UserDB();
            }
        }
        return singleton;
    }

    protected final int COL_USER_ID = 1;

    protected final int COL_FULL_NAME = 2;

    protected final int COL_ROLE_ID = 3;

    protected static final String[] KEY_COLUMNS = { "user.id" };

    protected String[] getKeyColumns() {
        return KEY_COLUMNS;
    }

    public Object buildKey(ResultSet rs) throws SQLException {
        return rs.getString(COL_USER_ID);
    }

    protected static final String[] REC_COLUMNS = { "id", "fullname", "role_id" };

    public String[] getRecColumns(Hashtable args) {
        return REC_COLUMNS;
    }

    protected Rec buildRec(ResultSet rs, Hashtable args) throws SQLException, DatabaseException, RecException {
        return new UserRec(rs.getString(COL_USER_ID), null, rs.getString(COL_FULL_NAME), rs.getString(COL_ROLE_ID));
    }

    protected final int ST_COL_FULL_NAME = 1;

    protected final int ST_COL_PASSWORD = 2;

    protected final int ST_COL_ROLE_ID = 3;

    protected final int ST_COL_USER_ID = 4;

    protected static final String ST_UPDATE = "UPDATE user SET fullname=? " + ", password=?" + ", role_id=?" + " WHERE id=?";

    protected static final String ST_INSERT = "INSERT INTO user (fullname" + ",password" + ",role_id" + ",id" + ") VALUES (?,?,?,?)";

    protected String getStoreStatement(int stmt_type) {
        switch(stmt_type) {
            case STORE_FIND:
                return null;
            case STORE_UPDATE:
                return ST_UPDATE;
            case STORE_INSERT:
                return ST_INSERT;
            default:
                throw new IllegalArgumentException();
        }
    }

    protected void setStoreParams(int stmt_type, PreparedStatement stmt, Rec rec) throws SQLException {
        UserRec user = (UserRec) rec;
        stmt.setString(ST_COL_FULL_NAME, user.getFullName());
        stmt.setString(ST_COL_ROLE_ID, user.getRoleId());
        stmt.setString(ST_COL_USER_ID, user.getId());
        stmt.setString(ST_COL_PASSWORD, user.getPassword());
    }

    /**
     * storeRec - OVERRIDE
     * <p>
     * Either insert into or update user table.
     */
    protected int storeRec(Connection conn, Rec rec) throws DatabaseException {
        if (conn == null || rec == null) throw new IllegalArgumentException();
        log.debug("storeRec: " + rec);
        try {
            User user = (User) rec;
            String user_id = validateUserId(user.getId());
            String stored_password = getEncodedPassword(conn, user_id);
            String raw_password = user.getPassword();
            boolean is_password_present = raw_password != null && raw_password.trim().length() > 0;
            if (stored_password == null && !is_password_present) throw new DatabaseException("password is required for new user");
            User user_to_store = new UserRec(user.getId(), (is_password_present ? encodePassword(user.getPassword()) : stored_password), user.getFullName(), user.getRoleId());
            return super.storeRec(conn, user_to_store);
        } catch (RecException e) {
            throw new DatabaseException("rec-problem storing user", e);
        }
    }

    /**
     * insert/update a vector of user recs
     */
    protected void storeRecs(Connection conn, Vector recs) throws DatabaseException {
        throw new DatabaseException("not supported");
    }

    /**
     * is the specified user_id/password valid?
     */
    public User getAuthenticUser(String user_id, String password, Hashtable args) throws DatabaseException {
        ConnectionPool pool = null;
        Connection conn = null;
        try {
            pool = ConnectionPool.getInstance();
            conn = pool.getConnection();
            return getAuthenticUser(conn, user_id, password, args);
        } finally {
            if (pool != null) pool.releaseConnection(conn);
        }
    }

    /**
     * return the user rec if credentials are valid
     *
     * TODO: I18N
     */
    protected User getAuthenticUser(Connection conn, String user_id, String password, Hashtable args) throws DatabaseException {
        String t_user_id = validateUserId(user_id);
        String t_password = validatePassword(password);
        String stored_pwd = getEncodedPassword(conn, t_user_id);
        if (stored_pwd == null) throw new DatabaseException("unable to acquire stored password for user id");
        if (!stored_pwd.equals(encodePassword(t_password))) throw new DatabaseException("password mismatch");
        return (User) getRec(conn, "id=" + HelperDB.quote(user_id), args);
    }

    /** validate a user_id */
    private static String validateUserId(String user_id) throws DatabaseException {
        return validateStr(user_id, User.MIN_USER_ID_LEN, "user_id");
    }

    /** validate a password */
    private static String validatePassword(String password) throws DatabaseException {
        return validateStr(password, User.MIN_PASSWORD_LEN, "password");
    }

    /** make sure a string is long enough and consists only of alphanum characters */
    private static String validateStr(String str, int min_len, String descript) throws DatabaseException {
        if (str == null) throw new DatabaseException("no " + descript + " specified");
        String t_str = str.trim();
        int len = t_str.length();
        if (len < min_len) throw new DatabaseException(descript + " must be at least " + min_len + " characters in length");
        for (int i = 0; i < len; i++) if (!Character.isLetterOrDigit(t_str.charAt(i))) throw new DatabaseException(descript + " must consist of letters or digits");
        return t_str;
    }

    /**
     * internal password encoder
     *
     * Example of output: "thw87s+craKYsuOOx5CfNRNlwYU="
     */
    protected static String encodePassword(String raw_password) throws DatabaseException {
        String clean_password = validatePassword(raw_password);
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_PASSWORD_DIGEST);
            md.update(clean_password.getBytes(DEFAULT_PASSWORD_ENCODING));
            String digest = new String(Base64.encodeBase64(md.digest()));
            if (log.isDebugEnabled()) log.debug("encodePassword: digest=" + digest);
            return digest;
        } catch (UnsupportedEncodingException e) {
            throw new DatabaseException("encoding-problem with password", e);
        } catch (NoSuchAlgorithmException e) {
            throw new DatabaseException("digest-problem encoding password", e);
        }
    }

    /**
     * internal retriever for encoded passwords
     *
     * returns NULL if user_id is no longer present.
     */
    private String getEncodedPassword(Connection conn, String user_id) throws DatabaseException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT password FROM user WHERE id=?");
            stmt.setString(1, user_id);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;
            return rs.getString(1);
        } catch (SQLException e) {
            throw new DatabaseException("sql-problem retrieving key count", e);
        } finally {
            HelperDB.safeClose(stmt);
        }
    }

    /**
     * logging object
     */
    protected static Log log = LogFactory.getLog(UserDB.class);
}
