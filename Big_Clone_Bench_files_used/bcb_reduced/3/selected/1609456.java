package encoders;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5PasswordEncoder implements IPasswordEncoder {

    @Override
    public String encode(String password) {
        String hash = null;
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(password.getBytes(), 0, password.length());
            hash = String.format("%1$032X", new BigInteger(1, m.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
