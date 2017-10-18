package com.jguigen.secure;

import java.security.*;

/**
 * @author Hale Pringle
 *
 * 
 */
public class SimpleEncrypt {

    int id;

    String name;

    String pwd;

    public SimpleEncrypt(int id, String name, String pwd) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
    }

    public String execute() {
        String strId = "";
        try {
            strId = Integer.toString(this.id);
        } catch (Exception ex) {
            strId = "";
        }
        String password = this.name.trim() + "|" + strId + "|" + pwd.trim();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = password.getBytes("UTF-8");
            md.update(buf);
            byte[] digest = md.digest();
            return new String(digest);
        } catch (Exception ex) {
            System.out.println("error setting up message digest (hash)");
        }
        return "error";
    }
}
