package jwebtk.password;

import java.security.MessageDigest;

/**
 * This class provides a simple one-way encrypted password (converted to printable characters) from a plain text string.
 */
public class Password {

    /**
     * This method returns a simple one-way encrypted password (converted to printable characters) from a plain text string.
     *
     * @param password a plain text password to be encrypted
     *
     * @return a printable plain text one-way encrypted password
     */
    public static String getEncryptedPassword(String password) throws PasswordException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new PasswordException(e);
        }
        return convertToString(md.digest());
    }

    static String convertToString(byte[] byteArray) {
        String encoding = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", str = "";
        for (int i = 0; i < byteArray.length; i++) {
            int index = Math.abs(byteArray[i] % (encoding.length() - 1));
            str += encoding.charAt(index);
        }
        return str;
    }
}
