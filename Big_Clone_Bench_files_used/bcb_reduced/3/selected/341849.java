package control;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import model.Aluno;
import model.Professor;
import model.Turma;
import model.Usuario;
import dao.DAOAluno;
import dao.DAOProfessor;
import dao.DAOTurma;
import dao.DAOUsuario;

public class PainelUsuarioBean {

    private String msg, nome, login, senha, tipo, logDel;

    public String getLogDel() {
        return logDel;
    }

    public void setLogDel(String logDel) {
        this.logDel = logDel;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<Aluno> getListaAlunos() {
        DAOAluno daoAlu = new DAOAluno();
        List<Aluno> lista = daoAlu.findAll();
        return lista;
    }

    public List<Professor> getListaProfessores() {
        DAOProfessor daoPro = new DAOProfessor();
        List<Professor> lista = daoPro.findAll();
        return lista;
    }

    public void cadastrar() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(senha.getBytes()));
        String pass = hash.toString(16);
        msg = "Usu�rio n�o Cadastrado - Login j� existe!";
        DAOUsuario daouser = new DAOUsuario();
        Usuario teste = daouser.findByLogin(login);
        if (teste == null) {
            if (tipo.equals("Aluno")) {
                DAOAluno daoAlu = new DAOAluno();
                try {
                    daoAlu.begin();
                    Aluno a = new Aluno();
                    a.setNome(nome);
                    a.setLogin(login);
                    a.setSenha(pass);
                    daoAlu.persist(a);
                    daoAlu.commit();
                    msg = new String("Aluno cadastrado!");
                } catch (Exception e) {
                    e.getMessage();
                }
            }
            if (tipo.equals("Professor")) {
                DAOProfessor daoPro = new DAOProfessor();
                try {
                    daoPro.begin();
                    Professor p = new Professor();
                    p.setNome(nome);
                    p.setLogin(login);
                    p.setSenha(pass);
                    daoPro.persist(p);
                    daoPro.commit();
                    msg = new String("Professor cadastrado!");
                } catch (Exception e) {
                    e.getMessage();
                }
            }
            nome = null;
            login = null;
            senha = null;
        }
    }

    public void deletar() {
        DAOUsuario daouser = new DAOUsuario();
        DAOTurma daoturma = new DAOTurma();
        Usuario u = daouser.findByLogin(logDel);
        if (tipo.equals("Aluno")) {
            List<Turma> tms = ((Aluno) u).getTurmas();
            for (int i = 0; i < tms.size(); ) {
                Turma turma = tms.get(i);
                turma.removeAluno((Aluno) u);
                daoturma.merge(turma);
            }
        } else {
            List<Turma> tms = ((Professor) u).getTurmas();
            for (int i = 0; i < tms.size(); ) {
                Turma turma = tms.get(i);
                turma.setProfessor(null);
            }
        }
        daouser.merge(u);
        daouser.remove(u);
        daouser.commit();
    }
}
