package org.i0o.utilplus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 字符串工具类
 * 
 * @author Felix
 */
public class StringUtil {

    private StringUtil() {
    }

    /**
	 * 判断指定字符串在指定字符串数组中的位置
	 * 
	 * @param searchStr 字符串
	 * @param strArray 字符串数组
	 * @param caseInsensetice 是否不区分大小写, true为不区分, false为区分
	 * @return 字符串在指定字符串数组中的位置, 如不存在则返回-1
	 */
    public static int getInArrayId(String searchStr, String[] strArray, boolean caseInsensetice) {
        if (!ValidateUtil.isBlank(searchStr)) {
            for (int i = 0; i < strArray.length; i++) {
                String tmpStr = strArray[i];
                if (!ValidateUtil.isBlank(tmpStr)) {
                    if (caseInsensetice) {
                        if (searchStr.toLowerCase().equals(strArray[i].toLowerCase())) {
                            return i;
                        }
                    } else {
                        if (searchStr.equals(strArray[i])) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }
        return -1;
    }

    /**
	 * 判断指定字符串在指定字符串数组中的位置
	 * 
	 * @param searchStr 字符串
	 * @param strArray 字符串数组
	 * @return 字符串在指定字符串数组中的位置, 如不存在则返回-1
	 */
    public static int getInArrayId(String searchStr, String[] strArray) {
        return getInArrayId(searchStr, strArray, false);
    }

    /**
	 * 分割字符串
	 * 
	 * @param strContent 字符串内容
	 * @param strSplit 分隔符
	 * @return 字符串数组
	 */
    public static String[] splitString(String strContent, String strSplit) {
        if (!ValidateUtil.hasBlank(strContent, strSplit)) {
            if (strContent.indexOf(strSplit) < 0) {
                String[] tmp = { strContent };
                return tmp;
            }
            return strContent.split(strSplit);
        }
        return new String[0];
    }

    /**
	 * 将字符串分割成指定大小的数组
	 * 
	 * @param strContent 字符串内容
	 * @param strSplit 分隔符
	 * @param count 指定数组大小
	 * @return 字符串数组
	 */
    public static String[] splitString(String strContent, String strSplit, int count) {
        String[] result = new String[count];
        String[] splited = splitString(strContent, strSplit);
        for (int i = 0; i < count; i++) {
            if (i < splited.length) {
                result[i] = splited[0];
            } else {
                result[i] = null;
            }
        }
        return result;
    }

    /**
	 * MD5加密字符串
	 * 
	 * @param sourceStr 原始
	 * @return 加密之后字符串
	 */
    public static String md5Encrypt(String sourceStr) {
        byte[] sourceByte = sourceStr.getBytes();
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(sourceByte);
            byte tmp[] = md.digest();
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
