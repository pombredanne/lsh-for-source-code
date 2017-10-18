package com.kwoksys.framework.util;

import org.apache.commons.codec.net.BCodec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.security.MessageDigest;

/**
 * This is for String manipulation methods.
 */
public class StringUtils {

    private static final Logger logger = Logger.getLogger(StringUtils.class.getName());

    /**
     * This is for taking an objects. If the objects is null, return the defaultValue.
     *
     * @param temp
     * @param defaultValue
     * @return ..
     */
    public static String replaceNull(Object temp, String defaultValue) {
        if (temp == null || temp.toString().trim().isEmpty()) {
            return defaultValue;
        } else {
            return temp.toString().trim();
        }
    }

    /**
     * This is for taking an objects. If the objects is null, return the defaultValue.
     */
    public static String replaceNull(Object temp) {
        return replaceNull(temp, "");
    }

    /**
     * Encodes FCKEditor characters. Any single quote with a blackward slash and a single quote.
     * Fckeditor requires encoding a certain characters for it to display correctly.
     */
    public static String encodeFckeditorValue(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "\\'").replace("\n", "").replace("\r", "");
    }

    /**
     * Encodes string for passing to sql queries.
     *
     * @param input
     * @return ..
     */
    public static String encodeSql(Object input) {
        return input.toString().replace("'", "''");
    }

    /**
     * Encodes VCard characters.
     * @param input
     * @return
     */
    public static String encodeVCard(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input;
    }

    /**
     * Matcher class can reference a group with a leading dollar sign. Not encoding the dollar sign could cause
     * illegal group reference exception.
     * @return
     */
    public static String encodeMatcherReplacement(String input) {
        return Matcher.quoteReplacement(input);
    }

    /**
     * Encode a string to base64. Supports UTF8.
     * @return
     */
    public static String encodeBase64Codec(String input) {
        BCodec codec = new BCodec();
        try {
            return codec.encode(input);
        } catch (EncoderException e) {
            logger.warning("Problem with base64 encoding.");
            return "";
        }
    }

    /**
     * Decode base64-encoded string. Supports UTF8.
     * @return
     */
    public static String decodeBase64Codec(String input) {
        if (input.isEmpty()) {
            return "";
        }
        BCodec codec = new BCodec();
        try {
            return codec.decode(input);
        } catch (DecoderException e) {
            logger.warning("Problem with base64 decoding.");
            return "";
        }
    }

    /**
     * Decode base64-encoded string.
     * @return
     */
    public static String decodeBase64(String input) {
        return new String(Base64.decodeBase64(input.getBytes()));
    }

    public static String hash(String plaintext) {
        if (plaintext == null) {
            return "";
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
            md.update(plaintext.getBytes("UTF-8"));
        } catch (Exception e) {
        }
        return new String(Base64.encodeBase64(md.digest()));
    }

    /**
     * Joins a list of strings.
     *
     * @param strings
     * @param token
     * @return ..
     */
    public static String join(String[] strings, String token) {
        if (strings == null) {
            return "";
        }
        return join(Arrays.asList(strings), token);
    }

    /**
     * Joins a list of strings.
     * @param strings
     * @param token
     * @return
     */
    public static String join(List<String> strings, String token) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        boolean appendToken = false;
        for (String string : strings) {
            if (!string.isEmpty()) {
                if (appendToken) {
                    buffer.append(token);
                }
                buffer.append(string);
                appendToken = true;
            }
        }
        return buffer.toString();
    }

    /**
     * Joins a list of maps.
     *
     * @param list
     * @param token
     * @return ..
     */
    public static String join(List<Map> maps, String key, String token) {
        StringBuilder buffer = new StringBuilder();
        boolean appendToken = false;
        for (Map map : maps) {
            if (!map.isEmpty()) {
                if (appendToken) {
                    buffer.append(token);
                }
                buffer.append(map.get(key));
                appendToken = true;
            }
        }
        return buffer.toString();
    }

    /**
     * Converts String array to a sql list (common seperated list)
     *
     * @param temp
     * @return ..
     */
    public static String prepareSqlList(String[] temp) {
        return join(temp, ",");
    }

    public static String decodeHtml(String input) {
        return StringEscapeUtils.unescapeHtml(input);
    }
}
