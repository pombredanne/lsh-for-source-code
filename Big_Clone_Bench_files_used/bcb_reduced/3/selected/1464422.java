package com.kongur.network.erp.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Encoder;

/**
 * @author gaojf
 * @version $Id: TopUtil.java,v 0.1 2012-1-4 ����03:22:25 gaojf Exp $
 */
public class TopUtil {

    private static Log log = LogFactory.getLog(TopUtil.class);

    /**
     * Checkstyle rule: utility classes should not have public constructor
     */
    public TopUtil() {
    }

    /**
     * ���Refresh Token����sessionʱ��URL
     * @param topAppKey
     * @param refreshToken
     * @param topSessionKey
     * @param appSecret
     * @param refreshUrl
     * @return
     */
    public static String getRefreshUrl(String topAppKey, String refreshToken, String topSessionKey, String appSecret, String refreshUrl) {
        String paramsString = "appkey" + topAppKey + "refresh_token" + refreshToken + "sessionkey" + topSessionKey;
        String rUrl = null;
        try {
            String sign = DigestUtils.md5Hex((paramsString + appSecret).getBytes("utf-8")).toUpperCase();
            if (log.isDebugEnabled()) {
                log.debug("sign:" + sign);
            }
            String signEncoder = URLEncoder.encode(sign, "utf-8");
            String appkeyEncoder = URLEncoder.encode(topAppKey, "utf-8");
            String refreshTokenEncoder = URLEncoder.encode(refreshToken, "utf-8");
            String sessionkeyEncoder = URLEncoder.encode(topSessionKey, "utf-8");
            rUrl = refreshUrl.replace("{appkey}", appkeyEncoder).replace("{sessionkey}", sessionkeyEncoder).replace("{refreshtoken}", refreshTokenEncoder).replace("{sign}", signEncoder);
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return rUrl;
    }

    /**
     * �Ѿ���BASE64������ַ�ת��ΪMap����
     * @param str
     * @return
     * @throws Exception
     */
    public static Map<String, String> convertBase64StringtoMap(String str) {
        if (str == null) return null;
        String keyvalues = null;
        try {
            keyvalues = new String(Base64.decodeBase64(URLDecoder.decode(str, "utf-8").getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] keyvalueArray = keyvalues.split("\\&");
        Map<String, String> map = new HashMap<String, String>();
        for (String keyvalue : keyvalueArray) {
            String[] s = keyvalue.split("\\=");
            if (s == null || s.length != 2) return null;
            map.put(s[0], s[1]);
        }
        return map;
    }

    /**
     * ��֤ǩ��
     * @param sign
     * @param parameter
     * @param secret
     * @return
     */
    public static boolean validateSign(String sign, String parameter, String secret) {
        return sign != null && parameter != null && secret != null && sign.equals(sign(parameter, secret));
    }

    /**
     * top����ǩ������
     * @param parameter
     * @param secret
     * @return
     */
    public static String sign(String parameter, String secret) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }
        byte[] digest = null;
        try {
            digest = md.digest((parameter + secret).getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
        BASE64Encoder encode = new BASE64Encoder();
        String a = encode.encode(digest);
        return a;
    }
}
