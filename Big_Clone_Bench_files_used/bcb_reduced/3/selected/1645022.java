package com.tx.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * 
 * 权限认证的工具类
 * 
 * @author Sunny
 * 
 */
public class AuthUtils {

    /**
	 * 浏览者天下网ID
	 */
    public static final String AUTH_OWNER = "TX-AUTH-OWNER";

    /**
	 * 应用ID
	 */
    public static final String AUTH_APP = "TX-AUTH-APP";

    /**
	 * 服务器SESSION
	 */
    public static final String AUTH_SESSION = "TX-AUTH-SESSION";

    /**
	 * 应用密钥
	 */
    public static final String AUTH_KEY = "TX-AUTH-KEY";

    /**
	 * 时间戳
	 */
    public static final String TIMESTAMP = "TX-AUTH-TIMESTAMP";

    /**
	 * SDK版本号
	 */
    public static final String SDK = "TX-SDK-VERSION";

    /**
	 * API版本号
	 */
    public static final String VERSION = "TX-API-VERSION";

    /**
	 * 签名(数据完整性验证)
	 */
    public static final String SIGN = "TX-SIGN";

    public static String sign(Map<String, String> protocal, Map<String, String> parameter, String secret) throws IOException {
        Map<String, String> sortedParams = new TreeMap<String, String>(protocal);
        sortedParams.putAll(protocal);
        Set<Entry<String, String>> paramSet = sortedParams.entrySet();
        StringBuilder query = new StringBuilder(secret);
        for (Entry<String, String> param : paramSet) {
            if (areNotEmpty(param.getKey(), param.getValue())) {
                query.append(param.getKey()).append(param.getValue());
            }
        }
        MessageDigest md5 = getMd5MessageDigest();
        byte[] bytes = md5.digest(query.toString().getBytes("utf-8"));
        StringBuilder sign = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex.toUpperCase());
        }
        return sign.toString();
    }

    private static MessageDigest getMd5MessageDigest() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * 检查指定的字符串列表是否不为空。
	 * 
	 * @param values 字符串列表
	 * @return true/false
	 */
    public static boolean areNotEmpty(String... values) {
        boolean result = true;
        if (values == null || values.length == 0) {
            result = false;
        } else {
            for (String value : values) {
                result &= !isEmpty(value);
            }
        }
        return result;
    }

    public static boolean isEmpty(String value) {
        int strLen;
        if (value == null || (strLen = value.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(value.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    public static String getAuthOwner() {
        return AUTH_OWNER;
    }

    public static String getAuthApp() {
        return AUTH_APP;
    }

    public static String getAuthSession() {
        return AUTH_SESSION;
    }

    public static String getAuthKey() {
        return AUTH_KEY;
    }

    public static String getTimestamp() {
        return TIMESTAMP;
    }

    public static String getSdk() {
        return SDK;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getSign() {
        return SIGN;
    }
}
