package socialocalize.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import storage.User;
import storage.UserDetails;

/**
 * Servlet implementation class PorfileEditServlet
 * 
 * This servlet is called by editprofile.jsp. It is used for updating
 * a user's profile information in the database, using information in
 * POST.
 */
public class ProfileEditServlet extends HttpServlet implements javax.servlet.Servlet {

    private static final long serialVersionUID = 1L;

    private EntityManager em;

    /**
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
    public ProfileEditServlet() {
        super();
        em = server.Server.em;
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameter("edit") != null) {
            try {
                User cu = (User) request.getSession().getAttribute("currentuser");
                UserDetails ud = cu.getUserDetails();
                String returnTo = "editprofile.jsp";
                if (!request.getParameter("password").equals("")) {
                    String password = request.getParameter("password");
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(new String(password).getBytes());
                    byte[] hash = md.digest();
                    String pass = new BigInteger(1, hash).toString(16);
                    cu.setClientPassword(pass);
                }
                ud.setFirstName(request.getParameter("fname"));
                ud.setLastName(request.getParameter("lname"));
                ud.setEmailAddress(request.getParameter("email"));
                ud.setAddress(request.getParameter("address"));
                ud.setZipcode(request.getParameter("zipcode"));
                ud.setTown(request.getParameter("town"));
                ud.setCountry(request.getParameter("country"));
                ud.setTrackingColor(request.getParameter("input1"));
                String vis = request.getParameter("visibility");
                if (vis.equals("self")) {
                    cu.setVisibility(0);
                } else if (vis.equals("friends")) {
                    cu.setVisibility(1);
                } else if (vis.equals("all")) {
                    cu.setVisibility(2);
                } else {
                    response.sendRedirect("error.jsp?id=8");
                }
                em.getTransaction().begin();
                em.persist(cu);
                em.getTransaction().commit();
                response.sendRedirect(returnTo);
            } catch (Throwable e) {
                e.printStackTrace();
                response.sendRedirect("error.jsp?id=5");
            }
            return;
        }
    }
}
