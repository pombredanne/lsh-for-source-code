package com.test.util;

import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.util.Assert;

public class CodeGenerator {

    private static String ALPHANUMERIC_STR;

    static {
        String numberStr = "0123456789";
        String aphaStr = "abcdefghijklmnopqrstuvwxyz";
        ALPHANUMERIC_STR = numberStr + aphaStr + aphaStr.toUpperCase();
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().toUpperCase();
    }

    public static String getSHADigest(String srcStr) {
        return getDigest(srcStr, "SHA-1");
    }

    public static String getMD5Digest(String srcStr) {
        return getDigest(srcStr, "MD5");
    }

    public static String getUpdateKey() {
        return getRandomStr(6);
    }

    public static String getRandomStr(int length) {
        int srcStrLen = ALPHANUMERIC_STR.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; ++i) {
            int maxnum = (int) (Math.random() * 1000.0D);
            int result = maxnum % srcStrLen;
            char temp = ALPHANUMERIC_STR.charAt(result);
            sb.append(temp);
        }
        return sb.toString();
    }

    private static String getDigest(String srcStr, String alg) {
        Assert.notNull(srcStr);
        Assert.notNull(alg);
        try {
            MessageDigest alga = MessageDigest.getInstance(alg);
            alga.update(srcStr.getBytes());
            byte[] digesta = alga.digest();
            return byte2hex(digesta);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String byte2hex(byte[] b) {
        StringBuffer hs = new StringBuffer();
        String stmp = "";
        for (int n = 0; n < b.length; ++n) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            if (stmp.length() == 1) {
                hs.append("0");
            }
            hs.append(stmp);
        }
        return hs.toString().toUpperCase();
    }

    public static void main(String[] args) {
        System.out.println(getUUID());
        System.out.println(getSHADigest("111111"));
        System.out.println(getRandomStr(10));
        System.out.println(getRandomStr(8));
        System.out.println(getUpdateKey());
    }
}
