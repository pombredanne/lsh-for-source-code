package com.taobao.api.util;

import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @version 2008-12-1
 * @author <a href="mailto:zixue@taobao.com">zixue</a>
 *
 */
public class EncryptUtil {

    /**
	 * 生成有效签名
	 * 
	 * @param params
	 * @param secret
	 * @return
	 */
    public static String signature(Map<String, CharSequence> params, String secret, String signName) {
        String result = null;
        if (params == null) return result;
        params.remove(signName);
        Map<String, CharSequence> treeMap = new TreeMap<String, CharSequence>();
        treeMap.putAll(params);
        Iterator<String> iter = treeMap.keySet().iterator();
        StringBuffer orgin = new StringBuffer(secret);
        while (iter.hasNext()) {
            String name = (String) iter.next();
            orgin.append(name).append(params.get(name));
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            result = byte2hex(md.digest(orgin.toString().getBytes("utf-8")));
        } catch (Exception e) {
            throw new java.lang.RuntimeException("sign error !");
        }
        return result;
    }

    /**
	 * 二行制转字符串
	 * 
	 * @param b
	 * @return
	 */
    private static String byte2hex(byte[] b) {
        StringBuffer hs = new StringBuffer();
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) hs.append("0").append(stmp); else hs.append(stmp);
        }
        return hs.toString().toUpperCase();
    }
}
