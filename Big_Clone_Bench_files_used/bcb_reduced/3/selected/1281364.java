package net.os.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dom4j.Document;
import org.springframework.util.StringUtils;

/**
 * 
 * @Description��
 * @Email: eliyanfei@126.com
 * @Author foo.li
 * @Time Sep 28, 2010 11:01:47 AM
 */
public final class StringsUtils {

    public static final String getNumberFromString(final String str, final char except) {
        String rst = "";
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                final char cc = str.charAt(i);
                if (cc >= '0' && cc <= '9' || cc == '.' || cc == except) {
                    rst += cc;
                }
            }
        }
        return rst;
    }

    public static final String getNumberFromString(final String str) {
        String rst = "";
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                final char cc = str.charAt(i);
                if (cc >= '0' && cc <= '9' || cc == '.') {
                    rst += cc;
                }
            }
        }
        return rst;
    }

    /**
	 * ������������һ�����зָ�����ַ�
	 * 
	 * @param array
	 *            ����
	 * @param delim
	 *            �ָ���
	 * @return ���Ӻ���ַ�
	 */
    public static String join(final Object array, final String delim) {
        final Class<?> c = array.getClass();
        if (!c.isArray()) {
            return array.toString();
        }
        final StringBuilder joined = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < Array.getLength(array); i++) {
            if (isFirst) isFirst = false; else joined.append(delim);
            joined.append(Array.get(array, i));
        }
        return joined.toString();
    }

    public static String join(final Collection<?> coll, final String delim) {
        if (coll == null || coll.size() < 1) {
            return null;
        }
        return join(coll.toArray(), delim);
    }

    public static String joinArray(final Collection<String> strList, final String delim) {
        return joinArray(strList.toArray(new String[strList.size()]), delim);
    }

    public static String joinArray(final String[] strArray, final String delim) {
        return joinArray(strArray, delim, true);
    }

    public static String joinArray(final String[] strArray, final String delim, final String left, final String right, final boolean filterNull) {
        final StringBuilder joinedBuf = new StringBuilder();
        boolean isFirst = true;
        for (String str : strArray) {
            str = left + str + right;
            if (filterNull && isBlank(str)) continue;
            if (isFirst) isFirst = false; else joinedBuf.append(delim);
            joinedBuf.append(str);
        }
        return joinedBuf.toString();
    }

    public static String joinArray(final String[] strArray, final String delim, final boolean filterNull) {
        return joinArray(strArray, delim, "", "", filterNull);
    }

    /**
	 * ��ݲ�����MD5�㷨������һ����ֵ
	 * 
	 * @param data
	 *            Ҫ���ܵ��ַ�
	 * @return ���ܺ���ַ�
	 */
    public static final synchronized String md5(final String data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data.getBytes());
            final byte[] b = md.digest();
            return toHexString(b);
        } catch (final Exception e) {
        }
        return "";
    }

    /**
	 * ת���ֽ����鵽ʮ��λ�ַ�<br>
	 * ���������ο�{@link #toByteArray}
	 * 
	 * @param res
	 *            �ֽ�����
	 * @return ʮ��λ�ַ�
	 */
    public static String toHexString(final byte[] res) {
        if (res == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(res.length << 1);
        for (final byte re : res) {
            String digit = Integer.toHexString(0xFF & re);
            if (digit.length() == 1) {
                digit = '0' + digit;
            }
            sb.append(digit);
        }
        return sb.toString().toUpperCase();
    }

    public static byte[] toByteArray(final String hexString) {
        if (hexString == null) {
            return null;
        }
        final int arrLength = hexString.length() >> 1;
        final byte buff[] = new byte[arrLength];
        for (int i = 0; i < arrLength; i++) {
            final int index = i << 1;
            final String digit = hexString.substring(index, index + 2);
            buff[i] = (byte) Integer.parseInt(digit, 16);
        }
        return buff;
    }

    public static StringBuffer getStringBuffer(final Reader reader) throws IOException {
        StringBuffer buf = null;
        if (reader != null) {
            buf = new StringBuffer();
            final BufferedReader bReader = new BufferedReader(reader);
            String str = bReader.readLine();
            if (str != null) {
                buf.append(str);
                while ((str = bReader.readLine()) != null) {
                    buf.append('\n');
                    buf.append(str);
                }
            }
        }
        return buf;
    }

    public static String getString(final Reader reader) throws IOException {
        final StringBuffer buf = getStringBuffer(reader);
        if (buf == null) {
            return null;
        }
        return buf.toString();
    }

    public static String trimQuotes(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        final char chars[] = str.toCharArray();
        int l = 0;
        int r = chars.length - 1;
        if (chars[l] == '"' || chars[l] == '\'' && chars[l] == chars[r]) {
            l++;
            r--;
            return new String(chars, l, r);
        }
        return str;
    }

    public static String convertNewlines(String input) {
        if (input == null) {
            return null;
        }
        input = input.replaceAll("\r\n", "<BR>");
        input = input.replaceAll("\n", "<BR>");
        return input;
    }

    public static String escapeHTML(final String string) {
        return null;
    }

    public static String javaMethod(final String input) {
        boolean skipLine = false, skipPara = false, readyForSL = false, readyForSP = false;
        final StringBuilder tokenbuf = new StringBuilder();
        String methodName = null;
        final Vector<String> tokens = new Vector<String>();
        char c;
        for (int i = 0; i < input.length(); i++) {
            c = input.charAt(i);
            if (skipLine || skipPara) {
                if (skipLine) {
                    if (c == '\n') {
                        skipLine = false;
                    }
                }
                if (skipPara) {
                    if (c == '*') {
                        if (i < input.length() - 1 && input.charAt(i + 1) == '/') {
                            skipPara = false;
                        }
                        i++;
                    }
                }
                continue;
            }
            if (readyForSL || readyForSP) {
                if (readyForSL) {
                    if (c == '/') {
                        readyForSL = false;
                        skipLine = true;
                        readyForSP = false;
                        continue;
                    }
                    readyForSL = false;
                }
                if (readyForSP) {
                    if (c == '*') {
                        readyForSP = false;
                        skipPara = true;
                        readyForSL = false;
                        continue;
                    }
                    readyForSP = false;
                }
            }
            if (c == '/') {
                readyForSL = true;
                readyForSP = true;
                continue;
            }
            if (c == '\n' || c == '\t' || c == '\r' || c == ' ' || c == '\f') {
                if (tokenbuf.length() != 0) {
                    final String token = tokenbuf.toString();
                    if (token.indexOf('(') != -1) {
                        methodName = token;
                        break;
                    }
                    tokens.add(token);
                    tokenbuf.setLength(0);
                }
            } else {
                tokenbuf.append(c);
            }
        }
        if (methodName != null && methodName.length() != 0) {
            methodName = methodName.substring(0, methodName.indexOf('('));
        }
        return methodName;
    }

    public static boolean isEmail(final String s) {
        if (isNotBlank(s)) {
            int i = 1;
            final int sLength = s.length();
            while (i < sLength && s.charAt(i) != '@') {
                i++;
            }
            if (i >= sLength - 1 || s.charAt(i) != '@') {
                return false;
            }
            return true;
        }
        return false;
    }

    public static String trimNull(final Object src, final String defValue) {
        return trimNull(String.valueOf(src), defValue);
    }

    public static String trimNull(final String src, final String defValue) {
        return trimNullByWEB(src, defValue);
    }

    public static String trimNull(final String src) {
        return trimNullByWEB(src, "");
    }

    /**
	 * �ж��ַ��Ƿ������ݡ���������ζ���ַ���Ϊ���ҳ��Ȳ�Ϊ�㡣
	 * 
	 * @param s
	 *            ��Ҫ�жϵ��ַ�
	 * @return �������򷵻�<code>true</code>�����򷵻�<code>false</code>
	 */
    public static final boolean hasContent(final String s) {
        return s != null && s.trim().length() > 0;
    }

    /**
	 * ������WEB�϶�һ��������null���ַ���д���,ʹ��ʾ��(����ʾ"null"֮����ҳ����)
	 * 
	 * @param src
	 *            ��Ҫ���д����Դ�ַ�
	 * @return �������ַ�
	 */
    public static String trimNullByWEB(final String src) {
        return trimNullByWEB(src, "&nbsp;");
    }

    /**
	 * ������WEB�϶�һ��������null���ַ���д���,ʹ��ʾ��(����ʾ"null"֮����ҳ����),������һ��Ĭ��ֵ,
	 * ����src���и�ĵĽ����һ����ֵ,���ظô����Ĭ��ֵ
	 * 
	 * @param src
	 *            ��Ҫ���д����Դ�ַ�
	 * @param defaultValue
	 *            ���ڵ�һ������Ϊ��ʱ���ص�Ĭ��ֵ
	 * @return �������ַ�
	 */
    public static String trimNullByWEB(final String src, final String defaultValue) {
        if (isBlank(src)) return defaultValue;
        String newValue = null;
        try {
            newValue = src.trim();
        } catch (final NullPointerException npe) {
            newValue = null;
        }
        if ("".endsWith(newValue) || null == newValue || "null".equals(newValue.toLowerCase())) return defaultValue;
        return newValue;
    }

    public static final String encode(final String value) {
        return encode(value, "UTF-8");
    }

    public static final String encode(final String value, final String charset) {
        try {
            return URLEncoder.encode(value, charset);
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static final String decode(final String value) {
        return decode(value, "UTF-8");
    }

    public static final String decode(final String value, final String charset) {
        try {
            return URLDecoder.decode(value, charset);
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
	 * �ж�Դ�ַ�src���Ƿ��target���ַ�,����src���Զ����ʽregxp�ָ�. ��:
	 * contains("A,B,C","A",",")������true contains("A;B;C","A",";")������true
	 * 
	 * @param src
	 *            Դ�ַ���Ϊ��.
	 * @param target
	 *            ����Ϊ��.
	 * @param regxp
	 *            �Զ����ַ��ָ�ʽ
	 * @return
	 */
    public static boolean contains(final String src, final String target, final String regxp) {
        if (isBlank(src)) return false;
        if (isBlank(target)) return false;
        if (isBlank(regxp)) {
            return src.indexOf(target) > -1;
        }
        final StringTokenizer st = new StringTokenizer(src, regxp);
        while (st.hasMoreElements()) {
            if (target.equals(st.nextElement())) {
                return true;
            }
        }
        return false;
    }

    /**
	 * ��Դ�ַ�src�н�������ΪparamName��ֵ�滻ΪnewParamValue;
	 * 
	 * @param paramName
	 *            �����Ը�ʽ"paramName="
	 * @param src
	 *            Դ�ַ�,��ʽ:"param1=value1;param2=value2;"
	 * @param newParamValue
	 *            ��ʽ:value1
	 * @return �����src���Ҳ���������paramName,�������κ���.
	 */
    public static String replaceParam(final String paramName, final String src, final String newParamValue) {
        if (null == paramName || "".equals(paramName)) return src;
        if (null == src || "".equals(src)) return paramName + newParamValue + ";";
        final StringBuilder buff = new StringBuilder();
        final StringTokenizer st = new StringTokenizer(src, ";");
        String TMP;
        boolean modif = false;
        while (st.hasMoreElements()) {
            TMP = (String) st.nextElement();
            if (!"".equals(TMP)) {
                if (TMP.startsWith(paramName)) {
                    if (modif) {
                        continue;
                    }
                    buff.append(paramName);
                    buff.append(newParamValue);
                    buff.append(";");
                    modif = true;
                } else {
                    buff.append(TMP);
                    buff.append(";");
                }
            }
        }
        return buff.toString();
    }

    /**
	 * �ж�Դ�ַ�src���Ƿ��target���ַ�,����src��","�ŷָ�. ��:contains("A,B,C","A")������true
	 * ������κ�һ����ֵ����ַ�������false;
	 * 
	 * @param src
	 * @param target
	 * @return
	 */
    public static boolean contains(final String src, final String target) {
        return contains(src, target, ",");
    }

    public static final boolean likesContains(final String[] array, final String valueItem) {
        if (null == array || isBlank(valueItem)) return false;
        for (final String value : array) {
            if (value.contains(valueItem)) return true;
        }
        return false;
    }

    public static final boolean likesContains(final String valueItem, final String[] array) {
        if (null == array || isBlank(valueItem)) return false;
        for (final String value : array) {
            if (valueItem.contains(value)) return true;
        }
        return false;
    }

    /**
	 * �ж�һ���ַ��������Ƿ����һ��������valueItem��ֵ,�����õ��˱ȽϷ���:ֱ�����(equals),�Կ�ͷ(startsWith),�Խ�β
	 * (endsWith);
	 * 
	 * @param array
	 * @param valueItem
	 * @return
	 */
    public static final boolean likesOut(final String[] array, final String valueItem) {
        if (null == array || isBlank(valueItem)) return false;
        for (final String value : array) {
            if (value.equals(valueItem) || value.startsWith(valueItem) || value.endsWith(valueItem)) return true;
        }
        return false;
    }

    /**
	 * �ж�һ���ַ�valueItem�Ƿ��������ַ�����array�е�һ��Ԫ��,�����õ��˱ȽϷ���:ֱ�����(equals),�Կ�ͷ(
	 * startsWith),�Խ�β(endsWith);
	 * 
	 * @param array
	 * @param valueItem
	 * @return
	 */
    public static final boolean likesIn(final String valueItem, final String[] array) {
        if (null == array || isBlank(valueItem)) return false;
        for (final String value : array) {
            if (valueItem.equals(value) || valueItem.startsWith(value) || valueItem.endsWith(value)) return true;
        }
        return false;
    }

    /**
	 * �ж�һ���������Ƿ����һ��ֵvalue
	 * 
	 * @param array
	 * @param value
	 * @return
	 */
    public static boolean contains(final Object[] array, final Object value) {
        return indexOf(array, value) > -1;
    }

    public static int indexOf(final Object[] array, final Object value) {
        if (isBlank(String.valueOf(value))) return -2;
        if (null == array) return -3;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return -1;
    }

    /**
	 * ��paramName��paramValue��ϳ�һ�������ʽ�Ŀʹ�.
	 * 
	 * @param paramName
	 *            ������
	 * @param paramValue
	 *            ����ֵ
	 * @return paramName=paramValue
	 */
    public static String toParam_Value(final String paramName, final String paramValue) {
        if (isNotBlank(paramName) && isNotBlank(paramValue)) return " " + paramName + "=\"" + paramValue + "\"";
        return "";
    }

    public static int getGBKStringLength(final String gbkStr) {
        return getGBKStringLength(gbkStr, 7);
    }

    public static int getGBKStringLength(final String gbkStr, final int fontSize) {
        int width;
        try {
            width = new String(gbkStr.getBytes("GBK"), "ISO-8859-1").length() * fontSize;
        } catch (final Exception e) {
            width = gbkStr.length() * 18;
        }
        return width;
    }

    public static String inputStream2String(final InputStream is) {
        final BufferedReader in = new BufferedReader(new InputStreamReader(is));
        final StringBuilder buffer = new StringBuilder();
        String line = "";
        try {
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
        } catch (final IOException e) {
        }
        return buffer.toString();
    }

    public static InputStream String2InputStream(final String str) {
        final ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
        return stream;
    }

    /**
	 * ȥ���ַ��е�һ���ǿո��ַ�ǰ������пո�
	 * 
	 * @param srcStr
	 * @return
	 */
    public static final String trimStart(final String srcStr) {
        if (isBlank(srcStr)) return srcStr;
        final StringBuilder buf = new StringBuilder(srcStr);
        while (buf.charAt(0) == ' ') {
            buf.deleteCharAt(0);
        }
        return buf.toString();
    }

    /**
	 * ȥ���ַ������һ���ǿո��ַ��������пո�
	 * 
	 * @param srcStr
	 * @return
	 */
    public static final String trimEnd(final String srcStr) {
        if (isBlank(srcStr)) return srcStr;
        final StringBuilder buf = new StringBuilder(srcStr);
        buf.reverse();
        while (buf.charAt(0) == ' ') {
            buf.deleteCharAt(0);
        }
        buf.reverse();
        return buf.toString();
    }

    /**
	 * ������,����3�����ϲ�ͬ�ַ�ʹ��"+"�����ʱ,�������Ե��ڵ��ô˷���.
	 * 
	 * @param strs
	 * @return
	 */
    public static final String unionString(final String... args) {
        if (null == args) return "";
        if (args.length == 1) return args[0];
        return joinArray(args, "", false);
    }

    public static final String u(final Object... args) {
        if (null == args) return "";
        if (args.length == 1) return String.valueOf(args[0]);
        final StringBuilder buf = new StringBuilder();
        for (final Object obj : args) {
            buf.append(String.valueOf(obj));
        }
        return buf.toString();
    }

    /**
	 * @see #u(Object...)
	 * @deprecated
	 * @param args
	 * @return
	 */
    @Deprecated
    public static final String unionObject(final Object... args) {
        return u(args);
    }

    public static final Formatter formatter = new Formatter();

    public static final void logWithDateTime(final String logInfo) {
        System.out.print(infoWithDateTime(logInfo, true));
    }

    public static final void logWithDateTime(final String logInfo, final boolean before) {
        System.out.print(infoWithDateTime(logInfo, before));
    }

    public static final String infoWithDateTime(final String logInfo) {
        return infoWithDateTime(logInfo, true);
    }

    public static final String infoWithDateTime(final String logInfo, final boolean before) {
        String format;
        if (before) format = unionString("[LOG] ", logInfo, " %1$tY-%<tm-%<td %<tH:%<tM:%<tS \n"); else format = unionString("[LOG] %1$tY-%<tm-%<td %<tH:%<tM:%<tS \n", logInfo);
        return formatter.format(format, new Date()).toString();
    }

    public static final String formatText(final String format, final Object... args) {
        return formatter.format(format, args).toString();
    }

    public static final String like(final Collection<String> array, final String dest) {
        if (null == array || array.size() == 0 || isBlank(dest)) return "__NULL_LIKE__";
        int maxLiked = 0, curLiked;
        String maxLikedValue = null;
        for (final String src : array) {
            curLiked = like(src, dest);
            if (curLiked == src.length() + 1) {
                maxLikedValue = src;
                break;
            }
            if (curLiked > maxLiked) {
                maxLiked = curLiked;
                maxLikedValue = src;
            }
        }
        if (null == maxLikedValue) return dest;
        return maxLikedValue;
    }

    public static final int like(final String src, final String dest) {
        if (isBlank(src) || isBlank(dest)) return 0;
        if (src.equalsIgnoreCase(dest)) return src.length();
        if (src.equals(dest)) return src.length() + 1;
        int liked = 0;
        final char[] srcCharArr = src.trim().toLowerCase().toCharArray();
        final char[] destCharArr = dest.trim().toLowerCase().toCharArray();
        try {
            for (int i = 0; i < srcCharArr.length; i++) {
                if (srcCharArr[i] == destCharArr[i]) liked++;
            }
        } catch (final Exception e) {
        }
        return liked;
    }

    /**
	 * �������ֶδ��ĵ�һ����ĸ��ɴ�д,���ھ�Сд
	 * 
	 * @param srcStr
	 * @return
	 */
    public static final String toWord(final String srcStr) {
        final StringBuilder buf = new StringBuilder(srcStr.toLowerCase());
        buf.insert(0, Character.toUpperCase(buf.charAt(0)));
        buf.deleteCharAt(1);
        return buf.toString();
    }

    /**
	 * �����ַ�<param>newString</param>�滻ָ���ַ�<param>line</param>
	 * �е����о��ַ�<param>oldString</param>
	 * 
	 * @param line
	 *            ִ���滻�������ַ�
	 * @param oldString
	 *            �����滻���ľ��ַ�
	 * @param newString
	 *            �滻������ַ�
	 * @return ����ִ���滻��������ַ�
	 */
    public static final String replace(final String line, final String oldString, final String newString) {
        if (line == null) {
            return null;
        }
        int i = 0;
        if ((i = line.indexOf(oldString, i)) >= 0) {
            final char[] line2 = line.toCharArray();
            final char[] newString2 = newString.toCharArray();
            final int oLength = oldString.length();
            final StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = line.indexOf(oldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    public static String replaceOnce(final String line, final String oldString, final String newString) {
        if (line == null) {
            return null;
        }
        final int i = line.indexOf(oldString);
        if (i < 0) {
            return line;
        }
        final StringBuffer buf = new StringBuffer(line.substring(0, i));
        buf.append(newString);
        buf.append(line.substring(i + oldString.length()));
        return buf.toString();
    }

    /**
	 * �����ַ�<param>newString</param>�滻ָ���ַ�<param>line</param>
	 * �е����о��ַ�<param>oldString</param>
	 * <P>
	 * ����ƥ����ַ�ʱ������ִ�Сд
	 * 
	 * @param line
	 *            ִ���滻�������ַ�
	 * @param oldString
	 *            �����滻���ľ��ַ�
	 * @param newString
	 *            �滻������ַ�
	 * @return ����ִ���滻��������ַ�
	 */
    public static final String replaceIgnoreCase(final String line, final String oldString, final String newString) {
        if (line == null) {
            return null;
        }
        final String lcLine = line.toLowerCase();
        final String lcOldString = oldString.toLowerCase();
        int i = 0;
        if ((i = lcLine.indexOf(lcOldString, i)) >= 0) {
            final char[] line2 = line.toCharArray();
            final char[] newString2 = newString.toCharArray();
            final int oLength = oldString.length();
            final StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = lcLine.indexOf(lcOldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
	 * �����ַ�<param>newString</param>�滻ָ���ַ�<param>line</param>
	 * �е����о��ַ�<param>oldString</param>
	 * <P>
	 * �������滻�Ĵ���
	 * 
	 * @param line
	 *            ִ���滻�������ַ�
	 * @param oldString
	 *            �����滻���ľ��ַ�
	 * @param newString
	 *            �滻������ַ�
	 * @param count
	 *            �滻�Ĵ���
	 * @return ����ִ���滻��������ַ�
	 */
    public static final String replace(final String line, final String oldString, final String newString, final int[] count) {
        if (line == null) {
            return null;
        }
        int i = 0;
        if ((i = line.indexOf(oldString, i)) >= 0) {
            int counter = 0;
            counter++;
            final char[] line2 = line.toCharArray();
            final char[] newString2 = newString.toCharArray();
            final int oLength = oldString.length();
            final StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = line.indexOf(oldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

    /**
	 * ��һ���ַ��ĳλ�ÿ�ʼ��ĳ�ַ���Ϊ�ָ�����зָ�(�õ�ÿ����Ϊ�ַ���ַ�����). String list[] =
	 * StringUtils.splitString("AAAA,BBBB,CCCC,DDDDD",0,',') list Ϊ {
	 * "AAAA","BBBB","CCCC","DDDD" }
	 * 
	 * @param str
	 *            ���ָ����ַ�
	 * @param istart
	 *            ��ʼλ��
	 * @param delimiter
	 *            �ָ���
	 * @return �ָ��Ľ��
	 */
    public static final String[] splitString(final String str, final int istart, final char delimiter) {
        if (str == null) {
            return null;
        }
        final int sl = str.length();
        int n = 0;
        for (int i = istart; i < sl; i++) {
            if (str.charAt(i) == delimiter) {
                n++;
            }
        }
        final String[] sa = new String[n + 1];
        int i = istart, j = 0;
        for (; i < sl; ) {
            final int iend = str.indexOf(delimiter, i);
            if (iend < 0) {
                break;
            }
            sa[j++] = str.substring(i, iend);
            i = iend + 1;
        }
        sa[j++] = str.substring(i);
        return sa;
    }

    public static final char DELIMITER = ';';

    public static final String[] splitString(final String str) {
        return splitString(str, DELIMITER);
    }

    /**
	 * ��һ���ַ���ĳ�ַ���Ϊ�ָ�����зָ�(�õ�ÿ����Ϊ�ַ���ַ�����).
	 * 
	 * @param str
	 *            ���ָ����ַ�
	 * @param delimiter
	 *            �ָ���
	 * @return �ָ��Ľ��
	 */
    public static final String[] splitString(final String str, final char delimiter) {
        return splitString(str, 0, delimiter);
    }

    public static final <T> List<List<T>> splitList(final List<T> srcList, final int splitIdx) {
        final List<List<T>> result = new ArrayList<List<T>>();
        final int size = srcList.size();
        if (size <= splitIdx) {
            result.add(srcList);
            return result;
        }
        final int cnt = size / splitIdx + 1;
        int gidx = 0;
        List<T> tmps;
        for (int i = 0; i < cnt; i++) {
            if (gidx >= size) break;
            if (gidx + splitIdx > size) tmps = srcList.subList(gidx, size); else tmps = srcList.subList(gidx, gidx + splitIdx);
            result.add(tmps);
            gidx += splitIdx;
        }
        return result;
    }

    public static final Map<String, List<String>> dialogformat(String str) {
        final Map<String, List<String>> result = new HashMap<String, List<String>>();
        final int strlen = str.length();
        if (str.trim().substring(strlen - 1).equals("|")) {
            str = str.substring(0, strlen - 1);
        }
        final String[] strtable = str.split("\\|");
        for (final String element : strtable) {
            final List<String> temp = new ArrayList<String>();
            String tablename = null;
            String colname = null;
            final int strtablelen = element.toString().length();
            String tempstrtable = element.trim();
            if (tempstrtable.substring(strtablelen - 1).equals(":")) {
                tempstrtable = tempstrtable.substring(0, strtablelen - 1);
            }
            final String[] tablesp = tempstrtable.trim().split("\\:");
            final String[] tablenames = tablesp[0].split("\\;");
            tablename = tablenames[0].trim();
            for (int j = 1; j < tablesp.length; j++) {
                final String[] coltemp = tablesp[j].split("\\;");
                colname = coltemp[0].trim();
                temp.add(colname);
            }
            result.put(tablename, temp);
        }
        return result;
    }

    /**
	 * Improved version of java.lang.String.split() that supports escape.
	 * Example: if you want to split a string with comma "," as separator and
	 * with double quotes as escape characters, use
	 * <code>split("one, two, \"a,b,c\"", ",", "\"");</code>. Result is a list
	 * of 3 strings "one", "two", "a,b,c".
	 * <p>
	 * <b>Note:</b> keep in mind to escape the chars [b]\()[]{^$|?*+.[/b] that
	 * are special regular expression operators!
	 * 
	 * @param string
	 *            String to split up by the given <i>separator</i>.
	 * @param separator
	 *            Split separator.
	 * @param escape
	 *            Optional escape character to enclose substrings that can
	 *            contain separators.
	 * @return Separated substrings of <i>string</i>.
	 * @since 2.6.0
	 */
    public static String[] split(final String string, final String separator, final String escape) {
        List<String> result = new ArrayList<String>();
        if (string != null && separator != null) {
            if (escape == null || "".equals(escape)) {
                result = Arrays.asList(string.split(separator));
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append("\\s*");
                sb.append(escape);
                sb.append("(.*?)");
                sb.append(escape);
                sb.append("\\s*");
                sb.append("|");
                sb.append("(?<=^|");
                sb.append(separator);
                sb.append(")");
                sb.append("[^");
                sb.append(separator);
                sb.append("]*");
                final String regEx = sb.toString();
                final Pattern p = Pattern.compile(regEx);
                final Matcher m = p.matcher(string);
                while (m.find()) {
                    result.add(m.group(1) != null ? m.group(1) : m.group());
                }
            }
        }
        return result.toArray(new String[0]);
    }

    public static final String[] splitMulti(final String string, final String escape, final String... separators) {
        Collection<String> strColl = new ArrayList<String>();
        strColl.add(string);
        strColl = splitMulti0(strColl, 0, escape, separators);
        return strColl.toArray(new String[strColl.size()]);
    }

    private static final Collection<String> splitMulti0(final Collection<String> strColl, final int separatorIdx, final String escape, final String[] separators) {
        if (null == separators || separators.length == 0) return strColl;
        if (separatorIdx >= separators.length) return strColl;
        final String separator = separators[separatorIdx];
        final Collection<String> result = new ArrayList<String>();
        String tmpArr[];
        for (final String tmpStr : strColl) {
            tmpArr = split(tmpStr, separator, escape);
            for (final String tmpItm : tmpArr) {
                if (!result.contains(tmpItm)) result.add(tmpItm);
            }
        }
        return splitMulti0(result, separatorIdx + 1, escape, separators);
    }

    public static String inSQL(final Object[] con, final boolean flag) {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < con.length; i++) {
            if (flag) {
                buf.append("'").append(con[i]).append("'");
            } else {
                buf.append(con[i]);
            }
            if (i != con.length - 1) {
                buf.append(",");
            }
        }
        return buf.toString();
    }

    /**
	 * <p>Checks if a String is whitespace, empty ("") or null.</p>
	 *
	 * <pre>
	 * isBlank(null)      = true
	 * isBlank("")        = true
	 * isBlank(" ")       = true
	 * isBlank("bob")     = false
	 * isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param str  the String to check, may be null
	 * @return <code>true</code> if the String is null, empty or whitespace
	 * @since 2.0
	 */
    public static boolean isBlank(final String str) {
        int strLen;
        if (str == null || "null".equals(str.toLowerCase()) || (strLen = str.length()) == 0 || str.trim().length() == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(str.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
	 * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
	 *
	 * <pre>
	 * StringUtils.isNotBlank(null)      = false
	 * StringUtils.isNotBlank("")        = false
	 * StringUtils.isNotBlank(" ")       = false
	 * StringUtils.isNotBlank("bob")     = true
	 * StringUtils.isNotBlank("  bob  ") = true
	 * </pre>
	 *
	 * @param str  the String to check, may be null
	 * @return <code>true</code> if the String is
	 *  not empty and not null and not whitespace
	 * @since 2.0
	 */
    public static boolean isNotBlank(final String str) {
        return !isBlank(str);
    }

    /**
	 * ����IP����
	 * @param ip
	 * @return
	 */
    public static String getIpLocation(String ip) {
        try {
            URI uri = new URI("http://www.youdao.com/smartresult-xml/search.s?type=ip&q=" + ip);
            Document docu = Dom4jUtils.readDom4jXmlDocument(uri.toURL().openStream());
            return docu.getRootElement().element("product").elementText("location");
        } catch (final Exception e) {
        }
        return "";
    }

    /**
	* ${}���ֵ�滻
	* @param value
	* @return
	*/
    public static String replate$(final String value, final Map<String, String> map, final String start$, final String end$) {
        int eIndex = 0;
        String tempValue = value;
        final int lens = tempValue.length();
        int sIndex = tempValue.indexOf(start$, eIndex);
        while (sIndex < lens && sIndex != -1) {
            eIndex = tempValue.indexOf(end$, sIndex) + 1;
            final String sub = tempValue.substring(sIndex, eIndex);
            final String rValue = trimNull(map.get(sub), sub);
            if (rValue != null) {
                tempValue = StringsUtils.replace(tempValue, sub, rValue);
            }
            sIndex = tempValue.indexOf(start$, eIndex);
        }
        return tempValue;
    }

    public static String replate$(final String value, final Map<String, String> map) {
        return replate$(value, map, start$, end$);
    }

    static final String start$ = "${";

    static final String end$ = "}";

    public static String[] tokenizeToStringArray(final String str) {
        return tokenizeToStringArray(str, defaultDelimiter);
    }

    public static String[] tokenizeToStringArray(final String str, final String delimiters) {
        return str == null ? null : StringUtils.tokenizeToStringArray(str, delimiters);
    }

    public static final String defaultDelimiter = ";";
}
