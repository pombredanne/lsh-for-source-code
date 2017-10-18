package it.atc.utils;

import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

public class Hash {

    public Hash() {
    }

    public static String doMD5(String s) throws Throwable {
        String s1;
        MessageDigest messagedigest = MessageDigest.getInstance("MD5");
        byte abyte0[] = s.getBytes();
        messagedigest.update(abyte0, 0, s.length());
        byte abyte1[] = messagedigest.digest();
        BASE64Encoder base64encoder = new BASE64Encoder();
        s1 = base64encoder.encode(abyte1);
        return s1;
    }

    public static String doMD5Hex(String s) throws Throwable {
        byte abyte1[];
        MessageDigest messagedigest = MessageDigest.getInstance("MD5");
        byte abyte0[] = s.getBytes();
        messagedigest.update(abyte0, 0, s.length());
        abyte1 = messagedigest.digest();
        return toHexString(abyte1);
    }

    public static String doSHA1(String s) throws Throwable {
        String s1;
        MessageDigest messagedigest = MessageDigest.getInstance("SHA");
        byte abyte0[] = s.getBytes();
        messagedigest.update(abyte0, 0, s.length());
        byte abyte1[] = messagedigest.digest();
        BASE64Encoder base64encoder = new BASE64Encoder();
        s1 = base64encoder.encode(abyte1);
        return s1;
    }

    public static String doSHA1Hex(String s) throws Throwable {
        byte abyte1[];
        MessageDigest messagedigest = MessageDigest.getInstance("SHA");
        byte abyte0[] = s.getBytes();
        messagedigest.update(abyte0, 0, s.length());
        abyte1 = messagedigest.digest();
        return toHexString(abyte1);
    }

    public static byte[] doSHA1Binary(String s) throws Throwable {
        byte abyte1[];
        MessageDigest messagedigest = MessageDigest.getInstance("SHA");
        byte abyte0[] = s.getBytes();
        messagedigest.update(abyte0, 0, s.length());
        abyte1 = messagedigest.digest();
        return abyte1;
    }

    private static String toHexString(byte abyte0[]) {
        StringBuffer stringbuffer = new StringBuffer();
        String as[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        for (int k = 0; k < abyte0.length; k++) {
            int i = (abyte0[k] & 0xf0) >> 4;
            int j = abyte0[k] & 0xf;
            stringbuffer.append(as[i]).append(as[j]);
        }
        return stringbuffer.toString();
    }
}
