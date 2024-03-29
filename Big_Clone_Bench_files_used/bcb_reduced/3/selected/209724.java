package dtec.wssfh.util;

import java.util.*;
import java.io.*;
import java.security.*;

public class MD5Util {

    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(getGravatarImageUrl("bluishoul@gmail.com"));
    }

    public static String getGravatarImageUrl(String email) {
        return "http://www.gravatar.com/avatar/" + MD5Util.md5Hex(email);
    }
}
