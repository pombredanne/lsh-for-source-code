package sk.naive.talker.util;

import sk.naive.talker.*;
import sk.naive.talker.adapter.TagConsts;
import sk.naive.talker.message.Category;
import sk.naive.talker.props.PermissionProperty;
import sk.naive.talker.command.*;
import java.util.*;
import java.util.logging.*;
import java.security.*;

/**
 * General utilities.
 *
 * @author <a href="mailto:virgo@naive.deepblue.sk">Richard "Virgo" Richter</a>
 * @version $Revision: 1.39 $ $Date: 2005/01/25 21:57:05 $
 */
public final class Utils {

    private static Logger logger;

    private Utils() {
    }

    /**
	 * Rozdelenie retazca na podretazce podla regularneho vyrazu.
	 *
	 * @param s Rozdelovany retazec.
	 * @param re Rozdelovac
	 * @param limit Maximalny pocet podretazcov (re aplikovany limit - 1 krat).
	 * @return Pole podretazcov - v kazdom pripade bude vratene pole, ale mozno bude
	 * mat dlzku 0 (aj pre s == null).
	 */
    public static String[] split(String s, String re, int limit) {
        if (s == null) {
            return new String[0];
        }
        return s.split(re, limit);
    }

    /**
	 * Rozdelenie retazca na slova.
	 *
	 * @param s Rozdelovany retazec.
	 * @param limit Maximalny pocet podretazcov (re aplikovany limit - 1 krat).
	 * @return Pole podretazcov - v kazdom pripade bude vratene pole, ale mozno bude
	 * mat dlzku 0 (aj pre s == null).
	 */
    public static String[] splitWords(String s, int limit) {
        return split(s, "\\s+", limit);
    }

    /**
	 * Finds up the first occurence of String value in Collection.
	 *
	 * @param value string to be found.
	 * @param c searched collection.
	 * @param ignoreCase
	 * @return full String value or null if nothing matches.
	 */
    public static String findFirstInCollection(String value, Collection<String> c, boolean ignoreCase) {
        if (value == null || value.length() == 0) {
            return null;
        }
        if (ignoreCase) {
            value = value.toLowerCase();
        }
        if (c.contains(value)) {
            return value;
        }
        for (String key : c) {
            if (adaptKey(key, ignoreCase).startsWith(value)) {
                return key;
            }
        }
        return null;
    }

    /**
	 * Finds up all occurences of String value in Collection.
	 *
	 * @param value string to be found.
	 * @param c searched collection.
	 * @param ignoreCase
	 * @return full all values or empty collection if nothing matches.
	 */
    public static Collection findAllInCollection(String value, Collection<String> c, boolean ignoreCase) {
        List foundValues = new LinkedList();
        if (value == null || value.length() == 0) {
            return foundValues;
        }
        if (ignoreCase) {
            value = value.toLowerCase();
        }
        for (String key : c) {
            if (adaptKey(key, ignoreCase).startsWith(value)) {
                foundValues.add(key);
            }
        }
        return foundValues;
    }

    private static String adaptKey(String key, boolean ignoreCase) {
        String testedKey = key;
        if (ignoreCase) {
            testedKey = testedKey.toLowerCase();
        }
        return testedKey;
    }

    public static String mapKeyQuote(String value) {
        return value.replaceAll("\\\\", "\\\\\\\\").replaceAll("=", "\\\\~");
    }

    public static String mapKeyUnqote(String value) {
        return value.replaceAll("\\\\~", "=").replaceAll("\\\\(.)", "$1");
    }

    public static final String hexDigits = "0123456789abcdef";

    /**
	 * Hexadecimal string for byte array - hexadecimal dump.
	 * Ide o lepsie citatelny tvar pre dlhsie hexa stringy.
	 *
	 * @param ba input byte array
	 * @return hexadecimal string
	 */
    public static String hexaString(byte[] ba) {
        return hexaString(ba, ba.length, false);
    }

    /**
	 * Hexadecimal string for byte array - hexadecimal dump.
	 * Ide o lepsie citatelny tvar pre dlhsie hexa stringy.
	 *
	 * @param ba input byte array
	 * @param formated true for formated output
	 * @return hexadecimal string
	 */
    public static String hexaString(byte[] ba, boolean formated) {
        return hexaString(ba, ba.length, formated);
    }

    /**
	 * Hexadecimal string for byte array - hexadecimal dump.
	 *
	 * @param ba input byte array
	 * @param len maximum output length
	 * @param formated true for formated output
	 * @return hexadecimal string
	 */
    public static String hexaString(byte[] ba, int len, boolean formated) {
        int ctx = 0;
        StringBuilder sb = new StringBuilder(ba.length * 2);
        if (len > ba.length) {
            len = ba.length;
        }
        for (int i = 0; i < len; i++) {
            if (formated && ctx % 16 == 0) {
                if (ctx > 0) {
                    sb.append('\n');
                }
                sb.append("0x");
                sb.append(hexDigits.charAt((ctx >> 12) & 15));
                sb.append(hexDigits.charAt((ctx >> 8) & 15));
                sb.append(hexDigits.charAt((ctx >> 4) & 15));
                sb.append(hexDigits.charAt(ctx & 15));
                sb.append("> ");
            }
            sb.append(hexaString(ba[i]));
            if (formated) {
                sb.append(" ");
            }
            ctx++;
        }
        return sb.toString();
    }

    public static String hexaString(byte b) {
        StringBuilder sb = new StringBuilder(2);
        sb.append(hexDigits.charAt((b >> 4) & 15));
        sb.append(hexDigits.charAt(b & 15));
        return sb.toString();
    }

