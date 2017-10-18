package hci.gnomex.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

public final class EncrypterService {

    private static EncrypterService instance;

    private EncrypterService() {
    }

    public synchronized String encrypt(String plaintext) {
        if (plaintext == null || plaintext.equals("")) {
            return plaintext;
        }
        String hash = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
            byte raw[] = md.digest();
            hash = Base64.encodeBase64String(raw).replaceAll("\r\n", "");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
        return hash;
    }

    public static synchronized EncrypterService getInstance() {
        if (instance == null) {
            return new EncrypterService();
        } else {
            return instance;
        }
    }
}
