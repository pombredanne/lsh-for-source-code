package org.com.cnc.common.android;

import java.math.BigInteger;
import java.security.MessageDigest;
import android.text.format.Time;

public class Common {

    public static final int SIZE_1 = 1;

    public static final int SIZE_2 = 2;

    public static final int SIZE_3 = 3;

    public static final int SIZE_4 = 4;

    public static final int SIZE_5 = 5;

    public static final int SIZE_6 = 6;

    public static final int SIZE_7 = 7;

    public static final int SIZE_8 = 8;

    public static final int SIZE_9 = 9;

    public static final int SIZE_10 = 10;

    public static final int SIZE_11 = 11;

    public static final int TIME_OUT = 15000;

    public static final String KEY01 = "key01";

    public static final String KEY02 = "key02";

    public static final String KEY03 = "key03";

    public static final String KEY04 = "key04";

    public static final String KEY05 = "key05";

    public static final String KEY06 = "key06";

    public static final String KEY07 = "key07";

    public static final String KEY08 = "key08";

    public static final String KEY09 = "key09";

    public static final String KEY10 = "key10";

    public static final int REQUESTCODE_01 = 1;

    public static final int REQUESTCODE_02 = 2;

    public static final int REQUESTCODE_03 = 3;

    public static final int REQUESTCODE_04 = 4;

    public static final int REQUESTCODE_05 = 5;

    public static final int REQUESTCODE_06 = 6;

    public static final int REQUESTCODE_07 = 7;

    public static final int REQUESTCODE_08 = 8;

    public static final int REQUESTCODE_09 = 9;

    public static final int REQUESTCODE_10 = 10;

    public static final int RESULTCODE_01 = 1;

    public static final int RESULTCODE_02 = 2;

    public static final int RESULTCODE_03 = 3;

    public static final int RESULTCODE_04 = 4;

    public static final int RESULTCODE_05 = 5;

    public static final int RESULTCODE_06 = 6;

    public static final int RESULTCODE_07 = 7;

    public static final int RESULTCODE_08 = 8;

    public static final int RESULTCODE_09 = 9;

    public static final int RESULTCODE_10 = 10;

    public static String convertStringToMD5(String toEnc) {
        try {
            MessageDigest mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(toEnc.getBytes(), 0, toEnc.length());
            return new BigInteger(1, mdEnc.digest()).toString(16);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean compare(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 != null || s2 != null) {
            return s1.equalsIgnoreCase(s2);
        }
        return false;
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    public static boolean isBeginFromAToZ(String text) {
        String compare = "zxcvbnmlkjhgfdsaqwertyuiop";
        if (text == null || "".endsWith(text)) {
            return false;
        }
        String first = text.toUpperCase().substring(0, 1);
        return compare.toUpperCase().contains(first);
    }

    public static void main(String[] args) {
        System.out.println(isBeginFromAToZ("aA"));
    }

    public static String getTimeMMDDYYY() {
        String time = "";
        Time currentTime = new Time();
        currentTime.setToNow();
        time += (currentTime.month < 10 ? "0" : "") + currentTime.month;
        time = "/" + (currentTime.monthDay < 10 ? "0" : "") + currentTime.monthDay;
        time += "/" + (currentTime.year);
        return time;
    }

    public static boolean isNullOrBlank(String txt) {
        return txt == null || txt != null && txt.trim().equals("");
    }

    public static boolean contains(String str1, String str2) {
        return str1 != null && str2 != null && str2.toUpperCase().contains(str1.toUpperCase());
    }

    public static String getTimeDDMMYYY() {
        String time = "";
        Time currentTime = new Time();
        currentTime.setToNow();
        time += (currentTime.monthDay < 10 ? "0" : "") + currentTime.monthDay;
        time += "/" + (currentTime.month < 10 ? "0" : "") + currentTime.month;
        time += "/" + (currentTime.year);
        return time;
    }
}
