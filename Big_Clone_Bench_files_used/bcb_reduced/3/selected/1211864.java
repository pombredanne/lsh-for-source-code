package org.neblipedia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    public static final char[] HEXADECIMAL = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static byte[] md5(byte[] by) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            return messagedigest.digest(by);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    public static byte[] md5(File archivo) {
        try {
            FileInputStream fis = new FileInputStream(archivo);
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            byte[] buff = new byte[4092];
            int r;
            while ((r = fis.read(buff, 0, 4092)) != -1) {
                messagedigest.update(buff, 0, r);
            }
            return messagedigest.digest();
        } catch (FileNotFoundException e) {
            return new byte[] {};
        } catch (NoSuchAlgorithmException e) {
            return new byte[] {};
        } catch (IOException e) {
            return new byte[] {};
        }
    }

    public static String md5(String txt) {
        return toString(md5(txt.getBytes()));
    }

    public static String toString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int low = (bytes[i] & 0x0f);
            int high = ((bytes[i] & 0xf0) >> 4);
            sb.append(HEXADECIMAL[high]);
            sb.append(HEXADECIMAL[low]);
        }
        return sb.toString();
    }
}
