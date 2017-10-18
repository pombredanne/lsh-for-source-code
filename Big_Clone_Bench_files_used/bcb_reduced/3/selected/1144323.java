package cn._2dland.uploader.utils;

import java.security.MessageDigest;

public class EncodeUtils {

    private static final String HEXSTRING = "0123456789abcdef";

    /**
	 * 计算MD5值，并返回字节流
	 * 
	 * @param data
	 * @return
	 */
    public static byte[] md5(byte[] data) {
        byte[] md5buf = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            md5buf = md5.digest(data);
        } catch (Exception e) {
            md5buf = null;
            e.printStackTrace(System.err);
        }
        return md5buf;
    }

    /**
	 * 计算MD5，并返回Hex字符串
	 * 
	 * @param data
	 * @return
	 */
    public static String hexMD5(byte[] data) {
        StringBuffer sb = new StringBuffer();
        byte[] buf = md5(data);
        if (buf != null) {
            for (int i = 0; i < buf.length; i++) {
                sb.append(byte2Hex(buf[i]));
            }
        }
        return sb.toString();
    }

    /**
	 * 计算SHA1，并返回字节流
	 * @param data
	 * @return
	 */
    public static byte[] sha1(byte[] data) {
        byte[] buf = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("sha1");
            buf = md5.digest(data);
        } catch (Exception e) {
            buf = null;
            e.printStackTrace(System.err);
        }
        return buf;
    }

    /**
	 * 计算SHA1，并返回Hex字符串
	 * 
	 * @param data
	 * @return
	 */
    public static String hexSHA1(byte[] data) {
        StringBuffer sb = new StringBuffer();
        byte[] buf = sha1(data);
        if (buf != null) {
            for (int i = 0; i < buf.length; i++) {
                sb.append(byte2Hex(buf[i]));
            }
        }
        return sb.toString();
    }

    /**
	 * 计算SHA1，并返回Hex字符串
	 * 
	 * @param data
	 * @return
	 */
    public static String hexSHA1(String data) {
        return hexSHA1(data.getBytes());
    }

    /**
	 * 将byte转换为Hex字符串
	 * 
	 * @param b
	 * @return
	 */
    private static String byte2Hex(byte b) {
        StringBuffer buf = new StringBuffer();
        buf.append(HEXSTRING.charAt((b & 0xf0) >> 4)).append(HEXSTRING.charAt(b & 0x0f));
        return buf.toString();
    }
}
