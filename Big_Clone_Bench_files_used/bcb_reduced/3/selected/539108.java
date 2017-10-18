package edu.osu.cse.be.services;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import sun.misc.BASE64Encoder;

/**
 * @author Mauktik
 *  
 */
public class PasswordService {

    private static PasswordService instance;

    /**
     *  
     */
    private PasswordService() {
    }

    /**
     * @param plaintext
     * @return
     */
    public synchronized String encrypt(String plaintext) {
        MessageDigest md = null;
        String hash = null;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(plaintext.getBytes("UTF-8"));
            byte raw[] = md.digest();
            hash = (new BASE64Encoder()).encode(raw);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return hash;
    }

    /**
	 * A factory method to get an instance of a PasswordService object. (uses
	 * the java singleton pattern)
	 * 
	 * @return PasswordService object
	 * 
	 * @uml.property name="instance"
	 */
    public static synchronized PasswordService getInstance() {
        if (instance == null) {
            return new PasswordService();
        } else {
            return instance;
        }
    }

    public static String getRandomPassword() {
        StringBuffer result = new StringBuffer();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random r = new Random();
        for (int i = 0; i < 8; i++) {
            result.append(chars.charAt(r.nextInt(62)));
        }
        return result.toString();
    }
}
