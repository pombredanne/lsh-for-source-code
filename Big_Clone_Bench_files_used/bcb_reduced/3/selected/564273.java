package org.login.utils;

import java.security.MessageDigest;
import java.util.Arrays;
import org.makagiga.commons.MLogger;
import org.makagiga.commons.annotation.Uninstantiable;
import org.makagiga.commons.crypto.CryptoUtils;
import org.makagiga.commons.crypto.MasterKey;
import org.makagiga.commons.io.Checksum;

/**
 * SHA 512 Adapter for easier use of SHA functions
 * @author Thotheolh
 */
public class SHAAdapter {

    @Uninstantiable
    private SHAAdapter() {
    }

    /**
     * Sets the master/login password to {@code password}.
     * {@code null} or empty array clears the current master/login password.
     * The password text will be encrypted internally.
     *
     * @param password the password in plain text
     *
     * @return {@code true} on success; otherwise {@code false}
     */
    public static boolean setMasterPassword(final char[] password) {
        if (System.getSecurityManager() == null) return false;
        try {
            return MasterKey.setMasterPassword(password);
        } catch (SecurityException exception) {
            MLogger.exception(exception);
            return false;
        }
    }

    public static byte[] doHashSHA(char[] password) {
        try {
            byte[] buffer = new String(password).getBytes("UTF8");
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(buffer);
            CryptoUtils.clear(password);
            CryptoUtils.clear(buffer);
            return md.digest();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String doHashAuto(char[] password) {
        if (Settings.sha.get()) {
            return Checksum.toString(doHashSHA(password));
        }
        return BCrypt.hashpw(new String(password), BCrypt.gensalt());
    }

    public static boolean doMatchSHA(char[] match, byte[] matchAgainst) {
        byte[] matchBytes = doHashSHA(match);
        if (Arrays.equals(matchAgainst, matchBytes)) {
            CryptoUtils.clear(matchAgainst);
            CryptoUtils.clear(matchBytes);
            System.err.println("SHAAdapter- doMatch(): Match OK!");
            return true;
        } else {
            System.err.println("SHAAdapter- doMatch(): Match FAILED!");
            CryptoUtils.clear(matchAgainst);
            CryptoUtils.clear(matchBytes);
            return false;
        }
    }

    public static boolean authenticate(char[] password) {
        try {
            return doMatchAuto(password, Settings.pass.get());
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean doMatchAuto(char[] password, String matchAgainst) {
        matchAgainst = matchAgainst.trim();
        if (Settings.sha.get()) {
            String matchBytes = doHashAuto(password);
            if (matchAgainst.equals(matchBytes)) {
                System.err.println("SHAAdapter- doHexMatch(): Match OK!");
                Settings.sha.no();
                Settings.pass.set(doHashAuto(password));
                CryptoUtils.clear(matchAgainst.toCharArray());
                CryptoUtils.clear(matchBytes.toCharArray());
                CryptoUtils.clear(password);
                Settings.sync();
                return true;
            } else {
                System.err.println("SHAAdapter- doHexMatch(): Match FAILED!");
                CryptoUtils.clear(matchAgainst.toCharArray());
                CryptoUtils.clear(matchBytes.toCharArray());
                CryptoUtils.clear(password);
                return false;
            }
        } else {
            return BCrypt.checkpw(new String(password), matchAgainst);
        }
    }
}
