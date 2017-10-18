package phex;

import java.util.*;
import java.security.*;

public class Digest {

    static Random sRandom = new Random(System.currentTimeMillis());

    public static byte[] computeDigest(String str) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            ServiceManager.log(e);
            byte[] data = new byte[4];
            data[0] = 0;
            data[1] = 0;
            data[2] = 0;
            data[3] = 0;
            return data;
        }
        byte[] data = new byte[str.length() << 1];
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            data[(i << 1)] = (byte) (ch & 0xFF);
            data[(i << 1) + 1] = (byte) ((ch >> 8) & 0xFF);
        }
        md.update(data);
        return md.digest();
    }

    public static byte[] computePasswordDigest(String str, byte[] key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            ServiceManager.log(e);
            byte[] data = new byte[16];
            return data;
        }
        byte[] data = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            data[i] = (byte) str.charAt(i);
        }
        md.update(data);
        md.update(key);
        return md.digest();
    }

    public static int foldBytesToInt(byte[] digest) {
        int result = 0;
        for (int i = 0; i < digest.length; i += 4) {
            int temp = (((int) digest[i]) << 24) | (((int) digest[i + 1]) << 16) | (((int) digest[i + 2]) << 8) | (((int) digest[i + 3]));
            if (result == 0) result = temp; else result ^= temp;
        }
        return result;
    }

    public static byte[] generateChallengeKey() {
        byte[] key = new byte[16];
        for (int i = 0; i < key.length; i++) {
            int n = sRandom.nextInt();
            key[i] = (byte) (((n) & 0xFF) ^ ((n >> 8) & 0xFF) ^ ((n >> 16) & 0xFF) ^ ((n >> 24) & 0xFF));
        }
        return key;
    }
}
