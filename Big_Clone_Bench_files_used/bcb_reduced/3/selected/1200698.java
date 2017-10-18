package org.aigebi.rbac.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**Simple util to hash passwod using MD5.
 * 
 * @author Ligong Xu
 * @version $Id: MD5PasswordHasher.java 1 2007-09-22 18:10:03Z ligongx $
 */
public class MD5PasswordHasher implements PasswordHasher {

    public MD5PasswordHasher() {
    }

    public String hashPassword(String pPassword) {
        return encodeDigestHash(md5Digest(pPassword));
    }

    /**
	 * Returns hash bytes in the hexadecimal.
	 */
    private String encodeDigestHash(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("byte array must not be null");
        }
        StringBuffer hex = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            hex.append(Character.forDigit((bytes[i] & 0XF0) >> 4, 16));
            hex.append(Character.forDigit((bytes[i] & 0X0F), 16));
        }
        return hex.toString();
    }

    private byte[] md5Digest(String pPassword) {
        if (pPassword == null) {
            throw new NullPointerException("input null text for hashing");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pPassword.getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot find MD5 algorithm");
        }
    }

    /** used for command lin */
    public static void main(String[] args) {
        MD5PasswordHasher md5 = new MD5PasswordHasher();
        if (args == null || args.length == 0) {
            System.out.println("Usage: MD5PasswordHasher mytext");
        }
        for (String elem : args) {
            String pw = elem;
            String hashed = md5.hashPassword(pw);
            System.out.println("Clear text=[" + pw + "] has hashed text=[" + hashed + "]");
        }
    }
}
