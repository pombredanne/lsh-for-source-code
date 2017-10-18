package org.nestframework.commons.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author austin
 * @version 1.0.0
 */
public class EncodeUtil {

    /**
	 * Compute MD5 value of a string.
	 * @param str String.
	 * @return MD5 of String.
	 */
    public static String md5(String str) {
        return encrypt("MD5", str);
    }

    public static String encrypt(String algorithm, String str) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(str.getBytes());
            StringBuffer sb = new StringBuffer();
            byte[] bytes = md.digest();
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i] & 0xFF;
                if (b < 0x10) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Encode Url.
     * @param url
     * @param encode
     * @return
     */
    public static String urlEncode(String url, String encode) {
        try {
            return URLEncoder.encode(url, encode);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Decode Url.
     * @param url
     * @param encode
     * @return
     */
    public static String urlDecode(String url, String encode) {
        try {
            return URLDecoder.decode(url, encode);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Base64 encode.
     * @param s source string.
     * @return encoded string.
     */
    public static String base64Encode(String s) {
        return new String(new BASE64Encoder().encode(s.getBytes()));
    }

    /**
     * Base64 decode.
     * @param s source string.
     * @return decoded string.
     * @throws IOException exception.
     */
    public static String base64Decode(String s) throws IOException {
        return new String(new BASE64Decoder().decodeBuffer(s));
    }
}
