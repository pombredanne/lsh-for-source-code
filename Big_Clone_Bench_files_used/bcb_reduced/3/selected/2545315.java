package com.jimsproch.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import com.jimsproch.data.HexUtilities;

/**
 * Demonstrates MD5 Checksums
 * @author Jim Sproch
 */
public class MD5 {

    public static void main(String[] args) {
        String message = "The quick brown fox jumps over the lazy dog";
        String hash = hashMessage(message);
        System.out.println(hash + " => " + message);
    }

    public static String hashMessage(String message) {
        try {
            return HexUtilities.fromBytes(hashData(message.getBytes("UTF8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hashData(byte[] bytes) {
        try {
            return MessageDigest.getInstance("MD5").digest(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
