package org.bigk.invoices.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static final char kHexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private DigestUtils() {
    }

    public static String stringToHex(String s) {
        byte stringBytes[] = s.getBytes();
        return bufferToHex(stringBytes);
    }

    public static String bufferToHex(byte buffer[]) {
        return bufferToHex(buffer, 0, buffer.length);
    }

    public static String bufferToHex(byte buffer[], int startOffset, int length) {
        StringBuffer hexString = new StringBuffer(2 * length);
        int endOffset = startOffset + length;
        for (int i = startOffset; i < endOffset; i++) appendHexPair(buffer[i], hexString);
        return hexString.toString();
    }

    public static String hexToString(String hexString) throws NumberFormatException {
        byte bytes[] = hexToBuffer(hexString);
        return new String(bytes);
    }

    public static byte[] hexToBuffer(String hexString) throws NumberFormatException {
        int length = hexString.length();
        byte buffer[] = new byte[(length + 1) / 2];
        boolean evenByte = true;
        byte nextByte = 0;
        int bufferOffset = 0;
        if (length % 2 == 1) evenByte = false;
        for (int i = 0; i < length; i++) {
            char c = hexString.charAt(i);
            int nibble;
            if (c >= '0' && c <= '9') nibble = c - 48; else if (c >= 'A' && c <= 'F') nibble = (c - 65) + 10; else if (c >= 'a' && c <= 'f') nibble = (c - 97) + 10; else throw new NumberFormatException((new StringBuilder("Invalid hex digit '")).append(c).append("'.").toString());
            if (evenByte) {
                nextByte = (byte) (nibble << 4);
            } else {
                nextByte += (byte) nibble;
                buffer[bufferOffset++] = nextByte;
            }
            evenByte = !evenByte;
        }
        return buffer;
    }

    private static void appendHexPair(byte b, StringBuffer hexString) {
        char highNibble = kHexChars[(b & 0xf0) >> 4];
        char lowNibble = kHexChars[b & 0xf];
        hexString.append(highNibble);
        hexString.append(lowNibble);
    }

    public static String digest(String in, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(in.getBytes());
        return bufferToHex(digest.digest());
    }

    public static String digestSHA1(String in) throws NoSuchAlgorithmException {
        return digest(in, "SHA1");
    }

    public static String digestMD5(String in) throws NoSuchAlgorithmException {
        return digest(in, "MD5");
    }

    public static void main(String args[]) throws NoSuchAlgorithmException {
        String s1 = digestSHA1("mateuszek").toUpperCase();
        String s2 = "4311fc7e369da7fb95ee7fe3df80443ed0d35892".toUpperCase();
        System.out.println(s1.compareTo(s2));
    }
}
