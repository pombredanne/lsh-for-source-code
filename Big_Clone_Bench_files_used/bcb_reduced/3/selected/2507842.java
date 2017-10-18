package br.ufpa.spider.mplan.logic;

import br.ufpa.spider.mplan.persistence.UserDAO;
import br.ufpa.spider.mplan.model.User;
import java.security.MessageDigest;
import org.zkoss.zul.Window;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import sun.misc.BASE64Encoder;

public class LoginController extends Window {

    private Textbox userName;

    private Textbox password;

    private Window window;

    private String login = "";

    public String option = "";

    public String organization = "";

    public String project = "";

    public String passwordu = "";

    Session session;

    public void onCreate() {
        window = ((Window) getFellow("win"));
        userName = ((Textbox) getFellow("user"));
        password = ((Textbox) getFellow("password"));
        session = window.getDesktop().getSession();
        if (Executions.getCurrent().getParameter("login") != null) {
            login = Executions.getCurrent().getParameter("login");
            session.setAttribute("login", login);
        }
        if (Executions.getCurrent().getParameter("password") != null) {
            passwordu = Executions.getCurrent().getParameter("password");
        }
        if (Executions.getCurrent().getParameter("option") != null) {
            option = Executions.getCurrent().getParameter("option");
            session.setAttribute("option", option);
        }
        if (Executions.getCurrent().getParameter("organization") != null) {
            organization = Executions.getCurrent().getParameter("organization");
            session.setAttribute("organization", organization);
        }
        if (Executions.getCurrent().getParameter("project") != null) {
            project = Executions.getCurrent().getParameter("project");
            session.setAttribute("project", project);
        }
        if (login != null) {
            User user = UserDAO.getUserByUserName(login);
            if (user != null) {
                String encodedPassword = null;
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    digest.update(user.getPassword().getBytes());
                    BASE64Encoder encoder = new BASE64Encoder();
                    encodedPassword = encoder.encode(digest.digest());
                } catch (Exception e) {
                }
                if (passwordu.compareTo(encodedPassword) == 0) {
                    session.setAttribute("user", user);
                    session.setAttribute("numero", 5);
                    session.setAttribute("option", option);
                    session.setAttribute("organization", organization);
                    session.setAttribute("project", project);
                    Executions.sendRedirect("menu.zul");
                }
            }
        }
    }

    public LoginController() {
    }

    public void autenticar() throws InterruptedException {
        User user = UserDAO.getUserByUserName(userName.getText());
        if (user == null || user.getPassword().compareTo(password.getText()) != 0) {
            try {
                Messagebox.show("Usuário inválido." + " Certifique-se que os dados foram" + " digitados corretamente.", "SPIDER CL", Messagebox.OK, Messagebox.ERROR);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            password.setText("");
            return;
        }
        Session session = window.getDesktop().getSession();
        session.setAttribute("user", user);
        session.setAttribute("numero", 5);
        Executions.sendRedirect("menu.zul");
    }
}
