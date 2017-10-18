package com.simpledata.util;

import java.io.*;
import java.security.*;

/**
* Some data manipulation tools usefull for Licensing schemas
*
*/
public class LicenseUtils {

    public static String z = "7G3FVDJAY9K2T8QUEXBL6W4Z5CNIMHPS1R";

    /**
	* Byte to readable chars
	*/
    public static String byteToChars(byte[] buff) {
        String res = "";
        for (int l = 0; l < buff.length; l++) {
            int i = buff[l] < 0 ? 256 + buff[l] : buff[l];
            res += z.charAt((i * z.length() / 256));
        }
        return res;
    }

    /**
	* return decimal value of a char (according to position in Z)
	*/
    public static int charToDec(char c) {
        return z.indexOf(c);
    }

    /**
	* return char corresponding to this Dec Value (modulo z ) (according to position in Z)
	*/
    public static char decToChar(int i) {
        while (i < 0) i = i + z.length();
        while (i >= z.length()) i = i - z.length();
        return z.charAt(i);
    }

    /**
	* get an hashcode from a String
	*/
    public static String mdSimple(String str, int length) {
        try {
            return mdSimple(str.getBytes("ISO-8859-1"), length);
        } catch (UnsupportedEncodingException e) {
        }
        return "";
    }

    /**
	* get an hashcode from an array of byte
	*/
    public static String mdSimple(byte[] array, int length) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        digest.update(array, 0, array.length);
        byte[] md5 = digest.digest();
        String res = byteToChars(md5);
        return res.substring(0, length);
    }

    /**
	* Mix Keys
	*/
    public static String mixKey(String keyComputer, String keyCompany) {
        String tail = "";
        for (int i = 0; i < keyComputer.length(); i++) {
            tail = tail + decToChar(charToDec(keyComputer.charAt(i)) + charToDec(keyCompany.charAt(i)));
        }
        return keyComputer + "" + tail;
    }

    /**
	* Un Mix Keys
	*/
    public static String[] unMixKey(String key) {
        String[] keys = new String[2];
        int keyLength = key.length() / 2;
        int expectedLength = key.length();
        keys[0] = key.substring(0, keyLength);
        keys[1] = "";
        String tail = key.substring(keyLength, expectedLength);
        for (int i = 0; i < keyLength; i++) {
            keys[1] = keys[1] + decToChar(charToDec(tail.charAt(i)) - charToDec(keys[0].charAt(i)));
        }
        return keys;
    }

    /**
	* AddCheckSum to code. 
	*/
    public static String addCheckSum(String key) {
        char[] keyc = key.toCharArray();
        int zz = 0;
        for (int l = 0; l < keyc.length; l++) {
            zz += charToDec(keyc[l]);
        }
        char sum = decToChar(zz);
        return key + sum;
    }

    /**
	* RemoveCheckSum from code .. return null if code invalid
	*/
    public static String removeCheckSum(String key) {
        char[] keyc = key.toCharArray();
        String res = "";
        int zz = 0;
        for (int l = 0; l < (keyc.length - 1); l++) {
            zz += charToDec(keyc[l]);
            res += keyc[l];
        }
        char sum = decToChar(zz);
        if (sum != keyc[keyc.length - 1]) {
            return null;
        }
        return res;
    }
}
