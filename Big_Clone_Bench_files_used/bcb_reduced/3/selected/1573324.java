package ro.gateway.aida.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>Title: Romanian AIDA</p>
 * <p>Description: :D application</p>
 * <p>Copyright: Copyright (comparator) 2003</p>
 * <p>Company: Romania Development Gateway </p>
 *
 * @author Mihai Popoaei, mihai_popoaei@yahoo.com, smike@intellisource.ro
 * @version 1.0-* @version $Id: HttpUtils.java,v 1.1 2004/10/24 23:37:20 mihaipostelnicu Exp $
 */
public class HttpUtils {

    public static String[] getParametersLike(HttpServletRequest request, String startName) {
        String[] result = null;
        ArrayList items = new ArrayList();
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            if (name.startsWith(startName)) {
                items.add(name);
            }
        }
        if (items.size() > 0) {
            result = new String[items.size()];
            items.toArray(result);
        }
        return result;
    }

    private static String toHex(byte b) {
        int i = b;
        String hex;
        if (i < 0) i = 256 + i;
        int lo = i % 16;
        int hi = i / 16;
        hex = "" + Character.forDigit(hi, 16);
        hex = hex + Character.forDigit(lo, 16);
        return hex;
    }

    /**
	 * no comments
	 */
    private static byte[] getKeyedDigest(byte[] buffer) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer);
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    /**
	 * no comments
	 */
    public static String getHash(String toHash) {
        if (toHash == null) {
            return null;
        }
        String hashed = "";
        byte[] hash = new byte[64];
        byte[] buffer = toHash.getBytes();
        hash = getKeyedDigest(buffer);
        for (int i = 0; i < hash.length; i++) hashed = hashed + toHex(hash[i]);
        return hashed;
    }

    public static String getReferer(HttpServletRequest request, String sessionAttrName, String defaultReferer, String[] invalidRefs) {
        String result = defaultReferer;
        HttpSession session = request.getSession();
        String tmp = (String) session.getAttribute(sessionAttrName);
        if (tmp == null) {
            tmp = request.getParameter("referer");
            if (tmp != null) tmp = tmp.trim();
            if ((tmp != null) && (tmp.length() < 1)) tmp = null;
            if (tmp == null) {
                tmp = request.getHeader("referer");
            }
            if ((invalidRefs != null) && (tmp != null)) {
                for (int i = 0; i < invalidRefs.length; i++) {
                    if (tmp.indexOf(invalidRefs[i]) != -1) {
                        tmp = null;
                        break;
                    }
                }
            }
        }
        if (tmp != null) result = tmp;
        return result;
    }

    /**
	 * tpt
	 *
	 * @param request
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
    public static String getValidTrimedString(HttpServletRequest request, String parameterName, String defaultValue) {
        String result = defaultValue;
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue != null) {
            parameterValue = parameterValue.trim();
            if (parameterValue.length() < 1) {
                parameterValue = null;
            }
        }
        if (parameterValue != null) {
            result = parameterValue;
        }
        return result;
    }

    /**
	 * tpt
	 *
	 * @param defaultValue
	 * @param value
	 * @return
	 */
    public static String getValidTrimedString(String value, String defaultValue) {
        String result = defaultValue;
        String parameterValue = value;
        if (parameterValue != null) {
            parameterValue = parameterValue.trim();
            if (parameterValue.length() < 1) {
                parameterValue = null;
            }
        }
        if (parameterValue != null) {
            result = parameterValue;
        }
        return result;
    }

    public static int getInt(HttpServletRequest request, String parameterName, int defaultValue) {
        int result = defaultValue;
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue != null) {
            try {
                result = Integer.parseInt(parameterValue);
            } catch (NumberFormatException nfEx) {
            }
        }
        return result;
    }

    public static int getInt(String value, int defaultValue) {
        int result = defaultValue;
        if (value != null) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException nfEx) {
            }
        }
        return result;
    }

    public static double getDouble(HttpServletRequest request, String parameterName, double defaultValue) {
        double result = defaultValue;
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue != null) {
            try {
                result = Double.parseDouble(parameterValue);
            } catch (NumberFormatException nfEx) {
            }
        }
        return result;
    }

    public static long getLong(HttpServletRequest request, String parameterName, long defaultValue) {
        long result = defaultValue;
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue != null) try {
            parameterValue.trim();
            if (parameterValue.length() < 1) return defaultValue;
            result = Long.parseLong(parameterValue);
        } catch (NumberFormatException nfEx) {
        }
        return result;
    }

    public static long getLong(String parameterValue, long defaultValue) {
        long result = defaultValue;
        if (parameterValue != null) try {
            parameterValue.trim();
            if (parameterValue.length() < 1) return defaultValue;
            result = Long.parseLong(parameterValue);
        } catch (NumberFormatException nfEx) {
        }
        return result;
    }

    public static String getAbsoluteURL(HttpServletRequest request, String relativeURL) {
        StringBuffer sb = new StringBuffer();
        sb.append("http://").append(request.getServerName()).append(":").append(request.getServerPort()).append(request.getContextPath()).append(relativeURL);
        return sb.toString();
    }

    public static int[] getInts(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) return null;
        int[] result = new int[0];
        for (int i = 0; i < values.length; i++) {
            int val = Integer.MIN_VALUE;
            try {
                val = Integer.parseInt(values[i]);
            } catch (Exception ex) {
            }
            if (val != Integer.MIN_VALUE) {
                int[] new_ints = new int[result.length + 1];
                System.arraycopy(result, 0, new_ints, 0, result.length);
                new_ints[result.length] = val;
                result = new_ints;
            }
        }
        return result;
    }

    public static String strPrint(String str, String nullVal) {
        return (str == null) ? nullVal : str;
    }

    public static void main(String[] args) {
        System.out.println(getHash("1234qwer"));
    }

    public static String getSortCriteria(HttpServletRequest request, String session_attr_name, String name, String default_value) {
        HttpSession session = request.getSession();
        String criteria = getValidTrimedString(request, name, null);
        if (criteria == null) criteria = default_value;
        if (criteria != null) {
            session.setAttribute(session_attr_name, criteria);
        } else {
            session.removeAttribute(session_attr_name);
        }
        return criteria;
    }
}
