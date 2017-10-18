package jamm.ldap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes a password using the MD5 algorithm.
 */
public class Md5Password extends LdapPassword {

    /**
     * Generates the raw 128-bit MD5 hash of the clear text.  Many
     * applications use the Base-64 encoded version of the MD5.
     *
     * @param clearText Clear text
     * @return A 16-byte array representing the MD5 hash of the clear
     * text.
     */
    public byte[] md5(String clearText) {
        MessageDigest md;
        byte[] digest;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(clearText.getBytes());
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e.toString());
        }
        return digest;
    }

    /**
     * Creates a hashed MD5 password in standard LDAP format.
     *
     * @param password Clear text password
     * @return MD5-hashed LDAP password
     */
    protected String doHash(String password) {
        return "{MD5}" + encodeBase64(md5(password));
    }

    /**
     * Not imlemented yet.
     *
     * @param hashedPassword A hashed password
     * @param password A clear text password
     * @return Always returns <code>false</code>
     */
    protected boolean doCheck(String hashedPassword, String password) {
        return false;
    }
}
