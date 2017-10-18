package cn.myapps.util;

import java.security.MessageDigest;

/**
 * The security
 */
public class Security {

    /**
     * Encrypt the string with the MD5 arithmetic
     * @param s Normal message that you want to convert.
     * @return The Encrypt string.
     * @throws Exception
     */
    public static String encodeToMD5(String s) throws Exception {
        if (s == null) return null;
        String digstr = "";
        MessageDigest MD = MessageDigest.getInstance("MD5");
        byte[] oldbyte = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            oldbyte[i] = (byte) s.charAt(i);
        }
        MD.update(oldbyte);
        byte[] newbyte = MD.digest(oldbyte);
        for (int i = 0; i < newbyte.length; i++) {
            digstr = digstr + newbyte[i];
        }
        return digstr;
    }

    public static void main(String[] args) {
        try {
            String pw = encodeToMD5("123");
            System.out.println("PASSWORD-->" + pw);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
