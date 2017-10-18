package de.hbrs.inf.atarrabi.action.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.log.Log;

/**
 * Class provides functionality for password handling.
 * 
 * @author Florian Quadt
 */
@Name("passwordManager")
@BypassInterceptors
public class PasswordManager {

    @Logger
    private Log log;

    private String digestAlgorithm;

    private String charset;

    /**
	 * Calculates the hash value of the given password.
	 * 
	 * @param plainTextPassword the password in plain text
	 * @return the password hash in hex
	 */
    public String hash(String plainTextPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
            digest.update(plainTextPassword.getBytes(charset));
            byte[] rawHash = digest.digest();
            return new String(org.jboss.seam.util.Hex.encodeHex(rawHash));
        } catch (NoSuchAlgorithmException e) {
            log.error("Digest algorithm #0 to calculate the password hash will not be supported.", digestAlgorithm);
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            log.error("The Character Encoding #0 is not supported", charset);
            throw new RuntimeException(e);
        }
    }

    public String getDigestAlgorithm() {
        return this.digestAlgorithm;
    }

    public void setDigestAlgorithm(String algorithm) {
        this.digestAlgorithm = algorithm;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}
