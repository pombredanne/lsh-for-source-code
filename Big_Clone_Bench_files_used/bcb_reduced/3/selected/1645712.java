package control;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import model.Administrador;
import dao.DAOAdministrador;
import model.Aluno;
import dao.DAOAluno;

public class DBServletFilter implements Filter {

    private DAOAdministrador da;

    private DAOAluno daoalu;

    public DBServletFilter() {
    }

    ;

    @Override
    public void destroy() {
        da.close();
    }

    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException, ServletException {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
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
        daoalu = new DAOAluno();
        Aluno alu = new Aluno();
        alu.setLogin("aluno");
        alu.setNome("Aluno");
        pass_temp = "aluno";
        md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        hash = new BigInteger(1, md.digest(pass_temp.getBytes()));
        pass = hash.toString(16);
        alu.setSenha(pass);
        try {
            daoalu.persist(alu);
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
