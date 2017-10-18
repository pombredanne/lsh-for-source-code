package org.dy.servlet.mvc.support;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import sun.misc.BASE64Encoder;

public class ActionUtils {

    public static ArrayList stringToArrayList(String str) {
        String[] strArr = str.split("\\|");
        ArrayList<String> tmp = new ArrayList<String>();
        if (str.length() > 0) {
            for (int i = 0; i < strArr.length; ++i) {
                tmp.add(strArr[i].toLowerCase());
            }
        }
        return tmp;
    }

    public static Date toDate(String str, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format == null ? "yyyy-MM-dd" : format);
        try {
            return sdf.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format == null ? "yyyy-MM-dd" : format);
        return sdf.format(date);
    }

    public static String encoderByMD5(String str) {
        String result = "";
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            BASE64Encoder base64en = new BASE64Encoder();
            result = base64en.encode(mDigest.digest(str.getBytes("utf-8")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String dirDir(String dir1, String dir2) {
        String split = getSplit();
        if (dir1.endsWith(split)) return dir1 + dir2; else return dir1 + split + dir2;
    }

    public static String getSplit() {
        String dirChar = "/";
        if (isChildIgnoreCase("windows", System.getProperty("os.name"))) {
            dirChar = "\\";
        }
        return dirChar;
    }

    private static boolean isChildIgnoreCase(String subString, String supString) {
        if ((supString.toLowerCase()).indexOf(subString.toLowerCase()) == -1) {
            return false;
        } else {
            return true;
        }
    }
}
