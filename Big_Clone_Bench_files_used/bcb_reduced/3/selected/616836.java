package com.mkk.kenji1016.util;

import org.springframework.util.StringUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Support password encryption(MD5[32]);
 * get random password .
 *
 * @author Shengzhao Li
 */
public class PasswordHandler {

    /**
     * Password encryption type: MD5
     */
    public static final String PASSWORD_ENCRYPTION_TYPE = "MD5";

    /**
     * Logger
     */
    private static LogHelper log = LogHelper.create(PasswordHandler.class);

    /**
     * Return a random password from {@link UUID},
     * the length is 8.
     *
     * @return Random password
     */
    public static String randomPassword() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, 8);
    }

    /**
     * Encrypt  password ,if original password is empty,
     * will call {@link #randomPassword()}  get a random original password.
     *
     * @param originalPassword Original password
     * @return Encrypted password
     */
    public static String encryptPassword(String originalPassword) {
        if (!StringUtils.hasText(originalPassword)) {
            originalPassword = randomPassword();
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance(PASSWORD_ENCRYPTION_TYPE);
            md5.update(originalPassword.getBytes());
            byte[] bytes = md5.digest();
            int value;
            StringBuilder buf = new StringBuilder();
            for (byte aByte : bytes) {
                value = aByte;
                if (value < 0) {
                    value += 256;
                }
                if (value < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(value));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            log.debug("Do not encrypt password,use original password", e);
            return originalPassword;
        }
    }
}
