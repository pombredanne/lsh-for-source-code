package com.litt.core.common;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Encoder;
import com.litt.core.security.MD5;
import com.litt.core.util.FormatDateTime;

/**
 * 
 * <b>标题：</b>基础功能类.
 * <pre><b>描述：</b>
 *    常用数据转换、日期处理、日期比较工具
 * </pre>
 * 
 * @author <a href="mailto:littcai@hotmail.com">空心大白菜</a>
 * @since 2006-08-30
 * @version 1.0
 */
public class Utility {

    private static final transient Log logger = LogFactory.getLog(Utility.class);

    public static String SEPARATOR = File.separator;

    public static boolean IS_DEBUG = true;

    private Utility() {
    }

    /**
	 * 获取系统运行时类路径下的配置文件(不推荐使用)
	 * 正式运行时可注释掉
	 */
    public static String getTestRunPath() {
        return System.getProperty("user.dir");
    }

    /**
     * 获取系统运行时类路径的绝对路径.（测试用）
     * 该方法在某些应用服务器上会取到另外的地址,故只做测试之用
     * 
     * @return String 系统类路径的绝对路径
     */
    public static String getClassLoaderPath() {
        return Thread.currentThread().getContextClassLoader().getResource("").getPath();
    }

    /**
     * 获取系统运行时的绝对路径.
     * 该方法获取的值需在项目启动时通过代码设置
     * 如：D:\TOMCAT\webapps\project
     * 
     * @return String 系统运行时的绝对路径,ROOT_PATH
     * 
     */
    public static String getRootPath() {
        return Constants.ROOT_PATH;
    }

