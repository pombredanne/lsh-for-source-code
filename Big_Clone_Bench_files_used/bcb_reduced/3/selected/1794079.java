package com.fddtool.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import com.fddtool.exception.IllegalParameterValueException;
import com.fddtool.exception.InvalidStateException;
import com.fddtool.exception.NullParameterException;
import com.fddtool.pd.common.IEntry;

/**
 * Collection of utility methods.
 * 
 * @author Mark Lesk, Serguei Khramtchenko
 */
public class Utils {

    public static final int UNCOMPARABLE = -2;

    public static final String CRLF = "\n\r";

    public static final long SECOND = 1000;

    public static final long MINUTE = 60 * SECOND;

    public static final long HOUR = 60 * MINUTE;

    public static final long DAY = 24 * HOUR;

    public static final long WEEK = 7 * DAY;

    public static final long YEAR = 365 * DAY;

    /**
     * The identifier of default hashing algorithm to use.
     */
    private static String DEFAULT_HASH_ALGORITHM = "MD5";

    /**
     * Checks to see if a parameter is null.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *      if (paramValue == null) {
     *          throw new NullParameterException(paramName);
     *      }
     *
     *      //Can now be replaced by :
     *      Utils.assertNotNullParam(paramValue, paramName);
     *  <p>
     *  This is not such a big deal for one parameter, but for 3 or 4
     *  it makes the code much clearer and also provides a single point
     *  to manage null parameter exceptions.
     *  <p>
     *  @param paramValue   the object to check if it is null
     *  @param paramName    the name of the parameter that is being checked
     *  @throws NullParameterException
     *              if the paramValue is null
     */
    public static void assertNotNullParam(Object paramValue, String paramName) {
        if (paramValue == null) {
            throw new NullParameterException(paramName);
        }
    }

    /**
     * Checks to see if a parameter is null.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *      if (paramValue != null) {
     *          throw new IllegalParameterValueException(paramName, paramName);
     *      }
     *
     *      //Can now be replaced by :
     *      Utils.assertNullParam(paramValue, paramName);
     *  <p>
     *  This is not such a big deal for one parameter, but for 3 or 4
     *  it makes the code much clearer and also provides a single point
     *  to manage null parameter exceptions.
     *  <p>
     *  @param paramValue   the object to check if it is null
     *  @param paramName    the name of the parameter that is being checked
     *  @throws IllegalParameterValueException
     *              if the paramValue is not null
     */
    public static void assertNullParam(Object paramValue, String paramName) {
        if (paramValue != null) {
            throw new IllegalParameterValueException(paramName, paramName);
        }
    }

    /**
     * Checks to see if a parameter is negative.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *      if (paramValue < 0) {
     *          throw new IllegalArgumentException(paramName);
     *      }
     *
     *      //Can now be replaced by :
     *      Utils.assertNonNegative(paramValue, paramName);
     * </pre>
     * 
     * <p>
     * This is not such a big deal for one parameter, but for 3 or 4 it makes
     * the code much clearer and also provides a single point to manage negative
     * parameter exceptions.
     * <p>
     * 
     * @param paramValue
     *            the object to check if it is negative
     * @param paramName
     *            the name of the parameter that is being checked
     * @throws IllegalArgumentException
     *             if the paramValue is negative
     */
    public static void assertNonNegative(int paramValue, String paramName) {
        if (paramValue < 0) {
            throw new IllegalArgumentException("Value of parameter " + paramName + " can not be negative");
        }
    }

    /**
     * Checks to see if a parameter is negative.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *      if (paramValue <= 0) {
     *          throw new IllegalArgumentException(paramName);
     *      }
     *
     *      //Can now be replaced by :
     *      Utils.assertNonNegative(paramValue, paramName);
     * </pre>
     * 
     * <p>
     * This is not such a big deal for one parameter, but for 3 or 4 it makes
     * the code much clearer and also provides a single point to manage negative
     * parameter exceptions.
     * <p>
     * 
     * @param paramValue
     *            the object to check if it is negative
     * @param paramName
     *            the name of the parameter that is being checked
     * @throws IllegalArgumentException
     *             if the paramValue is negative
     */
    public static void assertPositive(int paramValue, String paramName) {
        if (paramValue <= 0) {
            throw new IllegalArgumentException("Value of parameter " + paramName + " can not be negative or zero");
        }
    }

