package org.promotego.logic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.promotego.exceptions.NestedException;
import org.promotego.interfaces.PasswordHashTool;

public class Sha1Hash implements PasswordHashTool {

    public boolean match(String cryptedPassword, String password) {
        return hash(password).equals(cryptedPassword);
    }

    public String hash(String password) {
        MessageDigest sha1Digest;
        try {
            sha1Digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw NestedException.wrap(e);
        }
        sha1Digest.update(password.getBytes());
        StringBuilder retval = new StringBuilder("sha1:");
        retval.append(new String(Base64.encodeBase64(sha1Digest.digest())));
        return retval.toString();
    }
}
