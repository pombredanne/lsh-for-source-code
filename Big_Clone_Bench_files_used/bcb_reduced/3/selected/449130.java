package com.enterprise.app.framework.infra.security.util;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;
import org.springframework.security.authentication.encoding.PasswordEncoder;

/**
 * @author Gurumurthy Sithuraj
 *
 */
public class MD5PasswordEncoder implements PasswordEncoder {

    @Override
    public String encodePassword(final String password, final Object salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(salt.toString().getBytes());
            byte[] passwordHash = digest.digest(password.getBytes());
            Base64 encoder = new Base64();
            byte[] encoded = encoder.encode(passwordHash);
            return new String(encoded);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isPasswordValid(final String cypherPass, final String password, final Object salt) {
        return cypherPass.equals(this.encodePassword(password, salt));
    }
}
