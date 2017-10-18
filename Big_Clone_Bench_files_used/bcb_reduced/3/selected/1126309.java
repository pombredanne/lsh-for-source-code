package com.rapidlogix.monitor.web.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import com.rapidlogix.monitor.util.Base64Coder;

public class SecurityUtil {

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encodeBase64Param(String param) throws Exception {
        return URLEncoder.encode(Base64Coder.encodeString(param), "iso-8859-1");
    }

    public static String decodeBase64Param(String param) throws Exception {
        return Base64Coder.decodeString(URLDecoder.decode(param, "iso-8859-1"));
    }

    public static String encodeMD5(String param) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(param.getBytes());
        byte[] hash = digest.digest();
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return String.valueOf(buf);
    }
}
