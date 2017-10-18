package org.systemsbiology.apps.gui.server.provider.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * @author Mark Christiansen
 *
 */
public class PasswordEncrypter {

    private static final Logger log = Logger.getLogger(PasswordEncrypter.class);

    private PasswordEncrypter() {
    }

    /**
	 * @param password password to encrypt
	 * @return Encrypted password
	 */
    public static String encryptPassword(String password) {
        if (password == null) return null;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not found", e);
            return null;
        }
        digest.reset();
        digest.update(password.getBytes());
        return hexValue(digest.digest());
    }

    static String hexValue(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int x = 0xFF & bytes[i];
            hex.append(Integer.toHexString(x));
        }
        return hex.toString();
    }

    static byte[] getBytesForHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) v;
        }
        return bytes;
    }

    /**
	 * @param args password to encrypt
	 */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a apassword to encrypt");
            System.exit(0);
        }
        System.out.println(PasswordEncrypter.encryptPassword(args[0]));
        System.exit(0);
    }
}