    /**
     * Checks to see if a parameter is null.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *      if (paramValue == null) {
     *          throw new NullParameterException(paramName);
     *      }
     *
     *      //Can now be replaced by :
     *      Utils.assertNotNullParam(paramValue, paramName);
     *  <p>
     *  This is not such a big deal for one parameter, but for 3 or 4
     *  it makes the code much clearer and also provides a single point
     *  to manage null parameter exceptions.
     *  <p>
     *  @param paramValue   the object to check if it is null or empty
     *  @param paramName    the name of the parameter that is being checked
     *  @throws NullParameterException
     *              if the <code>paramValue</code> is <code>null</code>.
     *  @throws IllegalParameterValueException
     *              if the <code>paramValue</code> is an empty
     *              <code>String</code>.
     */
    public static void assertNotNullOrEmptyParam(String paramValue, String paramName) {
        if (paramValue == null) {
            throw new NullParameterException(paramName);
        }
        if (paramValue.trim().length() == 0) {
            throw new IllegalParameterValueException(paramName, paramValue);
        }
    }

    /**
     * Checks to see if two objects are equal to each other taking into account
     * the possibility that either one or both can be null.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *  if (nullsAreEqual) {
     *      return (((a == null) && (b == null)) ||
     *              ((a != null) && (a.equals(b))));
     *  } else {
     *      return ((a != null) && (a.equals(b)));
     *  }
     * </pre>
     * 
     * Useful when comparing a number of attributes in an equals method, makes
     * code more clear.
     * 
     * @param a
     *            an Object to compare for equality
     * @param b
     *            an Object to compare for equality
     * @param nullsAreEqual
     *            a boolean flag, when <code>true</code> results in
     *            <code>true</code> if both <code>a</code> and
     *            <code>b</code> are null, when <code>false</code> results
     *            in <code>false</code> if both <code>a</code> and
     *            <code>b</code> are null.
     * 
     * @return boolean <code>true</code> if the objects are both null, or they
     *         are equal based on a standard <code>equals</code> call,
     *         <code>false</code> otherwise.
     * @deprecated in favor of nullSafeEquals
     */
    public static boolean isEqual(Object a, Object b, boolean nullsAreEqual) {
        if (nullsAreEqual) {
            return (((a == null) && (b == null)) || ((a != null) && (a.equals(b))));
        } else {
            return ((a != null) && (a.equals(b)));
        }
    }

    /**
     * Checks to see if two objects are equal to each other taking into account
     * the possibility that either one or both can be null.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *  if (nullsAreEqual) {
     *      return (((a == null) && (b == null)) ||
     *              ((a != null) && (a.equals(b))));
     *  } else {
     *      return ((a != null) && (b != null) && (a.equals(b)));
     *  }
     * </pre>
     * 
     * Useful when comparing a number of attributes in an equals method, makes
     * code more clear.
     * 
     * @param a
     *            an Object to compare for equality
     * @param b
     *            an Object to compare for equality
     * @param nullsAreEqual
     *            a boolean flag, when <code>true</code> results in
     *            <code>true</code> if both <code>a</code> and
     *            <code>b</code> are null, when <code>false</code> results
     *            in <code>false</code> if both <code>a</code> and
     *            <code>b</code> are null.
     * 
     * @return boolean <code>true</code> if the objects are both null, or they
     *         are equal based on a standard <code>equals</code> call,
     *         <code>false</code> otherwise.
     */
    public static boolean nullSafeEquals(Object a, Object b, boolean nullsAreEqual) {
        if (nullsAreEqual) {
            return (((a == null) && (b == null)) || ((a != null) && (a.equals(b))));
        } else {
            return ((a != null) && (b != null) && (a.equals(b)));
        }
    }

