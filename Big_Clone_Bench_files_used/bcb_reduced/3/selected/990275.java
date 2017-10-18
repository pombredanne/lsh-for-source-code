package ro.gateway.aida.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import ro.gateway.aida.db.PersistenceToken;
import ro.gateway.aida.obj.BreadCrumb;

/**
 * LPD Static utils
 * 
 * @author Mihai Popoaei mihai_popoaei@yahoo.com;
 *         <p>
 *         Mihai Postelnicu mihai@ro-gateway.org
 */
public class Utils {

    public static String getProperty(PersistenceToken token, String name) {
        Hashtable ht = (Hashtable) token.getProperty("sysProps");
        return (String) ht.get(name);
    }

    /**
	 * no comments
	 */
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
        if (toHash == null) return null;
        String hashed = "";
        byte[] hash = new byte[64];
        byte[] buffer = toHash.getBytes();
        hash = getKeyedDigest(buffer);
        for (int i = 0; i < hash.length; i++) hashed = hashed + toHex(hash[i]);
        return hashed;
    }

    public static String getReferer(HttpServletRequest request, String sessionAttrName, String paramName, String defaultReferer, String[] invalidRefs) {
        String result = defaultReferer;
        HttpSession session = request.getSession();
        String tmp = (String) session.getAttribute(sessionAttrName);
        if (tmp == null) {
            tmp = request.getParameter("referer");
            if (tmp != null) tmp = tmp.trim();
            if ((tmp != null) && (tmp.length() < 1)) tmp = null;
            if (tmp == null) tmp = request.getHeader("referer");
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
            if (parameterValue.length() < 1) parameterValue = null;
        }
        if (parameterValue != null) result = parameterValue;
        return result;
    }

    /**
	 * Returneaza un array cu valorile parametrilor cu numele specificat sau
	 * null
	 * 
	 * @param request
	 * @param parameterName
	 * @return
	 */
    public static String[] getValidTrimedStrings(HttpServletRequest request, String parameterName) {
        String[] result = null;
        ArrayList items = new ArrayList();
        String[] values = request.getParameterValues(parameterName);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) continue;
                values[i] = values[i].trim();
                if (values[i].length() < 1) continue;
                items.add(values[i]);
            }
        }
        if (items.size() > 0) {
            result = new String[items.size()];
            items.toArray(result);
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

    public static Object getLocalisedSessionObj(HttpSession session, String key, String lang) {
        if (session.getAttribute(key) == null) return null; else {
            Localizable ret = (Localizable) session.getAttribute(key);
            ret.setViewLang(lang);
            return ret;
        }
    }

    public static void setBreadCrumbs(HttpServletRequest request, String section) {
        HttpSession session = request.getSession();
        session.setAttribute(BreadCrumb.CURRENT, section);
    }

    public static boolean fileNameLooksLikePic(String file_name) {
        if (file_name == null) return false;
        file_name = file_name.toUpperCase();
        return file_name.endsWith(".JPG") || file_name.endsWith(".BMP") || file_name.endsWith(".GIF") || file_name.endsWith(".JPEG") || file_name.endsWith(".TIF") || file_name.endsWith(".PNG");
    }

    /**
	 * Struts uses Maps to output parameters inside links. When the same map is
	 * toString'ed in another place, it results a string like this :
	 * {user_id=1357, event_id=125, id=8605} This method receives a key and
	 * returns the value from a stringed map like the one above
	 * 
	 * @param strMap
	 *            the stringed map
	 * @param strMapKey
	 *            the key
	 * @return the value
	 */
    public static String getStrMapAttr(String strMap, String strMapKey) {
        if (strMap == null) return null;
        int start = strMap.indexOf(strMapKey) + strMapKey.length();
        int end = strMap.indexOf(',', start);
        return strMap.substring(start + 1, end);
    }

    /**
	 * returns an MD5 digest of the input
	 * 
	 * @param message
	 *            the text that needs to be digested
	 * @return the digested MD5 string
	 */
    public static String convertToMD5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new String(md.digest(message.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
