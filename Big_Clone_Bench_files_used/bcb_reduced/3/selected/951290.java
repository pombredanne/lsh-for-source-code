package de.haumacher.timecollect.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.DecodingException;
import de.haumacher.timecollect.common.config.ValueFactory;

public class UserManager {

    public static boolean checkPassword(User user, String plaintextPassword) {
        byte[] passwordBytes = hashPassword(fromString(user.getSalt()), plaintextPassword);
        return Arrays.equals(passwordBytes, fromString(user.getPassword()));
    }

    private static SecureRandom RND = new SecureRandom();

    public static User create(String uid, String plaintextPassword) {
        byte[] saltBytes = new byte[8];
        RND.nextBytes(saltBytes);
        byte[] passwordBytes = hashPassword(saltBytes, plaintextPassword);
        return createUser(uid, toString(saltBytes), toString(passwordBytes));
    }

    protected static byte[] hashPassword(byte[] saltBytes, String plaintextPassword) throws AssertionError {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw (AssertionError) new AssertionError("No MD5 message digest supported.").initCause(ex);
        }
        digest.update(saltBytes);
        try {
            digest.update(plaintextPassword.getBytes("utf-8"));
        } catch (UnsupportedEncodingException ex) {
            throw (AssertionError) new AssertionError("No UTF-8 encoding supported.").initCause(ex);
        }
        byte[] passwordBytes = digest.digest();
        return passwordBytes;
    }

    private static String toString(byte[] bytes) {
        return Base64.encode(bytes);
    }

    private static byte[] fromString(String base64) {
        try {
            return Base64.decode(base64);
        } catch (DecodingException ex) {
            throw (AssertionError) new AssertionError("Base64 decoding failed.").initCause(ex);
        }
    }

    public static User createUser(String uid, String salt, String password) {
        User result = createUser();
        result.setUid(uid);
        result.setSalt(salt);
        result.setPassword(password);
        return result;
    }

    public static User createUser() {
        User result = ValueFactory.newInstance(User.class);
        return result;
    }

    public static void main(String[] args) {
        User root = create("root", "123");
        System.out.println(root);
    }
}
