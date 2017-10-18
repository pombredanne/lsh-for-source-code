package org.jabusuite.tools.text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author hilwers
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StringUtils {

    /**
     * Erase a specified char from a string.
     * @param s The otiginal-string
     * @param strToDelete The string to delete
     * @return The resulting string where strToDelete is deleted from s
     */
    public static String eraseChar(String s, String strToDelete) {
        String hs = "";
        StringTokenizer tokenizer = new StringTokenizer(s, strToDelete);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (strToDelete.indexOf(token) == -1) {
                hs += token;
            }
        }
        return hs;
    }

    /**
     * Creates a String from a float. The fracts may be delmitited by a comma.
     * @param s
     * @return
     * @throws Exception
     */
    public float string2Float(String s) throws Exception {
        s = s.replace(',', '.');
        float f = Float.parseFloat(s);
        return f;
    }

    /**
     * Creates a String from a double. The fracts may be delmitited by a comma.
     * @param s
     * @return
     * @throws Exception
     */
    public static double string2Double(String s) throws Exception {
        s = s.replace(',', '.');
        double f = Double.parseDouble(s);
        return f;
    }

    /**
     * Encodes a given string with the MD5-Algorithm
     * @param input The text to encode.
     * @return The md5-encoded String
     */
    public static String encodeMD5(String input) {
        MessageDigest md = null;
        byte[] byteHash = null;
        StringBuffer resultString = new StringBuffer();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException caught!");
            System.exit(-1);
        }
        md.reset();
        md.update(input.getBytes());
        byteHash = md.digest();
        for (int i = 0; i < byteHash.length; i++) {
            resultString.append(Integer.toHexString(0xFF & byteHash[i]));
        }
        return (resultString.toString());
    }
}
