package br.org.rfccj.siap;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;

    private String nome;

    private String identificacao;

    private String senha;

    private Perfil perfil;

    public Usuario(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    public void setIdentificacao(String identificacao) {
        this.identificacao = identificacao;
    }

    public void setSenha(String senha) {
        this.senha = criptografar(senha);
    }

    private String criptografar(String texto) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException cause) {
            throw new RuntimeException(cause);
        }
        return fromHexaToString(digest.digest(texto.getBytes()));
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean conferirSenha(String senha) {
        return this.senha.equals(criptografar(senha));
    }

    private static String fromHexaToString(byte[] bytes) {
        String s = new String();
        for (int i = 0; i < bytes.length; i++) s += Integer.toHexString((((bytes[i] >> 4) & 0xf) << 4) | (bytes[i] & 0xf));
        return s;
    }
}
