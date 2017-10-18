package com.openthinks.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A Class which provides some encryption methods
 * @author �ſ���
 *
 */
public class StringEncryption {

    /**
	 * Encrypt the specified string using SHA-1 algorithm
	 * 
	 * @param originalString
	 * @return
	 */
    public static String SHAEncrypt(String originalString) {
        String encryptedString = new String("");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(originalString.getBytes());
            byte b[] = md.digest();
            for (int i = 0; i < b.length; i++) {
                char[] digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
                char[] ob = new char[2];
                ob[0] = digit[(b[i] >>> 4) & 0X0F];
                ob[1] = digit[b[i] & 0X0F];
                encryptedString += new String(ob);
            }
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("the algorithm doesn't exist");
        }
        return encryptedString;
    }

    /**
	 * Encrypt the specified string using MD5 algorithm
	 * 
	 * @param OriginalString
	 * @return
	 */
    public static String MD5Encrypt(String OriginalString) {
        String encryptedString = new String("");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(OriginalString.getBytes());
            byte b[] = md.digest();
            for (int i = 0; i < b.length; i++) {
                char[] digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
                char[] ob = new char[2];
                ob[0] = digit[(b[i] >>> 4) & 0X0F];
                ob[1] = digit[b[i] & 0X0F];
                encryptedString += new String(ob);
            }
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("the algorithm doesn't exist");
        }
        return encryptedString;
    }

    public static void main(String args[]) {
        String test = "zhangjunlong";
        System.out.println("the original string is:" + test);
        System.out.println("encrypted by SHA-1 we get:" + SHAEncrypt(test));
        System.out.println("encrypted by MD5 we get:" + MD5Encrypt(test));
    }
}
