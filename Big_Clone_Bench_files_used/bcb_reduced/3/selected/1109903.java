package org.easyrec.utils.io;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class can do fancy things with strings.
 *
 * @author phlavac
 */
public class Text {

    /**
     * This function returns the given string without the last character.
     * e.g.
     * trimLast("peter")-->"pete"
     *
     * @param s
     * @return
     */
    public static String removeLast(String s) {
        if (Strings.isNullOrEmpty(s)) return s;
        return s.substring(0, s.length() - 1);
    }

    /**
     * Generates a 32 char MD5 hash from a string:
     * "hallo" --> "b6834520c5cf3df80886803e1af41b47"
     *
     * @param key
     * @return
     */
    public static String generateHash(String key) {
        key += "use_your_key_here";
        MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("MD5");
            md.reset();
            md.update(key.getBytes());
            byte[] bytes = md.digest();
            StringBuffer buff = new StringBuffer();
            for (int l = 0; l < bytes.length; l++) {
                String hx = Integer.toHexString(0xFF & bytes[l]);
                if (hx.length() == 1) buff.append("0");
                buff.append(hx);
            }
            return buff.toString().trim();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function return a given String with the first char converted to Uppercase
     * e.g. peter --> Peter
     *
     * @param s
     * @return
     */
    public static String capitalize(String s) {
        if (Strings.isNullOrEmpty(s)) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * This function matches 2 given strings on their maximum concurrence.
     * e.g.
     * http://satsrv01.researchstudio.at/wiki/index.php/Hauptseite
     * /wiki/index.php/article1
     * -->
     * http://satsrv01.researchstudio.at/wiki/index.php/article1
     *
     * @param left
     * @param right
     */
    public static String matchMax(String left, String right) {
        if (!Strings.isNullOrEmpty(right)) {
            if (right.startsWith("http://") || right.startsWith("https://")) {
                return right;
            } else {
                if (!Strings.isNullOrEmpty(left)) {
                    int offset = 0;
                    for (int i = 0; i < left.length(); i++) {
                        if (left.charAt(i) != right.charAt(offset)) {
                            offset = 0;
                        } else {
                            offset++;
                        }
                    }
                    return new StringBuilder(left).append(right.substring(offset, right.length())).toString();
                } else {
                    return right;
                }
            }
        } else {
            return right;
        }
    }

    /**
     * This function returns true if the given string
     * contains any of the chars %,>,<,',".
     *
     * @param stringToCheck
     * @return
     */
    public static boolean containsEvilSpecialChar(String stringToCheck) {
        return CharMatcher.anyOf("%<>'\"").countIn(stringToCheck) > 0;
    }

    /**
     * Extract a Substring between two Strings from the given String.
     *
     * @param sTest
     * @param prefix
     * @param suffix
     * @return
     */
    public static String containingString(String str, String prefix, String suffix) {
        if (Strings.isNullOrEmpty(str)) return str;
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(suffix);
        int prefixIdx = str.indexOf(prefix);
        int suffixIdx = str.indexOf(suffix, prefixIdx + 1);
        if (prefixIdx < 0 || suffixIdx < 0) return "";
        return str.substring(prefixIdx + 1, suffixIdx);
    }
}
