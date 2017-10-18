package org.nex.ts.server.common;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import org.nex.ts.TopicSpacesException;

/**
 *
 * @author park
 *
 */
public class StringUtils {

    private StringBuffer buf;

    private String tag;

    public StringUtils() {
        super();
    }

    /**
	 * <p>
	 * Take a <code>url</code> e.g. http://foo.org/something.html
	 * and return e.g. http:||foo.org|something.html
	 * </p>
	 * <p>
	 * This is needed when we use PSI values made from URLs as
	 * locators in proxies
	 * </p>
	 * @param url
	 * @return
	 */
    public static String sanitizeString(String url) {
        System.out.println("StringUtils.sanitize-" + url);
        StringBuilder buf = new StringBuilder();
        int len = url.length();
        char c;
        for (int i = 0; i < len; i++) {
            c = url.charAt(i);
            if ((c != '/') && (c != ' ') && (c != ':') && (c != '#') && (c != '.') && (c != '&') && (c != '?') && (c != '=')) buf.append(c);
        }
        String result = buf.toString();
        System.out.println("StringUtils.sanitize+" + result);
        return result;
    }

    public static String unSanitize(String san) {
        String result = san;
        result = result.replace('|', '/');
        result = result.replace('-', ':');
        result = result.replace('_', '.');
        return result;
    }

    /**
	 * Remove accents, etc, from <code>s</code>
	 * @param s
	 * @return
	 */
    public static String removeAccents(String s) {
        String result = s;
        result = result.replace('�', 'Y');
        result = result.replaceAll("� | � | � | �", "U");
        result = result.replaceAll("� | � | � | � | �", "O");
        result = result.replaceAll("� | � | � | �", "I");
        result = result.replaceAll("� | � | � | �", "E");
        result = result.replace('�', 'C');
        result = result.replaceAll("� | � | � | � | � | � | �", "A");
        result = result.replaceAll("[����]", "e");
        result = result.replaceAll("[��]", "u");
        result = result.replaceAll("[��]", "i");
        result = result.replaceAll("[��]", "a");
        return result;
    }

    /**
	 * Trim length to 255 max characters
	 * @param in
	 * @return
	 */
    public static String trimLength(String in) {
        if (in.length() > 255) return in.substring(0, 254);
        return in;
    }

    /**
	 * <p>
	 * Name strings are assumed to have no puncuation except spaces
	 * which are replaced with a '_'
	 * </p>
	 * @param in
	 * @return
	 */
    public static String cleanNameString(String in) {
        StringBuffer buf = new StringBuffer();
        int len = in.length();
        char c;
        for (int i = 0; i < len; i++) {
            c = in.charAt(i);
            if (c == ' ') buf.append('_'); else if (c != '\'' && c != '/') buf.append(c);
        }
        String result = buf.toString();
        System.out.println("StringUtils.sanitize+" + result);
        return result;
    }

    public void createXMLString(String tag) {
        this.tag = tag;
        buf = new StringBuffer("<" + tag);
    }

    public void addAttribute(String key, String value) {
        buf.append(key + "=\"" + value + "\" ");
    }

    public void addText(String text) {
        buf.append(">" + text + "</" + tag + ">");
    }

    public String getXMLString() {
        if (buf.toString().endsWith(">")) return buf.toString(); else return buf.toString() + "/>";
    }

    /**
	   * <p>
	   * <strong>TODO</strong>
	   * see if <code>locator</code> is a valid URI
	   * </p>
	   * @param locator
	   * @return
	   */
    public String locatorPolicy(String locator) {
        String result = locator;
        if (!locator.startsWith("http")) result = "#" + locator;
        return result;
    }

    /**
	   * <p>
	   * Return a formated date string from a string timestamp, e.g.
	   * 12.13.52  3:30pm
	   * </p>
	   * @param timestamp String
	   * @return String
	   */
    public static String longToShortDateString(String timestamp) {
        return longToShortDateString(Long.parseLong(timestamp));
    }

