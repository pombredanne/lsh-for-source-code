package org.jxstar.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jxstar.util.StringUtil;

/**
 * 密码生成类。
 *
 * @author TonyTan
 * @version 1.0, 2010-11-23
 */
public class Password {

    private static final String KEYVALUE = "QIAOLING";

    private static final byte[] MYKEY = { 12, 3, 2, 13, 6, 3, 2, 14, 9, 2, 8, 5 };

    /**
	 * 生成密码值
	 * @param text -- 明文值
	 * @return
	 */
    public static String md5(String text) {
        if (text == null || text.length() == 0) return "";
        byte[] src = text.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(src);
            byte[] bytes = md.digest(KEYVALUE.getBytes());
            return StringUtil.byte2hex(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * 自定义加密字符串
	 * @param text
	 * @return
	 */
    public static String encrypt(String text) {
        if (text == null || text.length() == 0) return "";
        byte[] bytes = text.getBytes();
        StringBuilder ret = new StringBuilder();
        for (int i = 0, n = bytes.length; i < n; i++) {
            int ib = (int) bytes[i];
            if (i < MYKEY.length) {
                ib += MYKEY[i];
            }
            int val = ib & 0xff;
            if (val < 16) ret.append("0");
            ret.append(Integer.toHexString(val));
        }
        return ret.toString().toUpperCase();
    }

    /**
	 * 自定义解密字符串
	 * @param text
	 * @return
	 */
    public static String decrypt(String text) {
        if (text == null || text.length() == 0) return "";
        char[] chars = text.toCharArray();
        StringBuilder ret = new StringBuilder();
        for (int i = 0, n = chars.length / 2; i < n; i++) {
            StringBuilder val = new StringBuilder();
            val.append(chars[i * 2]);
            val.append(chars[i * 2 + 1]);
            int ib = Integer.parseInt(val.toString(), 16);
            if (i < MYKEY.length) {
                ib -= MYKEY[i];
            }
            ret.append((char) ib);
        }
        return ret.toString();
    }
}
