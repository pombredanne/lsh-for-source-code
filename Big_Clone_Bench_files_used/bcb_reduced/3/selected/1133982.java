package beans;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;

public class Token {

    private String token;

    public Token(HttpServletRequest req) throws ServletException {
        HttpSession session = req.getSession(true);
        long systime = System.currentTimeMillis();
        byte[] time = new Long(systime).toString().getBytes();
        byte[] id = session.getId().getBytes();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(id);
            md5.update(time);
            token = toHex(md5.digest());
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public String toString() {
        return token;
    }

    private String toHex(byte[] digest) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < digest.length; i++) buf.append(Integer.toHexString((int) digest[i] & 0x00ff));
        return buf.toString();
    }
}
