package org.commonfarm.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.StringTokenizer;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonfarm.security.util.Base64;
import org.commonfarm.security.util.PasswordDigester;

/**
 * String Utility Class This is used to encode passwords programmatically
 * 
 * @author David Yang
 */
public class StringUtil {

    /** The <code>Log</code> instance for this class. */
    private static Log log = LogFactory.getLog(StringUtil.class);

    /**
     * Encode a string using algorithm specified in web.xml and return the
     * resulting encrypted password. If exception, the plain credentials
     * string is returned
     *
     * @param password Password or other credentials to use in authenticating
     *        this username
     * @param algorithm Algorithm used to do the digest
     *
     * @return encypted password based on the algorithm.
     */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Exception: " + e);
            return password;
        }
        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encodedPassword.length; i++) {
            if (((int) encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) encodedPassword[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * Encode a string using Base64 encoding. Used when storing passwords
     * as cookies.
     *
     * This is weak encoding in that anyone can use the decodeString
     * routine to reverse the encoding.
     *
     * @param str
     * @return String
     */
    public static String encodeString(String str) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        return new String(encoder.encodeBuffer(str.getBytes())).trim();
    }

    /**
     * Decode a string using Base64 encoding.
     *
     * @param str
     * @return String
     */
    public static String decodeString(String str) {
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        try {
            return new String(dec.decodeBuffer(str));
        } catch (IOException io) {
            throw new RuntimeException(io.getMessage(), io.getCause());
        }
    }

    /**
     * 
     * @param string
     * @return
     */
    public static String swapFirstLetterCase(String string) {
        StringBuffer sbuf = new StringBuffer(string);
        sbuf.deleteCharAt(0);
        if (Character.isLowerCase(string.substring(0, 1).toCharArray()[0])) {
            sbuf.insert(0, string.substring(0, 1).toUpperCase());
        } else {
            sbuf.insert(0, string.substring(0, 1).toLowerCase());
        }
        return sbuf.toString();
    }

    /**
     * 
     * @param origString
     * @param trimString
     * @return
     */
    public static String trim(String origString, String trimString) {
        int startPosit = origString.indexOf(trimString);
        if (startPosit != -1) {
            int endPosit = trimString.length() + startPosit;
            return origString.substring(0, startPosit) + origString.substring(endPosit);
        }
        return origString;
    }

    /**
     * @param letters
     */
    public static boolean contain(String origString, String containString) {
        if (origString.indexOf(containString) != -1) {
            return true;
        }
        return false;
    }

    /**
     * 
     * @param origString
     * @param stringToken
     */
    public static String getLastString(String origString, String stringToken) {
        StringTokenizer st = new StringTokenizer(origString, stringToken);
        String lastString = "";
        while (st.hasMoreTokens()) {
            lastString = st.nextToken();
        }
        return lastString;
    }

    /**
     * @param string
     * @param token
     */
    public static String[] getStringArray(String string, String token) {
        if (string.indexOf(token) != -1) {
            StringTokenizer st = new StringTokenizer(string, token);
            String[] stringArray = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++) {
                stringArray[i] = st.nextToken();
            }
            return stringArray;
        }
        return new String[] { string };
    }

    public static String[] getStringArray(String string) {
        StringTokenizer st = new StringTokenizer(string);
        String[] stringArray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            stringArray[i] = st.nextToken();
        }
        return stringArray;
    }

    /**
     * Replace if (string == null || string.equals(""))
     * @param string
     * @return
     */
    public static boolean notEmpty(String string) {
        if (string == null) return false;
        if (string.equals("")) return false;
        return true;
    }

    public static boolean isEmpty(String string) {
        if (string == null) return true;
        if (string.equals("")) return true;
        return false;
    }

    /**
     * Create base64 password
     */
    public static String createHash(String original) {
        byte[] digested = PasswordDigester.digest(original.getBytes());
        byte[] encoded = Base64.encode(digested);
        return new String(encoded);
    }

    public static boolean compareHash(String hashedValue, String unhashedValue) {
        return hashedValue.equals(createHash(unhashedValue));
    }

    /**
     * work$id - replace("work$id", "$", ".") - work.id
     * @param string
     * @param replaced
     * @param replace
     */
    public static String replace(String string, String replaced, String replace) {
        String newString = "";
        if (string.indexOf(replaced) != -1) {
            String s1 = string.substring(0, string.indexOf(replaced));
            String s2 = string.substring(string.indexOf(replaced) + 1);
            ;
            newString = s1 + replace + s2;
        }
        return newString;
    }

    /**
	 * Check if a String has length.
	 * <p><pre>
	 * StringUtil.hasLength(null) = false
	 * StringUtil.hasLength("") = false
	 * StringUtil.hasLength(" ") = true
	 * StringUtil.hasLength("Hello") = true
	 * </pre>
	 * @param str the String to check, may be <code>null</code>
	 * @return <code>true</code> if the String is not null and has length
	 */
    public static boolean hasLength(String str) {
        return (str != null && str.length() > 0);
    }

    /**
	 * Check if a String has text. More specifically, returns <code>true</code>
	 * if the string not <code>null<code>, it's <code>length is > 0</code>, and
	 * it has at least one non-whitespace character.
	 * <p><pre>
	 * StringUtil.hasText(null) = false
	 * StringUtil.hasText("") = false
	 * StringUtil.hasText(" ") = false
	 * StringUtil.hasText("12345") = true
	 * StringUtil.hasText(" 12345 ") = true
	 * </pre>
	 * @param str the String to check, may be <code>null</code>
	 * @return <code>true</code> if the String is not null, length > 0,
	 *         and not whitespace only
	 * @see java.lang.Character#isWhitespace
	 */
    public static boolean hasText(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return false;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String toString(Object obj) {
        return ToStringBuilder.reflectionToString(obj, ToStringStyle.DEFAULT_STYLE);
    }

    public static void main(String[] args) {
        String s = "work$id";
        s = StringUtil.replace(s, "$", ".");
        System.out.print(s);
    }
}
