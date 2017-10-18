package org.comoro.ext.login;

import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.sql.DataSource;
import net.tasecurity.taslib.util.RandomString;
import org.comoro.dao.AbstractDAO;

/**
 * Reference implementation helper class for simple logins.
 */
public final class ReferenceLoginManager extends AbstractDAO implements LoginManager {

    private static final int MAXIMUM_LOGINS = Integer.MAX_VALUE;

    private static final Logger log = Logger.getLogger(ReferenceLoginManager.class.getPackage().getName());

    private static ReferenceLoginManager instance;

    /**
     * Initializes dao with provided data source.
     * Non concurrent method, be aware.
     */
    public static void init(final DataSource pool) {
        instance = new ReferenceLoginManager(pool);
        instance.testVersion(pool, "$Id: ReferenceLoginManager.java,v 1.12 2009/07/27 11:55:03 pierluiggi Exp $");
    }

    /**
     * Gets the only instance of this class.
     * @throws RuntimeException if <code>init</code> method has not been called
     * yet.
     */
    public static ReferenceLoginManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Persistence not configured yet!");
        } else return instance;
    }

    private final DataSource pool;

    private final MessageDigest messageDigest;

    private final RandomString randomString;

    private final Random randomLength;

    private ReferenceLoginManager(final DataSource pool) {
        try {
            this.pool = pool;
            this.messageDigest = MessageDigest.getInstance("SHA");
            this.randomString = new RandomString();
            this.randomLength = new Random();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    public LoginAnswer login(final String login, final String password) {
        if (login == null || password == null) return new LoginAnswer.Denied();
        try {
            Connection conn = null;
            try {
                conn = pool.getConnection();
                final PreparedStatement select = conn.prepareStatement("select password, enabled, remainingUses from " + "passwords where userId=?");
                select.setString(1, login);
                final ResultSet rs = select.executeQuery();
                try {
                    if (rs.next()) {
                        final String pwd = rs.getString(1);
                        final boolean enabled = rs.getBoolean(2);
                        final int remainingUses = rs.getInt(3);
                        if (!enabled) return new LoginAnswer.Denied();
                        if (remainingUses == 0) {
                            disable(login);
                        }
                        decrementRemainingUses(login, remainingUses);
                        if (pwd.equals(sha1sum(password))) {
                            return new LoginAnswer.Accepted(1.0f, remainingUses);
                        } else {
                            return new LoginAnswer.Denied();
                        }
                    } else return new LoginAnswer.Denied();
                } finally {
                    if (rs != null) rs.close();
                    if (select != null) select.close();
                }
            } finally {
                if (conn != null) conn.close();
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error login user " + login, sqle);
            return new LoginAnswer.Error(sqle);
        }
    }

    public LoginAnswer login(final String login, final String password, final byte[] biometric) {
        return this.login(login, password);
    }

    public String createUser(final String login) {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final String newPassword = randomString.nextString(randomLength.nextInt(10) + 10);
                final PreparedStatement insert = conn.prepareStatement("insert into passwords " + "(userId, password, enabled, remainingUses) " + "values (?,?,?,?)");
                try {
                    insert.setString(1, login);
                    insert.setString(2, sha1sum(newPassword));
                    insert.setBoolean(3, true);
                    insert.setInt(4, 1);
                    insert.executeUpdate();
                    return newPassword;
                } finally {
                    if (insert != null) insert.close();
                }
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error creating user " + login, sqle);
            throw new RuntimeException(sqle);
        }
    }

    public void deleteUser(final String login) {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final PreparedStatement del = conn.prepareStatement("delete from passwords where userId=?");
                final PreparedStatement del2 = conn.prepareStatement("delete from userRoles where userId=?");
                try {
                    del.setString(1, login);
                    del2.setString(1, login);
                    del.executeUpdate();
                    del2.executeUpdate();
                } finally {
                    if (del != null) del.close();
                    if (del2 != null) del2.close();
                }
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error deleting user " + login, sqle);
            throw new RuntimeException(sqle);
        }
    }

    public boolean changePassword(final String login, final String oldPassword, final String newPassword) {
        if (login == null || oldPassword == null || newPassword == null) return false;
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final PreparedStatement update = conn.prepareStatement("update passwords set password=?, enabled=?, " + "remainingUses=? where userId=? and password=?");
                try {
                    update.setString(1, sha1sum(newPassword));
                    update.setBoolean(2, true);
                    update.setInt(3, MAXIMUM_LOGINS);
                    update.setString(4, login);
                    update.setString(5, sha1sum(oldPassword));
                    return update.executeUpdate() == 1;
                } finally {
                    if (update != null) update.close();
                }
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error changing password for user " + login, sqle);
            return false;
        }
    }

    public String resetPassword(final String login) {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final String newPassword = randomString.nextString(6);
                log.finest("Assigned new random password " + newPassword);
                final PreparedStatement update = conn.prepareStatement("update passwords set password=?, enabled=?, " + "remainingUses=? where userId=?");
                try {
                    update.setString(1, sha1sum(newPassword));
                    update.setBoolean(2, true);
                    update.setInt(3, 1);
                    update.setString(4, login);
                    update.executeUpdate();
                    return newPassword;
                } finally {
                    if (update != null) update.close();
                }
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error changing password for user " + login, sqle);
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Disables the user.
     */
    private void disable(final String login) {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final PreparedStatement update = conn.prepareStatement("update passwords set enabled=? " + "where userId=?");
                try {
                    update.setBoolean(1, false);
                    update.setString(2, login);
                    update.executeUpdate();
                } finally {
                    if (update != null) update.close();
                }
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error disbaling user " + login, sqle);
        }
    }

    /**
     * Disables the user.
     */
    private void decrementRemainingUses(final String login, final int remainingUses) {
        try {
            Connection conn = null;
            boolean autoCommit = false;
            try {
                conn = pool.getConnection();
                autoCommit = conn.getAutoCommit();
                conn.setAutoCommit(true);
                final PreparedStatement update = conn.prepareStatement("update passwords set remainingUses=? " + "where userId=? and remainingUses=?");
                try {
                    update.setInt(1, remainingUses - 1);
                    update.setString(2, login);
                    update.setInt(3, remainingUses);
                    update.executeUpdate();
                } finally {
                    if (update != null) update.close();
                }
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                }
            }
        } catch (SQLException sqle) {
            log.log(Level.SEVERE, "Error decrementing remaining uses for user " + login, sqle);
        }
    }

    /**
     * SHA1SSUM
     */
    private synchronized String sha1sum(final String text) {
        final byte[] hash = this.messageDigest.digest(text.getBytes());
        this.messageDigest.reset();
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            final String s = Integer.toHexString(hash[i] & 0xFF);
            if (s.length() == 1) sb.append("0");
            sb.append(s);
        }
        return sb.toString();
    }
}
