package com.carey.renren.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.activation.MimetypesFileTypeMap;
import com.carey.renren.RenRenOAuth;
import com.carey.renren.RenRenParameter;

public class RenRenHttpUtil {

    /**
	 * Return the MIME type based on the specified file name.
	 * 
	 * @param fileName
	 *            path of input file.
	 * @return MIME type.
	 */
    public static String getContentType(String fileName) {
        return new MimetypesFileTypeMap().getContentType(fileName);
    }

    /**
	 * Return the MIME type based on the specified file name.
	 * 
	 * @param file
	 *            File
	 * @return MIME type.
	 */
    public static String getContentType(File file) {
        return new MimetypesFileTypeMap().getContentType(file);
    }

    /**
	 * Return the list of query parameters based on the specified query string.
	 * 
	 * @param queryString
	 * @return the list of query parameters.
	 */
    public static List<RenRenParameter> getQueryParameters(String queryString) {
        if (queryString.startsWith("?")) {
            queryString = queryString.substring(1);
        }
        List<RenRenParameter> result = new ArrayList<RenRenParameter>();
        if (queryString != null && !queryString.equals("")) {
            String[] p = queryString.split("&");
            for (String s : p) {
                if (s != null && !s.equals("")) {
                    if (s.indexOf('=') > -1) {
                        String[] temp = s.split("=");
                        result.add(new RenRenParameter(temp[0], temp[1]));
                    }
                }
            }
        }
        return result;
    }

    /**
	 * Convert %XX
	 * 
	 * @param value
	 * @return
	 */
    public static String formParamDecode(String value) {
        int nCount = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '%') {
                i += 2;
            }
            nCount++;
        }
        byte[] sb = new byte[nCount];
        for (int i = 0, index = 0; i < value.length(); i++) {
            if (value.charAt(i) != '%') {
                sb[index++] = (byte) value.charAt(i);
            } else {
                StringBuilder sChar = new StringBuilder();
                sChar.append(value.charAt(i + 1));
                sChar.append(value.charAt(i + 2));
                sb[index++] = Integer.valueOf(sChar.toString(), 16).byteValue();
                i += 2;
            }
        }
        String decode = "";
        try {
            decode = new String(sb, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decode;
    }

    public static String md5(String string) {
        if (string == null || string.trim().length() < 1) {
            return null;
        }
        try {
            return getMD5(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String getMD5(byte[] source) {
        String s = null;
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source);
            byte tmp[] = md.digest();
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            s = new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static TreeMap<String, String> sigParams(TreeMap<String, String> params) {
        StringBuffer sb = new StringBuffer();
        for (Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        sb.append(RenRenOAuth.SecretKey);
        params.put("sig", RenRenHttpUtil.md5(sb.toString()));
        return params;
    }

    public static TreeMap<String, String> prepareParams(TreeMap<String, String> params, String format) {
        params.put("api_key", RenRenOAuth.APIKey);
        params.put("v", RenRenOAuth.ApiVersion);
        params.put("call_id", String.valueOf(System.currentTimeMillis()));
        params.put("format", format);
        return sigParams(params);
    }
}
