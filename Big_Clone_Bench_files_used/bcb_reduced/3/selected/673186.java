package net.yapbam.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.jdo.annotations.*;
import net.yapbam.server.exceptions.BadPassWordException;

@PersistenceCapable
public class User {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private String email;

    @Persistent
    private byte[] pwd;

    @Persistent
    private boolean isAdmin;

    public User(String email, String pwd, boolean isAdmin) {
        super();
        this.email = email;
        this.isAdmin = isAdmin;
        setPwd(pwd);
    }

    public void setPwd(String pwd) {
        this.pwd = encrypt(pwd);
    }

    public String getEMail() {
        return email;
    }

    public void checkPassword(String pwd) throws BadPassWordException {
        byte[] encrypted = encrypt(pwd);
        if (encrypted.length != this.pwd.length) {
            for (int i = 0; i < encrypted.length; i++) {
                if (encrypted[i] != this.pwd.length) throw new BadPassWordException();
            }
        }
    }

    private static byte[] encrypt(String password) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(password.getBytes("ISO8859-1"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }
}
