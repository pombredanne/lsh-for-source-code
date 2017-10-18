package org.ramadda.repository;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;
import java.io.*;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RepositoryUtil implements Constants {

    /** timezone */
    public static final TimeZone TIMEZONE_DEFAULT = TimeZone.getTimeZone("UTC");

    /** _more_ */
    public static final String FILE_SEPARATOR = "_file_";

    /**
     * _more_
     *
     * @param b1 _more_
     * @param b2 _more_
     *
     * @return _more_
     */
    public static String buttons(String b1, String b2) {
        return b1 + HtmlUtil.space(2) + b2;
    }

    /**
     * _more_
     *
     * @param b1 _more_
     * @param b2 _more_
     * @param b3 _more_
     *
     * @return _more_
     */
    public static String buttons(String b1, String b2, String b3) {
        return b1 + HtmlUtil.space(2) + b2 + HtmlUtil.space(2) + b3;
    }

    /**
     * _more_
     *
     * @param password _more_
     *
     * @return _more_
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(password.getBytes("UTF-8"));
            byte[] bytes = md.digest();
            String result = encodeBase64(bytes);
            return result.trim();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }

    /**
     * This is a routine created by Matias Bonet to handle pre-existing passwords that
     * were hashed via md5
     *
     * @param password The password
     *
     * @return hashed password
     */
    public static String hashPasswordForOldMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes("UTF-8"));
            byte messageDigest[] = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }

    /**
     * _more_
     *
     * @param b _more_
     *
     * @return _more_
     */
    public static String encodeBase64(byte[] b) {
        return javax.xml.bind.DatatypeConverter.printBase64Binary(b);
    }

    /**
     * Decode the given base64 String
     *
     * @param s Holds the base64 encoded bytes
     * @return The decoded bytes
     */
    public static byte[] decodeBase64(String s) {
        return javax.xml.bind.DatatypeConverter.parseBase64Binary(s);
    }

    /**
     * _more_
     *
     * @param formatString _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String formatString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.setTimeZone(TIMEZONE_DEFAULT);
        dateFormat.applyPattern(formatString);
        return dateFormat;
    }

    /**
     * This will prune out any leading &lt;unique id&gt;_file_&lt;actual file name&gt;
     *
     * @param fileName _more_
     *
     * @return _more_
     */
    public static String getFileTail(String fileName) {
        int idx = fileName.indexOf(FILE_SEPARATOR);
        if (idx >= 0) {
            fileName = fileName.substring(idx + FILE_SEPARATOR.length());
        } else {
            int idx1 = fileName.indexOf("-");
            if (idx1 >= 0) {
                int idx2 = fileName.indexOf("-", idx1);
                if (idx2 >= 0) {
                    idx = fileName.indexOf("_");
                    if (idx >= 0) {
                        fileName = fileName.substring(idx + 1);
                    }
                }
            }
        }
        idx = fileName.lastIndexOf("\\");
        if (idx >= 0) {
            fileName = fileName.substring(idx + 1);
        }
        String tail = IOUtil.getFileTail(fileName);
        return tail;
    }

    /**
     * Class MissingEntryException _more_
     *
     *
     * @author RAMADDA Development Team
     * @version $Revision: 1.3 $
     */
    public static class MissingEntryException extends Exception {

        /**
         * _more_
         *
         * @param msg _more_
         */
        public MissingEntryException(String msg) {
            super(msg);
        }
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public static String header(String h) {
        return HtmlUtil.div(h, HtmlUtil.cssClass(CSS_CLASS_HEADING_1));
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static final String encodeInput(String s) {
        s = HtmlUtil.urlEncode(s);
        s = s.replace("+", " ");
        return s;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        for (String s : args) {
            System.err.println(encodeInput(s));
        }
    }

    /**
     * _more_
     *
     * @param urls _more_
     *
     * @return _more_
     */
    public static List<RequestUrl> toList(RequestUrl[] urls) {
        List<RequestUrl> l = new ArrayList<RequestUrl>();
        for (RequestUrl r : urls) {
            l.add(r);
        }
        return l;
    }

    /**
     * _more_
     *
     * @param html _more_
     * @param left _more_
     *
     * @return _more_
     */
    public static String leftIndset(String html, int left) {
        return inset(html, 0, left, 0, 0);
    }

    /**
     * _more_
     *
     * @param html _more_
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String inset(String html, int top, int left, int bottom, int right) {
        return HtmlUtil.div(html, HtmlUtil.style(((top == 0) ? "" : "margin-top:" + top + "px;") + ((left == 0) ? "" : "margin-left:" + left + "px;") + ((bottom == 0) ? "" : "margin-bottom:" + bottom + "px;") + ((right == 0) ? "" : "margin-right:" + top + "px;")));
    }
}
