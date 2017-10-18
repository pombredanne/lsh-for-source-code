package org.maze.utils;

import java.security.MessageDigest;

/**
 *
 * @author Thomas
 */
public class Thumbprint {

    public static String digest(String value, String method) throws Exception {
        byte[] b_value = value.getBytes("UTF-8");
        MessageDigest algo = MessageDigest.getInstance(method);
        algo.reset();
        algo.update(b_value);
        byte[] buffer = algo.digest();
        return StringTool.byteArrayToString(buffer);
    }

    public static byte[] digest(byte[] b_value, String method) throws Exception {
        MessageDigest algo = MessageDigest.getInstance(method);
        algo.reset();
        algo.update(b_value);
        byte[] buffer = algo.digest();
        return buffer;
    }

    public static String digestBytesToString(byte[] b_value, String method) throws Exception {
        MessageDigest algo = MessageDigest.getInstance(method);
        algo.reset();
        algo.update(b_value);
        byte[] buffer = algo.digest();
        return StringTool.byteArrayToString(buffer);
    }
}
