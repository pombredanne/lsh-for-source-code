package com.jdg.utility;

import java.security.MessageDigest;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对字符串操作的工具类
 * 
 * @author david
 * @version 1.0.060925
 * @version 1.1.070402 by zhufei 将PK台系统中StringUtil移植到XBC中
 * @version 1.2 070414 增加nullOrBlack,notNull 方法 by mariqian
 */
public class StringUtil {

    /**
	 * 把source字符串中,出现src的字符替换成dest,返回替换后的字符串 例如:
	 * 
	 * @author:david
	 * @param:source,源字符串 src 源字符 dest 目标字符
	 * @return:解析后的字符串
	 */
    public static String replace(String source, String src, String dest) {
        if (source == null || "".equals(source)) {
            return "";
        }
        if (src == null || "".equals(src)) {
            return source;
        }
        String ret = "";
        if (source.indexOf(src) != -1) {
            while (source.indexOf(src) != -1) {
                ret += source.substring(0, source.indexOf(src));
                ret += dest;
                source = source.substring(source.indexOf(src) + src.length(), source.length());
            }
            ret += source;
        } else {
            ret = source;
        }
        return ret;
    }

    public static String getStringByRegex(String sourceString, String regex) {
        if (sourceString == null || "".equals(sourceString)) {
            return "";
        }
        if (regex == null || "".equals(regex)) {
            return sourceString;
        }
        String regEx = "\\^(.+)\\$";
        String str = "asd?sdsasnmd1alksd//sdbas^dnb$1asda$sd";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        str = m.replaceAll("\\^I HAVE BEAN RELACED\\$");
        System.out.println(str);
        return "";
    }

    /**
	 * 计算一个字符串的长度，2个字符计数1，1个中文计数1
	 * 
	 * @param s
	 *            字符串
	 * @return 长度
	 */
    public static int length(String s) {
        if (s == null || "".equals(s)) {
            return 0;
        }
        int size = 0;
        for (int i = 0; i < s.length(); i++) {
            size++;
            int ii = s.charAt(i);
            if (ii <= 0 || ii >= 126) size++;
        }
        return Math.round((float) size / 2);
    }

    /**
	 * 把source字符串中,指定位置替换成dest 指定位置replace
	 * 
	 * @see replace(String source, String dest,int startIndex,int endIndex)
	 */
    public static String replace(String source, String dest, int startIndex, int endIndex) {
        if (source == null || "".equals(source)) {
            return "";
        }
        if (dest == null || "".equals(dest)) {
            return source;
        }
        if (startIndex < 0) {
            return "";
        }
        if (endIndex > source.length()) {
            return "";
        }
        String ret = "";
        ret = source.substring(0, startIndex);
        ret += dest;
        ret += source.substring(endIndex, source.length());
        return ret;
    }

