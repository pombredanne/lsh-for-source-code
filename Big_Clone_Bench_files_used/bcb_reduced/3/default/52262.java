import java.io.*;
import java.security.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Manages the user accounts, i.e. password changes, user additions...
 * @version $Id: UsersModule.java,v 1.9 2001/07/22 20:23:00 brightice Exp $
 * @author Matthias Juchem <matthias at konfido.de>
 */
public final class UsersModule extends Module implements SessionService {

    private static final String moduleName = "userman";

    private static final String providedServices[] = { "session", "usermanager" };

    private static final String requiredServices[] = { "dbconnection", "configuration" };

    private static String pLogin;

    public UsersModule() {
    }

    public void initParameterNames() {
        super.initParameterNames();
        pLogin = configuration.getParameterName(this.moduleName, "login");
    }

    public boolean checkPassword(String login, String encrPasswd) throws InvalidParameterException {
        if (login == null) {
            throw new InvalidParameterException("login", "String");
        }
        if (encrPasswd == null) {
            throw new InvalidParameterException("encrPasswd", "String");
        }
        boolean ok = false;
        String arguments[] = { login, encrPasswd };
        ResultSet result = null;
        result = dbConnection.preparedQueryByKey(this.moduleName, "checkPassword", arguments);
        try {
            if (result.first()) {
                ok = result.isLast();
            }
        } catch (SQLException e) {
        }
        return (ok);
    }

    public boolean randomizePassword(String login, String email) {
        String password = getClearRandomPassword();
        String arguments[] = { encryptPassword(password), login };
        int result = 0;
        result = dbConnection.preparedUpdateByKey(this.moduleName, "setPassword", arguments);
        if (result == 1) {
            EMail mail = new EMail();
            mail.setHeader("nobody@archimed.math.uni-mannheim.de", email, "Your Bugflow account " + login);
            PrintWriter body = mail.getBodyWriter();
            mail.addToBCC(email);
            body.println("From: Bugflow - Do Not Reply <nobody@archimed.math.uni-mannheim.de>\n");
            body.println("Your Bugflow password for login " + login + " has been set to: " + password);
            mail.send();
            return (true);
        } else {
            return (false);
        }
    }

