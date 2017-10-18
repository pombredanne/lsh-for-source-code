package jamm.ldap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes a password using the SHA algorithm.
 */
public class ShaPassword extends LdapPassword {

    /**
     * Generates the raw 160-bit SHA hash of the clear text.
     *
     * @param clearText Clear text, as a string
     * @return A 20-byte array representing the SHA hash of the clear
     * text
     */
    public byte[] sha(String clearText) {
        return sha(clearText.getBytes());
    }

    /**
     * Generates the raw 160-bit SHA hash of the clear text.
     *
     * @param clearText Clear text, as a byte array
     * @return A 20-byte array representing the SHA hash of the clear
     * text
     */
    public byte[] sha(byte[] clearText) {
        MessageDigest md;
        byte[] digest;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(clearText);
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e.toString());
        }
        return digest;
    }

    /**
     * Creates a hashed SHA password in standard LDAP format.
     *
     * @param password Clear text password
     * @return SHA-hashed LDAP password
     */
    protected String doHash(String password) {
        return "{SHA}" + encodeBase64(sha(password));
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
