package net.sf.dpdesktop.module.util.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple class, providing the possibility to create an hash for a 
 * specific string.
 *
 * @author Heiner Reinhardt
 */
public class HashFactory {

    private final MessageDigest md5;

    public HashFactory() throws NoSuchAlgorithmException {
        this.md5 = MessageDigest.getInstance("MD5");
    }

    /**
     * This function creates an md5 hash for the string given in the
     * constructor. This function has an equal behaviour to the md5() function of PHP.
     *
     * Normally there is an NoSuchAlgorithmException thrown if MD5 algorithm is
     * not available. But as the availability of MD5 algorithm is checked
     * before, this exception will not be thrown.
     *
     * @return The hashed string.
     */
    public String getValue(String string) {
        StringBuffer hexString = new StringBuffer();
        md5.reset();
        md5.update(string.getBytes());
        byte[] result = md5.digest();
        for (int i = 0; i < result.length; i++) {
            hexString.append(Integer.toHexString((result[i] & 0xFF) | 0x100).toLowerCase().substring(1, 3));
        }
        return hexString.toString();
    }
}
