package com.meleva.usuarioServlets;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.meleva.bll.UsuarioBll;
import com.meleva.model.Usuario;

/**
 * Servlet implementation class Login
 */
@WebServlet("/Login")
public class Login extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
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
        String senha = "";
        String email = request.getParameter("EmailLogin");
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(request.getParameter("SenhaLogin").getBytes(), 0, request.getParameter("SenhaLogin").length());
            senha = new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Usuario usuario = UsuarioBll.getUsuarioByEmailAndSenha(email, senha);
        String redirect = request.getHeader("REFERER").replace("?msg=3", "").replace("&msg=3", "") + "?&msg=3";
        if (request.getHeader("REFERER").indexOf("?") != -1) {
            redirect = request.getHeader("REFERER").replace("?msg=3", "").replace("&msg=3", "") + "&msg=3";
        }
        if (usuario.getNome() != null) {
            HttpSession session = request.getSession();
            session.setAttribute("usuario", usuario);
            redirect = "index.jsp";
        }
        response.sendRedirect(redirect);
    }
}
