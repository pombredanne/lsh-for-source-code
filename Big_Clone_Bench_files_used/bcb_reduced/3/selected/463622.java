package quizgame.protocol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author rheo
 */
public abstract class Authenticate implements Packet {

    private String username;

    private byte[] passwordHash;

    public Authenticate(String username, String password) throws IOException {
        MessageDigest md;
        this.username = username;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Your system does not support the SHA algorithm.");
        }
        try {
            passwordHash = md.digest(password.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IOException("Failed to encode the password into bytes.");
        }
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public String getUsername() {
        return username;
    }
}
