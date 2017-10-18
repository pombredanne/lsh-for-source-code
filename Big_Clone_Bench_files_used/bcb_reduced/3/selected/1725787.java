package net.sourceforge.jfilecrypt.algorithms;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Yannick Heiber
 */
public class PasswordUtil {

    /**
     * returns a String with the right length for encryption.
     *
     * @param key
     *            the passowrd to be rightsized.
     * @return a String with the right length.
     */
    public static String getKeyWithRightLength(final String key, int keyLength) {
        if (keyLength > 0) {
            if (key.length() == keyLength) {
                return key;
            } else {
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    return "";
                }
                md.update(key.getBytes());
                byte[] hash = md.digest();
                if (keyLength > 20) {
                    byte nhash[] = new byte[keyLength];
                    for (int i = 0; i < keyLength; i++) {
                        nhash[i] = hash[i % 20];
                    }
                    hash = nhash;
                }
                return new String(hash).substring(0, keyLength);
            }
        } else {
            return key;
        }
    }
}
