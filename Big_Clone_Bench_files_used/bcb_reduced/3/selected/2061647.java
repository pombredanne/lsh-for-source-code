package control;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import dao.DAOAluno;
import dao.DAOUsuario;
import model.Aluno;
import model.Usuario;

/**
 * Servlet implementation class CadastrarAlunoServlet
 */
public class CadastrarAlunoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CadastrarAlunoServlet() {
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
        String nome = request.getParameter("nome");
        String user = request.getParameter("user");
        String pass_temp = request.getParameter("pass");
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(pass_temp.getBytes()));
        String pass = hash.toString(16);
        String msg = "Usu�rio n�o Cadastrado - Login j� existe!";
        DAOUsuario daouser = new DAOUsuario();
        Usuario teste = daouser.findByLogin(user);
        if (teste == null) {
            DAOAluno daoAlu = new DAOAluno();
            try {
                daoAlu.begin();
                Aluno a = new Aluno();
                a.setNome(nome);
                a.setLogin(user);
                a.setSenha(pass);
                daoAlu.persist(a);
                daoAlu.commit();
            } catch (Exception e) {
                e.getMessage();
            }
            nome = null;
            user = null;
            pass = null;
            msg = new String("Aluno cadastrado!");
        }
        request.setAttribute("msg", msg);
        RequestDispatcher rqd = request.getRequestDispatcher("painel.jsp");
        rqd.forward(request, response);
    }
}
