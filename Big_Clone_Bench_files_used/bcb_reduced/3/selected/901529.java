package jeeves.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import jeeves.exceptions.BadInputEx;
import jeeves.exceptions.BadParameterEx;
import jeeves.exceptions.MissingParameterEx;
import org.jdom.Element;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/** Generic utility class (static methods)
  */
public class Util {

    public static Element getChild(Element el, String name) throws MissingParameterEx {
        Element param = el.getChild(name);
        if (param == null) throw new MissingParameterEx(name, el);
        return param;
    }

    public static String getParam(Element el, String name) throws BadInputEx {
        if (el == null) throw new MissingParameterEx(name);
        Element param = el.getChild(name);
        if (param == null) throw new MissingParameterEx(name, el);
        String value = param.getTextTrim();
        if (value.length() == 0) throw new BadParameterEx(name, value);
        return value;
    }

    public static String getParam(Element el, String name, String defValue) {
        if (el == null) return defValue;
        Element param = el.getChild(name);
        if (param == null) return defValue;
        String value = param.getTextTrim();
        if (value.length() == 0) return defValue;
        return value;
    }

    public static int getParamAsInt(Element el, String name) throws BadInputEx {
        String value = getParam(el, name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadParameterEx(name, value);
        }
    }

    public static int getParam(Element el, String name, int defValue) throws BadParameterEx {
        String value = getParam(el, name, null);
        if (value == null) return defValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadParameterEx(name, value);
        }
    }

    public static boolean getParam(Element el, String name, boolean defValue) throws BadParameterEx {
        String value = getParam(el, name, null);
        if (value == null) return defValue;
        if (value.equals("true") || value.equals("on")) return true;
        if (value.equals("false")) return false;
        throw new BadParameterEx(name, value);
    }

    public static String getAttrib(Element el, String name) throws BadInputEx {
        String value = el.getAttributeValue(name);
        if (value == null) throw new MissingParameterEx("attribute:" + name, el);
        value = value.trim();
        if (value.length() == 0) throw new BadParameterEx("attribute:" + name, value);
        return value;
    }

    public static String getAttrib(Element el, String name, String defValue) {
        if (el == null) return defValue;
        String value = el.getAttributeValue(name);
        if (value == null) return defValue;
        value = value.trim();
        if (value.length() == 0) return defValue;
        return value;
    }

    /** replace occurrences of <p> in <s> with <r> */
    public static String replaceString(String s, String pattern, String replacement) {
        StringBuffer result = new StringBuffer();
        int i;
        while ((i = s.indexOf(pattern)) != -1) {
            result.append(s.substring(0, i));
            result.append(replacement);
            s = s.substring(i + pattern.length());
        }
        result.append(s);
        return result.toString();
    }

    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String scramble(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (byte b : md.digest()) sb.append(Integer.toString(b & 0xFF, 16));
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
