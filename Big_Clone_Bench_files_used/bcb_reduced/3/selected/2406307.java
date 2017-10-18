package org.template.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptography utilities.
 * 
 * @author Martin A. Heras
 * 
 */
public class CryptUtils {

    /**
	 * Calculates the SHA1 hash for the source given.
	 * 
	 * @param source
	 *            the message to be hashed with SHA1.
	 * @return the SHA1 hash for the given source.
	 * @throws NoSuchAlgorithmException
	 *             if SHA1 algorithm is not available.
	 */
    public static String calculateSHA1Hash(String source) throws NoSuchAlgorithmException {
        MessageDigest md;
        byte[] buffer, digest;
        String hash = "";
        buffer = source.getBytes();
        md = MessageDigest.getInstance("SHA1");
        md.update(buffer);
        digest = md.digest();
        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) hash += "0";
            hash += Integer.toHexString(b);
        }
        return hash;
    }
}
