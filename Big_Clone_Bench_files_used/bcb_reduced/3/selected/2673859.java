package org.fao.waicent.kids.giews.service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;

public class PasswordService {

    public String encrypt(String password) {
        String encrypted_pass = "";
        ByteArrayOutputStream output = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes("UTF-8"));
            byte byte_array[] = md.digest();
            output = new ByteArrayOutputStream(byte_array.length);
            output.write(byte_array);
            encrypted_pass = output.toString("UTF-8");
            System.out.println("password: " + encrypted_pass);
        } catch (Exception e) {
            System.out.println("Exception thrown: " + e.getMessage());
        }
        return encrypted_pass;
    }
}
