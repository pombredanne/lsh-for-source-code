package com.cromoteca.meshcms.server.toolbox;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Strings extends com.cromoteca.meshcms.client.toolbox.Strings {

    public static String getMD5(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(), 0, s.length());
            String result = new BigInteger(1, m.digest()).toString(16);
            while (result.length() < 32) {
                result = '0' + result;
            }
            return result;
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static String escapeRegexReplacement(String s) {
        return s.replaceAll("[\\\\\\$]", "\\\\$0");
    }

    /**
	 * Strips the HTML tags from a string.
	 *
	 * @param s the HTML String to be processed
	 *
	 * @return the stripped String.
	 */
    public static String stripHTMLTags(String s) {
        return s != null ? s.replaceAll("</?\\S+?[\\s\\S+]*?>", " ") : null;
    }

    public static String toTitleCase(String s) {
        char[] chars = s.trim().toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i])) {
                found = false;
            }
        }
        return String.valueOf(chars);
    }
}
