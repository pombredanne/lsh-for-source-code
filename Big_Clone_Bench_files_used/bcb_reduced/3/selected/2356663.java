package org.light.portal.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.sf.acegisecurity.providers.encoding.Md5PasswordEncoder;

/**
 * 
 * @author Jianmin Liu
 **/
public class HashUtil {

    public static String SHAHashing(String input) {
        String output = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
            byte[] original = input.getBytes("UTF-8");
            byte[] bytes = md.digest(original);
            for (int i = 0; i < bytes.length; i++) {
                output += Integer.toHexString((bytes[i] & 0xff) + 0x100).substring(1);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static String MD5Hashing(String input) {
        Md5PasswordEncoder encoder = new Md5PasswordEncoder();
        return encoder.encodePassword(input, null);
    }
}
