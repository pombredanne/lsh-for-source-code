package net.dongliu.jalus.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import net.dongliu.jalus.pojo.Post;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.lang.StringUtils;

public class MyUtils {

    public static Date parseDateCCT(String dateStr, String format) {
        Date date = parseDateByTimeZone(dateStr, "GMT+8", new Date(), format);
        return date;
    }

    /**
	 * 根据时区和date format解析日期
	 * @param dateStr
	 * @param timeZone
	 * @param defaultDate
	 * @param format
	 * @return
	 */
    public static Date parseDateByTimeZone(String dateStr, String timeZone, Date defaultDate, String format) {
        Date date;
        try {
            if (StringUtils.isNotEmpty(dateStr)) {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
                date = sdf.parse(dateStr);
            } else {
                date = defaultDate;
            }
        } catch (ParseException e) {
            date = defaultDate;
        }
        return date;
    }

    /**
	 * 隐藏文章不显示.
	 * @param post
	 */
    public static void processPost(Post post) {
        if (post.isAuthorized()) {
            post.setTitle("【此文章已隐藏】");
            post.setContent(" ");
        }
    }

    /**
	 * 隐藏文章不显示.
	 * @param post
	 */
    public static void processPostList(List<Post> postList) {
        if (postList != null) {
            for (Post post : postList) {
                processPost(post);
            }
        }
    }

    /**
	 * date to string
	 * @return
	 */
    public static String formatDateCCT(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return sdf.format(date);
    }

    /**
	 * 获得过去一天的时间
	 * @param date
	 */
    public static Date decreaseDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    /**
	 * 获得一天的起始时间  Timezone CCT
	 * @param date
	 */
    public static Date getDayBeginDateCCT(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
	 * 获得一天的结束时间 Timezone CCT
	 * @param date
	 */
    public static Date getDayEndDateCCT(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
	 * 获得一月的起始时间  Timezone CCT
	 * @param date
	 */
    public static Date getMonthBeginDateCCT(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
	 * 获得一月的结束时间  Timezone CCT
	 * @param date
	 */
    public static Date getMonthEndDateCCT(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * bytes转换成十六进制字符串
     */
    public static String byte2HexStr(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    /**
	 * 32位md5摘要
	 * @param input
	 * @return
	 */
    public static String md5(String input) {
        byte[] temp;
        try {
            MessageDigest messageDigest;
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(input.getBytes());
            temp = messageDigest.digest();
        } catch (Exception e) {
            return null;
        }
        return MyUtils.byte2HexStr(temp);
    }

    /**
	 * 汉字转拼音.
	 * @param str
	 * @return
	 */
    public static String cn2Spell(String str) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray()) {
            if (ch > 0x4E00 && ch < 0x9FA5) {
                try {
                    sb.append(PinyinHelper.toHanyuPinyinStringArray(ch, format)[0]);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
	 * 根据emai地址获取gravatar头像图片地址.
	 * @param email
	 * @return
	 */
    public static String getGravatarUrl(String email) {
        if (email == null || email.isEmpty()) {
            email = "default@default.com";
        }
        return "http://www.gravatar.com/avatar/" + MyUtils.md5(email) + "?s=64";
    }

    /**
	 * 在url中使用tag是，需要特殊处理/ , + 等字符
	 * @param tag
	 * @return
	 */
    public static String encodeTag(String tag) {
        if (tag == null) {
            return tag;
        } else {
            try {
                return URLEncoder.encode(tag.replace("/", "%2F").replace("+", "%2B"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return tag;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(cn2Spell("侧23范sadfasd围似d--0909328490*^*%&^%*&乎"));
    }
}
