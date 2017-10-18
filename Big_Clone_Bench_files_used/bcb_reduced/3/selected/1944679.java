package com.raimcomputing.pickforme.service.utility.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.raimcomputing.pickforme.common.vo.UserVo;
import com.raimcomputing.pickforme.service.utility.PasswordUtility;

public class PasswordUtilityImpl implements PasswordUtility {

    private static final int MAX_RANDOM = 94;

    private static final int ASCII_SPACE = 32;

    private static final int SALT_SIZE = 10;

    private Log log = LogFactory.getLog(PasswordUtilityImpl.class);

    public byte[] createNewPassword(String password, String salt) {
        return (hash(password + salt));
    }

    public String generateSalt() {
        Random random = new Random();
        StringBuffer salt = new StringBuffer();
        for (int i = 0; i < SALT_SIZE; i++) {
            int asciiChar = random.nextInt(MAX_RANDOM) + ASCII_SPACE;
            salt.append((char) asciiChar);
        }
        return (salt.toString());
    }

    /**
	 * 
	 */
    public boolean verifyPassphrase(UserVo user, String password) {
        byte[] suspect = hash(password + user.getSalt());
        if (suspect.length != user.getDigest().length) {
            return (false);
        }
        for (int i = 0; i < suspect.length; i++) {
            if (suspect[i] != user.getDigest()[i]) {
                return (false);
            }
        }
        return (true);
    }

    /**
	 * 
	 */
    private byte[] hash(String plaintext) {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to load SHA-1 digest creation code.", e);
        }
        return (sha.digest(plaintext.getBytes()));
    }
}
