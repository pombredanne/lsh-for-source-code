package org.vardb.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.vardb.CVardbException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oreilly.servlet.Base64Decoder;
import com.oreilly.servlet.Base64Encoder;

public final class CStringHelper {

    public static final String EMPTY_STRING = "";

    private CStringHelper() {
    }

    public static String toString(Object obj) {
        return ReflectionToStringBuilder.reflectionToString(obj);
    }

    public static ToStringBuilder stringBuilder(Object obj) {
        return new ToStringBuilder(obj);
    }

    public static EqualsBuilder equalsBuilder() {
        return new EqualsBuilder();
    }

    public static HashCodeBuilder hashCodeBuilder() {
        return new HashCodeBuilder();
    }

    public static HashCodeBuilder hashCodeBuilder(int initialNonZeroOddNumber, int multiplierNonZeroOddNumber) {
        return new HashCodeBuilder(initialNonZeroOddNumber, multiplierNonZeroOddNumber);
    }

    public static String deleteWhitespace(String str) {
        return StringUtils.deleteWhitespace(str);
    }

    public static String stripHtml(String str) {
        return str.replaceAll("<\\/?[^>]+>", "");
    }

    public static List<String> trim(Collection<String> list) {
        List<String> ids = new ArrayList<String>();
        for (String id : list) {
            id = id.trim();
            if (!CStringHelper.isEmpty(id)) ids.add(id);
        }
        return ids;
    }

    public static String replace(String str, String target, String replace) {
        List<String> list = split(str, target);
        return join(list, replace);
    }

    public static String join(Collection<? extends Object> collection, String delimiter) {
        StringBuilder buffer = new StringBuilder();
        for (Iterator<? extends Object> iter = collection.iterator(); iter.hasNext(); ) {
            buffer.append(iter.next().toString());
            if (iter.hasNext()) buffer.append(delimiter);
        }
        return buffer.toString();
    }

    public static String join(Object[] array, String delimiter) {
        StringBuilder buffer = new StringBuilder();
        for (int index = 0; index < array.length; index++) {
            buffer.append(array[index]);
            if (index < array.length - 1) buffer.append(delimiter);
        }
        return buffer.toString();
    }

    public static String join(double[] array, String delimiter) {
        StringBuilder buffer = new StringBuilder();
        for (int index = 0; index < array.length; index++) {
            buffer.append(array[index]);
            if (index < array.length - 1) buffer.append(delimiter);
        }
        return buffer.toString();
    }

    public static String padRight(String str, char pad, int length) {
        int remainder = length - str.length();
        String padded = str + repeatString(String.valueOf(pad), remainder);
        if (padded.length() > length) throw new CVardbException("padded string is longer than the specified length: [" + str + "] length=" + length);
        return padded;
    }

    public static String padLeft(String str, char pad, int length) {
        int remainder = length - str.length();
        String padded = repeatString(String.valueOf(pad), remainder) + str;
        if (padded.length() > length) System.out.println("padded string is longer than the specified length: [" + str + "] length=" + length);
        return padded;
    }

    public static String repeatString(String str, int numtimes) {
        StringBuilder buffer = new StringBuilder("");
        for (int index = 0; index < numtimes; index++) {
            buffer.append(str);
        }
        return buffer.toString();
    }

    public static String reverse(String str) {
        StringBuilder buffer = new StringBuilder(str);
        buffer.reverse();
        return buffer.toString();
    }

    public static String chunk(String original, int cols, String separator) {
        StringBuilder buffer = new StringBuilder();
        chunk(original, cols, separator, buffer);
        return buffer.toString();
    }

    public static void chunk(String original, int cols, String separator, StringBuilder buffer) {
        int length = original.length();
        int lines = (int) Math.ceil((double) length / (double) cols);
        int position = 0;
        for (int index = 0; index < lines; index++) {
            if (index < lines - 1) {
                buffer.append(original.substring(position, position + cols));
                buffer.append(separator);
                position += cols;
            } else buffer.append(original.substring(position));
        }
    }

    public static List<String> chunk(String original, int cols) {
        int length = original.length();
        int numlines = (int) Math.ceil((double) length / (double) cols);
        int position = 0;
        List<String> lines = new ArrayList<String>();
        for (int index = 0; index < numlines; index++) {
            if (index < numlines - 1) {
                lines.add(original.substring(position, position + cols));
                position += cols;
            } else lines.add(original.substring(position));
        }
        return lines;
    }

    public static List<String> split(String raw, String delimiter, boolean clean) {
        if (!clean) return split(raw, delimiter);
        return clean(split(raw.trim(), delimiter));
    }

