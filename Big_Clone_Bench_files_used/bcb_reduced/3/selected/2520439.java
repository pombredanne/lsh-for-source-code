package com.study.app.util;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class FunctionUtil {

    public static Date getLastDayofMonth(int year, int month) {
        int nextMonth = (month == 12) ? 1 : month + 1;
        int nextYear = (month == 12) ? year + 1 : year;
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.YEAR, nextYear);
        ca.set(Calendar.MONTH, nextMonth - 1);
        ca.set(Calendar.DAY_OF_MONTH, 1);
        ca.add(Calendar.DAY_OF_MONTH, -1);
        return ca.getTime();
    }

    public static Date getLastDayofCurrentMonth() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH) + 1;
        return getLastDayofMonth(year, month);
    }

    public static Date getFirstDayOfMonth(int year, int month) {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.YEAR, year);
        ca.set(Calendar.MONTH, month - 1);
        ca.set(Calendar.DAY_OF_MONTH, 1);
        return ca.getTime();
    }

    public static Date getFirstDayofCurrentMonth() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, 1);
        return ca.getTime();
    }

    public static String getTimeAsString(int seconds) {
        return FunctionUtil.getMonthStr(seconds / 3600) + ":" + FunctionUtil.getMonthStr((seconds % 3600) / 60) + ":" + FunctionUtil.getMonthStr(seconds % 60);
    }

    public static String formatWeekPeriod(int year, int month, int day) {
        Date startDate = null;
        Date endDate = null;
        Calendar ca = Calendar.getInstance(Locale.CHINA);
        ca.set(Calendar.YEAR, year);
        ca.set(Calendar.MONTH, month - 1);
        ca.set(Calendar.DAY_OF_MONTH, day);
        startDate = ca.getTime();
        ca.add(Calendar.DAY_OF_MONTH, 4);
        endDate = ca.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd");
        return formatter.format(startDate) + " - " + formatter.format(endDate);
    }

    public static String getYearMonthStr(int year, int month, String dateFormat) {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.YEAR, year);
        ca.set(Calendar.MONTH, month - 1);
        return new SimpleDateFormat(dateFormat).format(ca.getTime());
    }

    public static Map getRecentYearMap(boolean addFirstItem) {
        Map yearMap = new TreeMap();
        if (addFirstItem) {
            yearMap.put("<Select>", 0);
        }
        Calendar ca = Calendar.getInstance();
        int curYear = ca.get(Calendar.YEAR);
        for (int i = curYear - 4; i < curYear + 4; i++) {
            yearMap.put(Integer.toString(i), i);
        }
        return yearMap;
    }

    public static Map getMonthMap(boolean addFirstItem) {
        Map monthMap = new TreeMap();
        if (addFirstItem) {
            monthMap.put("<Select>", 0);
        }
        for (int i = 0; i < 12; i++) {
            monthMap.put(getMonthStr(i + 1), i + 1);
        }
        return monthMap;
    }

    public static String getMonthStr(int month) {
        String prefix = "";
        if (month < 10) {
            prefix = "0";
        }
        return prefix + Integer.toString(month);
    }

    public static String encodeByMd5(String str) {
        try {
            if (str == null) {
                str = "";
            }
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes("utf-8"));
            byte[] b = md5.digest();
            int i;
            StringBuffer buff = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buff.append("0");
                }
                buff.append(Integer.toHexString(i));
            }
            return buff.toString();
        } catch (Exception e) {
            return str;
        }
    }

    public static int getWorkDaysCount(Date startDate, Date endDate) {
        if (startDate.after(endDate)) {
            return 0;
        }
        Calendar startCA = Calendar.getInstance(Locale.CHINA);
        startCA.setTime(startDate);
        int firstWeekDay = startCA.get(Calendar.DAY_OF_WEEK);
        int retCount = (firstWeekDay == 1) ? 5 : 7 - firstWeekDay;
        startCA.add(Calendar.DAY_OF_MONTH, 1 - firstWeekDay);
        Calendar endCA = Calendar.getInstance(Locale.CHINA);
        endCA.setTime(endDate);
        int lastWeekDay = endCA.get(Calendar.DAY_OF_WEEK);
        retCount += (lastWeekDay == 7) ? 5 : lastWeekDay - 1;
        endCA.add(Calendar.DAY_OF_MONTH, 1 - lastWeekDay);
        if (startCA.equals(endCA)) {
            retCount -= 5;
        } else {
            startCA.add(Calendar.DAY_OF_MONTH, 7);
            while (startCA.before(endCA)) {
                startCA.add(Calendar.DAY_OF_MONTH, 7);
                retCount += 5;
            }
        }
        return retCount;
    }

    public static String translateSecondsToDateStr(int seconds, boolean dayIncluded, boolean signEnabled) {
        int timeValue = Math.abs(seconds);
        int d = 0;
        if (dayIncluded) {
            int fixDayHours = 3600 * ConstantUtil.WORK_HOURS_PER_DAY;
            d = timeValue / fixDayHours;
            timeValue = timeValue % fixDayHours;
        }
        int h = timeValue / 3600;
        timeValue = timeValue % 3600;
        int m = timeValue / 60;
        int s = timeValue % 60;
        StringBuffer tempStr = new StringBuffer();
        if (signEnabled) {
            tempStr = tempStr.append(seconds < 0 ? "-" : "+");
        }
        tempStr.append(d > 0 ? String.valueOf(d) + "d" : "");
        tempStr.append(h > 0 ? String.valueOf(h) + "h" : "");
        tempStr.append(m > 0 ? String.valueOf(m) + "m" : "");
        tempStr.append(s > 0 ? String.valueOf(s) + "s" : "");
        return tempStr.toString();
    }

    public static int translateDateStrToSeconds(String dateStr) {
        if (dateStr == null) return 0;
        if (dateStr.length() == 0) return 0;
        String dd = "", hh = "", mm = "", ss = "";
        char type = '0';
        char[] chr = dateStr.toCharArray();
        for (int i = chr.length - 1; i >= 0; i--) {
            if (chr[i] < '0' || chr[i] > '9') {
                type = chr[i];
            } else {
                switch(type) {
                    case 's':
                        ss = chr[i] + ss;
                        break;
                    case 'm':
                        mm = chr[i] + mm;
                        break;
                    case 'h':
                        hh = chr[i] + hh;
                        break;
                    case 'd':
                        dd = chr[i] + dd;
                        break;
                }
            }
        }
        int fixDayHours = 3600 * ConstantUtil.WORK_HOURS_PER_DAY;
        int d = dd.length() > 0 ? Integer.parseInt(dd) : 0;
        int h = hh.length() > 0 ? Integer.parseInt(hh) : 0;
        int m = mm.length() > 0 ? Integer.parseInt(mm) : 0;
        int s = ss.length() > 0 ? Integer.parseInt(ss) : 0;
        return d * fixDayHours + h * 3600 + m * 60 + s;
    }
}
