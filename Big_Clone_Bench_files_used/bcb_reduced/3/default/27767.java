import java.sql.*;
import org.pittjug.sql.pool.*;

/**
 *
 * @author  Carl
 */
public class InitialUsers {

    /**
     * Starts the PoolManager
     */
    public static void setUp() {
        System.out.println("In Setup : " + PoolManager.isIntialized());
        try {
            if (!PoolManager.isIntialized()) {
                PoolStartupPropertyFile ps = new PoolStartupPropertyFile();
                ps.init();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Validates this new Member and adds them to the Member Table.
     *  @throws ValidationException if one occurs
     */
    public static void add(String loginName, String password, String memberName, boolean emailAddressVisibleToOthers) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = PoolManager.getConnection();
            String sqlInsert = "Insert INTO member (LoginName, Password, MemberName, EmailAddressVisibleToOthers) VALUES (?, ?, ?, ?)";
            stmt = conn.prepareStatement(sqlInsert);
            stmt.setString(1, loginName);
            stmt.setBytes(2, digestPassword(password));
            stmt.setString(3, memberName);
            stmt.setInt(4, (emailAddressVisibleToOthers) ? 1 : 0);
            stmt.execute();
        } catch (SQLException se) {
            try {
                conn.rollback();
            } catch (SQLException ser) {
            }
            se.printStackTrace();
            throw se;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                    stmt = null;
                }
            } catch (SQLException se) {
            }
            try {
                if (conn != null) {
                    PoolManager.freeConnection(conn);
                    conn = null;
                }
            } catch (SQLException se) {
            }
        }
    }

    /**
     * Digests the Password using MD5 Disgestion.
     */
    public static byte[] digestPassword(String password) {
        byte[] b = password.getBytes();
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            synchronized (digest) {
                digest.reset();
                b = digest.digest(password.getBytes());
            }
        } catch (java.security.NoSuchAlgorithmException nsa) {
        }
        return b;
    }

    public static void main(String[] args) throws Exception {
        setUp();
        add("testuser1", "testuser1", "Test User1", true);
        add("testuser2", "testuser2", "Test User2", false);
        add("carl_trusiak", "catmanme", "Carl Trusiak", true);
    }
}
