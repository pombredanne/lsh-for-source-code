package net.f.auth;

import java.security.MessageDigest;

/**
 *
 * @author dahgdevash@gmail.com
 */
public class Digest {

    public static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static byte byteValue(char c) {
        byte v = 0;
        switch(c) {
            case 'A':
            case 'a':
                c = 10;
                break;
            case 'B':
            case 'b':
                c = 11;
                break;
            case 'C':
            case 'c':
                c = 12;
                break;
            case 'D':
            case 'd':
                c = 13;
                break;
            case 'E':
            case 'e':
                c = 14;
                break;
            case 'f':
            case 'F':
                c = 15;
                break;
            default:
                v = (byte) (c - '0');
                break;
        }
        return v;
    }

    public static String md5(String value) {
        String ret = "";
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            byte[] bytes = value.getBytes();
            int orig = bytes.length;
            int dest = orig / 16;
            if (bytes.length % 16 != 0) {
                dest += 16;
            }
            byte[] msg = new byte[dest];
            instance.update(bytes, 0, orig);
            int size = instance.digest(msg, 0, dest);
            ret = bytesToHex(msg, size);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    public static String bytesToHex(byte[] raw, int length) {
        char[] hex = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int value = (raw[i] + 256) % 256;
            int highIndex = value >> 4;
            int lowIndex = value & 0x0f;
            hex[i * 2 + 0] = HEX_DIGITS[highIndex];
            hex[i * 2 + 1] = HEX_DIGITS[lowIndex];
        }
        return new String(hex);
    }

    public static byte[] hexToBytes(String value) {
        int length = value.length();
        byte[] bytes = new byte[length / 2 + length % 2];
        for (int i = length - 1; i >= 0; i--) {
            byte v = byteValue(value.charAt(i));
            bytes[i / 2] += (byte) (v << 4 * (i % 2));
        }
        return bytes;
    }

    public static byte[] byteHash(String value) {
        int length = value.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = byteValue(value.charAt(i));
        }
        return bytes;
    }

    public static String toHex(String value) {
        byte[] bytes = value.getBytes();
        return Digest.bytesToHex(bytes, bytes.length);
    }

    public static String fromHex(String value) {
        byte[] bytes = Digest.hexToBytes(value);
        return new String(bytes);
    }
}