    /**
     * Executes a compareTo call on two objects taking into account the
     * possibility that either one or both can be null.
     * <p>
     * This method provides a shortcut for the following :
     * <p>
     * 
     * <pre>
     *  if ((nullsAreEqual && (a == null) && (b == null)) {
     *           return 0;
     *   }
     *
     *   if ((a == null) && (b == null)) {
     *       return -1;
     *   } else if ((a != null) && (b == null)) {
     *       return 1;
     *   } else if (a == null) && (b != null)) {
     *       return -1;
     *   } else if (a.getClass() != b.getClass()) {
     *       return -1;
     *   } else {
     *       return a.compareTo(b);
     *   }
     * </pre>
     * 
     * @param a
     *            an Object to compare
     * @param b
     *            an Object to compare
     * @param nullsAreEqual
     *            a boolean flag, when <code>true</code> results in 0 if both
     *            a and b are null.
     * 
     * @return integer 0 if objects are equal, -1 if a is less than b and 1 if a
     *         is greater than b.
     */
    public static <T extends Comparable<T>> int nullSafeCompare(T a, T b, boolean nullsAreEqual) {
        if (nullsAreEqual && (a == null) && (b == null)) {
            return 0;
        }
        if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        } else if (a.getClass() != b.getClass()) {
            return -1;
        } else {
            return a.compareTo(b);
        }
    }

    /**
     * Converts java.sql.Timestamp to java.util.Date. Even though Timestamp
     * descends from Date it is not equality compatible.
     * 
     * @param ts
     *            the Timestamp value to convert. Can be <code>null</code>.
     * @return the Date value corresponding to the give Timestamp. If ts
     *         parameter is <code>null</code>, <code>null</code>is
     *         returned.
     */
    public static Date timeStampToDate(Timestamp ts) {
        if (ts == null) {
            return null;
        } else {
            return new Date(ts.getTime() + ts.getNanos() / 1000000);
        }
    }

    /**
     * Utility for compare methods that performs default null checking and
     * equality checking.
     * <ul>
     * <li>if o1 and o2 are the same returns 0.</li>
     * <li>if both o1 and o2 are null returns 0.</li>
     * <li>if o1 is not null and o2 is null returns -1.</li>
     * <li>if o1 is null and o2 is not null returns 1.</li>
     * <li>otherwise returns Utils.UNCOMPARABLE to indicate it is indeterminate
     * and the calling compare method should continue with custom comparison.
     * The calling method can be assured that both objects are <b>not</b> null.</li>
     * </ul>
     * 
     * @param o1
     *            the first object to be compared.
     * @param o2
     *            the second object to be compared.
     * 
     * @return a negative 1, zero, or a positive 1 as the first argument is less
     *         than, equal to, or greater than the second. Negative 2 is
     *         returned if the comparison is indeterminate based on the
     *         predefined tests performed by this method.
     */
    public static int defaultCompare(Object o1, Object o2, Class<?> expectedClass) {
        int result = UNCOMPARABLE;
        if (o1 == o2) result = 0;
        if ((o1 == null) && (o2 == null)) result = 0;
        if ((o1 == null) && (o2 != null)) result = -1;
        if ((o1 != null) && (o2 == null)) result = 1;
        if ((o1 != null) && (o1.getClass() != expectedClass)) {
            throw new ClassCastException("For comparison expected class : " + expectedClass.getName() + ", but was class : " + o1.getClass().getName() + ".");
        }
        if ((o2 != null) && (o2.getClass() != expectedClass)) {
            throw new ClassCastException("For comparison expected class : " + expectedClass.getName() + ", but was class : " + o2.getClass().getName() + ".");
        }
        return result;
    }

    /**
     * Returns the unqualified class name for the given object.
     * <p>
     * 
     * @param obj
     *            the Object to determine the class name for.
     * @return the unqualified class name for the given object.
     * @throws NullParameterException
     *             if the <code>obj</code> parameter is <code>null</code>.
     */
    public static String determineClassName(Object obj) {
        Utils.assertNotNullParam(obj, "obj");
        String fqn = obj.getClass().getName();
        int lastDot = fqn.lastIndexOf(".");
        if (lastDot > -1) {
            fqn = fqn.substring(lastDot + 1);
        }
        return fqn;
    }

    /**
     * Creates <code>List</code> from elements in given <code>Iterator</code>
     * 
     * @param iterator
     *            the <code>Iterator</code> over elements to add to the
     *            <code>List</code> returned.
     * @return The <code>List</code> from elements in given
     *         <code>Iterator</code> if no elements found in the iterator,
     *         empty list is returned.
     */
    public static <T> List<T> createListFromIterator(Iterator<T> iterator) {
        List<T> result = new ArrayList<T>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    /**
     * Calculates number of items in the given iterator.
     * 
     * @param iterator
     *            the <code>Iterator</code> to calculate count for
     * @return integer the number of elements in the given iterator.
     */
    public static int calcIteratorItemsCount(Iterator<?> iterator) {
        int result = 0;
        while (iterator.hasNext()) {
            result++;
            iterator.next();
        }
        return result;
    }

    /**
     * Returns the <code>null</code> value of the parameter is
     * <code>null</code> or empty string. Otherwise returns the unchanged
     * parameter value.
     * 
     * @param value
     *            the String to nullify.
     * @return String value that is either <code>null</code> or non-empty
     *         string.
     */
    public static String nullify(String value) {
        if (value == null) return null;
        value = value.trim();
        if (value.length() == 0) return null;
        return value;
    }

    /**
     * Returns true if given string value is null or has no characters other
     * then space.
     * 
     * @param value
     *            the value to check
     * @return true if given string value is null or has no characters other
     *         then space.
     */
    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0 || value.trim().length() == 0;
    }

    /**
     * Creates a string of values delimited by the specified symbol.
     * 
     * @param list
     *            the Collection with values
     * @param delimiter
     *            to insert between values.
     * 
     * @return String consisting of value separated by delimiter.
     */
    public static String createDelimitedList(Collection<?> list, String delimiter) {
        assertNotNullParam(list, "list");
        assertNotNullParam(delimiter, "delimiter");
        Iterator<?> iter = list.iterator();
        StringBuffer buf = new StringBuffer();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof IEntry) {
                buf.append(((IEntry) o).getName());
            } else {
                buf.append(o);
            }
            if (iter.hasNext()) {
                buf.append(delimiter);
            }
        }
        return buf.toString();
    }

    /**
     * Parses the string containing a list of values.
     * 
     * @param list
     *            the String with a list of values
     * @param delimiters
     *            that separate values in the given string
     * @return List where each element contains one value from the given string.
     */
    public static List<String> parseDelimitedList(String list, String delimiters) {
        StringTokenizer tokenizer = new StringTokenizer(list, delimiters);
        List<String> result = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken());
        }
        return result;
    }

    /**
     * Converts boolean value to integer.
     * 
     * @param value
     *            boolean to be converted.
     * @return integer value of "1" if <code>true</code> was give, and value
     *         of "0" if <code>false</code>
     */
    public static int booleanToInt(boolean value) {
        if (value) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Converts string to boolean value.
     * 
     * @param value
     *            the String to be converted to boolean.
     * @return <code>true</code> if the parameter is one of the following:
     *         "yes", "y", "true" or non-zero integer value. Otherwise it
     *         returns <code>false</code>.
     * 
     */
    public static boolean string2Bool(String value) {
        if (value == null) {
            return false;
        } else if (value.compareToIgnoreCase("yes") == 0) {
            return true;
        } else if (value.compareToIgnoreCase("y") == 0) {
            return true;
        } else if (value.compareToIgnoreCase("true") == 0) {
            return true;
        } else {
            try {
                int v = Integer.parseInt(value);
                return v != 0;
            } catch (NumberFormatException ex) {
            }
        }
        return false;
    }

    /**
     * Compresses the given array of bytes.
     * 
     * @param data
     *            array of bytes to be compressed.
     * @return array of bytes compressed with ZIO algorithm.
     * 
     * @throws IOException
     *             if compression fails.
     */
    public static byte[] zip(byte[] data) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(buf);
        ZipEntry entry = new ZipEntry("data");
        out.putNextEntry(entry);
        out.write(data);
        out.flush();
        out.close();
        return buf.toByteArray();
    }

    /**
     * Extracts the named entry from the ZIP archive.
     * 
     * @param zipData
     *            the array of bytes that is a zipped archive.
     * @param entryName
     *            the name of entry to be extracted.
     * 
     * @return array of byte that represents extracted date for the entry. It
     *         will return <code>null</code> if the named entry is not found.
     * 
     * @throws IOException
     *             if error happens while reading ZIP archive.
     */
    public static byte[] extractZipEntry(byte[] zipData, String entryName) throws IOException {
        ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zipData));
        ZipEntry entry = stream.getNextEntry();
        while (entry != null) {
            if (entry.getName().equalsIgnoreCase(entryName)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] tmp = new byte[1024];
                int len = stream.read(tmp);
                while (len > 0) {
                    out.write(tmp, 0, len);
                    len = stream.read(tmp);
                }
                out.flush();
                out.close();
                return out.toByteArray();
            }
            entry = stream.getNextEntry();
        }
        return null;
    }

    /**
     * Extracts data from the given compressed array of bytes.
     * 
     * @param data
     *            array of bytes with compressed data.
     * @return array of bytes extracted from compressed data.
     * 
     * @throws IOException
     *             if extraction fails.
     */
    public static byte[] unzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream buf = new ByteArrayInputStream(data);
        ZipInputStream in = new ZipInputStream(buf);
        in.getNextEntry();
        byte[] tmp = new byte[1024];
        int len = in.read(tmp);
        while (len > 0) {
            out.write(tmp, 0, len);
            len = in.read(tmp);
        }
        return out.toByteArray();
    }

    /**
     * Rounds the given float value, so it only has the two digits after the
     * point.
     * 
     * @param value
     *            Float value to be rounded.
     * 
     * @return the float value with two digits after the point.
     */
    public static float roundFloat(Float value) {
        if (value != null) {
            int tmpResult = (int) value.floatValue() * 100;
            return ((float) tmpResult) / 100;
        } else {
            return 0.0f;
        }
    }

    /**
     * Returns a text in HTML-friendly format. This method returns the same text
     * as given in the parameter, but new line symbols are replaced with
     * <code>"BR"</code> elements.
     * 
     * @param text
     *            String with text to be converted into HTML-friendly form.
     * 
     * @return String with given text, or <code>null</code> if the text was
     *         not provided.
     */
    public static String getHtmlText(String text) {
        if (text != null) {
            return text.replaceAll("\n", "<br>");
        } else {
            return null;
        }
    }

    /**
     * Calculates hash value of the given array of bytes using the specified
     * hash algorithm.
     * 
     * @param content
     *            byte[] to calculate hash value for.
     * @param algorithm the String containing resulting hash.           
     * 
     * @return String containing the calculated hash. The format of hash string
     *         is this: "algorithm:hash". For example, if hash was created with
     *         MD5, the hash string may look like this "MD5:06B7D2....."
     */
    public static String calculateHash(String algorithm, byte[] content) {
        try {
            MessageDigest digester = MessageDigest.getInstance(algorithm);
            digester.update(content);
            byte[] data = digester.digest();
            StringBuffer result = new StringBuffer(algorithm);
            result.append(":");
            for (int i = 0; i < data.length; i++) {
                result.append(Integer.toHexString(data[i]));
            }
            return result.toString().toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            throw new InvalidStateException(ex);
        }
    }

    /**
     * Calculates hash value of the given array of bytes using the default hash
     * algorithm.
     * 
     * @param content
     *            byte[] to calculate hash value for.
     * 
     * @return String containing the calculated hash. 
     */
    public static String calculateHash(byte[] content) {
        return calculateHash(DEFAULT_HASH_ALGORITHM, content);
    }
}
