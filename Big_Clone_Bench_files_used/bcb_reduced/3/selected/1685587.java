package tools.security;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import engine.EngineDriver;

public class PasswordHashing {

    /**
	 * Creates a string using the MD5 hashing algorithm from a String object. 
	 * @param toHash String  String to be hashed
	 * @return String Returns hashed String using hex.
	 */
    public static String hashData(String toHash) {
        return hashString(toHash);
    }

    /**
	 * Creates a string using the MD5 hashing algorithm from a String object. 
	 * @param toHash char[]  String to be hashed
	 * @return String Returns hashed String using hex.
	 */
    public static String hashData(char[] toHash) {
        String token = new String(toHash);
        return hashString(token);
    }

    /**
	 * Compares a char[] to a pre-hashed MD5 string.
	 * 
	 * Use {@link #hashData(char[])} or {@link #hashData(String)} to create a
	 * String using the MD5 hashing algorithm.
	 * 
	 * @param toHash char[] Non-hashed char[] (such as from {@link javax.swing.JPasswordField})
	 * @param hashed String Pre-hashed String object.  Use {@link #hashData(char[])} or {@link #hashData(String)}
	 * to create a hashed String
	 * 
	 * @return   true if they match after hash, otherwise false.
	 */
    public static boolean hashMatch(char[] toHash, String hashed) {
        String token = PasswordHashing.hashData(toHash);
        if (token.equals(hashed)) return true; else return false;
    }

    /**
	 * Private code block to created a String object that is hashed
	 * using the MD5 hashing algorithm.  This method is private as to
	 * perform type-checking and conversion before calling this subprogram.
	 * 
	 * @param toHash String String object to hash
	 * 
	 * @return  String Returns a String object that is hashed MD5 into hex.
	 */
    private static String hashString(String toHash) {
        final StringBuilder hashed = new StringBuilder();
        MessageDigest algorithm = null;
        byte[] toHashBytes = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
            toHashBytes = toHash.getBytes("UTF-8");
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(PasswordHashing.class.getPackage().getName()).log(Level.WARNING, "ERROR: UnsupportedEncodingException thrown! String Not Hashed!", e);
        } catch (UnsupportedEncodingException e) {
            Logger.getLogger(PasswordHashing.class.getPackage().getName()).log(Level.WARNING, "ERROR: UnsupportedEncodingException thrown! String Not Hashed!", e);
        }
        algorithm.reset();
        algorithm.update(toHashBytes);
        byte[] digested = algorithm.digest();
        for (byte element : digested) {
            hashed.append(Character.forDigit((element >> 4) & 0xf, 16));
            hashed.append(Character.forDigit(element & 0xf, 16));
        }
        return hashed.toString();
    }
}
