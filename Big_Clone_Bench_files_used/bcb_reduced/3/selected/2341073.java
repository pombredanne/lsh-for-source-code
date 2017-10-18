package seedpod.webapp.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

public final class PasswordService {

    private static PasswordService _instance;

    private PasswordService() {
    }

    public synchronized String encrypt(String plaintext) throws SystemUnavailableException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new SystemUnavailableException(e.getMessage());
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new SystemUnavailableException(e.getMessage());
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public static synchronized PasswordService getInstance() {
        if (_instance == null) {
            _instance = new PasswordService();
        }
        return _instance;
    }

    public static void main(String[] args) {
        String password1 = "seedpod";
        String password2 = "seedpod";
        try {
            String enc1 = PasswordService.getInstance().encrypt(password1);
            String enc2 = PasswordService.getInstance().encrypt(password2);
            System.out.println(enc1);
            System.out.println("test enc1 = enc2:  " + enc1.equals(enc2));
        } catch (SystemUnavailableException e) {
            System.err.println(e.getMessage());
        }
    }
}
