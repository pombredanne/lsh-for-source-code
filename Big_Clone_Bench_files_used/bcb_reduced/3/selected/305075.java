package org.las.tools;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Md5 {

    private static final Log log = LogFactory.getLog(Md5.class);

    public static String getDigest(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] passwordMD5Byte = md.digest(code.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < passwordMD5Byte.length; i++) sb.append(Integer.toHexString(passwordMD5Byte[i] & 0XFF));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        }
    }

    public static String getDigest(byte[] code) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] passwordMD5Byte = md.digest(code);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < passwordMD5Byte.length; i++) sb.append(Integer.toHexString(passwordMD5Byte[i] & 0XFF));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        }
    }

    public static String getDigest(String seed, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(seed.getBytes("UTF-8"));
            byte[] passwordMD5Byte = md.digest(code.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < passwordMD5Byte.length; i++) sb.append(Integer.toHexString(passwordMD5Byte[i] & 0XFF));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        }
    }

    public static String getDigest(String seed, byte[] code) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(seed.getBytes("UTF-8"));
            byte[] passwordMD5Byte = md.digest(code);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < passwordMD5Byte.length; i++) sb.append(Integer.toHexString(passwordMD5Byte[i] & 0XFF));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error(e);
            return null;
        }
    }
}
