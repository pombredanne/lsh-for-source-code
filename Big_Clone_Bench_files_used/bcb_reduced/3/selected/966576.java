package com.liferay.portal.servlet.filters.context.sso;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import com.liferay.portal.kernel.jndi.JNDIUtil;
import sun.misc.BASE64Encoder;

public class SSOSubject implements Serializable {

    public static final String LOGIN_ATTRIBUTE_NAME = "USER_ID";

    public static final String NOT_AUTHENTICATED = "____NONE____";

    public static final String DATA_SOURCE_JNDI = "jdbc/LiferayPool";

    public static final String SSO_TOKEN_SEPARATOR = "\t~\t";

    public static final String SSO_TOKEN_SEPARATOR_REGEX = "\\t~\\t";

    public static final String FORCE_REAUTHENTICATION_URL = "/c/portal/logout?time=";

    public static final String SSOSUBJECT_KEY = "SSOSubject";

    public static final String SSOSUBJECT_LOGGED_OUT_KEY = "SSOSubject_Logged_Out";

    public static final int MIN_LASTACCESS_RESET = 10000;

    private static final long serialVersionUID = 4617585236653503530L;

    private static final Logger LOG = Logger.getLogger(SSOSubject.class.getName());

    private static final BASE64Encoder encoder = new BASE64Encoder();

    private static final DataSource dataSource;

    static {
        dataSource = getDataSource();
        LOG.finest("The class of the dataSource is: " + dataSource.getClass());
    }

    private String userId;

    private String screenName;

    private String subjectFirstname;

    private String subjectLastname;

    private String passwordHash;

    private String forceReauthentication;

    private String ssoTokenHash;

    private long lastAccessGMTMillis;

    private int duration;

    private boolean isAdmin;

    private SSOSubject(String forceAuthentication, String userId, boolean isAdmin, String passwordHash, String screenName, String firstName, String lastName, long lastAccessGMTMillis, int duration, String ssoTokenHash) {
        init(forceAuthentication, userId, isAdmin, passwordHash, screenName, firstName, lastName, lastAccessGMTMillis, duration, ssoTokenHash);
    }

    public SSOSubject(String ssoToken) {
        String[] ssoTokens = ssoToken.split(SSO_TOKEN_SEPARATOR_REGEX);
        LOG.finer("The number of ssoTokens in '" + ssoToken + "' is: " + ssoTokens.length);
        if (ssoTokens.length != 7) {
            init(FORCE_REAUTHENTICATION_URL + new Date().getTime());
        } else {
            init(null, ssoTokens[1], false, null, ssoTokens[2], ssoTokens[3], ssoTokens[4], Long.parseLong(ssoTokens[5]), Integer.parseInt(ssoTokens[6]), ssoTokens[0]);
            if (this.isTooOld()) {
                init(FORCE_REAUTHENTICATION_URL + new Date().getTime());
            }
        }
    }

    private final void init(String forceAuthentication) {
        init(forceAuthentication, null, false, null, null, null, null, 0, 0, null);
    }

    private final void init(String forceAuthentication, String userId, boolean isAdmin, String passwordHash, String screenName, String subjectFirstname, String subjectLastname, long lastAccessGMTMillis, int duration, String ssoTokenHash) {
        this.forceReauthentication = forceAuthentication;
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.passwordHash = passwordHash;
        this.screenName = screenName;
        this.subjectFirstname = subjectFirstname;
        this.subjectLastname = subjectLastname;
        this.duration = duration;
        this.lastAccessGMTMillis = lastAccessGMTMillis;
        this.ssoTokenHash = ((ssoTokenHash == null && forceAuthentication == null) ? calculateHash() : ssoTokenHash);
    }

    public void updateLastAccess() {
        this.lastAccessGMTMillis = System.currentTimeMillis();
        this.ssoTokenHash = calculateHash();
    }

    public boolean isTooOld() {
        boolean tooOld = System.currentTimeMillis() > (lastAccessGMTMillis + duration);
        LOG.finest("This subject is tooooo old: " + tooOld);
        return tooOld;
    }

    public boolean isAgedEnough() {
        return (System.currentTimeMillis() > (lastAccessGMTMillis + MIN_LASTACCESS_RESET));
    }

    public String getSSOToken() {
        return ssoTokenHash + SSO_TOKEN_SEPARATOR + userId + SSO_TOKEN_SEPARATOR + screenName + SSO_TOKEN_SEPARATOR + subjectFirstname + SSO_TOKEN_SEPARATOR + subjectLastname + SSO_TOKEN_SEPARATOR + lastAccessGMTMillis + SSO_TOKEN_SEPARATOR + duration;
    }

    public boolean authenticate(HttpSession session) throws UnsupportedEncodingException {
        if (forceReauthentication == null) {
            if (passwordHash == null) {
                login(session, lastAccessGMTMillis, ssoTokenHash, forceReauthentication);
            }
            String ssoTokenCheck = calculateHash();
            LOG.finer("Comparing '" + ssoTokenHash + "' and '" + ssoTokenCheck + "' is '" + ssoTokenHash.equals(ssoTokenCheck) + "'!!");
            return ssoTokenHash.equals(ssoTokenCheck);
        } else {
            return false;
        }
    }

