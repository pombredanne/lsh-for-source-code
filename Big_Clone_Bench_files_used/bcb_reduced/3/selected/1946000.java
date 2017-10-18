package org.informaticisenzafrontiere.openstaff.security;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class AuthenticationUtil {

    private static final String ALGORITHMS_DIGEST = "MD5";

    private static final int SALT_MIN_LEN = 5;

    private static final String ALGORITHMS_RANDOM = "SHA1PRNG";

    public static byte[] createPassword(byte password[], byte oldPassword[]) {
        byte result[] = null;
        try {
            MessageDigest sha = MessageDigest.getInstance(ALGORITHMS_DIGEST);
            SecureRandom random = null;
            if (oldPassword == null) random = SecureRandom.getInstance(ALGORITHMS_RANDOM);
            if (oldPassword != null) random = new SecureRandom(oldPassword);
            byte salt[] = random.generateSeed(SALT_MIN_LEN + password.length);
            sha.reset();
            sha.update(password);
            sha.update(salt);
            byte encr[] = sha.digest(salt);
            result = new byte[encr.length + salt.length];
            System.arraycopy(encr, 0, result, 0, encr.length);
            System.arraycopy(salt, 0, result, encr.length, salt.length);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    public static boolean verify(byte password[], byte dbpassword[]) {
        boolean result = false;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance(ALGORITHMS_DIGEST);
            sha.reset();
            int startAt = dbpassword.length - (SALT_MIN_LEN + password.length);
            if (startAt > 0) {
                byte salt[] = new byte[SALT_MIN_LEN + password.length];
                byte enc[] = new byte[startAt];
                System.arraycopy(dbpassword, startAt, salt, 0, salt.length);
                System.arraycopy(dbpassword, 0, enc, 0, enc.length);
                sha.update(password);
                sha.update(salt);
                byte toBytePassword[] = sha.digest(salt);
                result = MessageDigest.isEqual(toBytePassword, enc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
