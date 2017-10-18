package com.autoescola.core.util.security;

import com.autoescola.core.designerpatterns.TemplateMethod;
import com.autoescola.core.entity.security.User;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 *
 * @author leonardo luz fernandes
 * @version 0.1
 * @since 04/11/2010
 */
public abstract class LogonUtils {

    private static final String DIGEST_TYPE = "SHA-1";

    private static final String CHARSET = "UTF-8";

    private static final String RANDOM_ALGORITHM = "SHA1PRNG";

    private static final int BSALT_SIZE = 32;

    public static final int NUMBER_OF_INTERACTIONS = 100;

    private User user;

    public User getUser() throws Exception {
        if (this.user != null) {
            return user;
        }
        throw new Exception("Invalid username and password.");
    }

    public final boolean authenticate(String username, String password) throws Exception {
        boolean userExist = true;
        if (isBlank(username) || isBlank(password)) {
            userExist = false;
            username = "";
            password = "";
        }
        this.user = load(username);
        String digest = null;
        String salt = null;
        if (user != null) {
            digest = user.getPassword();
            salt = user.getSalt();
            if (isBlank(digest) || isBlank(salt)) {
                this.user = null;
                throw new Exception("Inconsistent salt or password altered.");
            }
        } else {
            userExist = false;
            digest = "000000000000000000000000000=";
            salt = "00000000000=";
            this.user = null;
        }
        byte[] bDigest = base64ToByte(digest);
        byte[] bSalt = base64ToByte(salt);
        byte[] pDigest = getHashValue(NUMBER_OF_INTERACTIONS, password, bSalt);
        return Arrays.equals(pDigest, bDigest) && userExist;
    }

    @TemplateMethod
    protected abstract User load(String username);

    public final void createUser(String username, String password) throws Exception {
        if (isBlank(username) == false && isBlank(password) == false) {
            if (load(username) == null) {
                shadow(username, password);
            } else {
                throw new Exception("Username taken.");
            }
        }
        throw new IllegalArgumentException("Username and password cannot be empty.");
    }

    private void shadow(String username, String password) throws Exception {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] bsalt = new byte[BSALT_SIZE];
        random.nextBytes(bsalt);
        byte[] digest = getHashValue(NUMBER_OF_INTERACTIONS, password, bsalt);
        String pwd = byteToBase64(digest);
        String salt = byteToBase64(bsalt);
        this.user = new User();
        user.setUsername(username);
        user.setPassword(pwd);
        user.setSalt(salt);
    }

    public final void newPassword(String username, String oldPassword, String newPassword) throws Exception {
        if (authenticate(username, oldPassword) && isBlank(newPassword) == false) {
            shadow(username, newPassword);
        }
    }

    public final byte[] getHashValue(int numberOfInteractions, String password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(DIGEST_TYPE);
        digest.reset();
        digest.update(salt);
        byte[] input = digest.digest(password.getBytes(CHARSET));
        for (int i = 0; i < numberOfInteractions; i++) {
            digest.reset();
            input = digest.digest(input);
        }
        return input;
    }

    private final byte[] base64ToByte(String data) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(data);
    }

    private final String byteToBase64(byte[] data) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    private final boolean isBlank(String value) {
        return value == null ? true : value.trim().length() == 0;
    }
}
