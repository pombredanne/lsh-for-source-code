package org.kablink.teaming.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.StringEncryptor;
import org.kablink.teaming.ConfigurationException;
import org.kablink.teaming.domain.User;
import org.kablink.util.PasswordEncryptor;

public class EncryptUtil {

    private static Log logger = LogFactory.getLog(EncryptUtil.class);

    private static final String PASSWORD_ENCRYPTION_ALGORITHM_BEFORE_INDIVIDUALIZATION = SPropsUtil.getString("user.password.encryption.algorithm", "MD5");

    private static final String PASSWORD_ENCRYPTION_ALGORITHM_AFTER_INDIVIDUALIZATION = SPropsUtil.getString("user.pwdenc.default", "SHA-256");

    private static final String[] ASYMMETRIC_ENCRYPTION_ALGORITHMS = new String[] { "SHA", "SHA-256", "MD5" };

    private static final String SOLE_SYMMETRIC_ENCRYPTION_ALGORITHM = "PBEWithMD5AndDES";

    public static String encryptSHA1(String... input) {
        return encrypt("SHA-1", input);
    }

    public static String encrypt(String algorithm, String[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) md.update(input[i].getBytes("UTF-8"));
            }
            byte[] messageDigest = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString((0xf0 & messageDigest[i]) >> 4));
                hexString.append(Integer.toHexString(0x0f & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return new StringBuffer().toString();
        }
    }

    public static boolean checkPassword(String passwordToCheck, User user) {
        if (passwordToCheck == null) return false;
        String alg = passwordEncryptionAlgorithmForMatching(user);
        if (alg.equals(SOLE_SYMMETRIC_ENCRYPTION_ALGORITHM)) {
            if (logger.isDebugEnabled()) logger.debug("Checking password for the user " + user.getName() + " using symmetric algorithm " + alg);
            return passwordToCheck.equals(getStringEncryptor().decrypt(user.getPassword()));
        }
        for (String asymAlg : ASYMMETRIC_ENCRYPTION_ALGORITHMS) {
            if (alg.equals(asymAlg)) {
                if (logger.isDebugEnabled()) logger.debug("Checking password for the user " + user.getName() + " using asymmetric algorithm " + alg);
                return PasswordEncryptor.encrypt(alg, passwordToCheck).equals(user.getPassword());
            }
        }
        return false;
    }

    public static String encryptPasswordForStorage(String passwordToStore, User user) {
        return encryptPassword(passwordEncryptionAlgorithmForStorage(user), passwordToStore, user.getName());
    }

    public static String passwordEncryptionAlgorithmForStorage(User user) {
        return PASSWORD_ENCRYPTION_ALGORITHM_AFTER_INDIVIDUALIZATION;
    }

    /**
	 * If the user's password is stored using symmetric encryption (i.e., reversible), 
	 * returns decrypted password. If the user doesn't have a password or the password
	 * is stored using asymmetric encryption, it returns <code>null</code>.
	 *  
	 * @param user
	 * @return
	 */
    public static String decryptPasswordForMatching(User user) {
        if (user.getPassword() == null) {
            if (logger.isDebugEnabled()) logger.debug("There is no password for the user " + user.getName());
            return null;
        }
        String alg = passwordEncryptionAlgorithmForMatching(user);
        if (alg.equals(SOLE_SYMMETRIC_ENCRYPTION_ALGORITHM)) {
            if (logger.isDebugEnabled()) logger.debug("Decrypting password for the user " + user.getName() + " using symmetric algorithm " + alg);
            return getStringEncryptor().decrypt(user.getPassword());
        }
        if (logger.isDebugEnabled()) logger.debug("Cannot decrypt password for the user " + user.getName() + " using asymmetric algorithm " + alg);
        return null;
    }

    public static String passwordEncryptionAlgorithmForMatching(User user) {
        String alg = user.getPwdenc();
        if (alg == null) {
            alg = PASSWORD_ENCRYPTION_ALGORITHM_BEFORE_INDIVIDUALIZATION;
        }
        return alg;
    }

    static String encryptPassword(String alg, String password, String username) {
        if (alg.equals(SOLE_SYMMETRIC_ENCRYPTION_ALGORITHM)) {
            if (logger.isDebugEnabled()) logger.debug("Encrypting password for the user " + username + " using symmetric algorithm " + alg);
            return getStringEncryptor().encrypt(password);
        }
        for (String asymAlg : ASYMMETRIC_ENCRYPTION_ALGORITHMS) {
            if (alg.equals(asymAlg)) {
                if (logger.isDebugEnabled()) logger.debug("Encrypting password for the user " + username + " using asymmetric algorithm " + alg);
                return PasswordEncryptor.encrypt(alg, password);
            }
        }
        throw new ConfigurationException("Invalid password encryption algorithm: " + alg);
    }

    static StringEncryptor getStringEncryptor() {
        return (StringEncryptor) SpringContextUtil.getBean("encryptor");
    }
}
