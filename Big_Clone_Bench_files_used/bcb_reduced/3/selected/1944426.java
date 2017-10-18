package com.apelon.common.security;

import com.apelon.common.xml.XML;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

/**
 * Provides encryption and management of the user token appended to the front
 * of secure Apelon XML messages (queries).
 *
 * @author Apelon, Inc.
 * @version 1.0
 */
public class ApelEncrypt {

    private static final int USERNAME_LENGTH = 32;

    private static final int PASSWORD_LENGTH = 32;

    private static final int ENC_PASSWORD_LENGTH = 48;

    private static final int MD5_HASH_LENGTH = 16;

    private static final String DIGEST_ALGORITHM = "MD5";

    private static final String ENCRYPTION_HEADER = "enc(";

    /**
     * The padding character mainly used for maintaining the username.  Client
     * applications cannot allow this character into usernames or passwords.
     * Currently using the tilde ( ~ ) character.
     * <p/>
     * Note:  Previously used the Copyright character (©).  This character
     * caused problems when the server and client were not using the
     * same encoding.
     *
     *  If a user name ends with ~ then this code will break, a highly unlikely result --BD
     */
    public static final char PAD_CHARACTER = '~';

    /**
     * Creates an authorization token, which is simply a String consisting
     * of a padded user name and encrypted password.
     * Both the username and password are padded to the maximum length.  If
     * either the user name or the password exceed 32 characters the operation
     * fails.  The password is then encryped with the padding included.
     * The token can then be appended to the front of any xml message to provide
     * authorization security.
     * <p/>
     * <pre>
     * The token in front of the query will be :
     * [username~~~~~~~~~~~~~~~~~~~~~~~~~~~password~~~~~~~~~~~~~~~~~~~~~~~~~~~]
     * |&lt;- USERNAME_LENGTH -&gt;|&lt;- PASSWORD_LENGTH -&gt;|
     * where '~' is the PAD_CHARACTER
     * </pre>
     *
     * @param username the user name to place in the token, cannot exceed 32
     *                 chars.
     * @param password the password to encrypt, cannot exceed 32 chars.
     * @return String the token, which is a String consisting of padded user name and
     *         a padded and encrypted password.
     * @throws Exception the operation fails if either the password or user name
     *                   are too long;  the encryption process may also throw
     *                   the exception.
     */
    public static String createToken(String username, String password) throws Exception {
        StringBuffer token = new StringBuffer(USERNAME_LENGTH + ENC_PASSWORD_LENGTH);
        token.append(padUsername(username));
        token.append(encode(password));
        return token.toString();
    }

    /**
     * This checks the message for the authorization token and places the
     * user name and encrypted password into a String array with two elements.
     * <pre>
     * The token in front of the query will be :
     * [username~~~~~~~~~~~~~~~~~~~~~~~~~~~password~~~~~~~~~~~~~~~~~~~~~~~~~~~]
     * |&lt;- USERNAME_LENGTH -&gt;|&lt;- PASSWORD_LENGTH -&gt;|
     * where '~' is the PAD_CHARACTER
     * </pre>
     *
     * @param query the query string to be checked for authorization info
     *              before being sent to the server
     * @return String[] an array of string with user name as the first element
     *         and password as the second element
     * @throws TokenNotFoundException when the token is not inserted into the
     *                                beginning of the message as expected.
     * @since DTS2.3
     */
    public static String[] extractToken(String query) throws TokenNotFoundException {
        String[] info = new String[2];
        String padded_username = query.substring(0, USERNAME_LENGTH);
        String username = null;
        if (padded_username.indexOf(PAD_CHARACTER) > 0) {
            username = padded_username.substring(0, padded_username.indexOf(PAD_CHARACTER));
        } else {
            if (padded_username.indexOf(XML.XML_DECL) >= 0) {
                throw new TokenNotFoundException("User authorization information was not provided.");
            } else {
                username = padded_username;
            }
        }
        info[0] = username;
        int passStartIndex = USERNAME_LENGTH;
        int passEndIndex = USERNAME_LENGTH + ENC_PASSWORD_LENGTH;
        String padded_password = query.substring(passStartIndex, passEndIndex);
        String password = padded_password.substring(0, ENC_PASSWORD_LENGTH);
        info[1] = password;
        return info;
    }

