package com.faceye.core.util.helper;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.security.MessageDigest;
import java.io.IOException;
import org.acegisecurity.providers.encoding.Md5PasswordEncoder;
import org.acegisecurity.providers.encoding.PasswordEncoder;

public class StringUtil {

    public static String[] split(String source, String identifer) {
        String[] result = source.split(identifer);
        return result;
    }

    public static String createID(String source) {
        return source + Calendar.YEAR + Calendar.MONTH + Calendar.DAY_OF_MONTH + Calendar.HOUR + Calendar.MINUTE + Calendar.SECOND;
    }

    private static String getCurDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        return df.format(new Date());
    }

    public static String randomInt() {
        Random random = new Random();
        return getCurDateTime() + Math.abs(random.nextInt()) % 1000;
    }

    public static String randPassword() {
        Random random = new Random();
        long timeMillis = System.currentTimeMillis();
        System.out.println(timeMillis);
        String password = (Math.abs(random.nextInt()) % 1000000) + "";
        return password;
    }

    public static String getSubStrigBetween(String source, String identifer) {
        String result = new String();
        return result;
    }

    public static int getFirstAppearOfIdentifer(String source, String identifer) {
        int result;
        if (source.indexOf(identifer) != -1) {
            result = source.indexOf(identifer);
        } else {
            result = source.length();
        }
        return result;
    }

    public static String getSubStringBeforeIdentier(String source, String identifer) {
        return source.substring(0, getFirstAppearOfIdentifer(source, identifer));
    }

    public static String getSubStringBeforeLastIdentifer(String source, String identifer) {
        if (source == null) {
            return null;
        }
        int index = getLastAppearOfIdentifer(source, identifer);
        return source.substring(0, index);
    }

    public static String getSubStringAfterIdentifer(String source, String identifer) {
        return source.substring(getFirstAppearOfIdentifer(source, identifer));
    }

    public static int getLastAppearOfIdentifer(String source, String identifer) {
        int result = source.lastIndexOf(identifer);
        return result;
    }

    public static String getSubStringAfterLastIdentifer(String source, String identifer) {
        return source.substring(getLastAppearOfIdentifer(source, identifer) + 1);
    }

    public static String getSubStringAfterDelSon(String source, int length) {
        return source.substring(length).toLowerCase();
    }

    public static String getSubStringBetweenIdentifer(String source, String firstidentifer, String secondidentifer) {
        return source.substring(getFirstAppearOfIdentifer(source, firstidentifer), getLastAppearOfIdentifer(source, secondidentifer) + 1);
    }

    public static String rightTrim(String source) {
        char[] item = source.toCharArray();
        int length = item.length;
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(item[i])) {
                index = i + 1;
            } else {
                break;
            }
        }
        return index == 0 ? source : source.substring(index);
    }

    public static String leftTrim(String source) {
        char[] item = source.toCharArray();
        int length = item.length;
        int index = 0;
        for (int i = length; i > 0; i--) {
            if (Character.isWhitespace(item[i])) {
                index = i - 1;
            } else {
                break;
            }
        }
        return index == 0 ? source : source.substring(0, index);
    }

    public static String toLowerCase(String source) {
        return null == source ? null : source.toLowerCase();
    }

    public static String toUpperCase(String source) {
        return null == source ? null : source.toUpperCase();
    }

    public static boolean isStartWith(String source, String identifer) {
        if (source == null || "".equals(source)) {
            return false;
        }
        if (identifer == null || "".equals(identifer)) {
            return false;
        }
        return source.startsWith(identifer);
    }

    public static boolean isIncludeIdentifer(String source, String identifer) {
        if (source.indexOf(identifer) != -1) {
            return true;
        } else {
            return false;
        }
    }

    public static String[] serchSource(String[] source, String identifer) {
        StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < source.length; i++) {
            if (isStartWith(source[i], identifer) || isIncludeIdentifer(source[i], identifer)) {
                sbf.append(source[i]);
                sbf.append("/$%#*.@!~/");
            }
        }
        return split(sbf.toString(), "/$%#*.@!~/");
    }

    public static String joinString(String first, String second) {
        return first + second;
    }

    public static String joinString(String source, int start, int end, String firstJoinIn, String secondJoinIn) {
        String result;
        String startwith = source.substring(0, start);
        String endwith = source.substring(end);
        String middle = source.substring(start, end);
        String resultMiddle = firstJoinIn + middle + secondJoinIn;
        result = startwith + resultMiddle + endwith;
        return result;
    }

    public static String timesString(String source, int times) {
        String result = new String();
        for (int i = 0; i < times; i++) {
            result += source;
        }
        return result;
    }

    public static String[] split(String source, int every) {
        int length = source.length();
        int arraySize;
        if (length % every == 0) {
            arraySize = length / every;
        } else {
            arraySize = length / every + 1;
        }
        String result[] = new String[arraySize];
        for (int i = 0; i < arraySize; i++) {
            if (source.substring(i * every).length() > every) {
                result[i] = source.substring(i * every, (i + 1) * every);
            } else {
                result[i] = source.substring(i * every);
            }
        }
        return result;
    }

    public static String split(String source, String insert, int every) {
        String result = new String();
        String[] temp = split(source, every);
        for (int i = 0; i < temp.length; i++) {
            result += temp[i] + insert;
        }
        return result;
    }

    public static String createCountHQL(String queryName) {
        String hql;
        if (queryName == null || "".equals(queryName)) {
            return null;
        }
        String itemSource = toLowerCase(queryName);
        if (itemSource.indexOf("from") == -1) {
            return null;
        }
        hql = "select count(*) " + queryName.substring(itemSource.indexOf("from"));
        return hql;
    }

    public static String createCountSQL(String sql) {
        String itemSource = toLowerCase(sql);
        if (itemSource.indexOf("from") == -1) {
            return null;
        }
        String countSQL = "select count(*) from (" + itemSource + ") xCount";
        return countSQL;
    }

    public static String createPageSQL(String sql, int pageSize, int startIndex) {
        String itemSource = toLowerCase(sql);
        if (itemSource == null) {
            return null;
        }
        return createMySQLPageSql(sql, pageSize, startIndex);
    }

    private static String createMySQLPageSql(String sql, int pageSize, int startIndex) {
        String result = sql + " limit " + startIndex + "," + pageSize;
        return result;
    }

    public static String createDelSQL(String tablename, String delIdentifer) {
        String result = null;
        if (tablename != null && !"".equals(tablename) && delIdentifer != null && !"".equals(delIdentifer)) {
            result = "delete from " + tablename + " where " + delIdentifer + " = ?";
            return result;
        }
        return result;
    }

    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
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
	 * Encode a string using Base64 encoding. Used when storing passwords as
	 * cookies.
	 * 
	 * This is weak encoding in that anyone can use the decodeString routine to
	 * reverse the encoding.
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

    public static String transFromArray(String[] source) {
        if (source == null || source.length < 1) {
            return null;
        }
        String result = new String();
        for (int i = 0; i < source.length; i++) {
            result += source[i];
        }
        return result;
    }

    public static boolean isAppearedMoreThanOnce(String source, char identifer) {
        boolean result = false;
        if (source == null || "".equals(source)) {
            return result;
        }
        return result;
    }
}
