package com.ourlinc.helloworld.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 提供通用的web处理方法
 * 
 * @author lipeiying
 * 
 */
public class WebUtils {

    public static final String ONLY_DAY = "yyyy-MM-dd";

    public static final String HAVA_MINUTES = "yyyy-MM-dd HH:mm";

    private WebUtils() {
    }

    /**
	 * 返回一个不为空的字符串
	 * 
	 * @param str
	 * @return
	 */
    public static String toString(String str) {
        return null == str ? "" : str;
    }

    /**
	 * 当s的长度等0或为空时返回真
	 * 
	 * @param str
	 * @return
	 */
    public static boolean isEmptyString(String s) {
        return null == s || 0 == s.length();
    }

    /**
	 * 把字符转化为整数，转化失败返回1
	 * 
	 * @param s
	 * @return
	 */
    public static int toInt(String s) {
        return toInt(s, 1);
    }

    public static int toInt(String s, int defaultValue) {
        int i = defaultValue;
        if (null != s) {
            try {
                i = Integer.parseInt(s);
            } catch (NumberFormatException e) {
            }
        }
        return i;
    }

    /**
	 * 把一个String对象转换成yyyy-MM-dd格式的Date对象
	 * 
	 * @param strOneDate
	 * @return
	 */
    public static final Date strFormatDate(String strOneDate) {
        return strFormatDate(strOneDate, ONLY_DAY);
    }

    /**
	 * 把一个String对象转换成yyyy-MM-dd HH:mm格式的Date对象
	 * 
	 * @param strOneDate
	 * @return
	 */
    public static final Date strFormatDateAndTime(String strOneDate) {
        return strFormatDate(strOneDate, HAVA_MINUTES);
    }

    /**
	 * 将String转化为指定格式的date对象
	 * 
	 * @param strOneDate
	 * @param type
	 *            date的格式，有yyyy-MM-dd格式，有yyyy-MM-dd HH:mm格式
	 * @return
	 */
    public static final Date strFormatDate(String strOneDate, String type) {
        DateFormat format = new SimpleDateFormat(type);
        Date date = new Date();
        try {
            date = format.parse(strOneDate);
        } catch (ParseException e) {
        }
        return date;
    }

    /**
	 * 把一个Date对象转换成yyyy-MM-dd格式的字符串
	 * 
	 * @param date
	 * @return 转换后的Date对象
	 */
    public static final String dateFormatToStr(Date date) {
        DateFormat format = new SimpleDateFormat(ONLY_DAY);
        return format.format(date);
    }

    /**
	 * 把一个Date对象转换成yyyy-MM-dd HH:mm格式的字符串
	 * 
	 * @param date
	 * @return
	 */
    public static final String dateAndTimeFormatToStr(Date date) {
        DateFormat format = new SimpleDateFormat(HAVA_MINUTES);
        return format.format(date);
    }

    public static final Date getClearDate() {
        String str = dateAndTimeFormatToStr(new Date());
        return strFormatDateAndTime(str);
    }

    /**
	 * base64解密
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
    public static byte[] decryptBASE64(String key) throws IOException {
        return new BASE64Decoder().decodeBuffer(key);
    }

    /**
	 * base64加密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
    public static String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key);
    }

    /**
	 * MD5加密
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
    public static byte[] encryptMD5(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(data);
        return md5.digest();
    }

    /**
	 * 用于将一个字符串按照一定的编码格式进行编码
	 * 
	 * @param s
	 *            需要编码的字符串
	 * @param enc
	 *            需要转为什么字符集
	 * @return 返回转换后的字符串
	 * @throws UnsupportedEncodingException
	 */
    public static final String decode(String s, String enc) throws UnsupportedEncodingException {
        return isEmptyString(s) ? "" : URLDecoder.decode(s, enc);
    }
}
