package com.jspx.security;

import java.security.MessageDigest;

public class MD5 {

    /**
     * 转换字节数组为16进制字串
     *
     * @param bytes 字节数组
     * @return 16进制字串
     */
    private static String dumpBytes(byte[] bytes) {
        int i;
        StringBuffer sb = new StringBuffer();
        for (i = 0; i < bytes.length; i++) {
            if (i % 32 == 0 && i != 0) {
                sb.append("\n");
            }
            String s = Integer.toHexString(bytes[i]);
            if (s.length() < 2) {
                s = "0" + s;
            }
            if (s.length() > 2) {
                s = s.substring(s.length() - 2);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String encode(String origin) {
        String resultString = origin;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = dumpBytes(md.digest(resultString.getBytes("UTF-8")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultString.toLowerCase();
    }

    public static void main(String[] args) {
        System.err.println(encode("111111"));
    }
}
