package it.unibz.izock.networking;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Class Utils just collects some utilities for the whole projects.
 */
public final class Utils {

    /**
	 * Gets the MD5 hash from string.
	 * 
	 * @param message a message
	 * 
	 * @return the MD5 hash from string
	 */
    public static String getMD5HashFromString(String message) {
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(message.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hashword;
    }
}
