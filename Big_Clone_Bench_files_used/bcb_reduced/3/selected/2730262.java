package org.knopflerfish.service.um.useradmin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for one-way encryption of, for example, user passwords.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class PasswdUtil {

    private static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    /**
     * Create salt and the a digest of the salt and the message.
     * 
     * @param message
     *            the message to calculate a digest for
     * @param digest
     *            buffer to write the resulting digest to, must be at least 20
     *            bytes long.
     * @return the salt used when calculating the digest.
     */
    public static synchronized byte[] saltAndDigest(String message, byte[] digest) {
        byte[] salt = Long.toString(System.currentTimeMillis()).getBytes();
        try {
            byte[] msg = message.getBytes("UTF-8");
            md.update(salt);
            md.update(msg);
            md.digest(digest, 0, 20);
        } catch (Exception ex) {
        }
        return salt;
    }

    /**
     * Create a digest from salt and a message.
     * 
     * @param message
     *            the message to calculate a digest for
     * @param salt
     *            the salt to use.
     * @return the resulting digest, will be 20 bytes long.
     */
    public static synchronized byte[] digest(String message, byte[] salt) {
        try {
            byte[] msg = message.getBytes("UTF-8");
            md.update(salt);
            md.update(msg);
        } catch (Exception ex) {
        }
        return md.digest();
    }
}
