package org.soda.dpws.addressing;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * @author pdaum
 * 
 */
public class RandomGUID extends Object {

    private String valueAfterMD5;

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
            throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
   * 
   */
    public RandomGUID() {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
        }
        try {
            long time = System.currentTimeMillis();
            long rand = 0;
            rand = myRand.nextLong();
            StringBuffer sb = new StringBuffer();
            sb.append(s_id);
            sb.append(":");
            sb.append(Long.toString(time));
            sb.append(":");
            sb.append(Long.toString(rand));
            md5.update(sb.toString().getBytes());
            byte[] array = md5.digest();
            sb.setLength(0);
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            valueAfterMD5 = sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public String toString() {
        String raw = valueAfterMD5.toLowerCase();
        StringBuffer sb = new StringBuffer();
        sb.append(raw.substring(0, 8));
        sb.append("-");
        sb.append(raw.substring(8, 12));
        sb.append("-");
        sb.append(raw.substring(12, 16));
        sb.append("-");
        sb.append(raw.substring(16, 20));
        sb.append("-");
        sb.append(raw.substring(20));
        return sb.toString();
    }
}
