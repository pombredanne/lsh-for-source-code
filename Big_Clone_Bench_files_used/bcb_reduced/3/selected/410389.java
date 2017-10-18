package org.paradise.dms.web.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * Description:md5摘要
 * 
 * Copyright (c) 2008-2009 paraDise sTudio(DT). All Rights Reserved.
 * 
 * @version 1.0 2009-3-18 上午02:08:28 李双江（paradise.lsj@gmail.com）created
 */
public class Md5Utils {

    /** * 工具类使用的protected构造方法. */
    protected Md5Utils() {
    }

    /**
	 * 为字符串生成md5摘要.
	 * 
	 * @param s
	 *            输入字符串
	 * @return 生成的摘要内容
	 * @throws NoSuchAlgorithmException
	 *             如果不支持md5算法就抛出异常
	 */
    public static final String md5(String s) throws NoSuchAlgorithmException {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        byte[] strTemp = s.getBytes();
        MessageDigest mdTemp = MessageDigest.getInstance("MD5");
        mdTemp.update(strTemp);
        byte[] md = mdTemp.digest();
        int j = md.length;
        char[] str = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }
}
