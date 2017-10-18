package net.sf.jerkbot.plugins.authentication.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;

/**
 * This class is still not used but will be used again to store the password hash instead of clearer text
 * A pure encryption will be used(ie. not password retrieval), a passphrase might be added later to reset a password
 */
public class EncryptionUtil {

    private static final Logger Log = LoggerFactory.getLogger(EncryptionUtil.class.getName());

    public static String encode(String username, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(username.getBytes());
            digest.update(password.getBytes());
            return new String(digest.digest());
        } catch (Exception e) {
            Log.error("Error encrypting credentials", e);
        }
        return null;
    }
}
