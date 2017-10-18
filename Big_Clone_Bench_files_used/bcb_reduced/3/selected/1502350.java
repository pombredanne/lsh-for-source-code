package com.tx.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5工具类
 * @author Crane
 *
 */
public class MD5 {

    /**
	 * 加密为MD5
	 * 
	 * @param source
	 * @return
	 */
    public static String encoder(String source) {
        byte[] md5Bytes = encoderForBytes(source);
        if (md5Bytes != null) {
            StringBuffer hexValue = new StringBuffer();
            for (int i = 0; i < md5Bytes.length; i++) {
                int val = ((int) md5Bytes[i]) & 0xff;
                if (val < 16) hexValue.append("0");
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString().toUpperCase();
        }
        return null;
    }

    /**
	 * 加密为MD5
	 * 
	 * @param source
	 * @return
	 */
    public static byte[] encoderForBytes(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            char[] charArray = source.toCharArray();
            byte[] byteArray = new byte[charArray.length];
            for (int i = 0; i < charArray.length; i++) byteArray[i] = (byte) charArray[i];
            return digest.digest(byteArray);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
