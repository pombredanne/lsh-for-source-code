package org.xmailserver.ctrlclnt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Utility class with string manipulation helper methods.
 * 
 * @author		$Author: kevinwilliams $
 * @version		$Revision: 1.1 $
 * @since		$Date: 2004/02/26 05:50:34 $
 */
public class StringUtils {

    /**
	 * Returns the MD5 hash of a string as a hex-format string object.
	 *
	 * @param original The String to calculate MD5 against.
	 *
	 * @return The hex format of the 16-byte return value.
	 */
    public static String getMD5Hash(String original) {
        StringBuffer sb = new StringBuffer();
        try {
            StringReader sr = null;
            int crypt_byte = 0;
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(original.getBytes());
            byte[] digest = md.digest();
            sr = new StringReader(new String(digest, "ISO8859_1"));
            while ((crypt_byte = sr.read()) != -1) {
                String hexString = Integer.toHexString(crypt_byte);
                if (crypt_byte < 16) {
                    hexString = "0" + hexString;
                }
                sb.append(hexString);
            }
        } catch (NoSuchAlgorithmException nsae) {
        } catch (IOException ioe) {
        }
        return sb.toString();
    }

    /**
	* Splits a multi-line String and returns an array with each line as a separate object in the array.
	*
	* @param original The multi-line String to be split.
	*
	* @return The separate lines of the original as a String array.
	*/
    public static String[] splitLines(String original) {
        BufferedReader br = new BufferedReader(new StringReader(original));
        String line;
        ArrayList matches = new ArrayList();
        try {
            while ((line = br.readLine()) != null) {
                matches.add(line);
            }
        } catch (IOException ioe) {
            return new String[] { original };
        }
        if (matches.size() < 1) {
            return new String[] {};
        }
        String[] result = new String[matches.size()];
        return (String[]) matches.toArray(result);
    }
}
