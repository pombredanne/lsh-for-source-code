package jsf;

import beans.UserSession;
import entity.Users;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author bezpechnyi
 */
@ManagedBean(name = "authorizationController")
@Stateless
public class AuthorizationController {

    private MessageDigest md;

    private String login;

    private String password;

    @EJB
    private beans.UsersFacade users;

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String authorize() {
        new UserSession().setCurrentUser(getUser());
        return "faces/index.xhtml";
    }

    private Users getUser() {
        return users.findByLoginAndPassword(login, md5(password));
    }

    public void logout() {
        new UserSession().logout();
    }

    private String md5(String str) {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(AuthorizationController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        byte[] bs = md.digest(str.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bs.length; i++) sb.append(Integer.toString((bs[i] & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }
}