    public static List<String> split(String raw, String delimiter) {
        if (raw == null) return Collections.emptyList();
        String[] arr = raw.split(delimiter);
        return Arrays.asList(arr);
    }

    public static List<String> splitLines(String str) {
        List<String> lines = new ArrayList<String>();
        for (String line : split(str.trim(), "\n")) {
            line = line.trim();
            if (CStringHelper.isEmpty(line)) continue;
            lines.add(line);
        }
        return lines;
    }

    public static List<Integer> splitInts(String str, String delimiter) {
        List<Integer> ints = new ArrayList<Integer>();
        if (!CStringHelper.hasContent(str)) return ints;
        str = str.trim();
        List<String> list = split(str, delimiter);
        for (String item : list) {
            ints.add(Integer.valueOf(item));
        }
        return ints;
    }

    public static List<Float> splitFloats(String str, String delimiter) {
        List<Float> floats = new ArrayList<Float>();
        List<String> list = split(str, delimiter);
        for (String item : list) {
            floats.add(Float.valueOf(item));
        }
        return floats;
    }

    public static List<Double> splitDoubles(String str, String delimiter) {
        List<Double> doubles = new ArrayList<Double>();
        List<String> list = split(str, delimiter);
        for (String item : list) {
            doubles.add(Double.valueOf(item));
        }
        return doubles;
    }

    public static Collection<Collection<Integer>> splitInts(Collection<Integer> ids, int max) {
        Collection<Collection<Integer>> lists = new ArrayList<Collection<Integer>>();
        Collection<Integer> list = new ArrayList<Integer>();
        lists.add(list);
        if (ids.size() <= max) {
            list.addAll(ids);
            return lists;
        }
        for (Integer id : ids) {
            if (list.size() >= max) {
                list = new ArrayList<Integer>();
                lists.add(list);
            }
            list.add(id);
        }
        return lists;
    }

    public static Collection<Collection<String>> splitStrings(Collection<String> ids, int max) {
        Collection<Collection<String>> lists = new ArrayList<Collection<String>>();
        if (ids.size() <= max) {
            lists.add(ids);
            return lists;
        }
        Collection<String> list = new ArrayList<String>();
        lists.add(list);
        for (String id : ids) {
            if (list.size() >= max) {
                list = new ArrayList<String>();
                lists.add(list);
            }
            list.add(id);
        }
        return lists;
    }

    public static List<String> clean(List<String> items) {
        List<String> list = new ArrayList<String>();
        for (String item : items) {
            item = item.trim();
            if (!CStringHelper.isEmpty(item) && !list.contains(item)) list.add(item);
        }
        return list;
    }

    public static String truncate(String str, int length) {
        if (str == null) return "";
        if (str.length() >= length) return str.substring(0, length); else return str;
    }

    public static String truncateEllipsis(String str, int length) {
        return truncate(str, length, "...");
    }

    public static String truncate(String str, int length, String trailing) {
        if (str == null) return "";
        if (str.length() < length) return str;
        int adjusted = length - trailing.length();
        return truncate(str, adjusted) + trailing;
    }

