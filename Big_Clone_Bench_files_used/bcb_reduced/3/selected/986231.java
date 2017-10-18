package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
* a way to get the password's MD5
*
* @author fken
*/
public class MD5 {

    String key;

    public MD5(String string) {
        this.key = this.digest(string).toString();
    }

    /**
* a way to get the password's MD5
*
* @author fken
* @return the MD5 of the specified string
* @param key the string you want to encrypt
*/
    public static String digest(String key) {
        byte[] keyArray = key.getBytes();
        byte[] result;
        try {
            result = MessageDigest.getInstance("MD5").digest(keyArray);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("MD5 is not supported");
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < result.length; ++i) {
            String hex = Integer.toHexString(result[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
