package Control;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import Entity.*;

public class CSecurity extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static final long serialVersionUID = 1L;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private enum Action {

        LOGIN, LOGOUT, CHECK
    }

    public CSecurity() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
        this.Actions();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
        this.Actions();
    }

    private void Actions() throws ServletException, IOException {
        switch(Action.valueOf(this.request.getParameter("a"))) {
            case LOGIN:
                this.login();
                break;
            case LOGOUT:
                this.logout();
                break;
            case CHECK:
                this.check();
                break;
        }
    }

    private void login() throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        EMember m = new EMember();
        System.out.print(m.readMember(email));
        if (m.readMember(email) == false) {
            response.sendRedirect("root/error.jsp?code=LOGINFALSE");
        } else {
            if (m.getPassword().equals(this.sha1(password))) {
                HttpSession s = request.getSession();
                s.setAttribute("mid", m.getId());
                s.setAttribute("power", m.getPower());
                response.sendRedirect("Member/index.jsp");
            } else {
                response.sendRedirect("root/error.jsp?code=PASSWORDFALSE");
            }
        }
    }

    private void logout() throws ServletException, IOException {
        HttpSession s = this.request.getSession();
        Enumeration<?> varnames = s.getAttributeNames();
        while (varnames.hasMoreElements()) {
            String var = (String) varnames.nextElement();
            s.removeAttribute(var);
        }
        this.response.sendRedirect("index.jsp");
    }

    private void check() throws ServletException, IOException {
        EDocument d = new EDocument();
        d.readDocument(Integer.parseInt(this.request.getParameter("did")));
        d.setChecked(Integer.parseInt(this.request.getParameter("checked")));
        d.updateDocument();
        this.response.sendRedirect("Security/checkdoc.jsp");
    }

    private String sha1(String s) {
        String encrypt = s;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.update(s.getBytes());
            byte[] digest = sha.digest();
            final StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < digest.length; ++i) {
                final byte b = digest[i];
                final int value = (b & 0x7F) + (b < 0 ? 128 : 0);
                buffer.append(value < 16 ? "0" : "");
                buffer.append(Integer.toHexString(value));
            }
            encrypt = buffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encrypt;
    }
}
