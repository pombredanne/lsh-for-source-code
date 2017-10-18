package itjava.view;

import itjava.model.ResultEntry;
import itjava.db.*;
import java.io.IOException;
import java.util.ArrayList;
import java.sql.*;
import java.security.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class CodeSearchServlet
 */
@WebServlet(description = "Logs in a student", urlPatterns = { "/LoginStudentServlet" })
public class LoginStudentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    ArrayList<ResultEntry> sourceCodes;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginStudentServlet() {
        super();
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Connection conn = null;
        PreparedStatement ucpst = null;
        PreparedStatement ucpst2 = null;
        ResultSet rs = null;
        try {
            String userLevelChoice = request.getParameter("newuserlevel");
            if (userLevelChoice.equals("student")) {
                String username = request.getParameter("username3");
                String password = request.getParameter("password3");
                byte[] defaultBytes = password.getBytes();
                MessageDigest algorithm = MessageDigest.getInstance("MD5");
                algorithm.reset();
                algorithm.update(defaultBytes);
                byte messageDigest[] = algorithm.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < messageDigest.length; i++) {
                    hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
                }
                messageDigest.toString();
                password = hexString + "";
                conn = DBConnection.GetConnection();
                String usercheck = "SELECT studentID, username FROM students WHERE username = ? AND password = ?";
                ucpst = conn.prepareStatement(usercheck);
                ucpst.setString(1, username);
                ucpst.setString(2, password);
                rs = ucpst.executeQuery();
                HttpSession session = request.getSession(true);
                String tempuserid = (String) session.getAttribute("userName");
                if (rs.next()) {
                    session.setAttribute("userName", rs.getString("username"));
                    session.setAttribute("userID", rs.getInt("studentID"));
                    session.setAttribute("userLevel", "student");
                    String updatequery = "UPDATE TutorialInfo SET createdBy = ?, userLevel=\"student\" WHERE createdBy = ?";
                    ucpst2 = conn.prepareStatement(updatequery);
                    ucpst2.setString(1, (String) session.getAttribute("userName"));
                    ucpst2.setString(2, tempuserid);
                    ucpst2.executeUpdate();
                    String redirectURL1 = "students.jsp";
                    response.sendRedirect(redirectURL1);
                } else {
                    String redirectURL2 = "students.jsp?error=4";
                    response.sendRedirect(redirectURL2);
                }
            } else {
                String username = request.getParameter("username3");
                String password = request.getParameter("password3");
                byte[] defaultBytes = password.getBytes();
                MessageDigest algorithm = MessageDigest.getInstance("MD5");
                algorithm.reset();
                algorithm.update(defaultBytes);
                byte messageDigest[] = algorithm.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < messageDigest.length; i++) {
                    hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
                }
                messageDigest.toString();
                password = hexString + "";
                conn = DBConnection.GetConnection();
                String usercheck = "SELECT teacherID, username FROM teachers WHERE username = ? AND password = ?";
                ucpst = conn.prepareStatement(usercheck);
                ucpst.setString(1, username);
                ucpst.setString(2, password);
                rs = ucpst.executeQuery();
                HttpSession session = request.getSession(true);
                String tempuserid = (String) session.getAttribute("userName");
                if (rs.next()) {
                    session.setAttribute("userName", rs.getString("username"));
                    session.setAttribute("userID", rs.getString("teacherID"));
                    session.setAttribute("userLevel", "teacher");
                    String updatequery = "UPDATE TutorialInfo SET createdBy = ?, userLevel=\"teacher\" WHERE createdBy = ?";
                    ucpst2 = conn.prepareStatement(updatequery);
                    ucpst2.setString(1, (String) session.getAttribute("userName"));
                    ucpst2.setString(2, tempuserid);
                    ucpst2.executeUpdate();
                    String redirectURL1 = "teachers.jsp";
                    response.sendRedirect(redirectURL1);
                } else {
                    String redirectURL2 = "teachers.jsp?error=4";
                    response.sendRedirect(redirectURL2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ucpst.close();
                ucpst2.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
