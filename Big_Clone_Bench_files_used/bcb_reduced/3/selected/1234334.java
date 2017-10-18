package com.hetaoblog.util;

import java.security.MessageDigest;

public class HetaoblogMd5Util {

    private static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String hardsalt = "asdfW2jLz$2kw";

    public static String encodePassword(String id, String password) {
        if ((null == id) || (null == password) || ("".equalsIgnoreCase(id.trim())) || ("".equalsIgnoreCase(password))) {
            return null;
        }
        String key = id.trim().toUpperCase() + hardsalt + password;
        String enKey = encodeMd5(key);
        enKey = password + enKey + hardsalt;
        String result = encodeMd5(enKey);
        return result;
    }

    private static String encodeMd5(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(key.getBytes());
            byte[] bytes = md.digest();
            String result = toHexString(bytes);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String toHexString(byte[] b) {
        StringBuffer buf = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; ++i) {
            buf.append(hexDigits[(b[i] & 0xf0) >>> 4]);
            buf.append(hexDigits[b[i] & 0x0f]);
        }
        return buf.toString();
    }
}
