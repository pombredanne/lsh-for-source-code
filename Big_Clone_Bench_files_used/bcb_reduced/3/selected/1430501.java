package org.torweg.pulse.util;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.torweg.pulse.invocation.lifecycle.Lifecycle;
import org.torweg.pulse.util.io.FastStringWriter;

/**
 * contains static utitity methods which are useful, when working with
 * {@code String}s.
 * 
 * @author Thomas Weber
 * @version $Revision: 2118 $
 */
public final class StringUtils {

    /**
	 * the hexadecimal numbers.
	 */
    private static final char[] HEX_NUMBERS = "0123456789ABCDEF".toCharArray();

    /**
	 * the base 60 numbers.
	 */
    private static final char[] BASE_62_NUMBERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
	 * 62.
	 */
    private static final BigInteger SIXTY_TWO = BigInteger.valueOf(62L);

    /**
	 * hidden default constructor.
	 */
    private StringUtils() {
        super();
    }

    /**
	 * returns the encoding of the given {@code String}.
	 * 
	 * @param s
	 *            the string to inspect
	 * @return the encoding
	 */
    public static String getEncoding(final String s) {
        return new java.io.InputStreamReader(new java.io.ByteArrayInputStream(s.getBytes())).getEncoding();
    }

    /**
	 * turns a given {@code byte[]} into a string of hexadecimal numbers.
	 * <p>
	 * For example {@code 127, 126} get {@code FFFE}.
	 * </p>
	 * 
	 * @param bs
	 *            the bytes
	 * @return the hexadecimal string
	 */
    public static String toHexString(final byte[] bs) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bs) {
            int num = b + 128;
            hexString.append(HEX_NUMBERS[num / 16]);
            hexString.append(HEX_NUMBERS[num % 16]);
        }
        return hexString.toString();
    }

    /**
	 * returns a base 62 string for the given {@code BigInteger}.
	 * 
	 * @param token
	 *            the number to be converted
	 * @return the base 62 representation
	 */
    public static String toBase62String(final BigInteger token) {
        BigInteger temp = token;
        StringBuilder base62String = new StringBuilder();
        if ((token.signum() < 0)) {
            base62String.append('-');
        }
        while (temp.abs().compareTo(SIXTY_TWO) > 0) {
            base62String.append(BASE_62_NUMBERS[temp.mod(SIXTY_TWO).intValue()]);
            temp = temp.divide(SIXTY_TWO);
        }
        base62String.append(BASE_62_NUMBERS[temp.mod(SIXTY_TWO).intValue()]);
        return base62String.toString();
    }

    /**
	 * returns a base 62 string for the given byte array.
	 * 
	 * @param bytes
	 *            the byte array
	 * @return the base 62 string
	 */
    public static String toBase62String(final byte[] bytes) {
        return toBase62String(new BigInteger(bytes));
    }

    /**
	 * returns the {@code BigInteger} represented by the given base 62 string.
	 * 
	 * @param base62
	 *            the base 62 encoded value
	 * @return the {@code BigInteger} represented by the given base 62 string
	 */
    public static BigInteger fromBase62String(final String base62) {
        BigInteger result = BigInteger.ZERO;
        BigInteger base = BigInteger.ONE;
        for (int i = 0; i < base62.length(); i++) {
            char c = base62.charAt(i);
            boolean found = false;
            for (int k = 0; k < 62; k++) {
                if (BASE_62_NUMBERS[k] == c) {
                    result = result.add(base.multiply(BigInteger.valueOf(k)));
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new NumberFormatException("Not a valid base 62 cypher: " + c);
            }
            base = base.multiply(SIXTY_TWO);
        }
        return result;
    }

    /**
	 * hashes the given string using {@code <em>SHA-512</em>}.
	 * 
	 * @param str
	 *            the string
	 * @return the hash value as an hexadecimal string
	 * @throws NoSuchAlgorithmException
	 *             if the hashing algorithm is not available
	 */
    public static String digest16(final String str) throws NoSuchAlgorithmException {
        return digest16(str.getBytes());
    }

    /**
	 * hashes the byte[] using {@code <em>SHA-512</em>}.
	 * 
	 * @param bytes
	 *            the bytes
	 * @return the hash value as an hexadecimal string
	 * @throws NoSuchAlgorithmException
	 *             if the hashing algorithm is not available
	 */
    public static String digest16(final byte[] bytes) throws NoSuchAlgorithmException {
        return toHexString(MessageDigest.getInstance("SHA-512").digest(Lifecycle.getSaltedHash(bytes)));
    }

    /**
	 * hashes the string using {@code <em>SHA-512</em>}.
	 * 
	 * @param str
	 *            the string
	 * @return the hash value as a base 62 string
	 * @throws NoSuchAlgorithmException
	 *             if the hashing algorithm is not available
	 */
    public static String digest62(final String str) throws NoSuchAlgorithmException {
        return digest62(str.getBytes());
    }

    /**
	 * hashes the byte[] using {@code <em>SHA-512</em>}.
	 * 
	 * @param bytes
	 *            the bytes
	 * @return the hash value as a base 62 string
	 * @throws NoSuchAlgorithmException
	 *             if the hashing algorithm is not available
	 */
    public static String digest62(final byte[] bytes) throws NoSuchAlgorithmException {
        return toBase62String(new BigInteger(MessageDigest.getInstance("SHA-512").digest(Lifecycle.getSaltedHash(bytes))));
    }

    /**
	 * returns the stack trace of the given {@code Throwable} as a string.
	 * 
	 * @param t
	 *            the throwable
	 * @return the stack trace of the given {@code Throwable} as a string
	 */
    public static String getStackTrace(final Throwable t) {
        FastStringWriter out = new FastStringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    /**
	 * Inserts a fraction-dot into the given string at after the given position
	 * (counting from the back) filling the string up with 0s (zeros) if too
	 * short.
	 * 
	 * @param s
	 *            the string to format
	 * @param pos
	 *            the position to insert the fraction-dot
	 * 
	 * @return the formatted string
	 */
    public static String insertFractionDot(final String s, final int pos) {
        return StringUtils.insertFromBack(s, pos, ".", "0").toString();
    }

    /**
	 * Inserts the given string <tt>insert</tt> at the specified position
	 * <tt>pos</tt> from the back of the string.
	 * 
	 * @param s
	 *            the string to be formatted
	 * @param pos
	 *            the position
	 * @param insert
	 *            the string to insert
	 * @param fill
	 *            the fill
	 * @return a {@code StringBuilder}
	 */
    public static StringBuilder insertFromBack(final String s, final int pos, final String insert, final String fill) {
        StringBuilder builder = new StringBuilder(s);
        while (builder.length() <= pos) {
            builder.insert(0, fill);
        }
        return builder.reverse().insert(pos, insert).reverse();
    }

    /**
	 * ensures that the given {@code String} does not exceed the given maximum
	 * length, cutting off all characters exceeding characters.
	 * <p>
	 * {@code null} values will be converted to empty strings.
	 * </p>
	 * 
	 * @param s
	 *            the string
	 * @param l
	 *            the maximum length
	 * @return a string that is guaranteed to be less than or equal in length to
	 *         the given maximum length
	 */
    public static String maxLength(final String s, final int l) {
        if (s == null) {
            return "";
        }
        if (s.length() <= l) {
            return s;
        }
        return s.substring(0, l);
    }

    /**
	 * returns either the given object's string representation or "null", if the
	 * given object is {@code null}.
	 * 
	 * @param o
	 *            the object
	 * @return either the given object's string representation or "null", if the
	 *         given object is {@code null}
	 */
    public static String getString(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString();
    }

    /**
	 * convenience method calling {@link #getEscapedForLiteral(String, QuoteCharacter)}
	 * using {@code QuoteCharacter.DOUBLE_QUOTE} as the quote character.
	 * 
	 * @param src
	 *            the source string
	 * @return the escaped string
	 */
    public static String getEscapedForLiteral(final String src) {
        return getEscapedForLiteral(src, QuoteCharacter.DOUBLE_QUOTE);
    }

    /**
	 * escapes a given String so it can be safely inserted into a JavaScript
	 * literal, {@literal i.e.} it escapes all contained quote characters, new
	 * lines with their escaped counterparts and removes all carriage returns.
	 * <p>
	 * {@code This "is" a test} becomes {@code This \"is\" a test}, if the quote
	 * char is {@code QuoteCharacter.DOUBLE_QUOTE}.
	 * </p>
	 * 
	 * @param src
	 *            the source string
	 * @param quote
	 *            the char used for quoting
	 * @return the escaped string
	 */
    public static String getEscapedForLiteral(final String src, final QuoteCharacter quote) {
        return src.replaceAll("\n", "\\\\n").replaceAll(quote.getStringRepresentation(), "\\\\" + quote.stringRepresentation).replaceAll("\r", "");
    }

    /**
	 * enumerates quote characters.
	 */
    public enum QuoteCharacter {

        /**
		 * single quote: {@code '}.
		 */
        SINGLE_QUOTE("'"), /**
		 * single quote: {@code "}.
		 */
        DOUBLE_QUOTE("\"");

        /**
		 * internal storage as string.
		 */
        private final String stringRepresentation;

        /**
		 * internal storage as char.
		 */
        private final char charRepresentation;

        /**
		 * private constructor.
		 * 
		 * @param s
		 *            the quote char as a String
		 */
        private QuoteCharacter(final String s) {
            this.stringRepresentation = s;
            this.charRepresentation = s.toCharArray()[0];
        }

        /**
		 * returns the quote as a {@code char}.
		 * 
		 * @return the char representation
		 */
        public char getCharRepresentation() {
            return charRepresentation;
        }

        /**
		 * returns the quote as a {@code String}.
		 * 
		 * @return the string representation
		 */
        public String getStringRepresentation() {
            return stringRepresentation;
        }
    }

    /**
	 * Simple {@code ListIterator&lt;String&gt;} that operates on a
	 * {@code String} split by a regex.
	 * 
	 * @author Daniel Dietz
	 * @version $Revision: 2118 $
	 * 
	 */
    public static class StringElementIterator implements ListIterator<String>, Iterable<String> {

        /**
		 * The split-array the {@code StringElementIterator} operates on.
		 */
        private final List<String> split;

        /**
		 * The index.
		 */
        private int index = -1;

        /**
		 * Creates a new {@code StringElementIterator} for the given
		 * {@code String} and regex.
		 * 
		 * @param string
		 *            the {@code String}
		 * @param regex
		 *            the regex for the split
		 */
        public StringElementIterator(final String string, final String regex) {
            super();
            this.split = Arrays.asList(string.split(regex));
        }

        /**
		 * Creates a new reversed {@code StringElementIterator} for the given
		 * {@code String} and regex.
		 * 
		 * @param string
		 *            the {@code String}
		 * @param regex
		 *            the regex for the split
		 * @param reverse
		 *            flag, indicating whether the iterator shall process the
		 *            split {@code String} in reverse order ( {@code true})
		 */
        public StringElementIterator(final String string, final String regex, final boolean reverse) {
            super();
            this.split = Arrays.asList(string.split(regex));
            if (reverse) {
                Collections.reverse(this.split);
            }
        }

        /**
		 * Creates a new {@code StringElementIterator} which iterates over the
		 * given string array.
		 * 
		 * @param stringArray
		 *            the {@code String[]}
		 */
        public StringElementIterator(final String[] stringArray) {
            super();
            this.split = Arrays.asList(stringArray);
        }

        /**
		 * Private copy constructor for {@link #iterator()}.
		 * 
		 * @param s
		 *            the list of string elements
		 */
        private StringElementIterator(final List<String> s) {
            super();
            this.split = s;
        }

        /**
		 * Not implemented.
		 * 
		 * @param o
		 *            unused
		 * 
		 * @throws UnsupportedOperationException
		 *             always
		 * 
		 * @see java.util.ListIterator#add(java.lang.Object)
		 */
        public final void add(final String o) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
		 * Returns whether the {@code StringElementIterator} has more elements.
		 * 
		 * @return {@code true} if the {@code Iterator} has more elements,
		 *         {@code false} otherwise
		 * 
		 * @see java.util.ListIterator#hasNext()
		 */
        public final boolean hasNext() {
            return (this.index + 1 < this.split.size());
        }

        /**
		 * Returns whether the {@code StringElementIterator} has an element
		 * prior to the current element.
		 * 
		 * @return {@code true} if the {@code Iterator} has previous elements,
		 *         {@code false} otherwise
		 * 
		 * @see java.util.ListIterator#hasPrevious()
		 */
        public final boolean hasPrevious() {
            return (this.index - 1 > -1);
        }

        /**
		 * Returns the next element.
		 * 
		 * @return the next element
		 * 
		 * @see java.util.ListIterator#next()
		 */
        public final String next() {
            this.index++;
            try {
                return this.split.get(this.index);
            } catch (IndexOutOfBoundsException e) {
                this.index = this.split.size() - 1;
                throw e;
            }
        }

        /**
		 * Returns the next index.
		 * 
		 * @return the next index
		 * 
		 * @see java.util.ListIterator#nextIndex()
		 */
        public final int nextIndex() {
            return (this.index + 1);
        }

        /**
		 * Returns the previous element.
		 * 
		 * @return the previous element
		 * 
		 * @see java.util.ListIterator#previous()
		 */
        public final String previous() {
            this.index--;
            try {
                return this.split.get(this.index);
            } catch (IndexOutOfBoundsException e) {
                this.index = -1;
                throw e;
            }
        }

        /**
		 * Returns the previous index.
		 * 
		 * @return the previous index
		 * 
		 * @see java.util.ListIterator#previousIndex()
		 */
        public final int previousIndex() {
            return (this.index - 1);
        }

        /**
		 * Not implemented.
		 * 
		 * @throws UnsupportedOperationException
		 *             always
		 * 
		 * @see java.util.ListIterator#remove()
		 */
        public final void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("remove() not supported for: " + this);
        }

        /**
		 * Not implemented.
		 * 
		 * @param o
		 *            unused
		 * 
		 * @throws UnsupportedOperationException
		 *             always
		 * 
		 * @see java.util.ListIterator#set(java.lang.Object)
		 */
        public final void set(final String o) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("set(" + o + ") not supported for: " + this);
        }

        /**
		 * Returns a new {@code StringElementIterator} operating on a new
		 * instance of the internal list.
		 * 
		 * @return this {@code StringElementIterator}
		 * 
		 * @see java.lang.Iterable#iterator()
		 */
        public final Iterator<String> iterator() {
            return new StringElementIterator(new ArrayList<String>(this.split));
        }
    }
}
