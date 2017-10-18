package servlets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author FeDoS
 */
class DoFunctions {

    private static MessageDigest algorithm;

    private static String md5val;

    public DoFunctions() {
    }

    String encrypt(String text) {
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
        }
        byte[] defaultBytes = text.getBytes();
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        md5val = hexString.toString();
        return md5val;
    }
}
