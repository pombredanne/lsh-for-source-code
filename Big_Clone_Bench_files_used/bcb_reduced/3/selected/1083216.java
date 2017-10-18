package org.osmius.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

    public static String byteToBase64(byte[] buf) {
        return new sun.misc.BASE64Encoder().encode(buf);
    }

    public static byte[] base64ToByte(String s) {
        byte[] buf = null;
        try {
            buf = new sun.misc.BASE64Decoder().decodeBuffer(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf;
    }

    public static byte[] getKeyedDigest(byte[] buffer, byte[] key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer);
            return md5.digest(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getMD5(String str) {
        return getMD5(str.getBytes());
    }

    public static String getMD5(byte[] data) {
        MD5 md5 = new MD5();
        md5.Init();
        md5.Update(data);
        return byteToBase64(md5.Final());
    }
}
