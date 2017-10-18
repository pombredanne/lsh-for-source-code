package org.translationcomponent.api.impl.utils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class StringHelper {

    /**
	 * Are the characters only blanks and line breaks?
	 * 
	 * @return true if the next text is a delimiter;
	 */
    public static final boolean isBlank(final String s) {
        if (s == null) {
            return true;
        }
        if (s.length() == 0) {
            return true;
        }
        for (int ii = 0; ii < s.length(); ii++) {
            if (!isBlank(s.charAt(ii))) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Is the character a delimiter?
	 * 
	 * @return true if the next text is a delimiter;
	 */
    public static final boolean isBlank(final char ch) {
        switch(ch) {
            case ' ':
                return true;
            case '\n':
                return true;
            case '\r':
                return true;
            case '\t':
                return true;
            case 0x39:
                return true;
            default:
                return false;
        }
    }

    /**
	 * 
	 * @param s
	 * @return true if the string contains a character that is not
	 *         ' ','\n','\t','\r'
	 */
    public static final boolean isNotBlank(String s) {
        if (s == null) {
            return false;
        }
        for (int ii = 0; ii < s.length(); ii++) {
            switch(s.charAt(ii)) {
                case ' ':
                    break;
                case '\n':
                    break;
                case '\r':
                    break;
                case '\t':
                    break;
                default:
                    return true;
            }
        }
        return false;
    }

    /**
	 * Helper method to translate string to upper-case.
	 * 
	 * @param word
	 * @return upper-case word.
	 */
    public static final char[] toUpperCase(final char[] word) {
        char[] out = new char[word.length];
        for (int ii = 0; ii < word.length; ii++) {
            out[ii] = Character.toUpperCase(word[ii]);
        }
        return out;
    }

    public static char[] concat(final char[] a, final char[] b) {
        char[] out = new char[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static String toString(final Exception e) {
        String s = e.getClass().getName();
        String message = e.getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    /**
	 * Encode a string using algorithm specified in web.xml and return the
	 * resulting encrypted password. If exception, the plain credentials string
	 * is returned
	 * 
	 * @param password
	 *            Password or other credentials to use in authenticating this
	 *            username
	 * @param algorithm
	 *            Algorithm used to do the digest
	 * 
	 * @return encypted password based on the algorithm.
	 */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            throw new IllegalArgumentException("Password encoding problemfor " + algorithm + ". " + e.getClass().getName() + ": " + e.getMessage());
        }
        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }
        return buf.toString();
    }

    public static Set<String> toImmutableSet(final String[] a) {
        return toImmutableSet(a, false);
    }

    public static Set<String> toImmutableSet(final String[] a, final boolean upperAndLowerCase) {
        if (a == null || a.length == 0) {
            return null;
        }
        if (a.length == 1) {
            if (a[0] == null || a[0].trim().length() == 0) {
                return null;
            }
            if (!upperAndLowerCase) {
                return Collections.singleton(a[0]);
            }
        }
        Set<String> s = new TreeSet<String>();
        add(a, s, upperAndLowerCase);
        return Collections.unmodifiableSet(s);
    }

    public static List<String> toImmutableList(final String[] a) {
        return toImmutableList(a, false);
    }

    public static List<String> toImmutableList(final String[] a, final boolean upperAndLowerCase) {
        if (a == null || a.length == 0) {
            return null;
        }
        if (a.length == 1) {
            if (a[0] == null || a[0].trim().length() == 0) {
                return null;
            }
            if (!upperAndLowerCase) {
                return Collections.singletonList(a[0]);
            }
        }
        List<String> l = new ArrayList<String>(a.length * (upperAndLowerCase ? 2 : 1));
        add(a, l, upperAndLowerCase);
        return Collections.unmodifiableList(l);
    }

    private static void add(final String[] a, final Collection<String> c, final boolean upperAndLowerCase) {
        for (String s : a) {
            s = s.trim();
            if (s.length() != 0) {
                if (upperAndLowerCase) {
                    c.add(s.toLowerCase());
                    c.add(s.toUpperCase());
                } else {
                    c.add(s);
                }
            }
        }
    }
}
