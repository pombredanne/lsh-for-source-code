package org.framework.crypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5 {

    /**
	 * <b>Description : </b>Encode la cha�ne pass�e en param�tre avec
	 * l�algorithme MD5 </br>
	 * 
	 * @param key
	 *            la cha�ne � encoder
	 * @return la valeur (string) hexad�cimale sur 32 bits
	 */
    public static String encode(String key) {
        byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
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
}
