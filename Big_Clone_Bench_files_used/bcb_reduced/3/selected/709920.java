package ru.point.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @author Mikhail Sedov [06.03.2009]
 */
public class Utils {

    private static final String ZEROS = "00000000000000000000000000000000";

    private static MessageDigest m;

    static {
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public static String md5(String source) {
        m.update(source.getBytes(), 0, source.length());
        String md5 = new BigInteger(1, m.digest()).toString(16);
        return ZEROS.substring(0, 32 - md5.length()) + md5;
    }

    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String formatCalendar(Calendar cal) {
        return dateFormat.format(cal.getTime());
    }

    public static Calendar getStartOfWeek(int year, int weekNo) {
        Calendar start = new GregorianCalendar(0, 0, 0, 0, 0, 0);
        start.setFirstDayOfWeek(Calendar.MONDAY);
        start.set(Calendar.YEAR, year);
        start.set(Calendar.WEEK_OF_YEAR, weekNo);
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return start;
    }

    public static Calendar getEndOfWeek(Calendar start) {
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.WEEK_OF_YEAR, 1);
        end.add(Calendar.SECOND, -1);
        return end;
    }
}
