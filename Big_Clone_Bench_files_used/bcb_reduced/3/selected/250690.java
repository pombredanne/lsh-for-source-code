package org.shake.lastfm.data;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    private static final String ALG_MD5 = "MD5";

    private static final String ENCODING = "UTF-8";

    public static String getMD5(String text) {
        if (text == null) {
            return null;
        }
        String result = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance(ALG_MD5);
            md5.update(text.getBytes(ENCODING));
            result = "" + new BigInteger(1, md5.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