    /**
	 * Normalize user name for later unified processing.
	 * <p>
	 * Method returns lowercased, 7bit approximated string. Login is
	 * always normalized name.
	 *
	 * @param s full featured name (diacritics, upcasing)
	 * @return normalized string without diacritics in lower case
	 */
    public static String normalize(String s) {
        if (s == null) {
            return null;
        }
        return sevenBitApproximation(s.toLowerCase());
    }

    /** Mirror of the unicode table from 00c0 to 017f without diacritics. */
    private static final String tab00c0 = "AAAAAAACEEEEIIII" + "DNOOOOO×ØUUUUYIß" + "aaaaaaaceeeeiiii" + "ðnooooo÷øuuuuyþy" + "AaAaAaCcCcCcCcDd" + "DdEeEeEeEeEeGgGg" + "GgGgHhHhIiIiIiIi" + "IiJjJjKkkLlLlLlL" + "lLlNnNnNnnNnOoOo" + "OoOoRrRrRrSsSsSs" + "SsTtTtTtUuUuUuUu" + "UuUuWwYyYZzZzZzF";

    /**
	 * Returns string without diacritics - 7 bit approximation.
	 * @param source string to convert
	 * @return corresponding string without diacritics
	 */
    public static String sevenBitApproximation(String source) {
        char[] vysl = new char[source.length()];
        char one;
        for (int i = 0; i < source.length(); i++) {
            one = source.charAt(i);
            if (one >= 'À' && one <= 'ſ') {
                one = tab00c0.charAt((int) one - 'À');
            }
            vysl[i] = one;
        }
        return new String(vysl);
    }

    public static byte[] digest(byte[] msg, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        return md.digest(msg);
    }

    public static String capitalizeFirst(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    public static String passwordString(String s) throws NoSuchAlgorithmException {
        return hexaString(digest(s.getBytes(), Main.getConfiguration().get("password.encryption.algorithm", "sha-1")));
    }

    public static String tag(String tag) {
        return tag(tag, null);
    }

    public static String tag(String tag, String params) {
        if (params != null) {
            return "<" + tag + "(" + params + ")>";
        } else {
            return "<" + tag + ">";
        }
    }

    public static String quote(String s) {
        s = replace(s, "([<>])", "<$1>");
        return replace(s, "\r?\n", tag(TagConsts.BR));
    }

    public static String unquote(String s) {
        s = replace(s, tag(TagConsts.BR), "\n");
        return replace(s, "<([<>])>", "$1");
    }

    public static String unquoteButBr(String s) {
        return replace(s, "<([<>])>", "$1");
    }

    public static String quoteButBr(String s) {
        s = quote(s);
        return replace(s, "<<>" + TagConsts.BR + "<>>", tag(TagConsts.BR));
    }

    private static String replace(String s, String from, String to) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll(from, to);
        return s;
    }

    public static String dateToTimestamp(Date date) {
        return String.valueOf(date.getTime());
    }

    public static Date timestampToDate(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Date(Long.parseLong(timestamp));
    }

    public static String timestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
	 * Throws an exception if user does not own specified permission.
	 *
	 * @param user tested user
	 * @param perm tested permission
	 * @throws PermissionException thrown if user does not own permission
	 * @see #ownsPermission(sk.naive.talker.User, String)
	 */
    public static void checkPermission(User user, String perm) throws PermissionException {
        if (!ownsPermission(user, perm)) {
            throw new PermissionException(perm);
        }
    }

    /**
	 * Returns true if user owns specified permission.
	 *
	 * @param user tested user
	 * @param perm tested permission
	 * @return true if user owns specified permission
	 * @see #checkPermission(sk.naive.talker.User, String)
	 */
    public static boolean ownsPermission(User user, String perm) {
        Set perms = (Set) user.getProperties().get(PermissionProperty.UPROP_PERMISSION);
        return (perms != null && (perms.contains(Consts.PERM_SUPERUSER) || perms.contains(perm)));
    }

    public static void warning(String message) {
        unexpectedExceptionWarning(message, null);
    }

    public static void unexpectedExceptionWarning(Throwable t) {
        unexpectedExceptionWarning("Unexpected exception warning...", t);
    }

    public static void unexpectedExceptionWarning(String message, Throwable t) {
        logger.log(Level.WARNING, message, t);
    }

    public static String formatCollection(Collection collection, String delim) {
        StringBuilder sb = new StringBuilder();
        for (Object o : collection) {
            addToBuffer(sb, o.toString(), delim);
        }
        return sb.toString();
    }

    public static void addToBuffer(StringBuilder sb, String value, String delim) {
        if (sb.length() > 0) {
            sb.append(delim);
        }
        sb.append(value);
    }

    /**
	 * Tests if user is ignoring this category of message.
	 * @param u Tested user.
	 * @return true, if user is ignoring this message category.
	 */
    public static boolean isIgnoring(User u, Category category) {
        if (category == null) {
            return false;
        }
        Set ignored = (Set) u.getProperties().get(Ignore.UPROP_IGNORED);
        if (ignored != null) {
            return ignored.contains(category);
        }
        return false;
    }

    public static Long createLong(Object o, int divideFactor) {
        if (o instanceof String) {
            return Long.parseLong((String) o) / divideFactor;
        }
        if (o instanceof Long) {
            return ((Long) o).longValue() / divideFactor;
        }
        return null;
    }

    public static Long createLong(Object o) {
        return createLong(o, 1);
    }

    public static Date createDate(Object o) {
        if (o == null) {
            return null;
        }
        return new Date(createLong(o));
    }

    static {
        logger = Logger.getLogger("nt.utils");
    }
}
