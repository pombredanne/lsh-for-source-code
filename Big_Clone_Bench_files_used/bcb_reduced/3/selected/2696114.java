package com.berd.core.utils;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.MessageDigest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import java.util.ArrayList;
import java.util.List;

/**
 * String Utility Class This is used to encode passwords programmatically
 *
 * <p>
 * <a h
 * ref="StringUtil.java.html"><i>View Source</i></a>
 * </p>
 *
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 */
public class StringUtil {

    private static final Log log = LogFactory.getLog(StringUtil.class);

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
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
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
        return encoder.encodeBuffer(str.getBytes()).trim();
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
     * 判断字符是否为空.
     *
     * @param source
     * @return String
     */
    public static boolean isEmpty(String source) {
        if (source == null || ("".equals(source))) return true;
        return false;
    }

    public static List detachString(String value, final String sign) {
        List<String> values = new ArrayList<String>();
        String target = value;
        while (true) {
            if (target == null || "".equals(target)) {
                return null;
            }
            int pos = target.indexOf(sign);
            if (pos != -1) {
                values.add(target.substring(0, pos));
                target = target.substring(pos + 1);
            } else {
                values.add(target);
                break;
            }
        }
        return values;
    }

    public static String addWhere(String hql) {
        Assert.hasText(hql);
        int beginPos = hql.toLowerCase().indexOf("where");
        Assert.isTrue(beginPos == -1, " hql : " + hql + " 查询语句已经包含关键字 'where'");
        return hql.trim() + " where ";
    }

    public static String addOrderBy(String hql, String orderBy) {
        Assert.hasText(hql);
        int beginPos = hql.toLowerCase().indexOf("order by");
        if (beginPos > 0) return hql; else return hql.trim() + " " + orderBy;
    }

    public static String filterWhere(String hql, Object[] query) {
        String result = hql;
        int endPos = hql.toLowerCase().indexOf("where");
        if ((query.length == 0) & (endPos != -1)) {
            result = hql.substring(0, endPos);
        }
        return result;
    }

    /**
     * 用指定的字符组合字符数组.
     *
     * @param array，division
     * @return String
     */
    public static String arrtoString(String[] array, String division) {
        String temp = "";
        for (String str : array) {
            temp = temp + str + division;
        }
        return temp;
    }

    /**
     * 在SQL的where子句中查找field出现的位置.
     *
     * @param where，field
     * @return int
     */
    public static int findString(String where, String field) {
        int tp = -1;
        int result = -1;
        where = where.toLowerCase();
        field = field.toLowerCase();
        while (where.length() > 0) {
            int pos = 0;
            int pos1 = where.indexOf(" and ");
            int pos2 = where.indexOf(" or ");
            if (pos1 > 0 || pos2 > 0) {
                if (pos1 > 0) {
                    if (pos2 > 0) {
                        if (pos1 > pos2) pos = pos2;
                    } else pos = pos1;
                } else pos = pos2;
                tp = tp + 1;
                if (where.substring(0, pos).trim().indexOf(field) > -1) {
                    result = tp;
                    break;
                }
                where = where.substring(pos + 3).trim();
            } else {
                if (where.indexOf(field) > -1) {
                    result = tp + 1;
                }
                break;
            }
        }
        return result;
    }

    /**
     * 
     *
     * @param where，field
     * @return int
     */
    public static boolean contains(String s, String text) {
        return contains(s, text, StringPool.COMMA);
    }

    public static boolean contains(String s, String text, String delimiter) {
        if ((s == null) || (text == null) || (delimiter == null)) {
            return false;
        }
        if (!s.endsWith(delimiter)) {
            s += delimiter;
        }
        int pos = s.indexOf(delimiter + text + delimiter);
        if (pos == -1) {
            if (s.startsWith(text + delimiter)) {
                return true;
            }
            return false;
        }
        return true;
    }

    public static int count(String s, String text) {
        if ((s == null) || (text == null)) {
            return 0;
        }
        int count = 0;
        int pos = s.indexOf(text);
        while (pos != -1) {
            pos = s.indexOf(text, pos + text.length());
            count++;
        }
        return count;
    }

