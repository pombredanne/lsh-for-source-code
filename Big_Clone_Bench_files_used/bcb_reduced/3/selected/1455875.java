package br.edu.ufcg.ourgridportal.server.persistencia;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.google.gwt.user.client.rpc.IsSerializable;
import sun.misc.BASE64Encoder;

public class EncryptPassword implements IsSerializable {

    public static String encrypt(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(password.getBytes());
            BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(digest.digest());
        } catch (NoSuchAlgorithmException ns) {
            ns.printStackTrace();
            return password;
        }
    }

    public static void main(String[] args) {
        System.out.println(EncryptPassword.encrypt("oi_oi"));
    }
}
