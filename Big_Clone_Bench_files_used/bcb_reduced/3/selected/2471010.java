package com.ingenta.clownbike.servlet;

import com.ingenta.common.security.*;
import com.ingenta.clownbike.*;
import com.ingenta.clownbike.odb.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Login extends ClownbikeServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        DatabaseTransaction transaction = null;
        try {
            transaction = beginTransaction();
            UserManager manager = (UserManager) transaction.getManager(User.class);
            String name = request.getParameter("name");
            String password = request.getParameter("password");
            User user = manager.findByLoginName(transaction, name);
            if (user == null) throw new ClownbikeException("unknown user name");
            String passwordDigest = MD5.digest(password);
            if (!passwordDigest.equals(user.getLoginPassword())) throw new ClownbikeException("unknown name and password combination");
            session.setAttribute("user", user);
            commit(transaction);
            response.sendRedirect(getReferrer(request));
        } catch (RuntimeException e) {
            e.printStackTrace();
            try {
                rollback(transaction);
            } catch (DatabaseException p) {
                e.printStackTrace();
            }
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                rollback(transaction);
            } catch (DatabaseException p) {
                e.printStackTrace();
            } finally {
                forward(request, response, new ClownbikeError(0, e));
            }
        }
    }
}
