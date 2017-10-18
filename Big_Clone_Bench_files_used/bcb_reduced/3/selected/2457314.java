package fi.mmmtike.tiira.auth;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import sun.misc.BASE64Encoder;
import fi.mmmtike.tiira.database.hsqldb.HsqlDataSource;
import fi.mmmtike.tiira.exception.TiiraException;

/**
 * Out-of-box authentication service that uses local database. 
 * This is an example of how to integrate TiiraDefaultAuthContainer
 * to user services. 
 * <p>
 * NOTE THAT THIS CLASS USES SIMPLE USER / PASSWORD AUTHENTICATION
 * AND HAS NOT BEEN CHECKED FOR VULNERABILITIES. DO NOT USE IN 
 * PRODUCTION IF YOUR APPLICATION NEEDS SECURE DATA. 
 * 
 * @author Tomi Tuomainen
 *
 */
public class TiiraDefaultAuthGate {

    private DataSource dataSource = new HsqlDataSource();

    private JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    private Long sessionTimeout = 60 * 10000l;

    public TiiraDefaultAuthGate() {
    }

    /**
   * Checks existing session.
   * 
   * @param sessionId  currently existing session
   * @return           sessionid, if session is still valid (null if not)
   */
    protected String authenticate(String sessionId) {
        Date refreshed = (Date) this.jdbcTemplate.queryForObject("select refreshed from session where ended is null and id = ?", new Object[] { sessionId }, Timestamp.class);
        Date now = Calendar.getInstance().getTime();
        if ((now.getTime() - refreshed.getTime()) > sessionTimeout) {
            return null;
        } else {
            jdbcTemplate.update("update session set refreshed = ? where id = ?", new Object[] { now, sessionId }, new int[] { Types.TIMESTAMP, Types.CHAR });
            return sessionId;
        }
    }

    /**
   * Attempts to create a new user session.
   * 
   * @param user  
   * @param password
   * @return      session id (if password was valid)
   */
    protected String authenticate(String user, String password) {
        if (!StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            if (passwordCorrect(user, password)) {
                return createNewSession(user);
            }
        }
        return null;
    }

    /**
   * Creates a new session for authenticated user.
   * @param user
   * @return  session id
   */
    protected String createNewSession(String user) {
        String sessionId = jdbcTemplate.queryForLong("call next value for session_seq") + "";
        jdbcTemplate.update("insert into session(id, user_id, refreshed) values (?, ?, ?)", new Object[] { sessionId, user, Calendar.getInstance().getTime() }, new int[] { Types.CHAR, Types.CHAR, Types.TIMESTAMP });
        return sessionId;
    }

    /**
   * 
   * @param sessionId
   * @return    user name holding currently the sessionId
   */
    protected String getUser(String sessionId) {
        String user = (String) jdbcTemplate.queryForObject("select user_id from session where id = ?", new Object[] { sessionId }, String.class);
        return user;
    }

    /**
   * Changes user password. (assuming that password has been already checked)
   * 
   * @param user
   * @param newPassword
   */
    protected void changePassword(String user, String newPassword) {
        String encrypted = encrypt(newPassword);
        jdbcTemplate.update("update user set password = ? where id = ?", new Object[] { encrypted, user });
    }

    /**
   * 
   * @param user
   * @param password
   * @return  if password was correct
   */
    protected boolean passwordCorrect(String user, String password) {
        String encrypted = encrypt(password);
        int count = this.jdbcTemplate.queryForInt("select count(1) from user where id = ? and password = ?", new Object[] { user, encrypted });
        return count == 1;
    }

    /**
   * 
   * @param sessionId session to be ended
   */
    protected void logOut(String sessionId) {
        jdbcTemplate.update("update session set ended = ? where id = ?", new Object[] { Calendar.getInstance().getTime(), sessionId });
    }

    /**
   * 
   * @param dataSource  authentication db data source
   */
    protected void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate.setDataSource(dataSource);
    }

    /**
   * @param text  
   * @return  encrypted text
   */
    protected String encrypt(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(text.getBytes("UTF-8"));
            byte raw[] = md.digest();
            String hash = (new BASE64Encoder()).encode(raw);
            return hash;
        } catch (Exception ex) {
            throw new TiiraException(ex);
        }
    }

    /**
   * 
   * @param sessionId
   * @return   user role names
   */
    @SuppressWarnings("unchecked")
    protected List<String> readRoles(String sessionId) {
        List<String> roles = jdbcTemplate.queryForList("select role_id from userrole join session on userrole.user_id = session.user_id where session.id = ? ", new Object[] { sessionId }, String.class);
        return roles;
    }

    protected Long getSessionTimeout() {
        return sessionTimeout;
    }

    protected void setSessionTimeout(Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
