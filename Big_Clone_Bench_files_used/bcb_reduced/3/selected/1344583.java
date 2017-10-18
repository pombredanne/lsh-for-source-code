package net.rsapollo.security;

import java.security.*;
import java.util.StringTokenizer;
import java.io.*;

public class MD5 {

    private static String getString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            sb.append((int) (0x00FF & b));
            if (i + 1 < bytes.length) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    private static byte[] getBytes(String str) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StringTokenizer st = new StringTokenizer(str, "-", false);
        while (st.hasMoreTokens()) {
            int i = Integer.parseInt(st.nextToken());
            bos.write((byte) i);
        }
        return bos.toByteArray();
    }

    static String getHexString(byte byteValues[]) {
        byte singleChar = 0;
        if (byteValues == null || byteValues.length <= 0) return null;
        String entries[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
        StringBuffer out = new StringBuffer(byteValues.length * 2);
        for (int i = 0; i < byteValues.length; i++) {
            singleChar = (byte) (byteValues[i] & 0xF0);
            singleChar = (byte) (singleChar >>> 4);
            singleChar = (byte) (singleChar & 0x0F);
            out.append(entries[(int) singleChar]);
            singleChar = (byte) (byteValues[i] & 0x0F);
            out.append(entries[(int) singleChar]);
        }
        String rslt = new String(out);
        return rslt;
    }

    public static String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(source.getBytes());
            return getHexString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean compare(String source, String md5string) {
        return md5(source) == md5string;
    }
}
