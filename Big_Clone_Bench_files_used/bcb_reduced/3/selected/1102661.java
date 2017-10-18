package com.sax.michael.annotations.cm.method;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * 加密器
 * Encrypt Passwords
 * @author Michael.yang
 * @dateTime 2011-5-25
 * @E-mail apac.yang@gmail.com
 */
class Encryption {

    private static final int ENCRYPTKEY = 0xFF;

    /**
	 * 加密MD5
	 * @param password
	 * @return encryptPassword
	 */
    static String encryptPassword(String password) {
        MessageDigest md5 = null;
        StringBuffer cacheChar = new StringBuffer();
        try {
            byte defaultByte[] = password.getBytes();
            md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(defaultByte);
            byte resultByte[] = md5.digest();
            for (int i = 0; i < resultByte.length; i++) {
                String hex = Integer.toHexString(ENCRYPTKEY & resultByte[i]);
                if (hex.length() == 1) {
                    cacheChar.append("c");
                }
                cacheChar.append(hex);
            }
            return cacheChar.toString();
        } catch (Exception e) {
            System.out.println("Encrypt Passwords Failed");
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * 
	 * @param sourcePass
	 * @param resultPass
	 * @return if sourcePass.equals(resultPass) return true else false;
	 */
    static boolean comparePassword(String sourcePass, String resultPass) {
        byte[] expectedBytes = null, actualBytes = null;
        try {
            expectedBytes = sourcePass.getBytes("UTF-8");
            actualBytes = resultPass.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("[ ExceptionMess: Can not get the character byte ]");
            e.printStackTrace();
        }
        int expectedLength = expectedBytes == null ? -1 : expectedBytes.length;
        int actualLength = actualBytes == null ? -1 : actualBytes.length;
        if (expectedLength != actualLength) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedLength; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private Encryption() {
    }
}
