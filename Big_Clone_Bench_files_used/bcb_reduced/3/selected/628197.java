package cn.myapps.alisoft.util;

import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * 服务请求辅助类
 * @author Chris
 *
 */
public class SignatureUtil {

    /**
     * 签名方法
     * @param params
     * @param secret
     * @return
     */
    public static String Signature(Map params, String secret) {
        String result = null;
        params.remove(Constants.PARAMETER_SIGN);
        try {
            Map treeMap = new TreeMap();
            treeMap.putAll(params);
            Iterator iter = treeMap.keySet().iterator();
            StringBuffer orgin = new StringBuffer(secret);
            while (iter.hasNext()) {
                String name = (String) iter.next();
                orgin.append(name).append(params.get(name));
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            result = byte2hex(md.digest(orgin.toString().getBytes("utf-8")));
        } catch (Exception ex) {
            throw new java.lang.RuntimeException("sign error !");
        }
        return result;
    }

    /**
     *
     * 二行制转字符串
     *
     * @param b
     *
     * @return
     *
     */
    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) hs = hs + "0" + stmp; else hs = hs + stmp;
        }
        return hs.toUpperCase();
    }
}
