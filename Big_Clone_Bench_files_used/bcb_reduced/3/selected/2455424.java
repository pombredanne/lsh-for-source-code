package com.georgeandabe.ignant;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import sun.misc.BASE64Encoder;
import com.georgeandabe.db.RowDocument;
import com.georgeandabe.db.SqlUtils;
import com.georgeandabe.db.TableDescriptor;
import com.georgeandabe.util.ArgumentUtils;

public class AuthResource extends DataResource {

    public static final String COOKIE_CHARS = "abcdefghijklmnopqrstuvwxyz1234567890";

    public static final String[] GUEST_NAMES = { "Moon", "Spoon", "Plume", "Bloom", "Thyme", "Rhyme", "Steel", "Boat", "Vase", "Book", "Screen", "Fenestra", "Farmer", "Door", "Squid", "Rocket", "Picker", "Page", "Lawn", "Food", "Plate", "Bean", "Horse", "Cat", "Fireplace", "Frame", "Chair", "Table", "Sofa", "Stair", "Counter", "Shelf", "Phone", "Robot", "Tree", "Key", "Pony" };

    public static final String LOGOUT_PARAM = "logout";

    private Random random = new Random();

    DataSource dataSource = null;

    private TableDescriptor accountTableDescriptor = null;

    public AuthResource(DataSource dataSource) {
        super("auth");
        ArgumentUtils.assertNotNull(dataSource);
        this.dataSource = dataSource;
        accountTableDescriptor = SqlUtils.getTableDescriptor(SqlUtils.ACCOUNT_TABLE_NAME, dataSource);
        addSubResource(new GuestResource());
        addSubResource(new MeResource());
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response, String[] pathElements) throws ServletException, IOException {
        String logoutParam = request.getParameter(LOGOUT_PARAM);
        RowDocument cookieAccountRow = getAuthedAccountRecord(request);
        if (request.getParameter("debug") != null) {
            String result = "";
            if (cookieAccountRow != null) {
                result += "person element: " + cookieAccountRow.getStringField(AuthDocument.USERNAME) + "\n";
                result += "authCookie: " + cookieAccountRow.getStringField(AuthDocument.COOKIE) + "\n";
            }
            result += "requestCookie: " + getRequestAuthCookie(request) + "\n";
            result += "logoutParam: " + logoutParam + "\n";
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.getOutputStream().write(result.getBytes());
            return;
        } else if ("true".equals(logoutParam)) {
            Cookie logoutCookie = new Cookie(AuthDocument.COOKIE, "logMeOut");
            logoutCookie.setPath("/");
            logoutCookie.setMaxAge(0);
            response.addCookie(logoutCookie);
            AuthDocument authDoc = new AuthDocument();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(authDoc.toString().getBytes());
            return;
        } else {
            AuthDocument authDoc = null;
            if (cookieAccountRow != null) {
                authDoc = new AuthDocument(cookieAccountRow.getStringField(AuthDocument.USERNAME), true, cookieAccountRow.getStringField(AuthDocument.ACCOUNT_LEVEL));
            } else {
                authDoc = new AuthDocument();
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(authDoc.toString().getBytes());
            return;
        }
    }

    public AuthDocument getAuthDocument(HttpServletRequest request) {
        RowDocument accountRecord = getAuthedAccountRecord(request);
        if (accountRecord != null) {
            return new AuthDocument(accountRecord.getStringField(AuthDocument.USERNAME), true, accountRecord.getStringField(AuthDocument.ACCOUNT_LEVEL));
        } else {
            return new AuthDocument();
        }
    }

    private RowDocument getAuthedAccountRecord(HttpServletRequest request) {
        return getAuthedAccountRecord(request, accountTableDescriptor, dataSource);
    }

    public static RowDocument getAuthedAccountRecord(HttpServletRequest request, TableDescriptor accountTableDescriptor, DataSource dataSource) {
        String cookie = getRequestAuthCookie(request);
        if (cookie == null) {
            return null;
        }
        return SqlUtils.findRow(accountTableDescriptor, AuthDocument.COOKIE, cookie, dataSource);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response, String[] pathElements) throws ServletException, IOException {
        try {
            String usernameParam = request.getParameter(AuthDocument.USERNAME);
            String passParam = request.getParameter(AuthDocument.PASSWORD);
            if (usernameParam != null || passParam != null) {
                RowDocument authedAccountRecord = authenticate(usernameParam, passParam);
                if (authedAccountRecord != null) {
                    String cookieValue = authedAccountRecord.getStringField(AuthDocument.COOKIE);
                    Cookie newCookie = new Cookie(AuthDocument.COOKIE, cookieValue);
                    newCookie.setPath("/");
                    newCookie.setMaxAge((int) (System.currentTimeMillis() + 31536000));
                    response.addCookie(newCookie);
                    AuthDocument authDoc = new AuthDocument(authedAccountRecord.getStringField(AuthDocument.USERNAME), true, authedAccountRecord.getStringField(AuthDocument.ACCOUNT_LEVEL));
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getOutputStream().write(authDoc.toString().getBytes());
                    return;
                } else {
                    AuthDocument authDoc = new AuthDocument();
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getOutputStream().write(authDoc.toString().getBytes());
                    return;
                }
            }
            RowDocument cookieAccountRecord = getAuthedAccountRecord(request);
            AuthDocument authDoc = null;
            if (cookieAccountRecord != null) {
                authDoc = new AuthDocument(cookieAccountRecord.getStringField(AuthDocument.USERNAME), true, cookieAccountRecord.getStringField(AuthDocument.ACCOUNT_LEVEL));
            } else {
                authDoc = new AuthDocument();
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(authDoc.toString().getBytes());
            return;
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    private class MeResource extends DataResource {

        public MeResource() {
            super("me");
        }

        public void doGet(HttpServletRequest request, HttpServletResponse response, String[] pathElements) throws ServletException, IOException {
            RowDocument cookieAccountRecord = getAuthedAccountRecord(request);
            AuthDocument authDoc = null;
            if (cookieAccountRecord != null) {
                authDoc = new AuthDocument(cookieAccountRecord.getStringField(AuthDocument.USERNAME), true, cookieAccountRecord.getStringField(AuthDocument.ACCOUNT_LEVEL));
            } else {
                authDoc = new AuthDocument();
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(authDoc.toString().getBytes());
        }
    }

    private class GuestResource extends DataResource {

        public GuestResource() {
            super("guest");
        }

        public void doPost(HttpServletRequest request, HttpServletResponse response, String[] pathElements) throws ServletException, IOException {
            String requestedGuestNameParameter = request.getParameter(AuthDocument.REQUESTED_GUEST_NAME_PARAM);
            String cookie = generateGuestCookie(requestedGuestNameParameter);
            Cookie newCookie = new Cookie(AuthDocument.COOKIE, cookie);
            newCookie.setPath("/");
            newCookie.setMaxAge(-1);
            response.addCookie(newCookie);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.getOutputStream().write(cookie.getBytes());
            return;
        }
    }

    private RowDocument authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }
        RowDocument accountRow = SqlUtils.findRow(accountTableDescriptor, AuthDocument.USERNAME, username, dataSource);
        if (accountRow == null) {
            return null;
        }
        if (!isValidPassword(password, accountRow.getStringField(AuthDocument.PASSWORD_HASH))) {
            return null;
        }
        if (accountRow.getStringField(AuthDocument.COOKIE) == null) {
            SqlUtils.setField(accountRow.getStringField(AuthDocument.USERNAME), AuthDocument.COOKIE, generateAuthCookie(false), accountTableDescriptor, dataSource);
            return SqlUtils.findRow(accountTableDescriptor, AuthDocument.USERNAME, accountRow.getStringField(AuthDocument.USERNAME), dataSource);
        }
        return accountRow;
    }

    private String generateGuestCookie(String requestedGuestName) {
        StringBuffer result = new StringBuffer();
        result.append(AuthDocument.GUEST_COOKIE_PREFIX);
        for (int i = 0; i < 3; i++) {
            result.append("_" + GUEST_NAMES[Math.abs(random.nextInt()) % GUEST_NAMES.length]);
        }
        String cleanedGuestName = cleanGuestName(requestedGuestName);
        if (cleanedGuestName != null) {
            result.append("_" + cleanedGuestName);
        }
        return result.toString();
    }

    private String cleanGuestName(String requestedGuestName) {
        if (requestedGuestName == null) {
            return null;
        }
        requestedGuestName = requestedGuestName.trim();
        StringBuffer result = new StringBuffer();
        int numNonWhitespace = 0;
        for (int i = 0; i < requestedGuestName.length(); i++) {
            if (Character.isDigit(requestedGuestName.charAt(i))) {
                numNonWhitespace++;
                result.append(requestedGuestName.charAt(i));
            } else if (Character.isLetter(requestedGuestName.charAt(i))) {
                result.append(requestedGuestName.charAt(i));
                numNonWhitespace++;
            } else if (Character.isWhitespace(requestedGuestName.charAt(i))) {
                result.append("_");
            }
        }
        if (numNonWhitespace == 0) {
            return null;
        }
        return result.toString();
    }

    public static String getRequestAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        for (int i = 0; cookies != null && i < cookies.length; i++) {
            if (AuthDocument.COOKIE.equals(cookies[i].getName())) {
                return cookies[i].getValue();
            }
        }
        return null;
    }

    public TableDescriptor getAccountTableDescriptor() {
        return accountTableDescriptor;
    }

    private String generateAuthCookie(boolean guest) {
        StringBuffer result = new StringBuffer(14);
        if (guest) {
            result.append(AuthDocument.GUEST_COOKIE_PREFIX);
        } else {
            result.append("tr");
        }
        for (int i = 0; i < 12; i++) {
            result.append(COOKIE_CHARS.charAt(Math.abs(random.nextInt()) % COOKIE_CHARS.length()));
        }
        return result.toString();
    }

    public String getPasswordHash(String password) {
        if (password == null || password.trim().length() == 0) {
            return null;
        }
        if (AuthDocument.cleanPassword(password) == null) {
            return null;
        }
        return encode(AuthDocument.cleanPassword(password));
    }

    public boolean isValidPassword(String password, String passwordHash) {
        if (password == null || passwordHash == null || password.trim().length() == 0 || AuthDocument.cleanPassword(password) == null) {
            return false;
        }
        return passwordHash.equals(encode(AuthDocument.cleanPassword(password)));
    }

    private String encode(String plaintext) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(plaintext.getBytes("UTF-8"));
            byte raw[] = md.digest();
            return (new BASE64Encoder()).encode(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error encoding: " + e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Error encoding: " + e);
        }
    }
}
