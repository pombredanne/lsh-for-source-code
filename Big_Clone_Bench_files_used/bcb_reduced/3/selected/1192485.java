package org.vinavac.entity.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Hoang Xuan Quang
 */
public class EntityUtil {

    private static final Logger logger = Logger.getLogger(EntityUtil.class.getName());

    /**
     * Method to encrypt password to Hex string using SHA-256 algorithm
     *
     * @param password raw password
     * @return password SHA-256 hash in hex string format
     */
    public static String getPasswordHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            byte[] byteData = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Unknow error in hashing password", e);
            return "Unknow error, check system log";
        }
    }
}