    /**
	 * md5加密算法
	 */
    public static String getMD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 检查是否含有字符
	 * 
	 * @param source
	 *            源字符串
	 * @param target
	 *            目标字符串
	 * @param token
	 *            分隔符
	 * @return true 包含 false 不包含
	 */
    public static boolean contains(String source, String target, String token) {
        if ("".equals(source) || "".equals(target) || "".equals(token)) {
            return false;
        }
        StringTokenizer st = new StringTokenizer(source, token);
        while (st.hasMoreTokens()) {
            String s = (String) st.nextElement();
            if (target.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] a) {
        StringUtil.displayBinaryArray(30, 58);
    }

    /**
	 * Check a string null or blank.
	 * 
	 * @param param
	 *            string to check
	 * @return boolean
	 */
    public static boolean nullOrBlank(String param) {
        return (param == null || param.trim().equals("")) ? true : false;
    }

    public static String notNull(String param) {
        return param == null ? "" : param.trim();
    }

    /**
	 * Parse a string to int.
	 * 
	 * @param param
	 *            string to parse
	 * @return int value, on exception return 0.
	 */
    public static int parseInt(String param, int defValue) {
        int i = defValue;
        try {
            i = Integer.parseInt(param);
        } catch (Exception e) {
        }
        return i;
    }

    public static long parseLong(String param, long defValue) {
        long l = defValue;
        try {
            l = Long.parseLong(param);
        } catch (Exception e) {
        }
        return l;
    }

    public static float parseFloat(String param, float defValue) {
        float f = defValue;
        try {
            f = Float.parseFloat(param);
        } catch (Exception e) {
        }
        return f;
    }

    public static double parseDouble(String param, double defValue) {
        double d = defValue;
        try {
            d = Double.parseDouble(param);
        } catch (Exception e) {
        }
        return d;
    }

    /**
	 * According to the tail character. remove the last one. return the changed
	 * string
	 * 
	 * @param sourceString
	 *            source string
	 * @param tail
	 *            tail string
	 * @return if the tail is not exist , return source string. Any exception ,
	 *         return source string. return changed string
	 * @author david
	 */
    public static String cutTail(String sourceString, String tail) {
        if (sourceString == null || "".equals(sourceString)) {
            return sourceString;
        }
        if (tail == null || "".equals(tail)) {
            return sourceString;
        }
        if (sourceString.lastIndexOf(tail) == sourceString.length() - 1) {
            return sourceString.substring(0, sourceString.length() - 1);
        } else {
            return sourceString;
        }
    }

    /**
	 * Parse a string to boolean.
	 * 
	 * @param param
	 *            string to parse
	 * @return boolean value, if param begin with(1,y,Y,t,T) return true, on
	 *         exception return false.
	 */
    public static boolean parseBoolean(String param) {
        if (nullOrBlank(param)) return false;
        switch(param.charAt(0)) {
            case '1':
            case 'y':
            case 'Y':
            case 't':
            case 'T':
                return true;
        }
        return false;
    }

    public static String encode2UTF8(String sourceString) {
        StringBuffer StrUrl = new StringBuffer();
        for (int i = 0; i < sourceString.length(); ++i) {
            switch(sourceString.charAt(i)) {
                case '+':
                    StrUrl.append("%2b");
                    break;
                default:
                    StrUrl.append(sourceString.charAt(i));
                    break;
            }
        }
        return StrUrl.toString();
    }

    /**
	 * Filter special character when URL transmission request parameters.
	 */
    public static String encodeURL(String text) {
        StringBuffer StrUrl = new StringBuffer();
        for (int i = 0; i < text.length(); ++i) {
            switch(text.charAt(i)) {
                case ' ':
                    StrUrl.append("%20");
                    break;
                case '+':
                    StrUrl.append("%2b");
                    break;
                case '\'':
                    StrUrl.append("%27");
                    break;
                case '/':
                    StrUrl.append("%2F");
                    break;
                case '.':
                    StrUrl.append("%2E");
                    break;
                case '<':
                    StrUrl.append("%3c");
                    break;
                case '>':
                    StrUrl.append("%3e");
                    break;
                case '#':
                    StrUrl.append("%23");
                    break;
                case '%':
                    StrUrl.append("%25");
                    break;
                case '&':
                    StrUrl.append("%26");
                    break;
                case '{':
                    StrUrl.append("%7b");
                    break;
                case '}':
                    StrUrl.append("%7d");
                    break;
                case '\\':
                    StrUrl.append("%5c");
                    break;
                case '^':
                    StrUrl.append("%5e");
                    break;
                case '~':
                    StrUrl.append("%73");
                    break;
                case '[':
                    StrUrl.append("%5b");
                    break;
                case ']':
                    StrUrl.append("%5d");
                    break;
                default:
                    StrUrl.append(text.charAt(i));
                    break;
            }
        }
        return StrUrl.toString();
    }

    public static String showNull(Object param, String defaultShow) {
        return (param == null) ? defaultShow : String.valueOf(param);
    }

    public static String filterToHtml(String value) {
        if (value == null) {
            return (null);
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\r') {
                result.append("<br>");
            } else if (ch == '\n') {
                if (i > 0 && value.charAt(i - 1) == '\r') {
                } else {
                    result.append("<br>");
                }
            } else {
                result.append(ch);
            }
        }
        return (result.toString());
    }

    public static void displayBinaryArray(int startNum, int endNum) {
        for (; startNum < endNum; startNum++) {
            int zeorNumber = startNum;
            StringBuffer binaryString = new StringBuffer("1");
            while (zeorNumber-- > 0) {
                binaryString.append("0");
            }
            System.out.println(Long.parseLong(binaryString.toString(), 2));
        }
    }
}
