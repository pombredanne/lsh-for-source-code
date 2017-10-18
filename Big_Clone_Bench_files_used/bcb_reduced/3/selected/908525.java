package org.privasphere.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PureMD5Crypt {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        byte[] plaintextBytes = null;
        byte[] buff = new byte[50];
        MessageDigest md = null;
        md = MessageDigest.getInstance("MD5");
        System.out.println();
        System.out.println("INFO:");
        System.out.println("md.getAlgorithm() -> " + md.getAlgorithm());
        System.out.println("md.getDigestLength() -> " + md.getDigestLength());
        System.out.println("md.getProvider() -> " + md.getProvider());
        System.out.println("md.getClass().getName() -> " + md.getClass().getName());
        String line = null;
        while (true) {
            System.out.println();
            System.out.println("Enter text ( or * if you want to quit ):");
            line = readLn(1024);
            if (line == null) break;
            line = line.trim();
            if (line.length() == 1 && line.charAt(0) == '*') break;
            plaintextBytes = line.getBytes();
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(plaintextBytes);
            buff = md.digest();
            System.out.println();
            System.out.println("MD5 signature as plaintext:");
            System.out.println(new String(buff));
            System.out.println("MD5 signature as HEX string:");
            printHexString(buff);
        }
    }

    public static void printHexString(byte[] buff) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < buff.length; i++) {
            String nextByteString = Integer.toHexString(0xFF & buff[i]);
            if (nextByteString.length() < 2) nextByteString = "0" + nextByteString;
            hexString.append(nextByteString);
            hexString.append("|");
        }
        System.out.println(hexString.toString());
    }

    public static String readLn(int maxLg) {
        byte lin[] = new byte[maxLg];
        int lg = 0, car = -1;
        try {
            while (lg < maxLg) {
                car = System.in.read();
                if ((car < 0) || (car == '\n')) break;
                lin[lg++] += car;
            }
        } catch (java.io.IOException e) {
            return (null);
        }
        if ((car < 0) && (lg == 0)) return (null);
        return (new String(lin, 0, lg));
    }
}
