package cn.gov.ydstats.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * 公共的打杂类,用来处理大部分类需要用到的公共方法
 */
public class CommonUtil {

    /**
	 * 以String类型获取request里的parameter
	 * @param request
	 * @param parameterName
	 * @return
	 */
    public static String getParameter(HttpServletRequest request, String parameterName) {
        return getParameter(request, parameterName, null);
    }

    /**
	 * 以String类型获取request里的parameter
	 * @param request
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
    public static String getParameter(HttpServletRequest request, String parameterName, String defaultValue) {
        String value = request.getParameter(parameterName);
        if (value != null) {
            try {
                if (request.getCharacterEncoding() == null) {
                    return new String(value.getBytes("ISO-8859-1"), "UTF-8");
                } else {
                    return value;
                }
            } catch (UnsupportedEncodingException e) {
            }
        }
        return defaultValue;
    }

    /**
	 * 以int类型获取request里的parameter
	 * @param request
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
    public static int getParameter(HttpServletRequest request, String parameterName, int defaultValue) {
        String value = request.getParameter(parameterName);
        if (isInteger(value)) {
            value = value.trim();
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    /**
	 * 以long类型获取request里的parameter
	 * @param request
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
    public static long getParameter(HttpServletRequest request, String parameterName, long defaultValue) {
        String value = request.getParameter(parameterName);
        if (isInteger(value)) {
            value = value.trim();
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    /**
	 * 以double类型获取request里的parameter
	 * @param request
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
    public static double getParameter(HttpServletRequest request, String parameterName, double defaultValue) {
        String value = request.getParameter(parameterName);
        if (value != null) {
            value = value.trim();
            try {
                return Double.valueOf(value);
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    /**
	 * 判断字符串是不是整数
	 * @param str
	 * @return
	 */
    public static boolean isInteger(String str) {
        if (str == null) return false;
        return Pattern.matches("^-?[0-9]+", str);
    }

    /**
	 * 判断字符串是不是正整数
	 * @param str
	 * @return
	 */
    public static boolean isPositiveInteger(String str) {
        if (str == null) return false;
        return Pattern.matches("[1-9]|[0-9]+[1-9]+|[1-9]+[0-9]+", str);
    }

    /**
	 * 根据格式化字符串获取日期,字符串必须是yyyy-MM-dd格式
	 * @param date
	 * @return
	 */
    public static Date parseDates(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 格式化指定日期
	 * @param date
	 * @return String yyyy-MM-dd 格式的字符串
	 */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
	 * 转换日期,只保留年月日
	 * @param date
	 * @return
	 */
    public static Date conversionDate(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(sdf.format(date));
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 判断是不是非空字符串
	 * @param str
	 * @return 返回true表示该字符串是非空字符串（长度大于0），返回false表示该字符串是空字符串（null或者""）
	 */
    public static boolean isNotEmpty(String str) {
        if (str == null) return false;
        return str.length() > 0;
    }

    /**
	 * 获取html里的纯文本内容
	 * @param html
	 * @return
	 */
    public static String getPlainText(String html) {
        return html.replace("&lt;", "<").replace("&gt;", ">").replaceAll("<[.[^<]]*>", "");
    }

    /**
	 * 对输入的字符串进行MD5加密
	 * @param strSrc
	 * @return
	 */
    public static String md5(String strSrc) {
        byte[] digest = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            digest = md5.digest(strSrc.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(byteHEX(digest[i]));
        }
        return sb.toString();
    }

    private static String byteHEX(byte byte0) {
        char ac[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        char ac1[] = new char[2];
        ac1[0] = ac[byte0 >>> 4 & 0xf];
        ac1[1] = ac[byte0 & 0xf];
        String s = new String(ac1);
        return s;
    }
}