    /**
     * Removes the leading username and encrypted password from the xml message
     * passed in.  This method does not verify that the authorization string
     * is actually a part of the message, just returns the first portion
     * of the string in the length of user name and password.
     *
     * @param query an xml query string with leading authorization information.
     * @return message less the authorization information of username and
     *         password.
     */
    public static String stripToken(String query) {
        return query.substring(USERNAME_LENGTH + ENC_PASSWORD_LENGTH);
    }

    private static transient String sk = ")*#@(1$7@*&$^@";

    /**
     *  Performs the inverse to the encrypt method
     * @see ApelEncrypt#encrypt(String, String)
     * @param sin
     * @return an array f two strings, the user and password
     * @throws Exception
     */
    public static String[] decrypt(String sin) throws Exception {
        if (hasEncryptHeader(sin)) {
            sin = removeEncryptHeader(sin);
        }
        Cipher crypto = Cipher.getInstance("Blowfish");
        SecretKeySpec key = new SecretKeySpec(sk.getBytes(), "Blowfish");
        crypto.init(Cipher.DECRYPT_MODE, key);
        byte[] out = crypto.doFinal(Base64.decode(sin));
        int user_ln = Integer.parseInt(new String(out, 0, 31), 2);
        String du = new String(out, 31, user_ln);
        String dp = new String(out, 31 + user_ln, (out.length - 31 - user_ln));
        String[] result = new String[2];
        result[0] = du;
        result[1] = dp;
        return result;
    }

    /**
     * This method removes an Apelon custom encryption header from
     * the encrypted string. This header is added by the ApelonEncrypter tool.
     *
     * @param encryptedStr the encrypted string
     *
     * @return encrypted string without the custom header
     * @throws java.lang.Exception
     *
     * @see ENCRYPTION_HEADER
     */
    private static String removeEncryptHeader(String encryptedStr) {
        String encrypted = encryptedStr;
        if (encryptedStr != null && encryptedStr.startsWith(ENCRYPTION_HEADER)) {
            encrypted = encryptedStr.substring(0 + ENCRYPTION_HEADER.length(), encryptedStr.length());
        }
        return encrypted;
    }

    /**
     * Checks if the encrypted string has a custom Apelon encryption header
     * added by the ApelonEncrypter tool.
     *
     * @param encryptedStr
     * @return
     */
    public static boolean hasEncryptHeader(String encryptedStr) {
        boolean encrypted = false;
        if (encryptedStr != null && encryptedStr.startsWith(ENCRYPTION_HEADER)) {
            encrypted = true;
        }
        return encrypted;
    }

    /**
     *
     * @param user
     * @param pass
     * @return a string encrypted and BASE64Encoded so that it can be stored
     * @throws Exception
     */
    public static String encrypt(String user, String pass) throws Exception {
        byte[] ub = user.getBytes();
        byte[] pb = pass.getBytes();
        byte[] input = new byte[31 + ub.length + pb.length];
        int ubl = ub.length;
        byte[] q_ln = (Integer.toBinaryString(ubl)).getBytes();
        int k = q_ln.length;
        for (int i = 30; i >= 0; i--) {
            if (k >= 1) {
                input[i] = q_ln[k - 1];
                k--;
            } else {
                input[i] = 48;
            }
        }
        int offset = 31;
        for (int j = 0; j < ub.length; j++) {
            input[offset + j] = ub[j];
        }
        offset += ub.length;
        for (int l = 0; l < pb.length; l++) {
            input[offset + l] = pb[l];
        }
        Cipher crypto = Cipher.getInstance("Blowfish");
        SecretKeySpec key = new SecretKeySpec(sk.getBytes(), "Blowfish");
        crypto.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encode(crypto.doFinal(input));
    }

