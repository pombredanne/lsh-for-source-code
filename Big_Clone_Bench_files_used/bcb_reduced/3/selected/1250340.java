package dev.cinema.struts;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author alessia
 */
public class CreateClientAccountForm extends org.apache.struts.validator.ValidatorForm {

    private String username;

    private String name;

    private String surname;

    private String email;

    private String phoneNumber;

    private String password;

    private String confirmPassword;

    private String retPage;

    public String getRetPage() {
        return retPage;
    }

    public void setRetPage(String retPage) {
        this.retPage = retPage;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public CreateClientAccountForm() {
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
