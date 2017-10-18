package control;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import model.Usuario;
import dao.DAOUsuario;

public class EditaUsuarioBean {

    private Usuario atual;

    private String login, senha, nome;

    private boolean novaSenha, chk;

    public boolean isChk() {
        return chk;
    }

    public void setChk(boolean chk) {
        this.chk = chk;
    }

    public boolean isNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(boolean novaSenha) {
        this.novaSenha = novaSenha;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Usuario getAtual() {
        return atual;
    }

    public void setAtual(Usuario atual) {
        this.atual = atual;
    }

    public void carregar() {
        login = atual.getLogin();
        nome = atual.getNome();
        novaSenha = false;
        chk = false;
    }

    public void atualizar() {
        DAOUsuario daouser = new DAOUsuario();
        Usuario u = daouser.findByLogin(atual.getLogin());
        u.setNome(nome);
        if (novaSenha) {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            BigInteger hash = new BigInteger(1, md.digest(senha.getBytes()));
            String pass = hash.toString(16);
            u.setSenha(pass);
        }
        daouser.begin();
        daouser.merge(u);
        daouser.commit();
        senha = null;
    }

    public void rendNovaSenha() {
        if (novaSenha) novaSenha = false; else novaSenha = true;
    }
}
