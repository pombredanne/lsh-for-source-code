package com.velocityme.utility;

import com.velocityme.interfaces.ServiceUnavailableException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 *
 * @author  Robert Crida Work
 */
public class PasswordEncrypter {

    private static PasswordEncrypter m_instance;

    /** Creates a new instance of PasswordEncrypter */
    private PasswordEncrypter() {
    }

    public synchronized String encrypt(String p_plainText) throws ServiceUnavailableException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
        try {
            md.update(p_plainText.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public static synchronized PasswordEncrypter getInstance() {
        if (m_instance == null) m_instance = new PasswordEncrypter();
        return m_instance;
    }
}
