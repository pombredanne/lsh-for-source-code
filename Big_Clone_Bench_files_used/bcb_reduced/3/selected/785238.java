package com.bird.util;

import java.net.*;
import java.util.*;
import java.security.*;

/**
 * @see Globals Unique Identifiers 
 *  ȫ��ͳһ��ʶ��
 *  
 */
public class RandomGUID extends Object {

    public String valueBeforeMD5 = "";

    public String valueAfterMD5 = "";

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

    public RandomGUID() {
        getRandomGUID(false);
    }

    public RandomGUID(boolean secure) {
        getRandomGUID(secure);
    }

    private void getRandomGUID(boolean secure) {
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: " + e);
        }
        try {
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
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            valueAfterMD5 = sb.toString();
        } catch (Exception e) {
            System.out.println("Error:" + e);
        }
    }

    public String toString() {
        String raw = valueAfterMD5.toUpperCase().substring(0, 27);
        return raw;
    }

    /**
     * Use Exceple 
     * @throws UnknownHostException   
     *  ��ݵ�ǰʱ��ͱ������������ GUID
     */
    public static void main(String args[]) throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost().toString());
        for (int i = 0; i < 100; i++) {
            RandomGUID myGUID = new RandomGUID();
            System.out.println("Seeding String=" + myGUID.valueBeforeMD5);
            System.out.println("rawGUID=" + myGUID.valueAfterMD5);
            System.out.println("RandomGUID=" + myGUID.toString());
        }
    }
}