    public String encryptPassword(String clearPassword) throws NullPointerException {
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new NullPointerException("NoSuchAlgorithmException: " + e.toString());
        }
        sha.update(clearPassword.getBytes());
        byte encryptedPassword[] = sha.digest();
        sha = null;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < encryptedPassword.length; i++) {
            result.append(Byte.toString(encryptedPassword[i]));
        }
        return (result.toString());
    }

    private String getClearRandomPassword() {
        String password;
        {
            final int pwLength = 6;
            final String abc = "abcdefghijklmnopqrstuvwxyz";
            java.util.Random random = new java.util.Random();
            byte rbytes[] = new byte[pwLength];
            random.nextBytes(rbytes);
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < pwLength; i++) {
                int index = (0 - rbytes[i]) % 26;
                if (index < 0) index = 0 - index;
                result.append(abc.charAt(index));
            }
            password = result.toString();
        }
        return (password);
    }

    public String getModuleName() {
        return (moduleName);
    }

    public String[] getProvidedServices() {
        return (providedServices);
    }

    public String[] getRequiredServices() {
        return (requiredServices);
    }

    public void writeBigMenu(HTMLDocument htdoc) {
        htdoc.h3("User Management").println("<ul>").print("<li>").servletAHREF(moduleName, "logout", null, "logout").println("</li>").print("<li>").servletAHREF(moduleName, "chpass", null, "change your password").println("</li>").print("<li>").servletAHREF(moduleName, "adduser", null, "add a new user").println("</li>").print("<li>").servletAHREF(moduleName, "list", null, "list all users").println("</li>").println("</ul>");
    }

    public String getMenuTitle() {
        return ("User Management");
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        String command = request.getParameter(pCommand);
        if (!isAuthenticatedSession(request.getSession(false))) {
            login(request, response, htdoc);
            return;
        }
        if (command == null) return;
        if (command.equals("login")) {
            login(request, response, htdoc);
        } else {
            if (command.equals("menu")) {
                writeBigMenu(htdoc);
            } else {
                if (command.equals("list")) {
                    list(request, response, htdoc);
                } else {
                    if (command.equals("chpass")) {
                        chpass(request, response, htdoc);
                    } else {
                        if (command.equals("adduser")) {
                            adduser(request, response, htdoc);
                        } else {
                            if (command.equals("edituser")) {
                                edituser(request, response, htdoc);
                            } else {
                                if (command.equals("activateuser")) {
                                    activateuser(request, response, htdoc);
                                } else {
                                    if (command.equals("deactivateuser")) {
                                        deactivateuser(request, response, htdoc);
                                    } else {
                                        if (command.equals("deleteuser")) {
                                            deleteuser(request, response, htdoc);
                                        } else {
                                            if (command.equals("logout")) {
                                                request.getSession(false).invalidate();
                                                htdoc.beginPage("Logout").h2("Logged out").servletAHREF(moduleName, "login", null, "login again");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isAuthenticatedSession(HttpSession session) {
        boolean result = false;
        if (pLogin != null) {
            result = (session.getAttribute(pLogin) != null);
        }
        return (result);
    }

    public void login(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        this.initParameterNames();
        HttpSession session = request.getSession(false);
        htdoc.beginPage("Login");
        if (isAuthenticatedSession(session)) {
            htdoc.println("You are already logged in...");
            return;
        }
        String login = request.getParameter(pLogin);
        String password = request.getParameter("password");
        if (login != null && password != null) {
            password = encryptPassword(password);
            try {
                if (checkPassword(login, password)) {
                    session.setAttribute(pLogin, login);
                    htdoc.sendRedirect(htdoc.getServletURL("main", "firstpage", null));
                } else {
                    htdoc.println("User " + login + " could not be authentificated.");
                }
            } catch (InvalidParameterException e) {
                System.err.println(e.toString());
                e.printStackTrace(System.err);
            }
        } else {
            password = null;
            htdoc.beginForm(moduleName, "login").h1("Login").println("Please enter your login name and your password.").br().println("To obtain a login account contact the webmaster.").field(htdoc.fdText, pLogin, "login: ", "", 16).field(htdoc.fdPassword, "password", "password: ", "", 16).field(htdoc.fdSubmit, pLogin, "", "login", 0).endForm();
        }
    }

    public String getLogoutAHREF(HTMLDocument htdoc, String text) {
        return (htdoc.getServletAHREF(this.moduleName, "logout", null, (text == null ? "" : text)));
    }

    private void list(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) {
        try {
            htdoc.h2("Users List").println("To (de)activate a user, click on no (or yes).").br().servletAHREF(moduleName, "adduser", null, "add a new user").br().br();
            ResultSet rs = dbConnection.preparedQueryByKey(this.moduleName, "getUsersList", null);
            htdoc.println("<table border=\"1\" cellpadding=\"2\">").print("<tr><th>&nbsp;</th><th>login</th><th>name</th><th>surname</th>").print("<th>e-mail</th><th>activated</th></tr>");
            while (rs.next()) {
                htdoc.print("<tr>");
                htdoc.print("<td>");
                htdoc.servletAHREF(moduleName, "deleteuser", pLogin + "=" + rs.getString(1), "del");
                htdoc.print(" ");
                htdoc.servletAHREF(moduleName, "edituser", pLogin + "=" + rs.getString(1), "edit");
                htdoc.print("</td><td>");
                htdoc.print(rs.getString(1));
                htdoc.print("</td><td>");
                htdoc.print(rs.getString(2));
                htdoc.print("</td><td>");
                htdoc.print(rs.getString(3));
                htdoc.print("</td><td>");
                htdoc.print(rs.getString(4));
                htdoc.print("</td><td>");
                if (rs.getBoolean(5)) {
                    htdoc.servletAHREF(moduleName, "deactivateuser", pLogin + "=" + rs.getString(1), "yes");
                } else {
                    htdoc.servletAHREF(moduleName, "activateuser", pLogin + "=" + rs.getString(1), "no");
                }
                htdoc.print("</td>");
                htdoc.println("</tr>");
            }
            htdoc.println("</table>");
        } catch (SQLException e) {
            htdoc.println(e.toString());
        }
    }

    private void adduser(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        boolean error = false;
        String login = request.getParameter(pLogin);
        String email = request.getParameter("email");
        htdoc.beginPage("Adding User").h2("Adding User");
        if (login == null) {
            error = true;
            login = "login";
        } else {
            if (login.equals("")) {
                error = true;
                login = "login";
            }
        }
        if (email == null) {
            error = true;
            email = "email";
        }
        if (error) {
            htdoc.println("Please enter the login name and the e-mail address" + " of the user you want to add.").br().twoFieldForm(moduleName, "adduser", pLogin, "login:", login, 16, "email", "e-mail:", email, 0);
        } else {
            String arguments[] = { login, email, "", "f" };
            int result = 0;
            result = dbConnection.preparedUpdateByKey(this.moduleName, "addUser", arguments);
            if (result == 1) {
                {
                    EMail mail = new EMail();
                    mail.setHeader("nobody@archimed.math.uni-mannheim.de", email, "Your new Bugflow account");
                    PrintWriter body = mail.getBodyWriter();
                    mail.addToBCC(email);
                    body.println("From: Bugflow - Do Not Reply <nobody@archimed.math.uni-mannheim.de>\n");
                    body.println("A new Bugflow account for login " + login + " has been created, but it is still locked.\n");
                    body.println("\nYour password will be mailed to you as soon as the account is unlocked.");
                    body.println("\n\nFollow this URL to login:");
                    body.println(htdoc.getServletURL("usermanager", "login", null));
                    mail.send();
                }
                htdoc.sendRedirect(htdoc.getServletURL(this.moduleName, "list", null));
            } else {
                htdoc.println("User insertion in database failed.");
            }
        }
    }

    private void chpass(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        boolean error = false;
        String login = (String) request.getSession(false).getAttribute(pLogin);
        String passwdo = request.getParameter("passwdo");
        String passwd1 = request.getParameter("passwd1");
        String passwd2 = request.getParameter("passwd2");
        htdoc.beginPage("Changing Password").h2("Changing Password");
        if (passwdo == null) {
            error = true;
            passwdo = "passwdo";
        } else {
            if (passwdo.equals("")) {
                error = true;
                passwdo = "passwdo";
            }
        }
        if (passwd1 == null) {
            error = true;
            passwd1 = "passwd1";
        } else {
            if (passwd1.equals("")) {
                error = true;
                passwd1 = "passwd1";
            }
        }
        if (passwd2 == null) {
            error = true;
            passwd2 = "passwd2";
        }
        if (error) {
            htdoc.println("Please enter the your login name, your old" + " password and twice the new password.").br().beginForm(moduleName, "chpass").field(htdoc.fdPassword, "passwdo", "old password: ", "", 16).field(htdoc.fdPassword, "passwd1", "new password: ", "", 16).field(htdoc.fdPassword, "passwd2", "new password: ", "", 16).field(htdoc.fdSubmit, "submit", null, "submit", 0).endForm();
        } else {
            passwdo = encryptPassword(passwdo);
            try {
                if (passwd1.equals(passwd2)) {
                    if (checkPassword(login, passwdo)) {
                        passwd1 = encryptPassword(passwd1);
                        passwd2 = encryptPassword(passwd2);
                        String arguments[] = { passwd1, login, passwdo };
                        int result = 0;
                        result = dbConnection.preparedUpdateByKey(this.moduleName, "changePassword", arguments);
                        if (result == 1) {
                            htdoc.sendRedirect(htdoc.getServletURL("main", "fallback", null));
                            return;
                        } else {
                            htdoc.println("Changing password in database failed.");
                            return;
                        }
                    } else {
                        htdoc.println("Error: wrong password");
                        return;
                    }
                } else {
                    htdoc.println("Error: new passwords did not match");
                    return;
                }
            } catch (InvalidParameterException e) {
                htdoc.println("Error: new passwords did not match");
                return;
            }
        }
    }

    private void deleteuser(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        boolean error = false;
        String login = request.getParameter(pLogin);
        String confirm = request.getParameter("confirm");
        htdoc.beginPage("Delete User").h2("Deleting User");
        if (login == null) {
            htdoc.println("deleteuser: Parameter login is missing.");
            error = true;
        }
        if (error) {
            htdoc.println("deleteuser: request had wrong format, aborting.");
            return;
        }
        if (confirm == null) {
            htdoc.println("Enter user login here to confirm deletion of user " + login + " :").br().beginForm(moduleName, "deleteuser").field(htdoc.fdText, "confirm", "login: ", "", 16).field(htdoc.fdHidden, pLogin, null, login, 16).field(htdoc.fdSubmit, "submit", null, "submit", 0).endForm();
        } else {
            if (confirm.equals(login)) {
                String arguments[] = { login };
                int result = 0;
                result = dbConnection.preparedUpdateByKey(this.moduleName, "deleteLogin", arguments);
                if (result == 1) {
                    htdoc.sendRedirect(htdoc.getServletURL(this.moduleName, "list", null));
                } else {
                    htdoc.println("User deletion from database failed.");
                }
            } else {
                htdoc.println("As the deletion has not been confirmed, user " + login + " has not been deleted.");
            }
        }
    }

    private void edituser(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        boolean error = false;
        String login = request.getParameter(pLogin);
        String email = request.getParameter("email");
        String name = request.getParameter("name");
        String surname = request.getParameter("surname");
        htdoc.beginPage("Editing User").h2("Editing User");
        if (login == null) {
            error = true;
            login = "login";
        } else {
            if (login.equals("")) {
                error = true;
                login = "login";
            }
        }
        if (email == null) {
            error = true;
        }
        if (error) {
            String arguments[] = { login };
            ResultSet rs = dbConnection.preparedQueryByKey(this.moduleName, "getUserByLogin", arguments);
            try {
                if (rs.next()) {
                    name = rs.getString(1);
                    surname = rs.getString(2);
                    email = rs.getString(3);
                    htdoc.beginForm(moduleName, "edituser").field(htdoc.fdHidden, pLogin, null, login, 0).field(htdoc.fdStatic, "", "user:", login, 0).field(htdoc.fdText, "name", "name:", name, 0).field(htdoc.fdText, "surname", "surname:", surname, 0).field(htdoc.fdText, "email", "e-mail:", email, 0).field(htdoc.fdSubmit, "submit", null, "submit", 0).endForm();
                    return;
                } else {
                    htdoc.println("User " + login + " not found in database.");
                    return;
                }
            } catch (SQLException e) {
                htdoc.println("Error while retreiving data from database.");
                return;
            }
        } else {
            String arguments[] = { name, surname, email, login };
            int result = 0;
            result = dbConnection.preparedUpdateByKey(this.moduleName, "changeUser", arguments);
            if (result == 1) {
                htdoc.sendRedirect(htdoc.getServletURL(this.moduleName, "list", null));
                return;
            } else {
                htdoc.println("User modification in database failed.");
                return;
            }
        }
    }

    private void activateuser(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        boolean error = false;
        String login = request.getParameter(pLogin);
        String randpass = request.getParameter("randpass");
        htdoc.beginPage("Activating User").h2("Activating User");
        if (login == null) {
            htdoc.println("Parameter login is missing");
            return;
        } else {
            if (login.equals("")) {
                htdoc.println("Parameter login is missing");
                return;
            }
        }
        if (randpass == null) {
            htdoc.servletAHREF(moduleName, "activateuser", pLogin + "=" + login + "&randpass=yes", "activate user and randomize password").br().servletAHREF(moduleName, "activateuser", pLogin + "=" + login + "&randpass=no", "activate user only");
        } else {
            String arguments[] = { login };
            int result = 0;
            result = dbConnection.preparedUpdateByKey(this.moduleName, "activateUser", arguments);
            if (randpass.equals("yes")) {
                if (!randomizePassword(login, "juchem@uni-mannheim.de")) {
                    htdoc.println("Password modification in database failed,");
                    htdoc.println(" user not activated.");
                    return;
                }
            }
            if (result == 1) {
                htdoc.sendRedirect(htdoc.getServletURL(this.moduleName, "list", null));
            } else {
                htdoc.println("User modification in database failed.");
            }
        }
    }

    private void deactivateuser(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        boolean error = false;
        String login = request.getParameter(pLogin);
        htdoc.beginPage("Deactivating User").h2("Deactivating User");
        if (login == null) {
            error = true;
            login = "login";
        } else {
            if (login.equals("")) {
                error = true;
                login = "login";
            }
        }
        if (error) {
            htdoc.println("Parameter login is missing");
        } else {
            String arguments[] = { login };
            int result = 0;
            result = dbConnection.preparedUpdateByKey(this.moduleName, "deactivateUser", arguments);
            if (result == 1) {
                htdoc.sendRedirect(htdoc.getServletURL(this.moduleName, "list", null));
            } else {
                htdoc.println("User modification in database failed.");
            }
        }
    }

    private void edituserform(HttpServletRequest request, HttpServletResponse response, HTMLDocument htdoc) throws IOException {
        htdoc.beginPage("Edit User").println("Dummy page: imagine user data here");
    }
}
