package org.jazzteam.hashing;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Mainframe
 */
@WebServlet(urlPatterns = { "/Mainframe" })
public class Mainframe extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
	 * @see HttpServlet#HttpServlet()
	 */
    public Mainframe() {
        super();
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String clientSideLoginDigest = (String) request.getParameter("login");
        String clientSidePasswordDigest = (String) request.getParameter("password");
        String login = "root";
        String password = "admin";
        String serverSideLoginDigest = null;
        String serverSidePasswordDigest = null;
        try {
            serverSideLoginDigest = new String(getDigest(login.getBytes()));
            serverSidePasswordDigest = new String(getDigest(password.getBytes()));
            serverSideLoginDigest = serverSideLoginDigest.format("%x", new BigInteger(1, getDigest(login.getBytes())));
            serverSidePasswordDigest = serverSidePasswordDigest.format("%x", new BigInteger(1, getDigest(password.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        PrintWriter printWriter = response.getWriter();
        printWriter.println("Login: client - " + clientSideLoginDigest + " , server - " + serverSideLoginDigest + " equals ? " + clientSideLoginDigest.equalsIgnoreCase(serverSideLoginDigest));
        printWriter.println("Password: client - " + clientSidePasswordDigest + " , server - " + serverSidePasswordDigest + " equals ? " + clientSidePasswordDigest.equalsIgnoreCase(serverSidePasswordDigest));
    }

    private byte[] getDigest(byte[] message) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.reset();
        messageDigest.update(message);
        return messageDigest.digest();
    }
}
