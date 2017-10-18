package de.hartmut.gwt.server.ldap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 *
 * @author hartmut
 */
public class PasswordUtil {

    public static String encryptSHA(String pwd) throws NoSuchAlgorithmException {
        MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(pwd.getBytes());
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(d.digest());
    }

    public static String ldapEncryptSHA(String pwd) throws NoSuchAlgorithmException {
        String hash = encryptSHA(pwd);
        return "{SHA}" + hash;
    }
}
