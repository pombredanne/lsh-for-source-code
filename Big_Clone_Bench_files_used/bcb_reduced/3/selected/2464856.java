package com.jandan.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

public class EncryptUtil {

    public static final String KEY_SHA = "SHA";

    /**
	 * BASE64加密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
    public static String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key);
    }

    /**
	 * SHA加密
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
    public static byte[] encryptSHA(byte[] data) throws Exception {
        MessageDigest sha = MessageDigest.getInstance(KEY_SHA);
        sha.update(data);
        return sha.digest();
    }

    public static String encryptPassword(String password) throws Exception {
        BigInteger sha = new BigInteger(EncryptUtil.encryptSHA(password.getBytes()));
        return sha.toString(32);
    }
}
