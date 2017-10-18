package net.teqlo.util;

import java.security.MessageDigest;
import java.util.UUID;
import net.teqlo.TeqloException;
import sun.misc.BASE64Encoder;

public class SecurityUtil {

    /**
	 * Encode the value and salt to an encrypted hash. 
	 * 
	 * @param value the value to hash
	 * @param salt salt to add to the value
	 * @return encrypted hash
	 */
    public static String hashValue(String password, String salt) throws TeqloException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes("UTF-8"));
            md.update(salt.getBytes("UTF-8"));
            byte raw[] = md.digest();
            char[] encoded = (new BASE64Encoder()).encode(raw).toCharArray();
            int length = encoded.length;
            while (length > 0 && encoded[length - 1] == '=') length--;
            for (int i = 0; i < length; i++) {
                if (encoded[i] == '+') encoded[i] = '*'; else if (encoded[i] == '/') encoded[i] = '-';
            }
            return new String(encoded, 0, length);
        } catch (Exception e) {
            throw new TeqloException("Security", "password", e, "Could not process password");
        }
    }

    /**
	 * Generates a unique value suitable as a salt value in encryption routines.
	 *
	 * @return salt value
	 */
    public static String makeSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
	 * Generates an encrypted hash based on a fix salt value. Not very secure because of the
	 * fixed salt. Use only where we cannot generate and store a unique salt value. Primarily
	 * intended for use with invitations, where we don't have a user account yet to store salt values.
	 * 
	 * @param value input value, case insensitive
	 * @return
	 * @throws TeqloException
	 */
    public static String getVerificationCode(String key) throws TeqloException {
        String value = key.toLowerCase();
        return SecurityUtil.hashValue(value, "60284eee-1f4f-" + value + "-a900-ea4cfc146a9e");
    }

    /**
	 * Tests a system verification code, based on getSystemVerificationCode. Key is case insensitive.
	 * 
	 * @param key the value to verify
	 * @param code the verification code
	 * @return
	 */
    public static boolean testVerificationCode(String key, String code) {
        try {
            return getVerificationCode(key).equals(code);
        } catch (TeqloException e) {
            Loggers.QUEUES.warn("Failed to test a verification code", e);
            return false;
        }
    }
}
