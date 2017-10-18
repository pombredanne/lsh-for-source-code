package es.manuel.maa.util;

import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;

public class PasswordEncoder implements org.springframework.security.authentication.encoding.PasswordEncoder {

    @Autowired
    @Qualifier(value = "digestAlgorithm")
    private String digestAlgorithm;

    @Override
    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        try {
            MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
            digest.reset();
            digest.update(((String) salt).getBytes("UTF-8"));
            return new String(digest.digest(rawPass.getBytes("UTF-8")));
        } catch (Throwable e) {
            throw new DataAccessException("Error al codificar la contrase�a", e) {

                private static final long serialVersionUID = 3880106673612870103L;
            };
        }
    }

    @Override
    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws DataAccessException {
        try {
            MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
            digest.reset();
            digest.update(((String) salt).getBytes("UTF-8"));
            String encodedRawPass = new String(digest.digest(rawPass.getBytes("UTF-8")));
            return encodedRawPass.equals(encPass);
        } catch (Throwable e) {
            throw new DataAccessException("Error al codificar la contrase�a", e) {

                private static final long serialVersionUID = -302443565702455874L;
            };
        }
    }
}
