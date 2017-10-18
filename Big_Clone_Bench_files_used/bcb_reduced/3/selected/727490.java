package com.easyExam.common.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * String Utility Class This is used to encode passwords programmatically
 * @author lgli
 *
 * Created on  2009-12-17
 */
public class StringUtil {

    private static final Log log = LogFactory.getLog(StringUtil.class);

    private StringUtil() {
    }

    /**
     * Generate a special length password string,
     * which contains of 'a'-'z', 'A'-'Z', '0'-'9' 
     * @param length  int
     * @return String
     */
    public static String randomPassword(int length) {
        StringBuffer password = new StringBuffer();
        int index = 0;
        while (index < length) {
            char ascii = (char) Math.floor(Math.random() * 125);
            if ((ascii >= 'a' && ascii <= 'z') || (ascii >= 'A' && ascii <= 'Z') || (ascii >= '0' && ascii <= '9')) {
                password.append(String.valueOf(ascii));
                index++;
            }
        }
        return password.toString();
    }

    /**
     * Encode a string using algorithm specified in web.xml and return the
     * resulting encrypted password. If exception, the plain credentials
     * string is returned
     *
     * @param password  Password or other credentials to use in authenticating
     *                  this username
     * @param algorithm Algorithm used to do the digest
     * @return encypted password based on the algorithm.
     */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Exception: " + e);
            return password;
        }
        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * Encode a string using Base64 encoding. Used when storing passwords
     * as cookies.
     * <p/>
     * This is weak encoding in that anyone can use the decodeString
     * routine to reverse the encoding.
     *
     * @param str String
     * @return String
     */
    public static String encodeString(String str) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        return encoder.encodeBuffer(str.getBytes()).trim();
    }

    /**
     * Decode a string using Base64 encoding.
     *
     * @param str String
     * @return String
     */
    public static String decodeString(String str) {
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        try {
            return new String(dec.decodeBuffer(str));
        } catch (IOException io) {
            throw new RuntimeException(io.getMessage(), io.getCause());
        }
    }

    public static String deNull(String str) {
        return (str == null) ? "" : str;
    }

    public static boolean isNull(String str) {
        return "".equals(deNull(str));
    }

    public static List<String> getListByToken(String str, String token) {
        if (str == null) {
            return new ArrayList<String>();
        }
        StringTokenizer st = new StringTokenizer(str, token);
        List<String> list = new ArrayList<String>();
        while (st.hasMoreElements()) {
            list.add(st.nextElement().toString().trim());
        }
        return list;
    }

    public static String getFirstUpperCaseVarName(String var) {
        if (var == null || "".equals(var)) {
            return var;
        }
        return var.substring(0, 1).toUpperCase() + var.substring(1, var.length());
    }

    public static String getFirstLowerCaseVarName(String var) {
        if (var == null || "".equals(var)) {
            return var;
        }
        return var.substring(0, 1).toLowerCase() + var.substring(1, var.length());
    }

    public static String getHtmlSafeString(String orginalStr) {
        if (orginalStr == null) {
            orginalStr = "";
        }
        orginalStr = StringUtils.replace(orginalStr, "&", "&amp;");
        orginalStr = StringUtils.replace(orginalStr, "<", "&lt;");
        orginalStr = StringUtils.replace(orginalStr, ">", "&gt;");
        return orginalStr;
    }

    public static boolean isNullOrEmpty(String str) {
        boolean flag = true;
        if (str != null) {
            str = str.trim();
            if (str.length() > 0) flag = false;
        }
        return flag;
    }
}
