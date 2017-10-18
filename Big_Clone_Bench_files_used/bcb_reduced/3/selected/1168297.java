package br.com.geostore.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

public class Security {

    public String geraSenha(String servletName) {
        String senha = null;
        Date data = new Date();
        SimpleDateFormat formatador = new SimpleDateFormat("dd/MM/yyyy");
        senha = formatador.format(data);
        senha = senha + "-GeoStore";
        senha = senha + "-" + servletName;
        return senha;
    }

    public String crypto(String input) {
        if (input != null) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                StringBuffer buffer = new StringBuffer();
                md5.reset();
                byte[] senha = md5.digest(input.getBytes());
                for (int i = 0; i < senha.length; i++) {
                    buffer.append(senha[i++]);
                }
                return buffer.toString().replace("-", "");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public boolean clientAutenticado(HttpServletRequest request, String servletName) {
        String senhaClient = request.getParameter("senhaClient");
        String senhaServer = this.crypto(this.geraSenha(servletName));
        return senhaClient.equals(senhaServer);
    }
}
