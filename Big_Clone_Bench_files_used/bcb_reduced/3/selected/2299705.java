package com.jot.system.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;
import sun.misc.BASE64Encoder;

public class Crypto {

    private static Random rand = new Random();

    private static byte[] getStartingValue() {
        StringBuffer dest = new StringBuffer();
        dest.append(rand.nextLong());
        dest.append(rand.nextLong());
        dest.append(new Date().getTime());
        dest.append(JotTime.get());
        dest.append("secrfet phrase 89jkbn This should be unguessable. Wibble friggle wobble frack");
        return dest.toString().getBytes();
    }

    public static ThreadLocal<MessageDigest> messageDigestThreadLocal = new ThreadLocal<MessageDigest>() {

        protected synchronized MessageDigest initialValue() {
            MessageDigest tmpmessagedigest = null;
            try {
                tmpmessagedigest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return tmpmessagedigest;
        }
    };

    private static byte[] lastRandomBytes = getStartingValue();

    public static void inject(byte[] bytes) {
    }

    public static String SHA1_Base64(byte[] source) {
        MessageDigest digest = messageDigestThreadLocal.get();
        digest.reset();
        digest.update(source);
        byte[] key = digest.digest();
        BASE64Encoder en = new BASE64Encoder();
        String res = en.encode(key);
        return res;
    }

    public static byte[] SHA1(byte[] source) {
        MessageDigest digest = messageDigestThreadLocal.get();
        digest.reset();
        digest.update(source);
        byte[] key = digest.digest();
        return key;
    }

    public static byte[] createSecretKeyBytes() {
        MessageDigest digest = messageDigestThreadLocal.get();
        digest.reset();
        digest.update(lastRandomBytes);
        int time = (int) System.currentTimeMillis();
        digest.update((byte) time);
        digest.update((byte) (time >> 8));
        digest.update((byte) (time >> 16));
        digest.update((byte) (time >> 24));
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        digest.update((byte) rand.nextInt());
        byte[] key = digest.digest();
        lastRandomBytes = key;
        return key;
    }

    public static String createSecretKeyString() {
        byte[] bytes = createSecretKeyBytes();
        BASE64Encoder en = new BASE64Encoder();
        String res = en.encode(bytes);
        return res;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            String test = createSecretKeyString();
            String test2 = createSecretKeyString();
            assert !test.equals(test2);
        }
        {
            String test = SHA1_Base64("a test string".getBytes());
            String test2 = SHA1_Base64("a test string".getBytes());
            assert test.equals(test2);
        }
    }
}
