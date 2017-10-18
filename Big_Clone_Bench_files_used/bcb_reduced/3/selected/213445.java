package br.softwaresolutions.virtualstore.util;

import java.security.MessageDigest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Classe respons�vel pela v�lida��o das requisi��es feitas pelos usu�rios atrav�s
 * sess�o ativa para o mesmo.
 */
public class CommandToken {

    /**
     * Cria um token de transa��o �nico e o armazena como uma cadeia de caracteres
     * hexadecimais na sess�o usu�rio e na sess�o como atributo.
     * @param req
     */
    public static void set(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        long sysTime = System.currentTimeMillis();
        byte[] time = new Long(sysTime).toString().getBytes();
        byte[] id = session.getId().getBytes();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(id);
            md5.update(time);
            String token = toHex(md5.digest());
            req.setAttribute("token", token);
            session.setAttribute("token", token);
        } catch (Exception e) {
            System.err.println("Unable to calculate MD5 digests");
        }
    }

    /**
     * Este m�todo � usado para validar uma solicita��o. Realiza � procura da
     * exist�ncia de um token na solicita��o e na sess�o, comparando-os.
     * @param req
     * @return
     */
    public static boolean isValid(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        String requestToken = req.getParameter("token");
        String sessionToken = (String) session.getAttribute("token");
        if (requestToken == null || sessionToken == null) return false; else return requestToken.equals(sessionToken);
    }

    private static String toHex(byte[] digest) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            buf.append(Integer.toHexString((int) digest[i] & 0x00ff));
        }
        return buf.toString();
    }
}
