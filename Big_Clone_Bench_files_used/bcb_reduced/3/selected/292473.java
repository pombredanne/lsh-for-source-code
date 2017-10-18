package openadmin.jaas;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.tomcat.util.buf.HexUtils;

public class Digest {

    static MessageDigest md = null;

    public Digest() {
        this("MD5");
    }

    public Digest(String digest) {
        try {
            md = MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException e) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
            }
        }
    }

    public String digest(String credentials) {
        if (md == null) {
            return credentials;
        }
        synchronized (this) {
            try {
                md.reset();
                md.update(credentials.getBytes());
                return (HexUtils.convert(md.digest()));
            } catch (Exception e) {
                return credentials;
            }
        }
    }
}