    public static boolean endsWith(String s, char end) {
        return startsWith(s, (new Character(end)).toString());
    }

    public static boolean endsWith(String s, String end) {
        if ((s == null) || (end == null)) {
            return false;
        }
        if (end.length() > s.length()) {
            return false;
        }
        String temp = s.substring(s.length() - end.length(), s.length());
        if (temp.equalsIgnoreCase(end)) {
            return true;
        } else {
            return false;
        }
    }

    public static String merge(String array[]) {
        return merge(array, StringPool.COMMA);
    }

    public static String merge(String array[], String delimiter) {
        if (array == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i].trim());
            if ((i + 1) != array.length) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String read(ClassLoader classLoader, String name) throws IOException {
        return read(classLoader.getResourceAsStream(name));
    }

    public static String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString().trim();
    }

    public static String replace(String s, char oldSub, char newSub) {
        return replace(s, oldSub, new Character(newSub).toString());
    }

    public static String replace(String s, char oldSub, String newSub) {
        if ((s == null) || (newSub == null)) {
            return null;
        }
        char[] c = s.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == oldSub) {
                sb.append(newSub);
            } else {
                sb.append(c[i]);
            }
        }
        return sb.toString();
    }

    public static String replace(String s, String oldSub, String newSub) {
        if ((s == null) || (oldSub == null) || (newSub == null)) {
            return null;
        }
        int y = s.indexOf(oldSub);
        if (y >= 0) {
            StringBuffer sb = new StringBuffer();
            int length = oldSub.length();
            int x = 0;
            while (x <= y) {
                sb.append(s.substring(x, y));
                sb.append(newSub);
                x = y + length;
                y = s.indexOf(oldSub, x);
            }
            sb.append(s.substring(x));
            return sb.toString();
        } else {
            return s;
        }
    }

    public static String replace(String s, String[] oldSubs, String[] newSubs) {
        if ((s == null) || (oldSubs == null) || (newSubs == null)) {
            return null;
        }
        if (oldSubs.length != newSubs.length) {
            return s;
        }
        for (int i = 0; i < oldSubs.length; i++) {
            s = replace(s, oldSubs[i], newSubs[i]);
        }
        return s;
    }

    public static String reverse(String s) {
        if (s == null) {
            return null;
        }
        char[] c = s.toCharArray();
        char[] reverse = new char[c.length];
        for (int i = 0; i < c.length; i++) {
            reverse[i] = c[c.length - i - 1];
        }
        return new String(reverse);
    }

    public static String shorten(String s) {
        return shorten(s, 20);
    }

    public static String shorten(String s, int length) {
        return shorten(s, length, "..");
    }

    public static String shorten(String s, String suffix) {
        return shorten(s, 20, suffix);
    }

    public static String shorten(String s, int length, String suffix) {
        if (s == null || suffix == null) {
            return null;
        }
        if (s.length() > length) {
            s = s.substring(0, length) + suffix;
        }
        return s;
    }

    public static String[] split(String s) {
        return split(s, StringPool.COMMA);
    }

    public static String[] split(String s, String delimiter) {
        if (s == null || delimiter == null) {
            return new String[0];
        }
        s = s.trim();
        if (!s.endsWith(delimiter)) {
            s += delimiter;
        }
        if (s.equals(delimiter)) {
            return new String[0];
        }
        List nodeValues = new ArrayList();
        if (delimiter.equals("\n") || delimiter.equals("\r")) {
            try {
                BufferedReader br = new BufferedReader(new StringReader(s));
                String line = null;
                while ((line = br.readLine()) != null) {
                    nodeValues.add(line);
                }
                br.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            int offset = 0;
            int pos = s.indexOf(delimiter, offset);
            while (pos != -1) {
                nodeValues.add(s.substring(offset, pos));
                offset = pos + delimiter.length();
                pos = s.indexOf(delimiter, offset);
            }
        }
        return (String[]) nodeValues.toArray(new String[0]);
    }

    public static boolean[] split(String s, String delimiter, boolean x) {
        String[] array = split(s, delimiter);
        boolean[] newArray = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            boolean value = x;
            try {
                value = Boolean.valueOf(array[i]).booleanValue();
            } catch (Exception e) {
            }
            newArray[i] = value;
        }
        return newArray;
    }

    public static double[] split(String s, String delimiter, double x) {
        String[] array = split(s, delimiter);
        double[] newArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            double value = x;
            try {
                value = Double.parseDouble(array[i]);
            } catch (Exception e) {
            }
            newArray[i] = value;
        }
        return newArray;
    }

    public static float[] split(String s, String delimiter, float x) {
        String[] array = split(s, delimiter);
        float[] newArray = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            float value = x;
            try {
                value = Float.parseFloat(array[i]);
            } catch (Exception e) {
            }
            newArray[i] = value;
        }
        return newArray;
    }

    public static int[] split(String s, String delimiter, int x) {
        String[] array = split(s, delimiter);
        int[] newArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            int value = x;
            try {
                value = Integer.parseInt(array[i]);
            } catch (Exception e) {
            }
            newArray[i] = value;
        }
        return newArray;
    }

    public static long[] split(String s, String delimiter, long x) {
        String[] array = split(s, delimiter);
        long[] newArray = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            long value = x;
            try {
                value = Long.parseLong(array[i]);
            } catch (Exception e) {
            }
            newArray[i] = value;
        }
        return newArray;
    }

    public static short[] split(String s, String delimiter, short x) {
        String[] array = split(s, delimiter);
        short[] newArray = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            short value = x;
            try {
                value = Short.parseShort(array[i]);
            } catch (Exception e) {
            }
            newArray[i] = value;
        }
        return newArray;
    }

    public static final String stackTrace(Throwable t) {
        String s = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintWriter(baos, true));
            s = baos.toString();
        } catch (Exception e) {
        }
        return s;
    }

    public static boolean startsWith(String s, char begin) {
        return startsWith(s, (new Character(begin)).toString());
    }

    public static boolean startsWith(String s, String start) {
        if ((s == null) || (start == null)) {
            return false;
        }
        if (start.length() > s.length()) {
            return false;
        }
        String temp = s.substring(0, start.length());
        if (temp.equalsIgnoreCase(start)) {
            return true;
        } else {
            return false;
        }
    }

    public static String trimTrailingZero(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            int value = Character.getNumericValue(s.charAt(i));
            if (value != 0) {
                return s.substring(0, i + 1);
            }
        }
        return StringPool.BLANK;
    }

    public static String trimLeading(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return s.substring(i, s.length());
            }
        }
        return StringPool.BLANK;
    }

    public static String trimTrailing(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return s.substring(0, i + 1);
            }
        }
        return StringPool.BLANK;
    }

    public static String wrap(String text) {
        return wrap(text, 80, "\n");
    }

    public static String wrap(String text, int width, String lineSeparator) {
        if (text == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new StringReader(text));
            String s = StringPool.BLANK;
            while ((s = br.readLine()) != null) {
                if (s.length() == 0) {
                    sb.append(lineSeparator);
                } else {
                    String[] tokens = s.split(StringPool.SPACE);
                    boolean firstWord = true;
                    int curLineLength = 0;
                    for (int i = 0; i < tokens.length; i++) {
                        if (!firstWord) {
                            sb.append(StringPool.SPACE);
                            curLineLength++;
                        }
                        if (firstWord) {
                            sb.append(lineSeparator);
                        }
                        sb.append(tokens[i]);
                        curLineLength += tokens[i].length();
                        if (curLineLength >= width) {
                            firstWord = true;
                            curLineLength = 0;
                        } else {
                            firstWord = false;
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return sb.toString();
    }

    public static String getIdFromCa(String CaSource) {
        if (isEmpty(CaSource)) return null;
        String val = null;
        int pos = CaSource.indexOf("T=");
        if (pos != -1) val = CaSource.substring(pos + 2);
        pos = val.indexOf(",");
        if (pos != -1) val = val.substring(0, pos);
        return val;
    }
}
