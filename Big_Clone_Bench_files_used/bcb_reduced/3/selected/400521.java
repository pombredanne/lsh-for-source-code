package org.zxframework.util;

import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

/**
 * Generate one-way hash for encoding passwords
 * 
 * @author Michael Brewer
 * @author Bertus Dispa
 * @author David Swann
 * @see <a href="http://www.devarticles.com/c/a/Java/Password-Encryption-Rationale-and-Java-Example/">Password Encryption</a>
 * 
 * @version 0.0.1 
 **/
public final class PasswordService {

    /** The single instance of the password service. **/
    private static PasswordService instance;

    /** Hide default constructor **/
    private PasswordService() {
        super();
    }

    /**
     * Encrpts a string using SHA encryption, useful for password encryption.
     * 
     * @param pstrPlainText The text you want to encrypt.
     * @return Returns the encrypted text.
     * @throws Exception Thrown if encrypt fails 
     */
    public String encrypt(String pstrPlainText) throws Exception {
        if (pstrPlainText == null) {
            return "";
        }
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(pstrPlainText.getBytes("UTF-8"));
        byte raw[] = md.digest();
        return (new BASE64Encoder()).encode(raw);
    }

    /**
     * @return Returns PasswordService instance.
     */
    public static synchronized PasswordService getInstance() {
        if (instance == null) {
            return new PasswordService();
        }
        return instance;
    }
}
