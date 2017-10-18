package com.macro10.util;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {

    public static String concatStrings(int start, int end, String seperator, Object... strings) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(strings[i]);
            if (i < end - 1) {
                sb.append(seperator);
            }
        }
        return sb.toString();
    }

    public static String concatStrings(int start, String seperator, Object... strings) {
        return concatStrings(start, strings.length, seperator, strings);
    }

    public static String concatString(String seperator, Object... strings) {
        return concatStrings(0, strings.length, seperator, strings);
    }

    public static String hashPassword(String password) {
        String hashStr = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes(Charset.defaultCharset()));
            BigInteger hash = new BigInteger(1, md5.digest());
            hashStr = hash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
        StringBuilder buffer = new StringBuilder(hashStr);
        while (buffer.length() < 32) {
            buffer.insert(0, '0');
        }
        return buffer.toString();
    }
}
