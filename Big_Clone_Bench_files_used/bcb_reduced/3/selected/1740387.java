package fr.insa.rennes.pelias.pexecutor.login;

import fr.insa.rennes.pelias.pexecutor.SessionBean1;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;

public class LoginBean {

    private String id;

    private String password;

    private Map<String, String> passwords;

    /** Creates a new instance of LoginBean */
    public LoginBean() {
        passwords = new Password().loadPasswords();
        id = "";
        password = "";
    }

    public LoginBean(String login, String pass) {
        id = login;
        password = pass;
        passwords = new Password().loadPasswords();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Vérifie si les identifiants de l'utilisateur sont corrects
     */
    public boolean validate() {
        if (getPasswords().containsKey(id)) {
            try {
                String pass = getPasswords().get(id);
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                byte[] pwd = messageDigest.digest(password.getBytes());
                String code = "";
                for (int i = 0; i < pwd.length; i++) {
                    code += pwd[i];
                }
                boolean result = code.equals(pass);
                if (result && id.equals("admin")) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    if (context != null) {
                        SessionBean1 session = (SessionBean1) context.getExternalContext().getSessionMap().get("SessionBean1");
                        if (session == null) {
                            session = new SessionBean1();
                            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("SessionBean1", session);
                        }
                        session.setAdmin(true);
                    }
                }
                return result;
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * @return the passwords
     */
    public Map<String, String> getPasswords() {
        return passwords;
    }

    /**
     * @param passwords the passwords to set
     */
    public void setPasswords(Map<String, String> passwords) {
        this.passwords = passwords;
    }
}
