package com.zpyr.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import sun.misc.BASE64Encoder;

public class StringUtil {

    public static String nvl(Object obj) {
        String retValue = "";
        if (obj == null) retValue = ""; else retValue = obj.toString();
        return retValue;
    }

    public static String nvl(Object obj, String defValue) {
        String retValue = "";
        if (obj == null) retValue = defValue; else retValue = obj.toString();
        return retValue;
    }

    public static int nvlInt(Object obj) {
        int retValue = 0;
        try {
            if (obj != null) retValue = Integer.valueOf(obj.toString());
        } catch (Exception e) {
            retValue = 0;
        }
        return retValue;
    }

    public static int nvlInt(Object obj, int defValue) {
        int retValue = 0;
        try {
            if (obj != null) retValue = Integer.valueOf(obj.toString()); else retValue = defValue;
        } catch (Exception e) {
            retValue = 0;
        }
        return retValue;
    }

    /**
	 * text <-> html start
	 * 사용 예. textarea 에서 입력한 문자를 html에서 제대로 보여주게 할 때 사용.
	 * 개행문자 -> <br /> 등.
	 */
    public static String textTOhtml(String text) {
        if (text == null) return "";
        text = replace(text, "\"", "&quot;");
        text = replace(text, "<", "&lt;");
        text = replace(text, ">", "&gt;");
        text = replace(text, "\n", "<br>");
        text = replace(text, "\r", "");
        text = replace(text, " ", "&nbsp;");
        text = replace(text, "'", "`");
        text = replace(text, "\"", "`");
        return text;
    }

    public static String htmlTOtext(String html) {
        if (html == null) return "";
        html = replace(html, "&quot;", "\"");
        html = replace(html, "&lt;", "<");
        html = replace(html, "&gt;", ">");
        html = replace(html, "<br>", "\n");
        html = replace(html, "", "\r");
        html = replace(html, "&nbsp;", " ");
        return html;
    }

    public static String replace(String origin, String source, String target) {
        if (origin == null) return "";
        int itmp = 0;
        String tmp = origin;
        StringBuffer sb = new StringBuffer("");
        while (tmp.indexOf(source) > -1) {
            itmp = tmp.indexOf(source);
            sb.append(tmp.substring(0, itmp));
            sb.append(target);
            tmp = tmp.substring(itmp + source.length());
        }
        sb.append(tmp);
        return sb.toString();
    }

    public static String encodeMD5(String value) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            BASE64Encoder encoder = new BASE64Encoder();
            md.update(value.getBytes());
            byte[] raw = md.digest();
            result = encoder.encode(raw);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getFilename(String fullName) {
        if (fullName == null) return "";
        String fileName = "";
        if (fullName.lastIndexOf('/') == -1) fileName = fullName; else fileName = fullName.substring(fullName.lastIndexOf('/') + 1, fullName.length());
        return fileName;
    }

    public static String getFileExtension(String fileName) {
        String retExt = "";
        if (fileName == null) {
            return retExt;
        }
        if (!(fileName.lastIndexOf(".") == -1)) {
            retExt = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        }
        return retExt;
    }

    public static void printMap(Map<Object, Object> map) {
        Set entries = map.entrySet();
        Iterator<Object> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry thisPair = (Map.Entry) iterator.next();
            System.out.println("# " + thisPair.getKey() + ": " + thisPair.getValue());
        }
    }

    public static String getWon(String strCurrency) {
        double dblCurrency = 0;
        try {
            dblCurrency = Double.valueOf(strCurrency).doubleValue();
        } catch (Exception e) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("###,##0");
        return df.format(dblCurrency);
    }
}
