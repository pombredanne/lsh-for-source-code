package de.lot.auth;

import java.security.MessageDigest;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.util.Hex;

/**
 * PasswordManager converts the actualt inserted password into a hash-value.
 * In that way the password is not saved in clear text to the database.
 * 
 * @author Stefan Kohler <kohler.stefan@gmail.com>
 */
@Name("passwordManager")
@BypassInterceptors
public class PasswordManager {

    private String digestAlgorithm;

    private String charset;

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

    public String hash(String plainTextPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
            digest.update(plainTextPassword.getBytes(charset));
            byte[] rawHash = digest.digest();
            return new String(Hex.encodeHex(rawHash));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PasswordManager instance() {
        return (PasswordManager) Component.getInstance(PasswordManager.class, ScopeType.EVENT);
    }
}
