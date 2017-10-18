package com.sanctuary.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * String Tools provides some simple static string tools. 
 * 
 * @author David Palmer
 * 
 * @version $Id: StringTools.java,v 1.3 2007/01/12 16:40:47 cvs Exp $
 */
public class StringTools {

    private static Log log = LogFactory.getLog(StringTools.class);

    /**
	 * return a string of a strack trace so that we can log it easily
	 * @param ex - exception we want to work with
	 * @return String of our stack trace
	 */
    public static String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String replaceEmptyLines(String in) {
        String[] lines = in.split("\\n");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().equals("")) {
                if (i < lines.length) sb.append(lines[i].trim() + '\n'); else sb.append(lines[i].trim());
            }
        }
        return sb.toString().trim();
    }

    /**
	 * Transform a string array into a list. A list is a string with each
	 * element delimited by some unique character (this defaults to a comma)
	 * 
	 * @param array - array to transform into a list
	 * @return - String our list
	 */
    public static String arrayToList(String[] array) {
        return arrayToList(array, ",");
    }

    /**
	 * Transform a string array into a delimited list (each element in the array
	 * being delimited by some delimiter specified as a parameter)
	 * 
	 * @param array - array to be transformed
	 * @param delim - character (or characters to be used as a delimiter)
	 * @return - String list
	 */
    public static String arrayToList(String[] array, String delim) {
        return arrayToList(array, delim, null);
    }

    public static String arrayToList(String[] array, String delim, String elementPrefix) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i < (array.length - 1)) if (elementPrefix != null) sb.append(elementPrefix + array[i] + delim); else sb.append(array[i] + delim); else if (elementPrefix != null) sb.append(elementPrefix + array[i]); else sb.append(array[i]);
        }
        return sb.toString();
    }

    public static boolean listContainsItem(String list, String item, String delimeter) {
        boolean hasItem = false;
        try {
            String[] array = listToArray(list, delimeter);
            for (int i = 0; i < array.length; i++) {
                String _e = array[i];
                if (_e.equalsIgnoreCase(item)) {
                    hasItem = true;
                }
            }
        } catch (Exception ex) {
            log.error(getStackTrace(ex));
        }
        return hasItem;
    }

    public static String[] listToArray(String list) {
        return listToArray(list, "\\,");
    }

    public static String[] listToArray(String list, String delim) {
        String[] array = list.split(delim);
        return array;
    }

    public static List<String> stringToArrayList(String list, String delim) {
        String[] l = listToArray(list, delim);
        List<String> _list = new ArrayList<String>();
        for (int i = 0; i < l.length; i++) {
            _list.add(l[i]);
        }
        return _list;
    }

    public static List<String> stringToArrayList(String list) {
        return stringToArrayList(list, "\\,");
    }

    public static String bytes2String(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            stringBuffer.append((char) bytes[i]);
        }
        return stringBuffer.toString();
    }

    public static String cleanString(List<String> pwList, String in, String replace) {
        String returnString = null;
        for (String item : pwList) {
            if (returnString == null) {
                returnString = in.replaceAll("(?i)\\b" + item + "\\b", replace);
            } else {
                returnString = returnString.replaceAll("(?i)\\b" + item + "\\b", replace);
            }
        }
        if (returnString != null) {
            return returnString.trim();
        } else {
            return in;
        }
    }

    public static String cleanString(List<String> pwList, String in) {
        return cleanString(pwList, in, "");
    }

    public static String getMD5(String s) throws Exception {
        MessageDigest complete = MessageDigest.getInstance("MD5");
        complete.update(s.getBytes());
        byte[] b = complete.digest();
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String list = "item1,item2,item3,item4";
        String[] array = listToArray(list);
        if (array == null) System.out.println("array is null"); else System.out.println("number of items: " + array.length);
    }

    public static String getPossessive(String s) {
        StringBuffer sb = new StringBuffer();
        if (s.endsWith("s")) {
            sb.append(s);
            sb.append('\'');
            return sb.toString();
        }
        sb.append(s);
        sb.append('\'');
        sb.append('s');
        return sb.toString();
    }

    public static String removeHtml(String in) {
        return in.replaceAll("\\<.*?\\>", "");
    }
}