    public String calculateHash() {
        return getHash(2, userId + SSO_TOKEN_SEPARATOR + passwordHash + SSO_TOKEN_SEPARATOR + lastAccessGMTMillis + SSO_TOKEN_SEPARATOR + duration, passwordHash);
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public String getUserId() {
        return userId;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getSubjectFirstname() {
        return subjectFirstname;
    }

    public String getSubjectLastname() {
        return subjectLastname;
    }

    public String getForceReauthentication() {
        return forceReauthentication;
    }

    public String toString() {
        return super.toString() + getSSOToken();
    }

    public int hashCode() {
        return getSSOToken().hashCode();
    }

    public boolean equals(Object ssoSubject) {
        if (ssoSubject == null || !(ssoSubject instanceof SSOSubject)) {
            return false;
        }
        return this.getSSOToken().equals(((SSOSubject) ssoSubject).getSSOToken());
    }

    private final void login(HttpSession session, long lastAccess, String ssoHash, String forceReauthn) {
        String[] authData = new String[4];
        boolean admin = false;
        Connection conn = null;
        Statement stmt = null;
        ResultSet res = null;
        try {
            String passwordQuery = "select password_, screenName, firstName, lastName from User_ u, Contact_ c where u.userId=c.userId and c.userId=" + this.userId;
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            res = stmt.executeQuery(passwordQuery);
            res.next();
            authData[0] = res.getString(1);
            authData[1] = res.getString(2);
            authData[2] = res.getString(3);
            authData[3] = res.getString(4);
            res.close();
            String adminQuery = "select r.name from User_ u, Users_Roles j, Role_ r where u.userId=" + this.userId + " and u.userId=j.userId and j.roleId=r.roleId and r.name='Administrator'";
            res = stmt.executeQuery(adminQuery);
            if (res.next()) admin = true; else admin = false;
        } catch (SQLException ex) {
            authData = null;
            admin = false;
            ex.printStackTrace();
        } finally {
            try {
                if (res != null) res.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        init(forceReauthn, this.userId, admin, authData[0], authData[1], authData[2], authData[3], lastAccess, session.getMaxInactiveInterval() * 1000, ssoHash);
    }

    public static final void logout(HttpSession session) {
        logout(null, null, session);
    }

    public static final void logout(HttpServletRequest req, HttpServletResponse res, HttpSession session) {
        try {
            session.removeAttribute(SSOSUBJECT_KEY);
            if (res != null) {
                Boolean isLoggedOut = (Boolean) session.getAttribute(SSOSUBJECT_LOGGED_OUT_KEY);
                if (isLoggedOut == null || !isLoggedOut) {
                    setCookie(res, SSOSUBJECT_KEY, NOT_AUTHENTICATED, "/", 0);
                    session.setAttribute(SSOSUBJECT_LOGGED_OUT_KEY, true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static final String getHash(int iterationNb, String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(salt.getBytes("UTF-8"));
            byte[] input = digest.digest(password.getBytes("UTF-8"));
            for (int i = 0; i < iterationNb; i++) {
                digest.reset();
                input = digest.digest(input);
            }
            String hashedValue = encoder.encode(input);
            LOG.finer("Creating hash '" + hashedValue + "' with iterationNb '" + iterationNb + "' and password '" + password + "' and salt '" + salt + "'!!");
            return hashedValue;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Problem in the getHash method.", ex);
        }
    }

    public static final SSOSubject login(String userId, HttpSession session) {
        SSOSubject ssoSubject = null;
        synchronized (dataSource) {
            ssoSubject = (SSOSubject) session.getAttribute(SSOSUBJECT_KEY);
            if (ssoSubject == null) {
                ssoSubject = new SSOSubject(null, userId, false, null, null, null, null, 0, 0, "");
                session.setAttribute(SSOSUBJECT_KEY, ssoSubject);
                session.setAttribute(SSOSUBJECT_LOGGED_OUT_KEY, false);
            } else {
                return null;
            }
        }
        if (ssoSubject != null) {
            ssoSubject.login(session, System.currentTimeMillis() - MIN_LASTACCESS_RESET, null, null);
        }
        return ssoSubject;
    }

    public static final DataSource getDataSource() {
        try {
            return (DataSource) JNDIUtil.lookup(new InitialContext(), DATA_SOURCE_JNDI);
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public static final String getNamedCookie(HttpServletRequest request, String name, String path, String cookieValue) throws UnsupportedEncodingException {
        boolean found = false;
        String result = null;
        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                int i = 0;
                while (!found && i < cookies.length) {
                    LOG.finest("CookieName: '" + cookies[i].getName() + "' CookiePath: '" + cookies[i].getPath() + "'" + " CookieValue: '" + cookies[i].getValue() + "'");
                    if (cookies[i].getName().equals(name)) {
                        if (!found && (cookieValue == null || cookies[i].getValue().equals(cookieValue))) {
                            found = true;
                            result = URLDecoder.decode(cookies[i].getValue(), "UTF-8");
                        }
                    }
                    i++;
                }
            }
        }
        return result;
    }

    public static final void setCookie(HttpServletResponse res, String cookieName, String value, String path, int expiry) throws UnsupportedEncodingException {
        if (res != null) {
            Cookie cookie = new Cookie(cookieName, URLEncoder.encode(value, "UTF-8"));
            cookie.setPath(path);
            cookie.setMaxAge(expiry);
            res.addCookie(cookie);
            LOG.finest("Adding cookie " + cookieName + " using passed in value: " + value + " with cookie value: " + cookie.getValue());
        }
    }
}
