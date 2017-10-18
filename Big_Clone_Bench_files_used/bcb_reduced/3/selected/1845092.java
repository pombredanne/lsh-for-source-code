package ase.eleitweg.common;

import java.io.*;
import java.security.*;
import java.util.*;

public class User implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final int NORMAL_USER = 1;

    public static final int ADMIN_USER = 2;

    private int id, type, phone;

    private String firstname, surname, email, passwdhash, username;

    private static User nullUser = new User(0, 0, 0, "", "", "", "", "");

    public static User getNullUser() {
        return nullUser;
    }

    public boolean isNullUser() {
        if (this.equals(nullUser)) {
            return true;
        }
        return false;
    }

    public User(int id, int type, int phone, String username, String firstname, String surname, String email, String passwdhash) {
        super();
        this.id = id;
        this.type = type;
        this.phone = phone;
        this.firstname = firstname;
        this.username = username;
        this.surname = surname;
        this.email = email;
        this.passwdhash = passwdhash;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPhone() {
        return phone;
    }

    public void setPhone(int phone) {
        this.phone = phone;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswdhash() {
        return passwdhash;
    }

    public void setPasswdhash(String passwdhash) {
        this.passwdhash = passwdhash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        if (this.type == User.ADMIN_USER) {
            return true;
        }
        return false;
    }

    public String toString() {
        return firstname + " " + surname;
    }

    public static String getMD5Hash(String in) {
        StringBuffer result = new StringBuffer(32);
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(in.getBytes());
            Formatter f = new Formatter(result);
            for (byte b : md5.digest()) {
                f.format("%02x", b);
            }
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }
}
