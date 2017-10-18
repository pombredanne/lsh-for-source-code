package com.meidusa.amoeba.mysql.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Methods for doing secure authentication with MySQL-4.1 and newer.
 * 
 * @author Mark Matthews
 * 
 * @version $Id: Security.java 3726 2005-05-19 15:52:24Z mmatthews $
 */
public class Security {

    private static final char PVERSION41_CHAR = '*';

    private static final int SHA1_HASH_SIZE = 20;

    /**
	 * Returns hex value for given char
	 */
    private static int charVal(char c) {
        return ((c >= '0') && (c <= '9')) ? (c - '0') : (((c >= 'A') && (c <= 'Z')) ? (c - 'A' + 10) : (c - 'a' + 10));
    }

    /**
	 * Creates key from old password to decode scramble Used in 4.1
	 * authentication with passwords stored pre-4.1 hashing.
	 * 
	 * @param passwd
	 *            the password to create the key from
	 * 
	 * @return 20 byte generated key
	 * 
	 * @throws NoSuchAlgorithmException
	 *             if the message digest 'SHA-1' is not available.
	 */
    static byte[] createKeyFromOldPassword(String passwd) throws NoSuchAlgorithmException {
        passwd = makeScrambledPassword(passwd);
        int[] salt = getSaltFromPassword(passwd);
        return getBinaryPassword(salt, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param salt
	 *            DOCUMENT ME!
	 * @param usingNewPasswords
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws NoSuchAlgorithmException
	 *             if the message digest 'SHA-1' is not available.
	 */
    static byte[] getBinaryPassword(int[] salt, boolean usingNewPasswords) throws NoSuchAlgorithmException {
        int val = 0;
        byte[] binaryPassword = new byte[SHA1_HASH_SIZE];
        if (usingNewPasswords) {
            int pos = 0;
            for (int i = 0; i < 4; i++) {
                val = salt[i];
                for (int t = 3; t >= 0; t--) {
                    binaryPassword[pos++] = (byte) (val & 255);
                    val >>= 8;
                }
            }
            return binaryPassword;
        }
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            val = salt[i];
            for (int t = 3; t >= 0; t--) {
                binaryPassword[t + offset] = (byte) (val % 256);
                val >>= 8;
            }
            offset += 4;
        }
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(binaryPassword, 0, 8);
        return md.digest();
    }

    private static int[] getSaltFromPassword(String password) {
        int[] result = new int[6];
        if ((password == null) || (password.length() == 0)) {
            return result;
        }
        if (password.charAt(0) == PVERSION41_CHAR) {
            String saltInHex = password.substring(1, 5);
            int val = 0;
            for (int i = 0; i < 4; i++) {
                val = (val << 4) + charVal(saltInHex.charAt(i));
            }
            return result;
        }
        int resultPos = 0;
        int pos = 0;
        int length = password.length();
        while (pos < length) {
            int val = 0;
            for (int i = 0; i < 8; i++) {
                val = (val << 4) + charVal(password.charAt(pos++));
            }
            result[resultPos++] = val;
        }
        return result;
    }

    private static String longToHex(long val) {
        String longHex = Long.toHexString(val);
        int length = longHex.length();
        if (length < 8) {
            int padding = 8 - length;
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < padding; i++) {
                buf.append("0");
            }
            buf.append(longHex);
            return buf.toString();
        }
        return longHex.substring(0, 8);
    }

    /**
	 * Creates password to be stored in user database from raw string.
	 * 
	 * Handles Pre-MySQL 4.1 passwords.
	 * 
	 * @param password
	 *            plaintext password
	 * 
	 * @return scrambled password
	 * 
	 * @throws NoSuchAlgorithmException
	 *             if the message digest 'SHA-1' is not available.
	 */
    static String makeScrambledPassword(String password) throws NoSuchAlgorithmException {
        long[] passwordHash = Util.newHash(password);
        StringBuffer scramble = new StringBuffer();
        scramble.append(longToHex(passwordHash[0]));
        scramble.append(longToHex(passwordHash[1]));
        return scramble.toString();
    }

    /**
	 * Encrypt/Decrypt function used for password encryption in authentication
	 * 
	 * Simple XOR is used here but it is OK as we crypt random strings
	 * 
	 * @param from
	 *            IN Data for encryption
	 * @param to
	 *            OUT Encrypt data to the buffer (may be the same)
	 * @param password
	 *            IN Password used for encryption (same length)
	 * @param length
	 *            IN Length of data to encrypt
	 */
    static void passwordCrypt(byte[] from, byte[] to, byte[] password, int length) {
        int pos = 0;
        while ((pos < from.length) && (pos < length)) {
            to[pos] = (byte) (from[pos] ^ password[pos]);
            pos++;
        }
    }

    /**
	 * Stage one password hashing, used in MySQL 4.1 password handling
	 * 
	 * @param password
	 *            plaintext password
	 * 
	 * @return stage one hash of password
	 * 
	 * @throws NoSuchAlgorithmException
	 *             if the message digest 'SHA-1' is not available.
	 */
    static byte[] passwordHashStage1(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        StringBuffer cleansedPassword = new StringBuffer();
        int passwordLength = password.length();
        for (int i = 0; i < passwordLength; i++) {
            char c = password.charAt(i);
            if ((c == ' ') || (c == '\t')) {
                continue;
            }
            cleansedPassword.append(c);
        }
        return md.digest(cleansedPassword.toString().getBytes());
    }

    /**
	 * Stage two password hashing used in MySQL 4.1 password handling
	 * 
	 * @param hash
	 *            from passwordHashStage1
	 * @param salt
	 *            salt used for stage two hashing
	 * 
	 * @return result of stage two password hash
	 * 
	 * @throws NoSuchAlgorithmException
	 *             if the message digest 'SHA-1' is not available.
	 */
    static byte[] passwordHashStage2(byte[] hashedPassword, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(salt, 0, 4);
        md.update(hashedPassword, 0, SHA1_HASH_SIZE);
        return md.digest();
    }

    public static byte[] scramble411(String password, String seed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] passwordHashStage1 = md.digest(password.getBytes());
        md.reset();
        byte[] passwordHashStage2 = md.digest(passwordHashStage1);
        md.reset();
        byte[] seedAsBytes = seed.getBytes();
        md.update(seedAsBytes);
        md.update(passwordHashStage2);
        byte[] toBeXord = md.digest();
        int numToXor = toBeXord.length;
        for (int i = 0; i < numToXor; i++) {
            toBeXord[i] = (byte) (toBeXord[i] ^ passwordHashStage1[i]);
        }
        return toBeXord;
    }

    /**
	 * Prevent construction.
	 */
    private Security() {
        super();
    }
}
