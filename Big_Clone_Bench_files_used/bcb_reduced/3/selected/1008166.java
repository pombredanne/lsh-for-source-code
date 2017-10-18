package org.sss.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import javax.mail.internet.MimeUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 公共功能部分
 * @author Jason.Hoo (latest modification by $Author: hujianxin78728 $)
 * @version $Revision: 377 $ $Date: 2009-03-14 05:31:31 -0400 (Sat, 14 Mar 2009) $
 */
public class PublicUtils {

    static final Log log = LogFactory.getLog(PublicUtils.class);

    public static final String DATE_FORMAT_DEFAULT = "yyyy/MM/dd";

    public static final String LANGUAGE_DEFAULT = "zh";

    public static final String COUNTRY_DEFAULT = "CN";

    private static final String[] hex = { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1A", "1B", "1C", "1D", "1E", "1F", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2A", "2B", "2C", "2D", "2E", "2F", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4A", "4B", "4C", "4D", "4E", "4F", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5A", "5B", "5C", "5D", "5E", "5F", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6A", "6B", "6C", "6D", "6E", "6F", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7A", "7B", "7C", "7D", "7E", "7F", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8A", "8B", "8C", "8D", "8E", "8F", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9A", "9B", "9C", "9D", "9E", "9F", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF", "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF", "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF", "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF", "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF", "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF" };

    /**
   * 将Unicode的字符串进行网页处理
   */
    public static String escape(String s) {
        StringBuffer sbuf = new StringBuffer();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') sbuf.append((char) ch); else if (ch >= 'a' && ch <= 'z') sbuf.append((char) ch); else if (ch >= 'A' && ch <= 'Z') sbuf.append((char) ch); else if (ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '_' || ch == '@' || ch == '.') sbuf.append((char) ch); else if (ch <= 0x007F) {
                sbuf.append('%');
                sbuf.append(hex[ch]);
            } else {
                sbuf.append('%');
                sbuf.append('u');
                sbuf.append(hex[(ch >>> 8)]);
                sbuf.append(hex[(0x00FF & ch)]);
            }
        }
        return sbuf.toString();
    }

    public static final String encodeBase64(String value) {
        try {
            ByteArrayOutputStream bos;
            if (value == null || "".equals(value)) return "";
            bos = new ByteArrayOutputStream();
            OutputStream encodedStream = MimeUtility.encode(bos, "base64");
            encodedStream.write(value.getBytes());
            return bos.toString("iso-8859-1");
        } catch (Exception e) {
            return "";
        }
    }

    /**
   * 计算 BASE64 形式 SHA 摘要算法字符串值.
   */
    public static final String digestPassword(String password) {
        MessageDigest md;
        ByteArrayOutputStream bos;
        if (password == null || password.equals("")) return "";
        try {
            md = MessageDigest.getInstance("SHA");
            byte[] digest = md.digest(password.getBytes("iso-8859-1"));
            bos = new ByteArrayOutputStream();
            OutputStream encodedStream = MimeUtility.encode(bos, "base64");
            encodedStream.write(digest);
            return bos.toString("iso-8859-1");
        } catch (Exception _) {
            return "";
        }
    }

    /**
   * 按指定格式转换日期字符串.
   */
    public static final String dateToString(Date date, String formatStr, Locale locale) {
        if (date == null) date = new Date();
        if (formatStr == null) formatStr = DATE_FORMAT_DEFAULT;
        if (locale == null) locale = new Locale(LANGUAGE_DEFAULT, COUNTRY_DEFAULT);
        SimpleDateFormat format = new SimpleDateFormat(formatStr, locale);
        return format.format(date);
    }

    public static final String dateToString(Date date, String formatStr) {
        return dateToString(date, formatStr, new Locale(LANGUAGE_DEFAULT, COUNTRY_DEFAULT));
    }

    public static final String dateToString(Date date) {
        return dateToString(date, DATE_FORMAT_DEFAULT, new Locale(LANGUAGE_DEFAULT, COUNTRY_DEFAULT));
    }

    public static final String sayTotalString(double value, String formatStr, Locale locale) {
        return "";
    }

    public static final String sayTotalString(double value, String formatStr) {
        return sayTotalString(value, formatStr, new Locale(LANGUAGE_DEFAULT, COUNTRY_DEFAULT));
    }

    public static final void debug(Object object) {
        if (object == null) log.info("Object is null!!!"); else if (object instanceof Enumeration) {
            Enumeration _enum = (Enumeration) object;
            while (_enum.hasMoreElements()) log.info(_enum.nextElement());
        } else {
            log.info("Object is " + object.getClass().getName());
        }
    }

    public static final long subDate(Date beginDate, Date endDate) {
        if (beginDate == null) return 0;
        if (endDate == null) return new Date().getTime() - beginDate.getTime(); else return endDate.getTime() - beginDate.getTime();
    }

    public static final String longTimeValueToMonthStr(long timeValue) {
        long month = timeValue / (1000L * 3600 * 24 * 30);
        long year = month / 12;
        month = month % 12;
        if (year > 0) {
            if (month == 0) return year + "年整"; else return year + "年另" + month + "个月";
        } else {
            if (month == 0) return "无"; else return month + "个月";
        }
    }

    public static final String getBeginEndDateToStr(Date beginDate, Date endDate, String formatStr, Locale locale) {
        if (beginDate == null) return ""; else if (endDate == null) return "从" + PublicUtils.dateToString(beginDate, formatStr, locale) + "至今"; else return "从" + PublicUtils.dateToString(beginDate, formatStr, locale) + "至" + PublicUtils.dateToString(endDate, formatStr, locale);
    }

    public static final String combinValues(Object[] objects) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; objects != null && i < objects.length; i++) {
            if (objects[i] != null) buffer.append("  ").append(objects[i]);
        }
        return buffer.toString();
    }

    /**
   * 起始时间(将时、分、秒设置为00:00:00)
   */
    public static final Date getBeginDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
   * 结束时间(将时、分、秒设置为23:59:59)
   */
    public static final Date getEndDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public static final String toString(long value, int length) {
        String r = Long.toString(value);
        for (int i = r.length(); i < length; i++) r = "0" + r;
        return r;
    }

    /**
   * 获取当前平台系统的当前版本信息
   */
    public static final String getVersion() {
        InputStream is = ClassLoader.getSystemResourceAsStream("META-INF/version.txt");
        byte[] version = new byte[1024];
        try {
            int length = is.read(version);
            return new String(version, 0, length);
        } catch (Exception _) {
            return "0.0.0-build0";
        }
    }
}
