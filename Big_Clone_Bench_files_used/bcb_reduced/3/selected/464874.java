package com.shared.beans;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import sun.misc.BASE64Encoder;
import com.shared.servlets.*;
import java.security.MessageDigest;
import java.sql.*;
import java.text.*;
import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.image.*;
import javax.imageio.ImageIO;

public class Util {

    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";

    public static final String SUCCESS_MESSAGE = "SUCCESS_MESSAGE";

    public static final String FORM_INPUT_PREFIX = "FORM_INPUT_PREFIX";

    public static final String PASSWORD_ALPHABET = "ABCDEFGHJKMNOPQRSTUVWX3456789";

    public static final String URL_REG_EXP = "https?://([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\.\\-,]*(\\?\\S+)?)?)?";

    public static String escapeForHTML(String str) {
        if (str == null) {
            return "";
        }
        str = str.replaceAll("&", "&#38;");
        str = str.replaceAll("\"", "&#34;");
        str = str.replaceAll("'", "&#39;");
        str = str.replaceAll("<", "&#60;");
        str = str.replaceAll(">", "&#62;");
        str = str.replaceAll(System.getProperty("line.separator"), "<br>");
        return str;
    }

    public static String escapeFromHTML(String str) {
        if (str == null) {
            return "";
        }
        str = str.replaceAll("&#38;", "&");
        str = str.replaceAll("&#34;", "\"");
        str = str.replaceAll("&#39;", "'");
        str = str.replaceAll("&#60;", "<");
        str = str.replaceAll("&#62;", ">");
        str = str.replaceAll("<br>", System.getProperty("line.separator"));
        return str;
    }

    public static String removeNewLines(String str) {
        if (str == null) {
            return "";
        }
        str = str.replaceAll("\n", "");
        str = str.replaceAll("\r", "");
        return str;
    }

    public static String insertUrlLinks(String str) {
        return str.replaceAll(URL_REG_EXP, "<a href='$0' target='_blank'>$0</a>");
    }

    public static String formatTimeStamp(Timestamp time) {
        SimpleDateFormat formattedTime = new SimpleDateFormat("M/d/yy h:mm:ss aaa");
        return formattedTime.format(time);
    }

    public static void setError(HttpServletRequest request, String error) {
        HttpSession session = request.getSession();
        session.setAttribute(ERROR_MESSAGE, error);
    }

    public static String displayError(HttpServletRequest request) {
        String error = "";
        HttpSession session = request.getSession();
        if (null != session.getAttribute(ERROR_MESSAGE)) {
            error = "<span style=\"color:#FF0000;\">" + (String) session.getAttribute(ERROR_MESSAGE) + "</span><p>";
        }
        session.removeAttribute(ERROR_MESSAGE);
        return error;
    }

    public static void setMessage(HttpServletRequest request, String message) {
        HttpSession session = request.getSession();
        session.setAttribute(SUCCESS_MESSAGE, message);
    }

    public static String displayMessage(HttpServletRequest request) {
        String message = "";
        HttpSession session = request.getSession();
        if (null != session.getAttribute(SUCCESS_MESSAGE)) {
            message = (String) session.getAttribute(SUCCESS_MESSAGE) + "<p>";
        }
        session.removeAttribute(SUCCESS_MESSAGE);
        return message;
    }

    public static void saveParamValue(HttpServletRequest request, String paramName, String paramValue) {
        HttpSession session = request.getSession();
        session.setAttribute(FORM_INPUT_PREFIX + paramName, paramValue);
    }

    public static String getParamValue(HttpServletRequest request, String paramName) {
        HttpSession session = request.getSession();
        String value = getNonNullValue((String) session.getAttribute(FORM_INPUT_PREFIX + paramName));
        session.removeAttribute(FORM_INPUT_PREFIX + paramName);
        return value;
    }

    public static String getHash(String str) {
        try {
            MessageDigest sha = MessageDigest.getInstance("MD5");
            byte[] strBytes = sha.digest(str.getBytes("UTF-8"));
            return new String(new BASE64Encoder().encode(strBytes));
        } catch (Exception e) {
            Log.msg("Util.getHash()", e);
            return null;
        }
    }

    public static String generateRandomString(String alphabet, int length) {
        if (null == alphabet || alphabet.length() < 2 || length < 1) {
            Log.msg("Util.getRandomString()", "You can't generate a random string when your alphabet is: " + alphabet + " and your lenght is: " + length);
            return null;
        }
        String randomString = "";
        int alphabetLength = alphabet.length();
        for (int rndStrLength = 0; rndStrLength < length; rndStrLength++) {
            char rndChar = alphabet.charAt((int) (Math.round(Math.random() * 10000) % alphabetLength));
            randomString += String.valueOf(rndChar);
        }
        return randomString;
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static String getNonNullValue(String str) {
        if (null == str) {
            return "";
        } else {
            return str;
        }
    }

    public static String join(Object[] items, String delimeter) {
        StringBuffer str = new StringBuffer();
        for (int itemIdx = 0; itemIdx < items.length; itemIdx++) {
            if (itemIdx == 0) {
                str.append(items[itemIdx].toString());
            } else {
                str.append(delimeter);
                str.append(items[itemIdx].toString());
            }
        }
        return str.toString();
    }
}
