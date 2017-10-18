package net.sf.qagesa.multimedia;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author Giovanni Novelli
 */
public class MD5Checksum {

    private static byte[] createChecksum(String str) throws NoSuchAlgorithmException {
        MessageDigest complete = MessageDigest.getInstance("MD5");
        complete.reset();
        return complete.digest(str.getBytes());
    }

    public static String get(String str) {
        String md5 = "";
        try {
            byte[] messageDigest = MD5Checksum.createChecksum(str);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
                md5 = hexString.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }
}
