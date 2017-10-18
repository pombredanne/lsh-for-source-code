package com.philip.journal.login.service.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import org.jasypt.util.password.StrongPasswordEncryptor;
import com.philip.journal.core.bean.User;

/**
 * @author cry30
 */
public final class PasswordUtil {

    /** Class logger instance. */
    private static Logger logger = Logger.getLogger(PasswordUtil.class);

    /** Encyptor instance. */
    private static StrongPasswordEncryptor spe = new StrongPasswordEncryptor();

    /** Utility class cannot have public constructor. */
    private PasswordUtil() {
    }

    /** Encrypt key. Not sure if we can change this. */
    private static final int ENCRYPT_KEY = 0xFF;

    /**
     * Encrypts the password.
     * 
     * @param user user entity.
     * @param password the raw password to encrypt.
     * @return encrypted password.
     */
    public static String encrypt(final User user, final String password) {
        return spe.encryptPassword(sha512Encrypt(user, password));
    }

    /**
     * RTFC.
     * 
     * @param user user entity.
     * @param password the raw password to encrypt.
     * @return encrypted password.
     */
    private static String sha512Encrypt(final User user, final String password) {
        final StringBuilder passwordClone = new StringBuilder(password);
        passwordClone.append(user.getUsername());
        final byte[] defaultBytes = passwordClone.toString().getBytes();
        String retval = null;
        try {
            final MessageDigest algorithm = MessageDigest.getInstance("SHA-512");
            algorithm.reset();
            algorithm.update(defaultBytes);
            final byte[] messageDigest = algorithm.digest();
            final StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(ENCRYPT_KEY & messageDigest[i]));
            }
            retval = hexString.toString();
        } catch (final NoSuchAlgorithmException nsae) {
            logger.debug(nsae.getMessage(), nsae);
        }
        return retval;
    }

    /**
     * Validates if the given password is valid.
     * 
     * @param user user entity object.
     * @param password - the password to validate against the user.
     * 
     * @return true if the password supplied is valid.
     */
    public static boolean checkPassword(final User user, final String password) {
        return spe.checkPassword(sha512Encrypt(user, password), user.getPassword());
    }

    /**
     * For quick testing only.
     * 
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        final StrongPasswordEncryptor bpe = new StrongPasswordEncryptor();
        logger.debug(bpe.encryptPassword("cry30"));
        final String passw1 = "cry3011111111111111111111111111111111111111111111111111111111";
        final String passw2 = "cry301";
        final String encrypted1 = bpe.encryptPassword(passw1);
        final String encrypted2 = bpe.encryptPassword(passw2);
        logger.debug(bpe.checkPassword(passw1, encrypted1));
        logger.debug(bpe.checkPassword(passw1, encrypted2));
        logger.debug(bpe.checkPassword(passw2, encrypted1));
        logger.debug(bpe.checkPassword(passw2, encrypted2));
        logger.debug(encrypted1.length());
        logger.debug(encrypted2.length());
    }
}
