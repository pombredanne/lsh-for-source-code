package com.redhat.gs.mrlogistics.auth;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import com.redhat.gs.mrlogistics.util.Base64;

/**
 * This is the default password hashing strategy for authenticating users
 */
@Entity
public class Sha1User extends LocalUser implements java.io.Serializable {

    public Sha1User() {
        super();
    }

    @Transient
    public void setPassword(String password) throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        digest.update(salt);
        byte[] hash = digest.digest(password.getBytes());
        super.setSalt(byteToBase64(salt));
        super.setPassHash(byteToBase64(hash));
    }

    public boolean isPassword(String password) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] salt = base64ToByte(super.getSalt());
        digest.update(salt);
        byte[] hash = digest.digest(password.getBytes());
        System.out.println(getPassHash() + " == " + byteToBase64(hash) + "?");
        return getPassHash().equals(byteToBase64(hash));
    }

    public static String byteToBase64(byte[] data) {
        return Base64.encodeBytes(data);
    }

    public static byte[] base64ToByte(String data) throws IOException {
        return Base64.decode(data);
    }
}
