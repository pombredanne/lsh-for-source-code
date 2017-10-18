package com.jacum.cms.security;

import com.jacum.cms.JacumCmsException;
import java.security.MessageDigest;

public class MD5Util {

    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte anArray : array) {
            sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (Exception e) {
            throw new JacumCmsException("Error calculating hash", e);
        }
    }
}
