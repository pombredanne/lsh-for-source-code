package net.jolm.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>UserPasswordHelper</code> provides method to convert the clear text
 * password to the byte array to be stored in the userPassword field in LDAP,
 * and method to verify the clear text password with the value of userPassword
 * field in LDAP.
 * 
 * @author Chunyun Zhao
 * @since 1.0
 */
public class UserPasswordHelper {

    private static Log log = LogFactory.getLog(UserPasswordHelper.class);

    private static String DEFAULT_ENCODING = "UTF-8";

    public static enum HashAlg {

        MD5, SHA, SMD5, SSHA
    }

    ;

    /**
	 * Calculates hash of clear text password to be stored in the userPassword
	 * field.
	 * 
	 * @param clearpass
	 *            The password in plaintext that should be hashed.
	 * @param alg
	 *            The algorithm to caculate the hash.
	 * @param salt
	 *            The salt that is to be used together with the schemes {SMD5}
	 *            and {SSHA}. Should be between 8 and 16 Bytes. salt should be
	 *            null for any other scheme.
	 * @return The base64-encoded hashed pwd with the following format: -
	 *         {MD5}base64(MD5-hash) for MD5 hashes - {SHA}base64(SHA-hash) for
	 *         SHA hashes - {SMD5}base64(MD5-hash+salt bytes) for SMD5 hashes -
	 *         {SSHA}base64(SHA-hash+salt bytes) for SSHA hashes Or null if t is
	 *         not one of 2, 3, 4, 5.
	 */
    public static byte[] clearPassToUserPassword(String clearpass, HashAlg alg, byte[] salt) {
        if (alg == null) {
            throw new IllegalArgumentException("Invalid hash argorithm.");
        }
        try {
            MessageDigest digester = null;
            StringBuilder resultInText = new StringBuilder();
            switch(alg) {
                case MD5:
                    resultInText.append("{MD5}");
                    digester = MessageDigest.getInstance("MD5");
                    break;
                case SMD5:
                    resultInText.append("{SMD5}");
                    digester = MessageDigest.getInstance("MD5");
                    break;
                case SHA:
                    resultInText.append("{SHA}");
                    digester = MessageDigest.getInstance("SHA");
                    break;
                case SSHA:
                    resultInText.append("{SSHA}");
                    digester = MessageDigest.getInstance("SHA");
                    break;
                default:
                    break;
            }
            digester.reset();
            digester.update(clearpass.getBytes(DEFAULT_ENCODING));
            byte[] hash = null;
            if (salt != null && (alg == HashAlg.SMD5 || alg == HashAlg.SSHA)) {
                digester.update(salt);
                hash = ArrayUtils.addAll(digester.digest(), salt);
            } else {
                hash = digester.digest();
            }
            resultInText.append(new String(Base64.encodeBase64(hash), DEFAULT_ENCODING));
            return resultInText.toString().getBytes(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            log.warn("Error occurred while hashing password ", uee);
            return new byte[0];
        } catch (java.security.NoSuchAlgorithmException nse) {
            log.warn("Error occurred while hashing password ", nse);
            return new byte[0];
        }
    }

    public static byte[] clearPassToMD5UserPassword(String clearpass) {
        return clearPassToUserPassword(clearpass, HashAlg.MD5, null);
    }

    public static byte[] clearPassToSHAUserPassword(String clearpass) {
        return clearPassToUserPassword(clearpass, HashAlg.SHA, null);
    }

    /**
	 * Verifies a given password against the password stored in the userPassword
	 * field in LDAP.. <p/> The userPassword-value should follow the following
	 * format: - {MD5}base64(MD5-hash) - {SHA}base64(SHA-hash) -
	 * {SMD5}base64(MD5-hash+salt bytes) - {SSHA}base64(SHA-hash+salt bytes) -
	 * plaintext password <p/> If the userPassword value does not start with one
	 * of the prefixes {MD5}, {SMD5}, {SHA} or {SSHA} it will be handled as a
	 * plaintext pwd. <p/>
	 * 
	 * @param clearpass
	 *            The password in plaintext that should be verified against the
	 *            hashed pwd stored in the userPassword
	 * @param userPassword
	 *            The original pwd stored in the userPassword value. field.
	 * @return True - if the given plaintext pwd matches with the hashed pwd in
	 *         the userPassword field, otherwise false.
	 */
    public static boolean verifyPassword(String clearpass, byte[] userPassword) {
        try {
            String hashFromClearText = null;
            String userPasswordInText = new String(userPassword, DEFAULT_ENCODING);
            if (userPasswordInText.startsWith("{MD5}")) {
                hashFromClearText = new String(clearPassToUserPassword(clearpass, HashAlg.MD5, null));
            } else if (userPasswordInText.startsWith("{SMD5}")) {
                byte[] hashPlusSalt = Base64.decodeBase64(userPasswordInText.substring(6).getBytes(DEFAULT_ENCODING));
                byte[] salt = ArrayUtils.subarray(hashPlusSalt, 16, hashPlusSalt.length);
                hashFromClearText = new String(clearPassToUserPassword(clearpass, HashAlg.SMD5, salt));
            } else if (userPasswordInText.startsWith("{SHA}")) {
                hashFromClearText = new String(clearPassToUserPassword(clearpass, HashAlg.SHA, null));
            } else if (userPasswordInText.startsWith("{SSHA}")) {
                byte[] hashPlusSalt = Base64.decodeBase64(userPasswordInText.substring(6).getBytes(DEFAULT_ENCODING));
                byte[] salt = ArrayUtils.subarray(hashPlusSalt, 20, hashPlusSalt.length);
                hashFromClearText = new String(clearPassToUserPassword(clearpass, HashAlg.SSHA, salt));
            } else {
                hashFromClearText = clearpass;
            }
            return hashFromClearText.equals(userPasswordInText);
        } catch (UnsupportedEncodingException uee) {
            log.warn("Error occurred while verifying password", uee);
            return false;
        }
    }
}