    /**
     * This returns a username which is be padded to required length if necessary.
     *
     * @param username
     * @return string customized username
     * @throws Exception
     */
    private static String padUsername(String username) throws Exception {
        String paddedUsername = username;
        char[] old_username = username.toCharArray();
        if (old_username.length < USERNAME_LENGTH) {
            char[] new_username = new char[USERNAME_LENGTH];
            for (int i = 0; i < old_username.length; i++) {
                new_username[i] = old_username[i];
            }
            for (int i = old_username.length; i < new_username.length; i++) {
                new_username[i] = PAD_CHARACTER;
            }
            paddedUsername = new String(new_username);
        } else if (username.length() > USERNAME_LENGTH) {
            throw new Exception("Username [" + username + "] not equal to " + "required length [" + USERNAME_LENGTH + "]");
        }
        return paddedUsername;
    }

    /**
     * Compares two encrypted password strings to determine if they are equal.
     *
     * @param password1 an encrypted password.
     * @param password2 an encrypted password.
     * @return true if the passwords equal; false if otherwise.
     */
    public static boolean isEqual(String password1, String password2) {
        return MessageDigest.isEqual(password1.getBytes(), password2.getBytes());
    }

    /**
     * Encrypts a String by generating a hash using one of the
     * Message Digest algorithms (MD5).
     *
     * @param password a clear text password to encrypt.
     * @return the encrypted password.
     */
    public static String encode(String password) throws Exception {
        return computeHash(password);
    }

    /**
     * This method computes the hash for a given string using one of the
     * Message Digest algorithms (MD5);  if the string is longer than
     * 16 chars, splits the password into two parts since the MD5 algorithm
     * generates hashes for 16 chars.
     *
     * @param password the string to hash, cannot be longer than 32 characters.
     * @return string digested value
     * @throws Exception
     */
    private static String computeHash(String password) throws Exception {
        String hashedPassword = null;
        if (password.length() > MD5_HASH_LENGTH) {
            String password1 = password.substring(0, MD5_HASH_LENGTH);
            String password2 = password.substring(MD5_HASH_LENGTH, password.length());
            hashedPassword = (getMD5Hash(password1) + getMD5Hash(password2));
        } else if (password.length() <= MD5_HASH_LENGTH) {
            hashedPassword = (getMD5Hash(password) + getMD5Hash(""));
        } else {
            hashedPassword = getMD5Hash(password);
        }
        return hashedPassword;
    }

    /**
     * This method computes the hash for a given string using one of the
     * Message Digest algorithms (MD5)
     *
     * @param password
     * @return
     * @throws Exception
     */
    private static String getMD5Hash(String password) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(DIGEST_ALGORITHM);
        String paddedPassword = password;
        char[] old_password = password.toCharArray();
        if (old_password.length < MD5_HASH_LENGTH) {
            char[] new_password = new char[MD5_HASH_LENGTH];
            for (int i = 0; i < old_password.length; i++) new_password[i] = old_password[i];
            for (int i = old_password.length; i < new_password.length; i++) new_password[i] = PAD_CHARACTER;
            paddedPassword = new String(new_password);
        }
        byte[] buffer = new byte[MD5_HASH_LENGTH];
        byte[] valBytes = paddedPassword.getBytes();
        for (int i = 0; i < valBytes.length; i++) buffer[i] = valBytes[i];
        md5.reset();
        md5.update(buffer, 0, MD5_HASH_LENGTH);
        byte[] hash_raw = new byte[MD5_HASH_LENGTH];
        hash_raw = md5.digest();
        return Base64.encode(hash_raw);
    }

    public static void main(String[] args) {
        try {
            ApelEncrypt ae = new ApelEncrypt();
            String pass5 = "12345";
            String pass10 = "1234567890";
            String pass32 = "12345678901234567890123456789012";
            String pass33 = "12345678901234567890123456789012";
            System.out.println(ae.encode(pass5));
            System.out.println(ae.encode(pass5).length());
            System.out.println(ae.encode(pass10));
            System.out.println(ae.encode(pass32));
            System.out.println(ae.encode(pass33));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
