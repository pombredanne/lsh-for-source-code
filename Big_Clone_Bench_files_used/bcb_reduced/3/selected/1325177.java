package codebush.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author Fution Bai
 * @since 1.0
 */
public class ContentUtil {

    public static String[] strWithCommasToStrArray(String s) {
        String s1 = s.replaceAll("，", ",");
        String[] result = s1.split(",");
        return result;
    }

    public static String MD5(String s) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        char[] charArray = s.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    /**
	 * used to encrypt the plain password
	 * @param plainPassword
	 * @return
	 */
    public static String passwordEncryption(String plainPassword) {
        return MD5(plainPassword + "fuck all beauties");
    }
}
