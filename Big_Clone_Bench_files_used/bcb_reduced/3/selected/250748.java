package gsu;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.security.*;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;
import models.*;

/**
 * Login servlet
 * @author travis
 * @version 1.0
 */
public class login extends HttpServlet {

    private Statement dbStatement;

    private ResultSet dbResultSet;

    private ResultSetMetaData dbResultSetMetaData;

    private Connection connection;

    private String error;

    private String notice;

    /**
     * Initializes the servlet
     * @throws javax.servlet.ServletException Exception
     */
    public void init() throws ServletException {
        super.init();
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException Exception
     * @throws java.io.IOException Exception
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletConfig config = getServletConfig();
        ServletContext context = config.getServletContext();
        try {
            String driver = context.getInitParameter("driver");
            Class.forName(driver);
            String dbURL = context.getInitParameter("db");
            String username = context.getInitParameter("username");
            String password = "";
            connection = DriverManager.getConnection(dbURL, username, password);
        } catch (ClassNotFoundException e) {
            System.out.println("Database driver not found.");
        } catch (SQLException e) {
            System.out.println("Error opening the db connection: " + e.getMessage());
        }
        String action = "";
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(300);
        if (request.getParameter("action") != null) {
            action = request.getParameter("action");
        } else {
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/login.jsp");
            dispatcher.forward(request, response);
            return;
        }
        if (action.equals("login")) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                error = "There was an error encrypting password.";
                session.setAttribute("error", error);
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/error.jsp");
                dispatcher.forward(request, response);
                return;
            }
            try {
                md.update(password.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                error = "There was an error encrypting password.";
                session.setAttribute("error", error);
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/error.jsp");
                dispatcher.forward(request, response);
                return;
            }
            String encrypted_password = (new BASE64Encoder()).encode(md.digest());
            try {
                String sql = "SELECT * FROM person WHERE email LIKE '" + username + "' AND password='" + encrypted_password + "'";
                dbStatement = connection.createStatement();
                dbResultSet = dbStatement.executeQuery(sql);
                if (dbResultSet.next()) {
                    Person person = new Person(dbResultSet.getString("fname"), dbResultSet.getString("lname"), dbResultSet.getString("address1"), dbResultSet.getString("address2"), dbResultSet.getString("city"), dbResultSet.getString("state"), dbResultSet.getString("zip"), dbResultSet.getString("email"), dbResultSet.getString("password"), dbResultSet.getInt("is_admin"));
                    String member_type = dbResultSet.getString("member_type");
                    String person_id = Integer.toString(dbResultSet.getInt("id"));
                    session.setAttribute("person", person);
                    session.setAttribute("member_type", member_type);
                    session.setAttribute("person_id", person_id);
                } else {
                    notice = "Your username and/or password is incorrect.";
                    request.setAttribute("notice", notice);
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/index.jsp");
                    dispatcher.forward(request, response);
                    return;
                }
            } catch (SQLException e) {
                error = "There was an error trying to login. (SQL Statement)";
                session.setAttribute("error", error);
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/error.jsp");
                dispatcher.forward(request, response);
                return;
            }
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/index.jsp");
            dispatcher.forward(request, response);
            return;
        } else {
            notice = "Unable to log you in.  Please try again.";
            request.setAttribute("notice", notice);
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/login.jsp");
            dispatcher.forward(request, response);
            return;
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException Exception
     * @throws java.io.IOException Exception
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException Exception
     * @throws java.io.IOException Exception
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return String
     */
    public String getServletInfo() {
        return "Short description";
    }
}
