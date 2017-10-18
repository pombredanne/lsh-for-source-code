package utils;

import java.io.UnsupportedEncodingException;
import java.security.*;

public class Hash {

    public static String getMD5(String clear) {
        return getHash("MD5", clear);
    }

    public static String getSHA1(String clear) {
        return getHash("SHA1", clear);
    }

    private static String getHash(String hash, String clear) {
        try {
            MessageDigest md = MessageDigest.getInstance(hash);
            md.update(clear.getBytes("UTF-8"));
            byte[] bytes = md.digest();
            String str = new String();
            for (int i = 0; i < bytes.length; ++i) str += Integer.toHexString(0xF0 & bytes[i]).charAt(0) + Integer.toHexString(0x0F & bytes[i]);
            return str;
        } catch (NoSuchAlgorithmException exc) {
        } catch (UnsupportedEncodingException exc) {
        }
        return "";
    }
}
