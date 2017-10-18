package de.vsis.coordination.coordinator.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class: Generates an activity id (string representation of uuid)
 * 
 * @author Michael von Riegen
 */
public class MD5Generator {

    /**
	 * Executes the md5 generation for a string
	 * 
	 * @return identifier
	 */
    public static String execute(String hash) {
        String md5sum = hash;
        try {
            StringBuffer strBuffer = new StringBuffer();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = hash.getBytes();
            md.update(hashBytes);
            byte[] hashedBytes = md.digest();
            for (int i = 0; i < hashedBytes.length; i++) {
                strBuffer.append(Integer.toHexString(0xFF & hashedBytes[i]));
            }
            return strBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5sum;
    }
}
