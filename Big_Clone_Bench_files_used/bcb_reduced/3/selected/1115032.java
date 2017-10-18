package com.vertigrated.text;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lots of generic string manipulation routines. Most are convience wrappers around common idioms. Most also use JDK classes
 * such as BreakIterator where possible for internationalizaion if at all possible.
 */
public class StringUtil {

    /**
     * get the platform specific line separator for the current running jvm
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * This is provides Python-esque "slicing" behavior.
     * So you can lop off the end with negative values.
     * StringUtil.slice("abcd", -2, -1) == "cd"
     *
     * @param str   String to slice
     * @param start start of slice
     * @param end   end of slice
     * @return a slice of the string
     */
    public static String slice(final String str, final int start, final int end) {
        int e;
        int s;
        if (end < 0) {
            e = str.length() + end;
        } else if (end == 0) {
            e = str.length();
        } else {
            e = end;
        }
        if (start < 0) {
            s = str.length() + start;
        } else if (start > str.length()) {
            s = str.length() - 1;
        } else {
            s = start;
        }
        return str.substring(s, e);
    }

    public static String[] toArray(final List<String> l) {
        final String[] array = new String[l.size()];
        return l.toArray(array);
    }

    /**
     * Since the string being checked are guaranteed always not null and most likely
     * the same for the first dozen or so characters we use this to test equality instead.
     * We can skip the null and type tests that String.equals() does and start the test from the end of the string
     * We start from the end, since the hash code at the end of the strings are less alike than the ~15 char long
     * screen names at the beginning of the string.
     *
     * @param s1 string 1
     * @param s2 string 2
     * @return true of the strings are equal false if not
     */
    public static boolean fastReverseEquals(final String s1, final String s2) {
        if (s1.length() == s2.length()) {
            final char c1[] = s1.toCharArray();
            final char c2[] = s2.toCharArray();
            int n = s1.length();
            while (--n >= 0) {
                if (c1[n] != c2[n]) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * This returns a 32 character string that is an MD5 hash code of the key and the data
     *
     * @param data string to be hashed
     * @return 32 character hex encoded string MD5 hash code
     */
    public static String generateMD5(final String data) {
        final StringBuilder hash = new StringBuilder(64);
        try {
            final MessageDigest sha1 = MessageDigest.getInstance("MD5");
            sha1.update(data.getBytes());
            final byte[] digest = sha1.digest();
            for (byte aDigest : digest) {
                String hex = Integer.toHexString(aDigest);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                hex = hex.substring(hex.length() - 2);
                hash.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
        }
        return hash.toString();
    }

    /**
     * This returns a 32 character string that is an SHA1 hash code of the key and the data
     *
     * @param data string to be hashed
     * @return 32 character hex encoded string SHA1 hash code
     */
    public static String generateSHA1(final String data) {
        final StringBuilder hash = new StringBuilder(40);
        try {
            final MessageDigest sha1 = MessageDigest.getInstance("sha1");
            sha1.update(data.getBytes());
            final byte[] digest = sha1.digest();
            for (byte aDigest : digest) {
                String hex = Integer.toHexString(aDigest);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                hex = hex.substring(hex.length() - 2);
                hash.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
        }
        return hash.toString();
    }

    public static String booleanToString(final boolean b) {
        return b ? "TRUE" : "FALSE";
    }

    public static String booleanToString(final boolean b, final String t, final String f) {
        return b ? t : f;
    }

    public static String removeChars(final String source, final String charsToExclude) {
        final StringBuilder sb = new StringBuilder(source.length());
        final CharacterIterator ci = new StringCharacterIterator(source);
        char c = ci.first();
        while (c != CharacterIterator.DONE) {
            if (charsToExclude.indexOf(c) == -1) {
                sb.append(c);
            }
            c = ci.next();
        }
        return sb.toString();
    }

    public static String generateRandomString(final String charPool, final int length) {
        final Random rnd = new Random();
        final int charPoolLength = charPool.length();
        char c;
        double r;
        int pos;
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            r = rnd.nextDouble();
            pos = (int) Math.abs(r * (charPoolLength - 1));
            c = charPool.charAt(pos);
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Count the number of words in a string. This uses BreakIterator so it should internationalize easily.
     *
     * @param s string to count words in
     * @return number of words in string
     */
    public static int wordCount(final String s) {
        int i = 0;
        if (s != null && s.length() > 0) {
            final BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(s);
            int start = bi.first();
            for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
                if (end - start > 1) {
                    i = i + 1;
                }
            }
        }
        return i;
    }

    public static String unWrapString(final String s) {
        return StringUtil.unWrapString(s, 1);
    }

    public static String unWrapString(final String s, final int length) {
        String result = null;
        if (length * 2 > s.length()) {
            result = s.substring(length, s.length() - length);
        }
        return result;
    }

    /**
     * Tests to see if the given <code>char</code> is contained in a String.
     *
     * @param s string
     * @param c character
     * @return true if the string contains the character
     */
    public static boolean containsChar(final String s, final char c) {
        return s.indexOf(c) > -1;
    }

    /**
     * Create a List<String> of "words" from a string. This uses BreakIterator so it is internationalizable.<p>
     * This does NOT split on spaces or white space, so it does NOT work like <code>StringTokenizer</code> and should
     * NOT be considered as such.
     *
     * @param s string to split into a list of words
     * @return list of words from the string
     */
    public static List<String> stringToWordList(final String s) {
        final int wordCount = StringUtil.wordCount(s);
        final List<String> sl = new ArrayList<String>(StringUtil.wordCount(s));
        if (wordCount > 0) {
            final BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(s);
            int start = bi.first();
            for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
                if (end - start > 1) {
                    sl.add(s.substring(start, end));
                }
            }
        }
        return sl;
    }

    /**
     * This takes a char and creates a String of the count in length with the char duplicated count number of times.
     *
     * @param c     character to duplicate
     * @param count how many to duplicate
     * @return string of characters
     */
    public static String stringOfChar(final char c, final int count) {
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static String unwrap(final String s) {
        return s.substring(1, s.length() - 1);
    }

    public static String unwrap(final String s, final char c) {
        return StringUtil.unwrap(s, c, c);
    }

    public static String unwrap(final String s, final char beginChar, final char endChar) {
        if (StringUtil.isNullOrEmptyString(s)) {
            return "";
        } else if (s.charAt(0) == beginChar && s.charAt(s.length() - 1) == endChar) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    public static String wrapWithChars(final char prefix, final String str, final char suffix) {
        return new StringBuilder(str.length() + 2).append(prefix).append(str).append(suffix).toString();
    }

    /**
     * This takes a String and returns a new String with char prepended and appended.
     *
     * @param str string to wrap
     * @param c   character to wrap string with
     * @return string wrapped with character
     */
    public static String wrapWithChar(final String str, final char c) {
        return new StringBuilder(str.length() + 2).append(c).append(str).append(c).toString();
    }

    /**
     * This takes a String and prepends and appends a single quote character to the String.
     *
     * @param str string to wrap
     * @return wrapped string
     */
    public static String wrapWithSingleQuotes(final String str) {
        return StringUtil.wrapWithChar(str, '\'');
    }

    /**
     * This takes a String and returns a String with double quote characters prepended and appended.
     *
     * @param str String to wrap
     * @return str wrapped with double quotes
     */
    public static String wrapWithDoubleQuotes(final String str) {
        return StringUtil.wrapWithChar(str, '"');
    }

    public static String padLeftWithChar(final String str, final char c, final int count) {
        final StringBuilder sb = new StringBuilder(count);
        sb.append(StringUtil.stringOfChar(c, count - str.length())).append(str);
        return sb.toString();
    }

    public static String padRightWithChar(final String str, final char c, final int count) {
        final StringBuilder sb = new StringBuilder(count);
        sb.append(str).append(StringUtil.stringOfChar(c, count - str.length()));
        return sb.toString();
    }

    public static String padLeftWithSpace(final String str, final int count) {
        return StringUtil.padLeftWithChar(str, ' ', count);
    }

    public static String padRightWithSpace(final String str, final int count) {
        return StringUtil.padRightWithChar(str, ' ', count);
    }

    public static String alignLeft(final String target, final String str, final char c) {
        return StringUtil.padLeftWithChar(str, c, target.length() - str.length());
    }

    public static String alignLeft(final String target, final String str) {
        return StringUtil.alignLeft(target, str, ' ');
    }

    public static String alignRight(final String target, final String str, final char c) {
        return StringUtil.padRightWithChar(str, c, target.length() - str.length());
    }

    public static String alignRight(final String target, final String str) {
        return StringUtil.alignRight(target, str, ' ');
    }

    public static String alignCenter(final String str, final int width) {
        int l;
        final int r;
        l = (width - str.length()) / 2;
        r = l;
        if (r + l != width) {
            l = l + 1;
        }
        return StringUtil.padRightWithSpace(StringUtil.padLeftWithSpace(str, l), r);
    }

    public static String removeWhiteSpace(final String str) {
        final StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    public static int countCharsInString(final String s, final char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public static boolean isNullOrEmptyString(final String string) {
        return !(string != null && string.length() > 0);
    }

    /**
     * This takes a String object and ensures that the Object reference is not <code>null</code> if it is, it returns an empty
     * String object, otherwise it returns the original string.
     *
     * @param string input string
     * @return empty string if input is null otherwise input string
     */
    public static String nullToEmptyString(final String string) {
        return string == null ? "" : string;
    }

    /**
     * A more informative toString method. This method uses reflection to introspect the members of a class
     * and return the details as a String. Usage... from your toString override: Util.toString(this);
     *
     * @param ob object to stringify
     * @return stringified object
     */
    public static String toString(final Object ob) {
        final StringBuilder sb = new StringBuilder(ob.getClass().getName() + StringUtil.LINE_SEPARATOR);
        try {
            final Field[] fields = ob.getClass().getDeclaredFields();
            for (final Field field : fields) {
                field.setAccessible(true);
                final Object fob = field.get(ob);
                sb.append("   ").append(field.getName()).append(" : ").append(fob).append(StringUtil.LINE_SEPARATOR);
                if (fob instanceof Object[]) {
                    final Object[] oba = (Object[]) fob;
                    for (Object anOba : oba) {
                        sb.append("      ").append(anOba).append(StringUtil.LINE_SEPARATOR);
                    }
                }
            }
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        return sb.toString();
    }

    public static int countRunners(final String s, final int startIndex, final int dupThreshold, final char charToCount) {
        int pos = startIndex;
        int count = 1;
        char p = charToCount;
        char c;
        while (pos < s.length()) {
            c = s.charAt(pos);
            if (c == p) {
                count++;
            } else if (count > dupThreshold) {
                break;
            } else {
                count = 1;
            }
            p = c;
            pos++;
        }
        return count > dupThreshold ? count : 0;
    }

    public static String compressRunners(final String s, final int startIndex, final int dupThreshold, final char charToCount) {
        final StringBuilder sb = new StringBuilder(s.length());
        int pos = startIndex;
        int count = 1;
        char c;
        while (pos < s.length()) {
            c = s.charAt(pos);
            if (c == charToCount) {
                count++;
                if (count <= dupThreshold) {
                    sb.append(c);
                }
            } else {
                count = 1;
                sb.append(c);
            }
            pos++;
        }
        return sb.toString();
    }

    public static String compressRunners(final String s, final int dupThreshold) {
        final StringBuilder sb = new StringBuilder(s.length());
        int pos = 0;
        int count = 1;
        char p = Character.MIN_VALUE;
        char c;
        while (pos < s.length()) {
            c = s.charAt(pos);
            if (c == p) {
                count++;
                if (count <= dupThreshold) {
                    sb.append(c);
                }
            } else {
                count = 1;
                sb.append(c);
            }
            p = c;
            pos++;
        }
        return sb.toString();
    }

    private StringUtil() {
    }
}
