package uk.azdev.openfire.net.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtil {

    private static final String ULTIMATE_ARENA_POSTFIX = "UltimateArena";

    CryptoUtil() {
        throw new RuntimeException("CryptoUtil is not meant to be instantiated");
    }

    public static String getHashedPassword(String username, String password, String salt) {
        String userPassHash = sha1HashAsHexString(username + password + ULTIMATE_ARENA_POSTFIX);
        return sha1HashAsHexString(userPassHash + salt);
    }

    public static String sha1HashAsHexString(String s) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(IOUtil.encodeString(s));
            return IOUtil.printByteArray(sha1.digest(), false, false);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No cryptographic provider for SHA-1 configured");
        }
    }
}
