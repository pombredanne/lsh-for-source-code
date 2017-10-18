package dev.cinema.struts;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author brushtyler
 */
public class LoginForm extends org.apache.struts.validator.ValidatorForm {

    private String username;

    private String password;

    public String getUsername() {
        return this.username;
    }

    /**
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *
     */
    public LoginForm() {
        super();
    }

    public String md5(String password) {
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
        }
        m.update(password.getBytes(), 0, password.length());
        return new BigInteger(1, m.digest()).toString(16);
    }
}
