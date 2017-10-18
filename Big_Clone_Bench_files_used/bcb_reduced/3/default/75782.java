import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.*;
import javax.servlet.http.*;
import util.DBConnection;

/**
 * This servlet is used to handle requests for user login.
 * 
 * @author Josef Hardi
 *
 */
public class UserLogin extends HttpServlet {

    /** Database connection **/
    private Connection conn = null;

    /**
	* @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	*/
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        conn = DBConnection.getConnection();
        HttpSession session = request.getSession(true);
        String username = request.getParameter("username").toString();
        String password = request.getParameter("password").toString();
        if (username != "" && username != null && password != "" && password != null) {
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                String query = "SELECT username, password " + "FROM UserRegister " + "WHERE username = ? AND password = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, MD5Sum(password));
                result = stmt.executeQuery();
                if (result.next()) {
                    session.setAttribute("username", result.getString("username"));
                    response.sendRedirect("browse.jsp");
                } else {
                    request.setAttribute("errorMessage", "Invalid username or password");
                    RequestDispatcher rd = request.getRequestDispatcher("login.jsp");
                    rd.forward(request, response);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (result != null) result.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            response.sendRedirect("login.jsp");
        }
    }

    private String MD5Sum(String input) {
        String hashtext = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(input.getBytes());
            byte[] digest = md.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            hashtext = bigInt.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashtext;
    }
}
