package au.org.ala.cas.encoding;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jasig.cas.authentication.handler.PasswordEncoder;

public class MyPasswordEncoder implements PasswordEncoder {

    private String salt;

    private String algorithm;

    private boolean base64Encoding;

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setBase64Encoding(boolean base64Encoding) {
        this.base64Encoding = base64Encoding;
    }

    public String encode(final String password) {
        String salted = password + "{" + this.salt + "}";
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(this.algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        byte[] digest;
        try {
            digest = messageDigest.digest(salted.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported!");
        }
        if (base64Encoding) {
            return new String(Base64.encode(digest));
        } else {
            return new String(digest);
        }
    }
}
