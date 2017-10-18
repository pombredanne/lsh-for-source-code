import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.DBConnection;

/**
 * This servlet is used to handle requests for changing administrator's password.
 * 
 * @author Josef Hardi
 *
 */
public class ChangePassword extends HttpServlet {

    /** Database connection **/
    private Connection conn = null;

    /**
	* @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	*/
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        conn = DBConnection.getConnection();
        String oldPassword = request.getParameter("txt_old_password").toString();
        if (oldPassword != "" && oldPassword != null) {
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                String query = "SELECT username " + "FROM UserRegister " + "WHERE password = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, MD5Sum(oldPassword));
                result = stmt.executeQuery();
                if (result.next()) {
                    String newPassword = request.getParameter("txt_new_password").toString();
                    String retypePassword = request.getParameter("txt_retype_password").toString();
                    if (newPassword.equals(retypePassword)) {
                        updatePassword(newPassword);
                        request.setAttribute("errorMessage", "Password changed!");
                        RequestDispatcher rd = request.getRequestDispatcher("login.jsp");
                        rd.forward(request, response);
                    } else {
                        request.setAttribute("errorMessage", "Please re-type the new password correctly!");
                        RequestDispatcher rd = request.getRequestDispatcher("change.jsp");
                        rd.forward(request, response);
                    }
                } else {
                    request.setAttribute("errorMessage", "Invalid old password");
                    RequestDispatcher rd = request.getRequestDispatcher("change.jsp");
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

    private void updatePassword(String newPassword) throws SQLException {
        PreparedStatement stmt = null;
        String query = "UPDATE UserRegister " + "SET password = ? " + "WHERE id = 1";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, MD5Sum(newPassword));
        stmt.executeUpdate();
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
