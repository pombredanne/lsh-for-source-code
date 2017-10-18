package org.ljtoolkit.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;

/**
 * This is a utility class used for Live Journal's challenge/response authentication.
 * 
 * @author Troy Bourdon
 *
 */
public class MD5 {

    /**
	 * This method serves as a utility for Live Journal's challange/response
	 * authentication scheme. The user's password is encoded where it is used
	 * in conjunction the the issued challenge for authentication.
	 * 
	 * @param text The user's unencoded password.
	 * @return The user's ecoded password.
	 */
    public static String getEncodedHex(String text) {
        MessageDigest md = null;
        String encodedString = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Hex hex = new Hex();
        encodedString = new String(hex.encode(md.digest()));
        md.reset();
        return encodedString;
    }
}
