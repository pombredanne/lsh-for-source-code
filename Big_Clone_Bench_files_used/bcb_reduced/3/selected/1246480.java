package icescrum2.service.beans;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CEncrypt {

    public static String getEncodedPassword(String key) throws Exception {
        byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception();
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }

    public static boolean testPassword(String clearTextTestPassword, String encodedActualPassword) {
        String encodedTestPassword;
        try {
            encodedTestPassword = CEncrypt.getEncodedPassword(clearTextTestPassword);
            return (encodedTestPassword.equals(encodedActualPassword));
        } catch (Exception e) {
            return false;
        }
    }
}
