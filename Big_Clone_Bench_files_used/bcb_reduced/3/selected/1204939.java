package com.sporthenon.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class StringUtils {

    public static final String EMPTY = "N/A";

    public static boolean notEmpty(Object obj) throws ClassCastException {
        return (obj != null && ((String) obj).length() > 0);
    }

    public static String implode(Iterable<String> tValues, String sSeparator) {
        StringBuffer sb = new StringBuffer();
        for (String s : tValues) sb.append((sb.toString().length() > 0 ? sSeparator : "") + s);
        return sb.toString();
    }

    public static int countIn(String s, String pattern) {
        return (s != null ? s.split(pattern).length - 1 : 0);
    }

    public static Integer toInt(Object o) {
        return (notEmpty(o) ? new Integer(String.valueOf(o)) : 0);
    }

    public static String toMD5(String s) throws Exception {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(s.getBytes(), 0, s.length());
        return new BigInteger(1, m.digest()).toString(16);
    }

    public static ArrayList<Integer> eqList(String s) {
        ArrayList<Integer> lst = new ArrayList<Integer>();
        if (s != null) for (String s_ : s.split(";")) if (s_.matches("EX.*")) {
            s_ = s_.replaceAll("\\D", "");
            int n1 = Integer.parseInt(String.valueOf(s_.charAt(0)));
            int n2 = (s_.length() > 1 ? Integer.parseInt(String.valueOf(s_.charAt(1))) : n1) + 1;
            if (lst.size() > 0) lst.add(-1);
            for (int i = n1; i <= n2; i++) lst.add(i);
        }
        return lst;
    }

    public static String toTextDate(String dt, Locale l) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat dftxt = new SimpleDateFormat("MMMM d, yyyy", l);
        return dftxt.format(df.parse(dt));
    }

    public static String formatNumber(Integer n) {
        DecimalFormat df = new DecimalFormat("###,###.##");
        return df.format(n);
    }
}
