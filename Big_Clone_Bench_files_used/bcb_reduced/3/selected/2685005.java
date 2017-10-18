package com.coyou.admailreg.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * GUID生成器
 * 
 * @author 贾楠
 */
public class GUIDGenerator extends Object {

    private static Random myRand;

    private static SecureRandom mySecureRand;

    private static String s_id;

    static {
        mySecureRand = new SecureRandom();
        long secureInitializer = mySecureRand.nextLong();
        myRand = new Random(secureInitializer);
        try {
            s_id = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
    * 缺省构造器
    */
    public GUIDGenerator() {
    }

    /**
    * 缺省的产生随机GUID的方法
    * @return 正确返回32字节的字符串，错误则返回长度为零的字符串
    */
    public static String genRandomGUID() {
        return genRandomGUID(false);
    }

    /**
    * 产生随机GUID的方法,考虑产生GUID的效率，将来可以应用设计模式，先生成一堆id并缓存
    * @param secure true  : 带安全选项，用安全随机数对象生成
    *               false : 不带安全选项，用基本随机数对象生成
    * @return 正确返回32字节的字符串，错误则返回长度为零的字符串
    */
    private static String genRandomGUID(boolean secure) {
        String valueBeforeMD5 = "";
        String valueAfterMD5 = "";
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: " + e);
            return valueBeforeMD5;
        }
        long time = System.currentTimeMillis();
        long rand = 0;
        if (secure) {
            rand = mySecureRand.nextLong();
        } else {
            rand = myRand.nextLong();
        }
        sbValueBeforeMD5.append(s_id);
        sbValueBeforeMD5.append(":");
        sbValueBeforeMD5.append(Long.toString(time));
        sbValueBeforeMD5.append(":");
        sbValueBeforeMD5.append(Long.toString(rand));
        valueBeforeMD5 = sbValueBeforeMD5.toString();
        md5.update(valueBeforeMD5.getBytes());
        byte[] array = md5.digest();
        String strTemp = "";
        for (int i = 0; i < array.length; i++) {
            strTemp = (Integer.toHexString(array[i] & 0XFF));
            if (strTemp.length() == 1) {
                valueAfterMD5 = valueAfterMD5 + "0" + strTemp;
            } else {
                valueAfterMD5 = valueAfterMD5 + strTemp;
            }
        }
        return valueAfterMD5.toUpperCase();
    }

    public static void main(String args[]) {
        for (int i = 1; i < 10; i++) {
            System.out.println(Integer.toString(i) + " : " + genRandomGUID());
        }
    }
}
