package fonction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5 {

    public static String encode(String password) {
        password = "!:�2A7U��&L?(-0@]}],hu6UGHut" + password + "T-urue(-IYT5gjgfe(����_���*mnfvdx";
        byte[] uniqueKey = password.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        StringBuilder hashString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else hashString.append(hex.substring(hex.length() - 2));
        }
        return hashString.toString();
    }

    public static Boolean verify(String hash, String password) {
        return hash.compareTo(Md5.encode(password)) == 0;
    }
}