    /**
     * 读取系统类路径下的配置文件.
     * 配置文件的路径获取方式也是通过线程容器得到的，需要测试是否能兼容某些应用服务器
     * 
     * @param propertiesName 属性配置文件名称
     * 
     * @throws IOException
     */
    public static void initProps(String propertiesName) throws IOException {
        System.getProperties().load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesName));
    }

    /**
     * 从System属性对象中获得属性值.
     * 
     * @param keyValue 属性唯一索引值
     * 
     * @return String 属性值
     */
    public static String getProperty(String keyValue) {
        return System.getProperties().getProperty(keyValue);
    }

    /**
	 * 获取windows登陆用户名
	 * @return
	 */
    public static String getWindowsUser() {
        return System.getProperty("user.name");
    }

    /**
     * 将ISO-8859-1编码的字符转换成UTF-8
     * @param str 需要编码转换的字符串
     * @return String 转换过后的字符串
     */
    public static String ecodingString(String str) {
        String reStr = "";
        reStr = str == null ? "" : str;
        reStr = reStr.trim();
        try {
            reStr = new String(reStr.getBytes("ISO-8859-1"), "UTF-8");
        } catch (Exception e) {
            logger.error("编码转换失败", e);
        }
        return reStr;
    }

    /**
     * 将ecodeA编码的字符转换成ecodeB.
     * 
     * @param ecodeB 目标编码类型
     * @param ecodeA 原编码类型
     * @param str 需要编码转换的字符串
     * 
     * @return String 转换过后的字符串
     */
    public static String ecodingString(String str, String ecodeA, String ecodeB) {
        String reStr = "";
        reStr = str == null ? "" : str;
        reStr = reStr.trim();
        try {
            reStr = new String(reStr.getBytes(ecodeA), ecodeB);
        } catch (Exception e) {
            logger.error("编码转换失败", e);
        }
        return reStr;
    }

    /**
     * 字符串分割，使用StringTokenizer,不支持空字符串（不推荐使用，目前统一使用spitString方法）
     * @param value 需要分割的字符串
     * @param spit 分隔符
     * @return String[] 分割后的字符串数组
     */
    public static String[] tokenString(String value, String spit) {
        StringTokenizer spitToker = new StringTokenizer(value, spit);
        int count = spitToker.countTokens();
        String[] ret = new String[count];
        int i = 0;
        while (spitToker.hasMoreTokens()) {
            ret[i] = spitToker.nextToken();
            i++;
        }
        return ret;
    }

    /**
	 * 将字符串按指定分隔符转换成数组.
	 * 使用.split方法，部分支持空字符串(JDK1.4以上使用).
	 * 
	 * @param value 需要分割的字符串
	 * @param split 分隔符
	 * @return String[] 分割后的字符串数组
	 */
    public static String[] splitString(String value, String split) {
        String[] ret = value.split(split);
        return ret;
    }

    /**
	 * 将字符串按指定分隔符转换成数组.
	 * 使用common-lang的StringUtils，支持空字符串.
	 * 
	 * @param value 需要分割的字符串
	 * @param split 分隔符
	 * @return String[] 分割后的字符串数组
	 */
    public static String[] splitStringAll(String value, String split) {
        return StringUtils.splitPreserveAllTokens(value, split);
    }

    /**
	 * 将数组转换成用逗号分割的字符串.
	 * 该方法主要用于SQL语句多字段的拼接
	 * 
	 * @param values 需要合并的字符串数组
	 * 
	 * @return String合并后的字符串
	 */
    public static String joinString(String[] values) {
        StringBuffer ret = new StringBuffer();
        if (values == null) return ""; else {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null && !values[i].trim().equals("")) {
                    ret.append(values[i]);
                    ret.append(",");
                }
            }
        }
        int point = ret.lastIndexOf(",");
        if (point > 0) return ret.substring(0, point); else return ret.toString();
    }

    /**
	 * 将字符串数组按指定分隔符拼接成一个字符串.
	 * 该方法主要用于SQL语句多字段的拼接
	 * 
	 * @param values 需要合并的字符串数组
	 * @param split 分隔符
	 * 
	 * @return String合并后的字符串
	 */
    public static String joinString(Object[] values, String split) {
        return StringUtils.join(values, split);
    }

    /**
	 * 字符串截取，主要用于超链文字过长的截取，后面加...
	 * 
	 * @param str 需要被截取的字符串
	 * @param len 被截取后剩余的长度	 
	 * 
	 * @return String 被截取后的字符串
	 */
    public static String subTitle(String str, int len) {
        if (str == null || str.trim().equals("")) return "";
        if (str.length() <= len) return str; else return str.substring(0, len) + "...";
    }

    /**
     * 判断传入的字符串对象是否为NULL或空，是则返回true，否则返回false
     * @param s 传入的字符串对象
     * @return boolean 
     */
    public static boolean isEmpty(String s) {
        if (StringUtils.isEmpty(s)) return true; else return false;
    }

    /**
	 * 如果对象为NULL则返回空字符串.
	 * 该方法常用于避免页面null值显示
	 * 
	 * @param o 可转变成String类型的对象
	 * @return String 非空字符串对象
	 */
    public static String trimNull(Object o) {
        if (o == null) return "";
        return o.toString();
    }

    /**
	 * 如果对象为NULL则返回默认字符串
	 * @param o 可转变成String类型的对象
	 * @param defaultValue 默认值
	 * @return String 非空字符串对象
	 */
    public static String trimNull(Object o, String defaultValue) {
        if (o == null) return defaultValue;
        return o.toString();
    }

    /**
	 * 如果对象为NULL则返回默认整型数
	 * @param o 可转变成String类型的对象
	 * @param defaultValue  默认值
	 * @return String 非空字符串对象
	 */
    public static int trimNull(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        return Integer.valueOf(o.toString().trim()).intValue();
    }

    /**
	 * 将字符串转换成boolean类型,默认值false。转换失败也返回false（可以是1、0、yes、no、true、false）
	 * @param s 可转换成boolean型的字符串
	 * @return boolean 转换后的值
	 */
    public static boolean parseBoolean(String s) {
        if (isEmpty(s)) return false;
        if (s.equals("1")) return true; else if (s.equals("0")) return false; else if (s.equalsIgnoreCase("yes")) return true; else if (s.equalsIgnoreCase("no")) return false;
        try {
            return Boolean.valueOf(s).booleanValue();
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    /**
	 * 将字符串转换成boolean类型，失败则返回默认值（可以是1、0、yes、no、true、false）
	 * @param s 可转换成boolean型的字符串
	 * @param defaultValue 默认值
	 * @return boolean 转换后的值
	 */
    public static boolean parseBoolean(String s, boolean defaultValue) {
        if (isEmpty(s)) return defaultValue;
        if (s.equals("1")) return true; else if (s.equals("0")) return false; else if (s.equalsIgnoreCase("yes")) return true; else if (s.equalsIgnoreCase("no")) return false;
        try {
            return Boolean.valueOf(s).booleanValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成Boolean类型，失败则返回默认值（可以是1、0、yes、no、true、false）
	 * @param s  可转换成boolean型的字符串
	 * @param defaultValue  默认值
	 * @return Boolean  转换后的值
	 */
    public static Boolean parseBoolean(String s, Boolean defaultValue) {
        if (isEmpty(s)) return defaultValue;
        if (s.equals("1")) return Boolean.valueOf(true); else if (s.equals("0")) return Boolean.valueOf(false); else if (s.equalsIgnoreCase("yes")) return Boolean.valueOf(true); else if (s.equalsIgnoreCase("no")) return Boolean.valueOf(false);
        try {
            return Boolean.valueOf(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成byte类型,默认值0
	 * @param s
	 * @return
	 */
    public static byte parseByte(String str) {
        try {
            return Byte.parseByte(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * 将字符串转换成byte类型,默认值defaultValue
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static byte parseByte(String s, byte defaultValue) {
        try {
            return Byte.parseByte(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成Byte类型,默认值defaultValue
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static Byte parseByte(String s, Byte defaultValue) {
        try {
            return Byte.valueOf(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成short类型,默认值0
	 * @param s
	 * @return
	 */
    public static short parseShort(String s) {
        try {
            return Short.parseShort(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * 将字符串转换成short类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static short parseShort(String s, short defaultValue) {
        try {
            return Short.parseShort(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成Short类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static Short parseShort(String s, Short defaultValue) {
        try {
            return Short.valueOf(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成int类型,默认值0
	 * @param s
	 * @return
	 */
    public static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * 将Integer型转换成int型，默认值0
	 * @param s
	 * @return
	 */
    public static int parseInt(Integer s) {
        try {
            return (s == null ? 0 : s.intValue());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * 将Integer型转换成int型，默认值defaultValue
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static int parseInt(Integer s, int defaultValue) {
        try {
            return (s == null ? 0 : defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成int类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成Integer类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static Integer parseInt(String s, Integer defaultValue) {
        try {
            return new Integer(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成long类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
	 * 将字符串转换成long类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static long parseLong(String s, long defaultValue) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成long类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static Long parseLong(String s, Long defaultValue) {
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成double类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0D;
        }
    }

    /**
	 * 将字符串转换成double类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static double parseDouble(String s, double defaultValue) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成double类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static Double parseDouble(String s, Double defaultValue) {
        try {
            return Double.valueOf(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成float类型
	 * @param s
	 * @return
	 */
    public static float parseFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * 将字符串转换成float类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static float parseFloat(String s, float defaultValue) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将字符串转换成BigDecimal类型
	 * @param s
	 * @param 0
	 * @return
	 */
    public static BigDecimal parseBigDecimal(String s) {
        try {
            return new java.math.BigDecimal(s);
        } catch (Exception e) {
            return new BigDecimal(0);
        }
    }

    /**
	 * 将字符串转换成BigDecimal类型
	 * @param s
	 * @param defaultValue
	 * @return
	 */
    public static BigDecimal parseBigDecimal(String s, BigDecimal defaultValue) {
        try {
            return new java.math.BigDecimal(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将系统标准格式日期转换成DATE型
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseDate(String datetime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(datetime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将整型日期转换成DATE型
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseDate(int datetime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            return dateFormat.parse(String.valueOf(datetime));
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将long型时间转换成DATE
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseDate(long datetime) {
        try {
            Date date = new Date(datetime);
            return date;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将系统标准格式日期转换成DATE型
	 * 转换失败则返回NULL.
	 * 
	 * @param defaultValue 当转换出错时的默认时间
	 * @param datetime 时间字符串
	 * 
	 * @return the date
	 */
    public static Date parseDate(String datetime, Date defaultValue) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(datetime);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将系统标准格式日期转换成DATE型
	 * 转换失败则返回NULL.
	 * 
	 * @param datetime 时间字符串
	 * @param format 字符串格式
	 * 
	 * @return the date
	 */
    public static Date parseDate(String datetime, String format) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(datetime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将系统标准格式时间转换成DATE型
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseDateTime(String datetime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(datetime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将毫秒级时间转换成DATE型
	 * 转换失败则返回NULL
	 * @param datetime 毫秒级时间
	 */
    public static Date parseDateTime(long datetime) {
        try {
            return new Date(datetime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将系统标准格式时间转换成DATE型
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseDateTime(String datetime, Date defaultValue) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(datetime);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
	 * 将系统标准格式时间转换成DATE型
	 * 转换失败则返回NULL.
	 * 
	 * @param datetime 时间字符串
	 * @param format 字符串格式
	 * 
	 * @return the date
	 */
    public static Date parseDateTime(String datetime, String format) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(datetime);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将Date转换成Timestamp类型
	 * 转换失败则返回NULL
	 */
    public static Timestamp parseDate2Timestamp(Date date) {
        try {
            return new Timestamp(date.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将Date转换成Timestamp类型
	 * 转换失败则返回defaultValue转换的时间
	 */
    public static Timestamp parseDate2Timestamp(Date date, Date defaultValue) {
        try {
            return new Timestamp(date.getTime());
        } catch (Exception e) {
            return new Timestamp(defaultValue.getTime());
        }
    }

    /**
	 * 将java.sql.Date型时间转换成DATE
	 * java.sql.Date(年月日)
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseFromSqlDate(java.sql.Date datetime) {
        try {
            Date date = new Date(datetime.getTime());
            return date;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将java.sql.Time型时间转换成DATE
	 * java.sql.Time(时分秒)
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseFromSqlTime(java.sql.Time datetime) {
        try {
            Date date = new Date(datetime.getTime());
            return date;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 将java.sql.Timestamp型时间转换成DATE
	 * 转换失败则返回NULL
	 * @param datetime 时间字符串
	 */
    public static Date parseFromSqlTimestamp(java.sql.Timestamp datetime) {
        try {
            Date date = new Date(datetime.getTime());
            return date;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 得到今天的整型表示，例如：返回'20060830'.
     * 
     * @return int 当前日期的整型表示
     */
    public static int getCurrentDateInt() {
        return FormatDateTime.formatDateInt(new Date());
    }

    /**
     * 得到当前的年月，以整型输出，格式:YYYYMM
     * @return
     */
    public static int getCurrentYeatMonthInt() {
        Calendar dStart = Calendar.getInstance();
        dStart.setTime(new Date());
        return dStart.get(Calendar.YEAR) * 100 + (dStart.get(Calendar.MONTH) + 1);
    }

    /**
     * 得到当前的年份，以整型输出，格式:YYYY
     * @return
     */
    public static int getCurrentYearInt() {
        Calendar dStart = Calendar.getInstance();
        dStart.setTime(new Date());
        return dStart.get(Calendar.YEAR);
    }

    /**
     * 得到当前的月份，以整型输出，格式:MM
     * @return
     */
    public static int getCurrentMonthInt() {
        Calendar dStart = Calendar.getInstance();
        dStart.setTime(new Date());
        return dStart.get(Calendar.MONTH);
    }

    /**
     * 得到当前时刻的时间字符串,例如：返回'2006-08-30 16:00:00'.
     * 
     * @return String 当前时间的标准格式表示
     */
    public static String getCurrentDateTime() {
        return FormatDateTime.formatDateTime(new Date());
    }

    /**
     * 得到当前时刻的时间字符串,例如：返回'2006-08-30 16:00:00.001'.
     * 
     * @return Timestamp 当前系统时间的Timestamp表示
     */
    public static Timestamp getCurrentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * 得到当前时刻的指定格式字符串 例如：样式'yyyy-MM-dd HH:mm',返回'2006-08-30 16:00'.
     * 
     * @param style 显示格式
     * 
     * @return String 当前时间指定样式的表示
     */
    public static String getCurrentFormatDateTime(String style) {
        return FormatDateTime.formatDateTime(new Date(), style);
    }

    /**
     * 得到当前时间的中文字符串，例如：返回'2006年08月30日 16时00分00秒'.
     * 
     * @return String 当前时间的中文表示
     */
    public static String getCurrentDateTimeCn() {
        return FormatDateTime.formatDateTimeCn(new Date());
    }

    /**
     * 得到当前日期的中文格式化输出，格式:YYYY年MM月DD日.
     * 
     * @param date 日期
     * 
     * @return 
     */
    public static String getCurrentDateCn() {
        return FormatDateTime.formatDateCn(new Date());
    }

    /**
     * 获得日期的整数型表示， 例如："20060831"
     * @param datetime 指定日期
     * @return int 整型日期
     */
    public static int getDateInt(Date datetime) {
        return FormatDateTime.formatDateInt(datetime);
    }

    /**
     * 返回当前日期时间字符串+3位随机数，格式'20030809161245001'作为文件名的唯一标识(用途：文件唯一标识).
     * 
     * @return String 日期时间+3位随机数字符串
     */
    public static String currentToFileName() {
        String fileName = FormatDateTime.formatDateTime(new Date(), "yyyyMMddHHmmss");
        String choose = "0123456789";
        Random random = new Random();
        char[] sRand = { '0', '0', '0' };
        char temp;
        for (int i = 0; i < 3; i++) {
            temp = choose.charAt(random.nextInt(10));
            sRand[i] = temp;
        }
        fileName += sRand.toString();
        return fileName;
    }

    /**
	   * 判断指定日期所在的年份是否为闰年
	   * @param date 指定日期
	   * @return boolean 是闰年返回true，不是返回false
	   */
    public static boolean isLeapYear(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar.isLeapYear(calendar.get(GregorianCalendar.YEAR));
    }

    /**
	   * 得到当前时间的前或后N天
	   * @param n 加减天数，正数加，负数减
	   * @return Date
	   */
    public static Date getBeAfDay(int n) {
        return getBeAfDay(new Date(), n);
    }

    /**
	   * 得到指定时间的前或后N天
	   * @param n 加减天数，正数加，负数减
	   * @return Date
	   */
    public static Date getBeAfDay(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, n);
        return calendar.getTime();
    }

    /**
	   * 得到当前时间的前或后N小时
	   * @param n 加减小时，正数加，负数减
	   * @return Date
	   */
    public static Date getBeAfHour(int n) {
        return getBeAfHour(new Date(), n);
    }

    /**
	   * 得到指定时间的前或后N小时
	   * @param date 指定时间
	   * @param n 加减小时，正数加，负数减
	   * @return Date
	   */
    public static Date getBeAfHour(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, n);
        return calendar.getTime();
    }

    /**
	   * 得到当前时间的前或后N分钟
	   * @param n 加减分钟，正数加，负数减
	   * @return Date
	   */
    public static Date getBeAfMinute(int n) {
        return getBeAfMinute(new Date(), n);
    }

    /**
	   * 得到指定时间的前或后N分钟
	   * @param date 指定时间
	   * @param n 加减分钟，正数加，负数减
	   * @return Date
	   */
    public static Date getBeAfMinute(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, n);
        return calendar.getTime();
    }

    /**
	   * 得到当前时间的前或后N秒
	   * @param n 加减秒，正数加，负数减
	   * @return Date 
	   */
    public static Date getBeAfSecond(int n) {
        return getBeAfSecond(new Date(), n);
    }

    /**
	   * 得到指定时间的前或后N秒
	   * @param date 指定时间
	   * @param n 加减秒，正数加，负数减
	   * @return Date 返回值
	   */
    public static Date getBeAfSecond(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, n);
        return calendar.getTime();
    }

    /**
     * 比较当前时间是否在两个时间当中.
     * 
     * @param endDateTime
     * @param beginDateTime 
     * @return boolean 在返回true，不在返回false
     */
    public static boolean isBetween(Date beginDateTime, Date endDateTime) {
        java.util.Date currentDate = new java.util.Date();
        if (currentDate.after(beginDateTime) && currentDate.before(endDateTime)) return true; else return false;
    }

    /**
     * 比较日期1和日期2大小
     * @param date1 日期1
     * @param date2 日期2
     * @return int 正数则date1>date2则date1=date2,负数则date1<date2
     */
    public static int compareDateInt(int date1, int date2) {
        if (date1 > date2) return 1; else if (date1 == date2) return 0; else return -1;
    }

    /**
     * 比较日期1和日期2大小
     * @param date1 日期1
     * @param date2 日期2
     * @return int 正数则date1>date2则date1=date2,负数则date1<date2
     */
    public static int compareDateInt(String date1, String date2) {
        int date11 = Utility.getDateInt(Utility.parseDate(date1));
        int date22 = Utility.getDateInt(Utility.parseDate(date2));
        if (date11 > date22) return 1; else if (date11 == date22) return 0; else return -1;
    }

    /**
     * 比较指定日期和当前日期大小
     * @param date 指定日期
     * @return int 正数则date>当前日期 ,0则date=当前日期 ,负数则date<当前日期
     */
    public static int compareDateInt(int date) {
        int currentDate = Utility.getCurrentDateInt();
        if (date > currentDate) return 1; else if (date == currentDate) return 0; else return -1;
    }

    /**
     * 比较指定日期和当前日期大小
     * @param datetime指定日期
     * @return int 正数则day1>当前日期 ,0则day1=当前日期 ,负数则day1<当前日期
     */
    public static int compareDateInt(String datetime) {
        int day1 = Utility.getDateInt(Utility.parseDate(datetime));
        int day2 = Utility.getCurrentDateInt();
        if (day1 > day2) return 1; else if (day1 == day2) return 0; else return -1;
    }

    /**
     * 比较日期datetime和当前日期
     * @param datetime 指定日期
     * @return int 正数则datetime>当前日期 ,0则datetime=当前日期 ,负数则datetime<当前日期
     */
    public static int compareDateInt(Date datetime) {
        int day1 = Utility.getDateInt(datetime);
        int day2 = Utility.getCurrentDateInt();
        if (day1 > day2) return 1; else if (day1 == day2) return 0; else return -1;
    }

    /**
		 * 比较beginTime和endTime 
		 * @param beginTime
		 * @param endTime
		 * @return int 正数则beginTime>endTime,0则beginTime=endTime,负数则beginTime<endTime	
		 */
    public static int compareDateTime(Date beginTime, Date endTime) {
        if (beginTime.after(endTime)) return 1; else if (beginTime.before(endTime)) return -1; else return 0;
    }

    /**
     * 比较beginTime和endTime的大小
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return int 正数则beginTime>endTime,0则beginTime=endTime,负数则beginTime<endTime
     */
    public static int compareDateTime(String beginTime, String endTime) {
        java.util.Date date1 = Utility.parseDate(beginTime);
        java.util.Date date2 = Utility.parseDate(endTime);
        if (date1.after(date2)) return 1; else if (date1.before(date2)) return -1; else return 0;
    }

    /**
     * 比较指定日期时间和当前日期时间大小
     * @param datetime 需要比较的日期时间
     * @return int 正数则datetime>当前日期,0则datetime=当前日期,负数则datetime<当前日期
     */
    public static int compareDateTime(String datetime) {
        java.util.Date currentDate = new java.util.Date();
        java.util.Date date1 = Utility.parseDate(datetime);
        if (date1.after(currentDate)) return 1; else if (date1.before(currentDate)) return -1; else return 0;
    }

    /**
     * 比较datetime和当前日期
     * @param datetime
     * @param 当前日期
     * @return int 正数则datetime>当前日期,0则datetime=当前日期,负数则datetime<当前日期
     */
    public static int compareDateTime(Date datetime) {
        java.util.Date currentDate = new java.util.Date();
        if (datetime.after(currentDate)) return 1; else if (datetime.before(currentDate)) return -1; else return 0;
    }

    /**
     * 得到两个日期间相差的天数,包括了小时、分秒，如果两个时间相差不超过一天按0天算
     * @param date1 开始日期
     * @param date2 结束日期
     * @return int 天数
     */
    public static int getBetweenDaysFull(Date date1, Date date2) {
        long diff = date2.getTime() - date1.getTime();
        long days = diff / 24 / 3600 / 1000;
        return new Long(days).intValue();
    }

    /**
     * 得到两个日期间相差的天数,只按天算，即使实际时间间隔小于1天
     * @param date1 开始日期
     * @param date2 结束日期
     * @return int 天数
     */
    public static int getBetweenDays(Date date1, Date date2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date1 = dateFormat.parse(dateFormat.format(date1));
            date2 = dateFormat.parse(dateFormat.format(date2));
        } catch (Exception e) {
            logger.error("类型转换失败", e);
        }
        long diff = date2.getTime() - date1.getTime();
        long days = diff / 24 / 3600 / 1000;
        return new Long(days).intValue();
    }

    /**
     * 得到两个日期间相差的小时数
     * @param date1 开始日期
     * @param date2 结束日期
     * @return int 小时数
     */
    public static int getBetweenHours(Date date1, Date date2) {
        long diff = date2.getTime() - date1.getTime();
        long days = diff / 3600 / 1000;
        return new Long(days).intValue();
    }

    /**
     * 得到两个日期间相差的分钟数
     * @param date1 开始日期
     * @param date2 结束日期
     * @return int 分钟数
     */
    public static int getBetweenMinutes(Date date1, Date date2) {
        if (date1 == null || date2 == null) return 0;
        long diff = date2.getTime() - date1.getTime();
        long days = diff / 60 / 1000;
        return new Long(days).intValue();
    }

    /**
     * 得到两个日期间相差的秒数
     * @param date1 开始日期
     * @param date2 结束日期
     * @return int 秒数
     */
    public static int getBetweenSeconds(Date date1, Date date2) {
        if (date1 == null || date2 == null) return 0;
        long diff = date2.getTime() - date1.getTime();
        long days = diff / 1000;
        return new Long(days).intValue();
    }

    /**
    * 获得URL中的文件名
    * @param url URL
    * @return String 文件名
    */
    public static String getUrlFileName(String url) {
        try {
            int point = url.lastIndexOf("/") + 1;
            return url.substring(point);
        } catch (Exception e) {
            logger.error("获取文件名失败", e);
            return "";
        }
    }

    /**
	 * 获得本地文件全路径名中的文件名
	 * @param filePath 文件全路径名
	 * @return String 文件名
	 */
    public static String getLocalFileName(String filePath) {
        try {
            int point = filePath.lastIndexOf(File.separator) + 1;
            return filePath.substring(point);
        } catch (Exception e) {
            logger.error("获取文件名失败", e);
            return "";
        }
    }

    /**
     * 获取文件名中的前缀，失败返回空
     * @param filePath 文件名
     * @return String 前缀名
     */
    public static String getFilePrefix(String filePath) {
        try {
            int point = filePath.lastIndexOf(".");
            int index = filePath.lastIndexOf(File.separator) + 1;
            return filePath.substring(index, point);
        } catch (Exception e) {
            logger.error("获取文件后缀名失败", e);
            return "";
        }
    }

    /**
	 * 获取文件后缀名，失败返回空
	 * @param filePath 文件名
	 * @return String 后缀名
	 */
    public static String getFileExt(String filePath) {
        try {
            int point = filePath.lastIndexOf(".");
            return filePath.substring(point);
        } catch (Exception e) {
            logger.error("获取文件后缀名失败", e);
            return "";
        }
    }

    /**
	 * 基本数据类型转换：double转换为long型
	 * @param value
	 * @return long
	 */
    public static long doubleToLong(double value) {
        Double d = new Double(value);
        return d.longValue();
    }

    /**
	 * 基本数据类型转换：long转换为double型.
	 * @param vlaue
	 * @return double
	 */
    public static double longToDouble(long value) {
        return Double.parseDouble(String.valueOf(value));
    }

    /**
     * 基本数据类型转换：double转Int.
     * @param value
     * @return int
     */
    public static int doubleToInt(double value) {
        return new Double(value).intValue();
    }

    /**
     * 记录登陆操作的错误日志并重定向到登陆页面
     */
    public static void NotLoginException(HttpServletRequest request, HttpServletResponse response, String errMsg, String url) throws Exception {
        RequestDispatcher rd = request.getRequestDispatcher(url);
        request.setAttribute("ERROR_MSG", errMsg);
        rd.forward(request, response);
    }

    /**
     * 记录登陆操作的错误日志并重定向到登陆页面
     */
    public static void NotLoginException(HttpServletRequest request, HttpServletResponse response, Exception e, String url) throws Exception {
        RequestDispatcher rd = request.getRequestDispatcher(url);
        request.setAttribute("ERROR_MSG", e.getMessage());
        rd.forward(request, response);
    }

    /**
     * 记录登陆操作的错误日志并重定向到当前页面
     */
    public static void NotLoginException(HttpServletRequest request, HttpServletResponse response, String errMsg) throws Exception {
        request.setAttribute("ERROR_MSG", errMsg);
    }

    /**
     * 记录登陆操作的错误日志并重定向到当前页面
     */
    public static void NotLoginException(HttpServletRequest request, HttpServletResponse response, Exception e) throws Exception {
        request.setAttribute("ERROR_MSG", e.getMessage());
    }

    /**
     * MD5加密字符串
     * @param str 需要加密的字符串
     * @return String 加密后的字符串
     */
    public static String MD5EncodeString(String str) {
        MD5 md5 = new MD5();
        String md5Pass = md5.getMD5ofStr(str);
        return md5Pass;
    }

    /**
	 * MD5加密字符串
	 * @param encodeType 加密方式:md5of16,md5of32,BASE64
	 * @param str 需要加密的字符串
	 * @return String 加密后的字符串
	 */
    public static String encodeString(String encodeType, String str) {
        if (encodeType.equals("md5of16")) {
            MD5 m = new MD5();
            return m.getMD5ofStr16(str);
        } else if (encodeType.equals("md5of32")) {
            MD5 m = new MD5();
            return m.getMD5ofStr(str);
        } else {
            try {
                MessageDigest gv = MessageDigest.getInstance(encodeType);
                gv.update(str.getBytes());
                return new BASE64Encoder().encode(gv.digest());
            } catch (java.security.NoSuchAlgorithmException e) {
                logger.error("BASE64加密失败", e);
                return null;
            }
        }
    }

    /**
     * 
     * 字符串替换(jdk1.3中使用)
     * 
     * @param source 原字符串
     * @param toReplace 需要替换的内容
     * @param replacement 替换后的内容
     * @return
     * 
     * @author caiyuan
     */
    public static String replaceAll(String source, String toReplace, String replacement) {
        int idx = source.lastIndexOf(toReplace);
        if (idx != -1) {
            StringBuffer ret = new StringBuffer(source);
            ret.replace(idx, idx + toReplace.length(), replacement);
            while ((idx = source.lastIndexOf(toReplace, idx - 1)) != -1) {
                ret.replace(idx, idx + toReplace.length(), replacement);
            }
            source = ret.toString();
        }
        return source;
    }

    public static void main(String[] args) throws Exception {
        String a = new String("水电费".getBytes("UTF-8"), "GBK");
        String b = new String(a.getBytes("GBK"), "UTF-8");
        System.out.println(a);
        System.out.println(b);
    }
}
