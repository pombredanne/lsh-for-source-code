package ru.adv.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Генератор уникальных идентификаторов.
 * @version $Revision: 1.6 $
 */
public class UniqId {

    private UniqId() {
    }

    private static SecureRandom seeder;

    private static ByteBuffer midValue = new ByteBuffer();

    static {
        byte[] bytes;
        try {
            InetAddress inet = InetAddress.getLocalHost();
            bytes = inet.getAddress();
        } catch (java.net.UnknownHostException e) {
            bytes = new byte[4];
        }
        midValue.append(bytes);
        midValue.append(System.identityHashCode(new Object()));
        try {
            byte[] buff = new byte[10];
            BufferedInputStream is = new BufferedInputStream(new FileInputStream("/dev/urandom"));
            Stream.nonBlockRead(is, buff);
            is.close();
            seeder = new SecureRandom(buff);
        } catch (Exception e) {
            seeder = new SecureRandom();
        }
        seeder.nextInt();
    }

    private static ByteBuffer getByteBuffer() {
        long timeNow = System.currentTimeMillis();
        int timeLow = (int) (timeNow & 0xFFFFFFFFL);
        int node = seeder.nextInt();
        return new ByteBuffer(16).append(timeLow).append(midValue).append(node);
    }

    public static String getUUID() {
        return getByteBuffer().toString(16);
    }

    public static String get() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new ByteBuffer(md.digest(getByteBuffer().getBytes()), false).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e.getMessage());
        }
    }

    public static void main(String[] argv) {
        long count = 1;
        if (argv.length > 0) {
            count = Long.parseLong(argv[0]);
        }
        for (long i = 0; i < count; i++) {
            System.out.println(get());
        }
    }
}