    private static final String CHARSET = "US-ASCII";

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CVardbException(e);
        }
    }

    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CVardbException(e);
        }
    }

    public static String generateID() {
        try {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            String randomNum = String.valueOf(prng.nextInt());
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] result = sha.digest(randomNum.getBytes());
            String id = hexEncode(result);
            return id;
        } catch (NoSuchAlgorithmException e) {
            throw new CVardbException(e);
        }
    }

    /**
	  * The byte[] returned by MessageDigest does not have a nice
	  * textual representation, so some form of encoding is usually performed.
	  *
	  * This implementation follows the example of David Flanagan's book
	  * "Java In A Nutshell", and converts a byte array into a String
	  * of hex characters.
	  *
	  * Another popular alternative is to use a "Base64" encoding.
	  */
    private static String hexEncode(byte[] input) {
        StringBuilder buffer = new StringBuilder();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int index = 0; index < input.length; ++index) {
            byte b = input[index];
            buffer.append(digits[(b & 0xf0) >> 4]);
            buffer.append(digits[b & 0x0f]);
        }
        return buffer.toString();
    }

    public static String[] convertToArray(List<String> list) {
        String[] arr = new String[list.size()];
        list.toArray(arr);
        return arr;
    }

    public static String formatDecimal(Float value, int numdecimals) {
        if (value == null) return "";
        return formatDecimal((double) value, numdecimals);
    }

    public static String formatDecimal(Double value, int numdecimals) {
        if (value == null) return "";
        return String.format("%." + numdecimals + "f", value);
    }

    public static String formatScientificNotation(Double value, int numdecimals) {
        if (value == null) return "";
        String pattern = "0." + CStringHelper.repeatString("#", numdecimals) + "E0";
        DecimalFormat format = new DecimalFormat(pattern);
        String formatted = format.format(value);
        if ("0E0".equals(formatted)) formatted = "0";
        return formatted;
    }

    public static void checkIdentifier(String identifier) {
        if (!CStringHelper.hasContent(identifier)) throw new CVardbException("identifier is null or empty: [" + identifier + "]");
    }

    public static boolean containsHtml(String str) {
        String regex = "</?\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/?>";
        return str.matches(regex);
    }

    public static boolean containsLinks(String str) {
        return str.toLowerCase().indexOf("<a href=") != -1;
    }

    public static boolean isSpam(String str) {
        return (CStringHelper.containsHtml(str) || CStringHelper.containsLinks(str));
    }

    public static boolean hasContent(Object obj) {
        if (obj == null) return false;
        String value = obj.toString();
        if (value.length() == 0) return false;
        value = value.trim();
        if (value.length() == 0) return false;
        return !EMPTY_STRING.equals(value);
    }

    public static Map<String, Object> createMap(Object... args) {
        int size = args.length / 2;
        if (args.length % 2 != 0) throw new CVardbException("name/value args should be provided as a multiple of 2: " + CStringHelper.join(args, ","));
        Map<String, Object> map = new HashMap<String, Object>();
        for (int index = 0; index < size; index++) {
            String name = (String) args[index * 2];
            Object value = args[index * 2 + 1];
            if (name == null) {
                System.out.println("arg name is null for arg " + index + " and value " + value);
                continue;
            }
            if (value == null) {
                System.out.println("arg value is null for name " + name);
                continue;
            }
            map.put(name, value);
        }
        return map;
    }

    public static String extractBetween(String str, String prefix, String suffix) {
        int start = str.indexOf(prefix);
        if (start == -1) throw new CVardbException("can't find prefix \"" + prefix + "\" in string: " + str);
        start += prefix.length();
        int end = str.indexOf(suffix, start);
        if (end == -1) throw new CVardbException("can't find suffix \"" + suffix + "\" in string: " + str);
        return str.substring(start, end);
    }

    public static String getText(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder buffer = new StringBuilder();
        for (int index = 0; index < bytes.length; index++) {
            buffer.append((char) bytes[index]);
        }
        return buffer.toString();
    }

    public static String escape(String value) {
        return CStringHelper.replace(value, "'", "''");
    }

    public static List<String> escape(Collection<String> values) {
        List<String> newvalues = new ArrayList<String>();
        for (String value : values) {
            newvalues.add(escape(value));
        }
        return newvalues;
    }

    public static List<String> escape(String[] values) {
        List<String> newvalues = new ArrayList<String>();
        for (String value : values) {
            newvalues.add(escape(value));
        }
        return newvalues;
    }

    public static boolean isEmptyJson(String str) {
        return (!CStringHelper.hasContent(str) || "null".equals(str) || "{}".equals(str));
    }

    public static String toJson(Object... args) {
        Object obj = (args.length == 1) ? args[0] : CStringHelper.createMap(args);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }

    public static String encodeBase64(String unencoded) {
        return Base64Encoder.encode(unencoded);
    }

    public static String decodeBase64(String encoded) {
        return Base64Decoder.decode(encoded);
    }

    public static boolean isEmailAddress(String email) {
        return !(!CStringHelper.hasContent(email) || email.indexOf('@') == -1 || email.indexOf('.') == -1);
    }

    public static String parenthesize(String value) {
        return "(" + value + ")";
    }

    public static String quote(String str) {
        return "\"" + str + "\"";
    }

    public static boolean isEmpty(String str) {
        return str == null || EMPTY_STRING.equals(str);
    }

    public static String extractParameterFromArray(String[] arr) {
        if (arr.length != 1) throw new CVardbException("should be exactly one value for parameter: " + arr);
        return arr[0];
    }

    public static String checkResolvedProperty(String property, String value) {
        if (!CStringHelper.hasContent(value)) throw new CVardbException("property " + property + " is not set: " + value);
        if (value.indexOf("${") != -1) throw new CVardbException("property " + property + " has unresolved placeholder: " + value);
        return value;
    }

    public static void main(String[] argv) {
        int num = Integer.parseInt(argv[0]);
        for (int index = 0; index < num; index++) {
            System.out.println(generateID());
        }
    }
}
