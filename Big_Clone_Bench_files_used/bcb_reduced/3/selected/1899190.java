package org.tju.ebs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import sun.misc.BASE64Encoder;

public class StringUtil {

    public StringUtil() {
    }

    public static String toJson(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    public static Object json2Obj(String json, Class<?> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(json, clazz);
    }

    public Properties getProperties(String fileName) {
        System.out.println(fileName);
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Properties p = new Properties();
        try {
            p.load(inputStream);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return p;
    }

    public static String getMD5String(String str) {
        if (str == null || "".equals(str.trim())) {
            return "";
        }
        String result = "";
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BASE64Encoder base64en = new BASE64Encoder();
        try {
            result = base64en.encode(md5.digest(str.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Boolean isIntegerValue(String value) {
        Boolean result = false;
        try {
            Integer.parseInt(value);
            result = true;
        } catch (NumberFormatException e) {
            result = false;
        }
        return result;
    }

    public static Boolean isEmail(String value) {
        Boolean result = true;
        if (value.indexOf("@") < 1) {
            result = false;
        }
        if (value.indexOf(".") < 1) {
            result = false;
        } else {
            String temp = value.substring(value.indexOf("."), value.length());
            if (temp.length() <= 1) {
                result = false;
            }
        }
        return result;
    }

    public static Boolean checkStringMaxLen(String value, int minValue, int maxValue) {
        Boolean result = true;
        if (value.length() > maxValue) {
            result = false;
        }
        if (value.length() < minValue) {
            result = false;
        }
        return result;
    }

    public static Boolean checkDateRange(int value, int minValue, int maxValue) {
        Boolean result = true;
        if (value < minValue) result = false;
        if (value < maxValue) result = false;
        return result;
    }

    public static Boolean checkDateRange(double value, double minValue, double maxValue) {
        Boolean result = true;
        if (value < minValue) result = false;
        if (value < maxValue) result = false;
        return result;
    }

    public static Boolean isFloat(String value) {
        Boolean result = false;
        try {
            Double.parseDouble(value);
            result = true;
        } catch (NumberFormatException e) {
            result = false;
        }
        return result;
    }

    public static String getRandomString() {
        String result = "";
        Random r = new Random();
        int i = 0;
        int c;
        while (i < 10) {
            c = r.nextInt(122);
            if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122)) {
                result = result + (char) c;
                i++;
            }
        }
        return result;
    }

    public static boolean isNull(String str) {
        boolean isNull = false;
        if (null == str || str.equalsIgnoreCase("null") || str.trim().equalsIgnoreCase("") || str.trim().equalsIgnoreCase("undefined")) {
            isNull = true;
        }
        return isNull;
    }

    public static String getFirstAlphabetLowcase(String str) {
        String result = "";
        String firstAlphabet = str.substring(0, 1);
        String remainString = str.substring(1, str.length());
        firstAlphabet = firstAlphabet.toLowerCase();
        result = firstAlphabet + remainString;
        return result;
    }

    public static String getFirstAlphabetUpper(String str) {
        String result = "";
        String firstAlphabet = str.substring(0, 1);
        String remainString = str.substring(1, str.length());
        firstAlphabet = firstAlphabet.toUpperCase();
        result = firstAlphabet + remainString;
        return result;
    }

    public static String getUpdatedStr(String original, String code) {
        if (code.length() > original.length()) {
            return code;
        } else {
            return original.substring(0, original.length() - code.length()) + code;
        }
    }

    public static String formatString(String str, int length, String prefix) {
        int diff = length - str.length();
        String temp = "";
        for (int i = 0; i <= diff; i++) {
            temp = temp + prefix;
        }
        return temp + str;
    }

    @SuppressWarnings("unchecked")
    public static String[] getStrList(String str, String seperateLabel) {
        String result[] = null;
        Vector v = getStringList(str, seperateLabel);
        int i = v.size();
        if (i > 0) {
            result = new String[i];
        }
        int m = 0;
        Iterator it = v.iterator();
        while (it.hasNext()) {
            result[m] = (String) it.next();
            m++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Vector getStringList(String str, String seperateLabel) {
        Vector v = new Vector();
        int i = 0;
        setStringList(v, i, str, seperateLabel);
        return v;
    }

    @SuppressWarnings("unchecked")
    private static void setStringList(Vector stringList, int i, String str, String seperateLabel) {
        int index = str.indexOf(seperateLabel);
        if (index > 0) {
            String subString = str.substring(0, index);
            stringList.add(subString);
            str = str.substring(index + 1, str.length());
            i++;
            setStringList(stringList, i, str, seperateLabel);
        } else {
            stringList.add(str);
        }
    }

    public static String getStrByClazzName(String str) {
        String result = "";
        int length = str.length();
        boolean upperNext = false;
        String temp = "";
        for (int i = 0; i < length; i++) {
            temp = str.substring(i, i + 1);
            if (temp.equals("_")) {
                upperNext = true;
            } else {
                if (upperNext) {
                    temp = temp.toUpperCase();
                    upperNext = false;
                }
                result = result + temp;
            }
        }
        return result;
    }
}