    /**
	   * Return a formated date string from a long timestamp
	   * @param timestamp long
	   * @return String
	   */
    public static String longToShortDateString(long timestamp) {
        Date d = new Date(timestamp);
        return DateFormat.getDateInstance().format(d);
    }

    /**
	   * <p>
	   * Return a formated date string from a string timestamp, e.g.
	   * January 12, 1952  3:30:32pm
	   * </p>
	   * @param timestamp String
	   * @return String
	   */
    public static String longToLongDateString(String timestamp) {
        return longToLongDateString(Long.parseLong(timestamp));
    }

    /**
	   * Return a formated date string from a long timestamp
	   * @param timestamp long
	   * @return String
	   */
    public static String longToLongDateString(long timestamp) {
        Date d = new Date(timestamp);
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(d);
    }

    /**
	   * <p>
	   * Return a formated date string from a string timestamp, e.g.
	   * Tuesday, April 12, 1952 AD  3:30:42pm PST
	   * </p>
	   * @param timestamp String
	   * @return String
	   */
    public static String longToFullDateString(String timestamp) {
        return longToFullDateString(Long.parseLong(timestamp));
    }

    /**
	   * Return a formated date string from a long timestamp
	   * @param timestamp long
	   * @return String
	   */
    public static String longToFullDateString(long timestamp) {
        Date d = new Date(timestamp);
        return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(d);
    }

    /**
	   * Need to convert to GMT and add 'Z' to pattern
	   * @param timestamp long
	   * @return String
	   */
    public static String longToISO8601String(long timestamp) {
        Date d = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.format(d);
        return sdf.toPattern();
    }

    /**
	   * <p>
	   * Return an <code>MD5 Hash</code> of of <code>inString</code><br/>
	   * e.g.<br/>
	   * http://www.weather.com/ --> 6cfedbe75f413c56b6ce79e6fa102aba
	   * </p>
	   * @param inString String
	   * @return String
	   * @throws TopicSpacesException
	   */
    public static String md5Hash(String inString) throws TopicSpacesException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(inString.getBytes());
            byte[] array = md5.digest();
            StringBuffer buf = new StringBuffer();
            int len = array.length;
            for (int i = 0; i < len; i++) {
                int b = array[i] & 0xFF;
                buf.append(Integer.toHexString(b));
            }
            return buf.toString();
        } catch (Exception x) {
            throw new TopicSpacesException(x);
        }
    }

    /**
		 * Datestring to date long. Truncates time and just uses yy,mm,dd
		 * @param time String
		 * @return long
		 * code borrowed and adapted from {@link org.nex.ts.server.model.tago.DeliciousPullParser}
		 * <p> was: "2008/07/24 18:28:09 +0000"<br />
		 * is: 2008-08-24T01:29:20Z  which is a delicious-style date string
		 * </p>
		 * <p>Date strings returned to old format. Must now look for both</p>
		 */
    public static long getDate(String time) {
        long result = 0;
        String w = time;
        String y, m, d, hh, mm, ss;
        int where = w.indexOf('-');
        if (where == -1) where = w.indexOf('/');
        y = w.substring(0, where);
        w = w.substring(where + 1);
        where = w.indexOf('-');
        if (where == -1) where = w.indexOf('/');
        m = w.substring(0, where);
        w = w.substring(where + 1);
        where = w.indexOf('T');
        if (where == -1) where = w.indexOf(' ');
        d = w.substring(0, where);
        d = d.trim();
        w = w.substring(where + 1);
        hh = w.substring(0, 1);
        mm = w.substring(3, 4);
        ss = w.substring(6, 7);
        Calendar c = new GregorianCalendar(Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d), Integer.parseInt(hh), Integer.parseInt(mm), Integer.parseInt(ss));
        result = c.getTimeInMillis();
        return result;
    }
}
