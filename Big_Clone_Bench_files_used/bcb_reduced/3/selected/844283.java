package com.jpress.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

public class General {

    public General() {
    }

    ;

    public String md5(String string) {
        String sen = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(string.getBytes()));
        sen = hash.toString(16);
        return sen;
    }

    public boolean validarEmail(String string) {
        Pattern padrao = Pattern.compile("[_a-z0-9-]+(.[_a-z0-9-]+)*@[a-z0-9-]+(.[a-z0-9-]+)*(.[a-z]{2,3})");
        Matcher matcher = padrao.matcher(string);
        return matcher.matches();
    }

    public String paginaOrigem(String string) {
        return string.substring(string.lastIndexOf("/") + 1, string.length());
    }

    public static void mensagemDeErro(String mensagem) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, mensagem, null));
    }

    public static void mensagemDeSucesso(String mensagem) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, mensagem, null));
    }

    public static void mensagemDeAviso(String mensagem) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, mensagem, null));
    }
}
