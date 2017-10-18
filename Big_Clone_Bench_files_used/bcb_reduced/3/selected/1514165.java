package com.sns2Life.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ��ݼ��ܽ���ʵ����
 * 
 * @author
 * @version v1.0.0
 */
public class MDEncryption {

    private static Log log = LogFactory.getLog(MDEncryption.class);

    public byte[] encryptMsg(String encryptString) {
        byte[] encryptByte = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(encryptString.getBytes());
            encryptByte = messageDigest.digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return encryptByte;
    }

    public String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) hs = hs + "0" + stmp; else hs = hs + stmp;
            if (n < (b.length - 1)) hs = hs + ":";
        }
        return hs.toUpperCase();
    }

    private char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
	 * Transform <code>byte</code> array to <code>String</code>.
	 * 
	 * @param ba the <code>byte</code> array to transform to string.
	 * @return the transformed string from specified <code>byte</code> array.
	 */
    public String byte2String(byte ba[]) {
        int length = ba.length;
        char buf[] = new char[length * 2];
        int i = 0;
        int j = 0;
        while (i < length) {
            int k = ba[i++];
            buf[j++] = HEX_DIGITS[k >>> 4 & 0xf];
            buf[j++] = HEX_DIGITS[k & 0xf];
        }
        return new String(buf);
    }

    /**
	 * Transform <code>String</code> to <code>byte</code> array.
	 * 
	 * @param hex the string to transform.
	 * @return the transformed <code>byte</code> array from specified string.
	 */
    public byte[] string2Byte(String hex) {
        int len = hex.length();
        byte buf[] = new byte[(len + 1) / 2];
        int i = 0;
        int j = 0;
        if (len % 2 == 1) buf[j++] = (byte) fromDigit(hex.charAt(i++));
        while (i < len) buf[j++] = (byte) (fromDigit(hex.charAt(i++)) << 4 | fromDigit(hex.charAt(i++)));
        return buf;
    }

    private static int fromDigit(char ch) {
        if (ch >= '0' && ch <= '9') return ch - 48;
        if (ch >= 'A' && ch <= 'F') return (ch - 65) + 10;
        if (ch >= 'a' && ch <= 'f') return (ch - 97) + 10; else return 0;
    }

    public static boolean isEqual(byte[] bytes1, byte[] bytes2) {
        return MessageDigest.isEqual(bytes1, bytes2);
    }

    /**
	 * ��ֵ����MD�Ĳ���
	 * 
	 * @param _value String ��ҪMD5ֵ
	 * @return ����MD��Ľ��
	 * @throws Exception
	 */
    public static String mdString(String _value) throws Exception {
        StringBuffer hexValue = new StringBuffer();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        String value = format.format(date);
        if (_value != null) {
            value += _value;
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte b[] = md.digest(value.getBytes());
        for (int i = 0; i < b.length; i++) {
            int val = ((int) b[i]) & 0xff;
            if (val < 16) hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static String encodeMD5(String msg) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte msgBytes[] = msg.getBytes();
            md.update(msgBytes);
            digest = Base64.encodeBytes(md.digest());
        } catch (Exception e) {
            log.error(e);
        }
        return digest;
    }
}
