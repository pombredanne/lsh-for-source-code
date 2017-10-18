package com.framework.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO_DOCUMENT_ME
 * 
 * @since 1.0
 */
public class StringUtils {

    public static final Comparator<Object> LENGTH_COMPARATOR = new LengthComparator();

    public static final boolean CASE_SENSITVE = true;

    public static final boolean CASE_INSENSITVE = false;

    public static final String URL_SEPARATOR = ":";

    private static Log LOG = LogFactory.getLog(StringUtils.class);

    public static boolean isQuoted(String s) {
        if (empty(s)) {
            return false;
        }
        String trimmed = s.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        char c = trimmed.charAt(0);
        if ((c == '"' || c == '\'') && trimmed.charAt(trimmed.length() - 1) == c) {
            return true;
        }
        return false;
    }

    /**
    * Returns true if and only if the input string is null or of zero
    * length
    */
    public static boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    /**
    * Returns a string comprising the string values of the elements of
    * the collection joined with the given string.
    */
    public static String join(Collection<?> c, String delimeter) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<?> it = c.iterator(); it.hasNext(); ) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(delimeter);
            }
        }
        return sb.toString();
    }

    /**
    * Returns a string comprising the string values of the elements of
    * the collection joined with the given string.
    */
    public static String join(Object[] array, String delimeter) {
        StringBuilder sb = new StringBuilder();
        if (array != null && array.length > 0) {
            sb.append(array[0]);
            for (int i = 1; i < array.length; i++) {
                sb.append(delimeter);
                sb.append(array[i]);
            }
        }
        return sb.toString();
    }

    /**
    * Determines if the given string represents a simple number, without
    * the overhead of a try/catch NumberFormatException
    */
    public static boolean isSimpleNumber(String s) {
        for (int i = 0, count = s.length(); i < count; ++i) {
            char c = s.charAt(i);
            if (!(Character.isDigit(c) || (i == 0 && (c == '-' || c == '+')))) {
                return false;
            }
        }
        return true;
    }

    /**
    * Computes the 128-bit (16 byte) MD5 hash of the given input string
    * 
    * @param s input string of arbitrary length
    */
    public static byte[] computeMD5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(s.getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
    * Base-64 encodes the given byte array
    */
    public static String base64Encode(byte[] bytes) {
        return new String((new Base64()).encode(bytes));
    }

    /**
    * Base-64 decodes the given string into a byte array. The string must
    * have been encoded with {@link base64Encode}
    */
    public static byte[] base64Decode(String s) {
        return (new Base64()).decode(s.getBytes());
    }

    /**
    * URL encodes the given byte array into a URL-safe string
    */
    public static String urlEncode(byte[] bytes) {
        return new String((new URLCodec()).encode(bytes));
    }

    /**
    * URL encodes the given string into a URL-safe string
    */
    public static String urlEncode(String s) {
        return new String((new URLCodec()).encode(s.getBytes()));
    }

    /**
    * Decodes the given URL-safe string. This method may not commonly be
    * needed because the servlet API will implicitly perform URL decoding
    * on input parameters.
    */
    public static byte[] urlDecode(String s) {
        try {
            return (new URLCodec()).decode(s.getBytes());
        } catch (DecoderException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
    * Decodes the given URL-safe string. This method may not commonly be
    * needed because the servlet API will implicitly perform URL decoding
    * on input parameters.
    */
    public static String urlDecodeToString(String s) {
        try {
            return new String((new URLCodec()).decode(s.getBytes()));
        } catch (DecoderException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
    * Breaks the input string into a series of lines with the given
    * column width.
    */
    public static String breakLines(String in, int colWidth) {
        int len = in.length();
        StringBuilder out = new StringBuilder(len + (len / colWidth) + 1);
        for (int pos = 0; pos < len; pos += colWidth) {
            int end = (pos + colWidth) > len ? len : pos + colWidth;
            out.append(in.substring(pos, end));
            out.append("\n");
        }
        return out.toString();
    }

    /**
    * Computes a simple hash based on n letters or digits of the given
    * string
    */
    public static int nLetterHash(String s, int n) {
        return nLetterHash(s, n, 0);
    }

    /**
    * Computes a simple hash based on n letters or digits of the given
    * string, starting at the index
    */
    public static int nLetterHash(String s, int n, int index) {
        int hash = 0;
        for (int i = index, max = s.length(); i < max && (i - index < n); ++i) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                hash = 37 * hash + c;
            }
        }
        return hash;
    }

    /**
    * Computes an array of the n letter hashes at every position in
    * string s
    */
    public static int[] nLetterHashes(String s, int n) {
        return nLetterHashes(s, n, false);
    }

    /**
    * Computes an array of the n letter hashes at every position in
    * string s
    */
    public static int[] nLetterHashes(String s, int n, boolean startOfWordOnly) {
        Set<Integer> hashes = new TreeSet<Integer>();
        for (int i = 0, count = s.length(); i < count; ++i) {
            if (startOfWordOnly) {
                if (Character.isLetterOrDigit(s.charAt(i)) && (i == 0 || !Character.isLetterOrDigit(s.charAt(i - 1)))) {
                    hashes.add(new Integer(nLetterHash(s, n, i)));
                }
            } else {
                hashes.add(new Integer(nLetterHash(s, n, i)));
            }
        }
        int[] hashesArr = new int[hashes.size()];
        int i = 0;
        for (Iterator<Integer> it = hashes.iterator(); it.hasNext(); ) {
            hashesArr[i++] = it.next().intValue();
        }
        return hashesArr;
    }

    public static boolean contains(String body, String target) {
        return indexOf(body, target) >= 0;
    }

    public static int count(String body, String target, boolean caseSensitive) {
        int count = 0;
        int pos = 0;
        while (pos >= 0) {
            pos = indexOf(body, target, caseSensitive, false, pos);
            if (pos >= 0) {
                ++count;
                pos += target.length();
            }
        }
        return count;
    }

    public static boolean contains(String body, String target, boolean caseSensitive) {
        return indexOf(body, target, caseSensitive) >= 0;
    }

    public static boolean contains(String body, String target, boolean caseSensitive, boolean allowPostFix) {
        return indexOf(body, target, caseSensitive, allowPostFix, 0) >= 0;
    }

    public static int indexOf(String body, String target) {
        return indexOf(body, target, true, false, 0);
    }

    public static int indexOf(String body, String target, boolean caseSensitive) {
        return indexOf(body, target, caseSensitive, false, 0);
    }

    public static int indexOf(String body, String target, boolean caseSensitive, boolean allowPostFix, int startPos) {
        if (!caseSensitive) {
            body = body.toLowerCase();
            target = target.toLowerCase();
        }
        int bodyLen = body.length();
        int targetLen = target.length();
        for (int startIndex = body.indexOf(target, startPos); startIndex >= 0; startIndex = body.indexOf(target, startIndex + targetLen)) {
            if (startIndex > 0 && Character.isLetterOrDigit(body.charAt(startIndex - 1))) {
                LOG.trace("Found " + target + " in text but skipping because of bad prefix. Text=" + body + " prefix=" + body.charAt(startIndex - 1));
                continue;
            }
            if (!allowPostFix) {
                int endIndex = startIndex + targetLen;
                if ((endIndex < bodyLen) && Character.isLetterOrDigit(body.charAt(endIndex))) {
                    LOG.trace("Found " + target + " in text but skipping because of bad postfix. Text=" + body + " postfix=" + body.charAt(endIndex));
                    continue;
                }
            }
            return startIndex;
        }
        return -1;
    }

    private static class LengthComparator implements Comparator<Object>, java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private LengthComparator() {
        }

        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                return (o2 == null) ? 0 : -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.toString().length() - o2.toString().length();
        }
    }

    public static String getField(String s, int field, char delim) {
        return getFields(s, new int[] { field }, delim)[0];
    }

    /**
    * Returns a string array containing the specified fields from the
    * given string. The fields are 1-indexed (to match up with the
    * convention of the database spec)
    */
    public static String[] getFields(String s, int[] fieldsToGet, char delim) {
        for (int i = 1; i < fieldsToGet.length; ++i) {
            if (fieldsToGet[i] <= fieldsToGet[i - 1]) {
                throw new IllegalArgumentException("Fields must be in ascending order");
            }
        }
        String[] result = new String[fieldsToGet.length];
        StringBuilder field = new StringBuilder();
        int fieldNo = 1;
        int fieldIndex = 0;
        for (int i = 0, count = s.length(); i < count && fieldIndex < fieldsToGet.length; ++i) {
            char c = s.charAt(i);
            if (fieldNo == fieldsToGet[fieldIndex]) {
                if (c == delim) {
                    result[fieldIndex] = field.toString();
                    field.delete(0, field.length());
                    ++fieldIndex;
                    ++fieldNo;
                    if (fieldIndex == result.length) {
                        return result;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == delim) {
                    ++fieldNo;
                }
            }
        }
        if (fieldNo == fieldsToGet[fieldIndex]) {
            result[fieldIndex] = field.toString();
            ++fieldIndex;
        }
        if (fieldIndex < fieldsToGet.length) {
            throw new IllegalArgumentException("Entry did not have " + fieldsToGet[fieldIndex] + " fields");
        }
        return result;
    }

    public static String replace(String s, char target, String[] values) {
        StringBuilder out = new StringBuilder();
        int begin = 0;
        int valuesIndex = 0;
        for (int targetPos = s.indexOf(target); targetPos >= 0; targetPos = s.indexOf(target, begin)) {
            if (targetPos > begin) {
                out.append(s.substring(begin, targetPos));
            }
            if (valuesIndex >= values.length) {
                throw new IllegalArgumentException("Not enough values provided (string: " + s + ", values: " + toString(values));
            }
            out.append(values[valuesIndex++]);
            begin = targetPos + 1;
        }
        if (valuesIndex < values.length) {
            throw new IllegalArgumentException("Too many values provided (string: " + s + ", values: " + toString(values));
        }
        if (begin < s.length()) {
            out.append(s.substring(begin));
        }
        return out.toString();
    }

    public static String toString(Object[] arr) {
        return Arrays.asList(arr).toString();
    }

    /**
    * Coverts a Collection into a delimited String using the specified
    * <code>delChar</code>.
    * 
    * @param strCollection
    * @param delChar The Character to use as a delimiter.
    * @return A delimited String.
    * @since 1.0
    */
    public static final String toStringDelimited(Collection<String> strCollection, final char delChar) {
        if (strCollection == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String str : strCollection) {
            result.append(str + delChar);
        }
        if (strCollection.size() > 0) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    /**
    * Returns a string representation of the given map in the form:
    * {key1=val1, key2=val2, ...keyN=valN}
    */
    public static String toString(Map<?, ?> m) {
        if (m == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        for (Iterator<?> it = m.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<?, ?> next = (Map.Entry<?, ?>) it.next();
            sb.append(next.getKey()).append("=").append(next.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
    * Parses the given comma-delimited (or ":" separated) string into a
    * string array.
    */
    public static String[] toStringArray(String s) {
        if (s == null) {
            return null;
        }
        if (s.indexOf(URL_SEPARATOR) > -1) {
            String result[] = s.split(URL_SEPARATOR);
            return result;
        } else {
            return s.split(",");
        }
    }

    /**
    * Parses the given comma-delimited string into a list.
    */
    public static List<String> toList(String s) {
        if (s == null) {
            return null;
        }
        return Arrays.asList(toStringArray(s));
    }

    /**
    * Parses the given comma-delimited string into a set.
    */
    public static Set<String> toSet(String s) {
        if (s == null) {
            return null;
        }
        List<String> l = toList(s);
        Set<String> result = new HashSet<String>(l.size());
        result.addAll(l);
        return result;
    }

    /**
    * Parses the given comma-delimited string into a set.
    */
    public static Set<String> toSet(String[] strArr) {
        if (strArr == null) {
            return null;
        }
        List<String> l = Collections.emptyList();
        for (int i = 0; i < strArr.length; i++) {
            l.add(strArr[i]);
        }
        Set<String> result = new HashSet<String>(l.size());
        result.addAll(l);
        return result;
    }

    /**
    * Parses the given comma-delimited string into an int array.
    */
    public static int[] toIntArray(String s) {
        if (StringUtils.empty(s)) {
            return new int[0];
        }
        String[] sarr = s.split(",");
        int[] result = new int[sarr.length];
        for (int i = 0; i < sarr.length; ++i) {
            result[i] = Integer.parseInt(sarr[i]);
        }
        return result;
    }

    /**
    * Parses the given comma-delimited string into a long array.
    */
    public static long[] toLongArray(String s) {
        if (StringUtils.empty(s)) {
            return new long[0];
        }
        String[] sarr = s.split(",");
        long[] result = new long[sarr.length];
        for (int i = 0; i < sarr.length; ++i) {
            result[i] = Long.parseLong(sarr[i]);
        }
        return result;
    }

    /**
    * Parses the given comma-delimited string into a Long array.
    */
    public static Long[] toLongObjectArray(String s) {
        if (StringUtils.empty(s)) {
            return new Long[0];
        }
        String[] sarr = s.split(",");
        Long[] result = new Long[sarr.length];
        for (int i = 0; i < sarr.length; ++i) {
            result[i] = new Long(sarr[i]);
        }
        return result;
    }

    /**
    * Trims instances of the given character from before and after the
    * given string (a generic version of the native String.trim() method)
    */
    public static String trim(String s, char c) {
        while (!StringUtils.empty(s) && s.charAt(0) == c) {
            s = s.substring(1);
        }
        while (!StringUtils.empty(s) && s.charAt(s.length() - 1) == c) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
    * Returns the character equivalent of some of the common HTML
    * entities. This is useful in SQL literals b/c MySQL does not seem to
    * offer a way to escape a semi-colon.
    */
    public static char toChar(String htmlEntity) {
        if (empty(htmlEntity)) {
            return '\0';
        } else if ("&quot;".equals(htmlEntity)) {
            return '"';
        } else if ("&gt;".equals(htmlEntity)) {
            return '>';
        } else if ("&lt;".equals(htmlEntity)) {
            return '<';
        } else if ("&amp;".equals(htmlEntity)) {
            return '&';
        } else {
            return '\0';
        }
    }

    public static String substringToChar(String s, char c, int begin, int maxLen) {
        int end = Math.min(s.length(), begin + maxLen);
        for (int i = begin; i < end; ++i) {
            if (s.charAt(i) == c) {
                end = i;
                break;
            }
        }
        return s.substring(begin, end);
    }

    public static String quoteSqlLiteral(Object o) {
        if (o == null) {
            return "null";
        }
        String in = o.toString();
        StringBuilder sb = new StringBuilder("'");
        for (int i = 0, count = in.length(); i < count; ++i) {
            char c = in.charAt(i);
            if (c == '&') {
                String entity = substringToChar(in, ';', i, 5);
                char cEntity = toChar(entity);
                if (cEntity != '\0') {
                    c = cEntity;
                }
            }
            switch(c) {
                case '-':
                    sb.append(c);
                    if (i + 1 < count && in.charAt(i + 1) == '-') sb.append(' ');
                    break;
                case '\'':
                case '\\':
                case '"':
                    sb.append('\\');
                default:
                    sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    public static String replaceAll(String source, char target, String replacement) {
        StringBuilder out = new StringBuilder();
        int begin = 0;
        for (int targetPos = source.indexOf(target); targetPos >= 0; targetPos = source.indexOf(target, begin)) {
            if (targetPos > begin) {
                out.append(source.substring(begin, targetPos));
            }
            out.append(replacement);
            begin = targetPos + 1;
        }
        if (begin < source.length()) {
            out.append(source.substring(begin));
        }
        return out.toString();
    }

    public static String removeTags(String s) {
        String cleanText = s.replaceAll("<.*?>", "");
        return cleanText;
    }

    public static String removeTagsAndTruncate(String in, int len) {
        String cleanText = removeTags(in);
        if (cleanText.length() > len && len > 3) {
            cleanText = cleanText.substring(0, len - 3) + "...";
        }
        return cleanText;
    }

    public static String replaceAll(String s, String target, String replacement) {
        StringBuilder sb = null;
        int tlen = target.length();
        int bi = 0;
        for (int n = s.indexOf(target); n >= 0; n = s.indexOf(target, bi)) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append(s.substring(bi, n));
            if (replacement != null) {
                sb.append(replacement);
            }
            bi = n + tlen;
        }
        if (sb == null) {
            return s;
        } else {
            sb.append(s.substring(bi));
            return sb.toString();
        }
    }

    /**
    * A null-safe compare using the default String compareTo method.
    */
    public static int compare(String s1, String s2, boolean nullIsFirst) {
        if (s1 == null) {
            return (s2 == null) ? 0 : (nullIsFirst ? -1 : 1);
        } else if (s2 == null) {
            return nullIsFirst ? 1 : -1;
        }
        return s1.compareTo(s2);
    }

    public static String condense(String s) {
        return condense(s, "");
    }

    public static String condense(String s, String replacementChars) {
        String result = s.toLowerCase().replaceAll("(and|the|[&])+", replacementChars);
        result = result.replaceAll("([%])+", "\\%");
        return result;
    }

    /**
    * This method converts a string into true or false. A null value is
    * understood to mean false.
    * 
    * @param is_valid
    * @return true for values (true, TRUE, 1,tRue, etc.)
    */
    public static boolean toBoolean(String is_valid) {
        boolean result = false;
        if (is_valid != null) {
            if (is_valid.trim().equalsIgnoreCase("true") || is_valid.trim().equalsIgnoreCase("1")) {
                result = true;
            }
        }
        return result;
    }

    /**
    * This method converts number of seconds (NOT the num. seconds since
    * the Epoc. as in Date.getTime()) to "h:mm:ss"
    * 
    * @param timeInSeconds
    * @return
    */
    public static String convertToHms(long timeInSeconds) {
        long hours, minutes;
        String minStr = "00";
        String secStr = "00";
        String hoursStr = "0";
        if (timeInSeconds > 0) {
            hours = timeInSeconds / 3600;
            hoursStr = "" + hours;
            timeInSeconds = timeInSeconds - (hours * 3600);
            minutes = timeInSeconds / 60;
            minStr = "" + minutes;
            if (minStr.length() == 1) {
                minStr = "0" + minStr;
            }
            timeInSeconds = timeInSeconds - (minutes * 60);
            secStr = "" + timeInSeconds;
            if (secStr.length() == 1) {
                secStr = "0" + secStr;
            }
        }
        return hoursStr + ":" + minStr + ":" + secStr;
    }

    /**
    * Convert duration string to seconds. String currently takes the
    * format of HH:MM:SS
    * 
    */
    public static long convertDurationToSec(String value) {
        try {
            StringTokenizer strTok = new StringTokenizer(value, ":");
            if (strTok.hasMoreTokens()) {
                long hours = Long.parseLong(strTok.nextToken());
                if (strTok.hasMoreTokens()) {
                    long minutes = Long.parseLong(strTok.nextToken());
                    if (strTok.hasMoreTokens()) {
                        long seconds = Long.parseLong(strTok.nextToken());
                        return (hours * 3600) + (minutes * 60) + seconds;
                    }
                }
            }
        } catch (NumberFormatException ex) {
            LOG.warn("Error converting duration: " + value, ex);
        }
        return -1;
    }
}
