package org.gnomus.util;

import com.google.appengine.repackaged.com.google.common.base.StringUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class encryption {

    public static String sha1(String s) {
        return StringUtil.bytesToHexString(encryption.sha1(strings.toUtf8(s)), null);
    }

    public static byte[] sha1(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(b, 0, b.length);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }
}
