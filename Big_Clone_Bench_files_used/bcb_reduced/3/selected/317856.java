package com.once.server.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

public class HashUtils {

    private static final Logger m_logger = Logger.getLogger(HashUtils.class);

    public static String getHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] < 0 ? bytes[i] + 256 : bytes[i];
            if (b < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    public static byte[] getBytes(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i = i + 2) {
            String hex = hexString.substring(i, i + 2);
            int b = Integer.parseInt(hex, 16);
            bytes[i / 2] = b > 128 ? (byte) (b - 256) : (byte) b;
        }
        return bytes;
    }

    public static String md5(byte[] source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(source);
            return getHexString(bytes);
        } catch (Throwable t) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Unable to calculate MD5", t);
            }
            return null;
        }
    }

    public static String md5(String source) {
        try {
            return md5(source.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Unable to calculate MD5", ex);
            }
            return null;
        }
    }

    public static String md5(File source) {
        try {
            return md5(getBytes(source));
        } catch (Throwable t) {
            m_logger.debug("Unable to calculate MD5", t);
            return null;
        }
    }

    public static String sha(byte[] source) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] bytes = md.digest(source);
            return getHexString(bytes);
        } catch (Throwable t) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Unable to calculate SHA", t);
            }
            return null;
        }
    }

    public static String sha(String source) {
        try {
            return sha(source.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            m_logger.debug("Unable to calculate SHA", ex);
            return null;
        }
    }

    public static String sha(File source) {
        try {
            return sha(getBytes(source));
        } catch (Throwable t) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Unable to calculate SHA", t);
            }
            return null;
        }
    }

    private static byte[] getBytes(File source) throws IOException {
        FileInputStream fi = new FileInputStream(source);
        byte[] bytes = new byte[fi.available()];
        fi.read(bytes);
        fi.close();
        return bytes;
    }
}
