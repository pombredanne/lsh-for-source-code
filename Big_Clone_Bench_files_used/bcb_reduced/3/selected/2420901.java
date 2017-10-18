package org.lightcommons.encoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 声明：此类引用于<a href="http://www.commontemplate.org">commontemplate</a>
 * @author 梁飞
 */
public class SHA {

    private SHA() {
    }

    public static String encode(byte[] src) {
        if (src == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            return new String(HEX.encode(md.digest(src)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encode(String src) {
        if (src == null) return null;
        return encode(src.getBytes());
    }
}
