package com.ctrcv.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class Md5Utils {

    public static String md5(String data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] ds = md.digest(StringUtils.getBytesUtf8(data));
            String s = Base64.encodeBase64String(ds);
            return s.replace("\r", "").replace("\n", "");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
