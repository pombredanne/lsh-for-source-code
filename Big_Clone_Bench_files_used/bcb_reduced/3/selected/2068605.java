package control;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import model.Administrador;
import dao.DAOAdministrador;

public class DBServletListener implements ServletContextListener {

    private DAOAdministrador da;

    public void contextInitialized(ServletContextEvent sce) {
        da = new DAOAdministrador();
        Administrador adm = new Administrador();
        adm.setLogin("admin");
        adm.setNome("Administrador");
        String pass_temp = "admin";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(pass_temp.getBytes()));
        String pass = hash.toString(16);
        adm.setSenha(pass);
        try {
            da.persist(adm);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        da.close();
    }
}
